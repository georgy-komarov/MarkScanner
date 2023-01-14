package ml.komarov.markscanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.graphics.ImageFormat.NV21
import android.media.Image
import android.os.Bundle
import android.renderscript.*
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import ml.komarov.markscanner.databinding.ActivityBacodeBinding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@ExperimentalGetImage
class BarcodeActivity : AppCompatActivity() {
    private var _binding: ActivityBacodeBinding? = null
    private val binding: ActivityBacodeBinding get() = _binding!!

    private val barcodeOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()

    private val barcodeScanner = BarcodeScanning.getClient(barcodeOptions)

    private val barcodeExecutor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    private fun invert(original: Bitmap): Bitmap? {
        val rgbMask = 0x00FFFFFF

        // Create mutable Bitmap to invert, argument true makes it mutable
        val inversion = original.copy(Bitmap.Config.ARGB_8888, true)

        // Get info about Bitmap
        val width = inversion.width
        val height = inversion.height
        val pixels = width * height

        // Get original pixels
        val pixel = IntArray(pixels)
        inversion.getPixels(pixel, 0, width, 0, 0, width, height)

        // Modify pixels
        for (i in 0 until pixels) pixel[i] = pixel[i] xor rgbMask
        inversion.setPixels(pixel, 0, width, 0, 0, width, height)

        // Return inverted Bitmap
        return inversion
    }

    private fun JPEGToBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int, quality: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), quality, out)
        return out.toByteArray()
    }

    private fun YUV420toNV21(image: Image): ByteArray? {
        val crop: Rect = image.cropRect
        val format: Int = image.format
        val width = crop.width()
        val height = crop.height()
        val planes: Array<Image.Plane> = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer: ByteBuffer = planes[i].buffer
            val rowStride: Int = planes[i].rowStride
            val pixelStride: Int = planes[i].pixelStride
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private val barcodeAnalyser: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { img ->
        val mediaImage = img.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, img.imageInfo.rotationDegrees)
            val reversedBitmap = YUV420toNV21(mediaImage)?.let { it ->
                NV21toJPEG(
                    it, img.width, img.height, quality = 75
                )?.let { invert(JPEGToBitmap(it)) }
            }
            if (reversedBitmap != null) {
                val reversedImage = InputImage.fromBitmap(
                    reversedBitmap,
                    img.imageInfo.rotationDegrees
                )
                // Original image
                Tasks.await(barcodeScanner.process(reversedImage))
                    .forEach(this@BarcodeActivity::returnBarcodeResult)
            }
            // Negative image
            Tasks.await(barcodeScanner.process(inputImage))
                .forEach(this@BarcodeActivity::returnBarcodeResult)
        }
        img.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityBacodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.v(TAG, "Number of cores ${Runtime.getRuntime().availableProcessors()}")
        askPermission(*REQUIRED_PERMISSION) {
            startCameraPreview()
        }.onDeclined {
            // Handle denied permissions here
        }
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@BarcodeActivity)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                // Camera Preview Setup
                val cameraPreview = Preview.Builder()
                    .build()
                    .also { previewBuilder ->
                        previewBuilder.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                    }

                // Preview frame analysis
                val imageAnalysis = ImageAnalysis.Builder()
                    .setImageQueueDepth(1)
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(barcodeExecutor, barcodeAnalyser)

                // Hook every thing in camera preview
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@BarcodeActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageAnalysis,
                    cameraPreview
                )
            },
            ContextCompat.getMainExecutor(this@BarcodeActivity)
        )
    }

    override fun onDestroy() {
        if (!barcodeExecutor.isShutdown) {
            barcodeExecutor.shutdown()
        }
        super.onDestroy()
    }

    private fun returnBarcodeResult(barcodeResult: Barcode) {
        val intent = Intent()
        intent.putExtra("DATA", barcodeResult.displayValue)
        setResult(RESULT_OK, intent)
        finish() // finishing activity
    }

    companion object {
        const val TAG = "BarcodeActivity"
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }
}