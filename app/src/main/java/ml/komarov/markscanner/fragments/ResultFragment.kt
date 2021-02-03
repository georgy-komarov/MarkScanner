package ml.komarov.markscanner.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.fragment_result.view.*
import ml.komarov.markscanner.App
import ml.komarov.markscanner.R
import ml.komarov.markscanner.db.AppDatabase
import ml.komarov.markscanner.db.History
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ResultFragment : Fragment() {
    companion object {
        lateinit var waitDialog: AlertDialog

        private val client = OkHttpClient()

        const val fnc1char = (232).toChar()
        const val gs1char = (29).toChar()

        fun newInstance(): ResultFragment {
            val args = Bundle()

            val fragment = ResultFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var root: View? = null
    private var menuRoot: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        root = inflater.inflate(R.layout.fragment_result, container, false)

        activity!!.title = getString(R.string.result)
        waitDialog = createWaitDialog()

        val args = arguments!!
        val code = args.getString("code")
        val id = args.getLong("id", 0)

        if (code != null && id == (0).toLong()) {
            getResult(code)
        } else if (id > 0) {
            fillData(id)
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menuRoot = menu
        inflater.inflate(R.menu.result_menu, menu)

        updateFullLogVisibility()

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuFullResult -> {
                val db: AppDatabase = App.instance!!.getDatabase()!!
                val historyDao = db.historyDao()!!

                val codeDataFromHistory = historyDao.getHistory(arguments!!.getLong("id"))?.data

                if (codeDataFromHistory != null) {
                    val args = Bundle()
                    args.putString("json", codeDataFromHistory)

                    val newFullLogFragment = FullLogFragment.newInstance()
                    newFullLogFragment.arguments = args

                    activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, newFullLogFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .addToBackStack(FullLogFragment::class.qualifiedName)
                        .commit()
                } else {
                    Toast.makeText(context, "Упс... Что-то пошло не так...", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            R.id.menuRefreshResult -> {
                getResult(arguments!!.getString("code")!!)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createWaitDialog(): AlertDialog {
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Делаем запрос к ЧЗ...")
        builder.setMessage("Пожалуйста, подождите.")
        builder.setCancelable(false)
        return builder.create()
    }

    private fun getResult(code: String) {
        val parsedCode = parseCode(code)

        if (parsedCode != null) {
            getCodeData(parsedCode)
        } else {
            Toast.makeText(context, "Некорректный код: $code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseCode(code: String): String? {
        if (code.startsWith("(01)") && code.contains("(21)")) {  // human-readable
            val pattern =
                "^[\\x1d\\xe8]?\\((?<gtinPrefix>01)\\)(?<gtin01>\\d{14})[\\x1d\\xe8]?\\((?<serialPrefix>21)\\)(?<serial21>.{6,27})(\\(\\d\\d\\))?.*\$"
            val regex = Regex(pattern)
            val codeFormatHumanReadable = regex.find(code)

            if (codeFormatHumanReadable != null) {
                val codeGroups = (codeFormatHumanReadable.groups)

                val gtin01prefix = codeGroups[1]?.value
                val gtin01 = codeGroups[2]?.value
                val serial21prefix = codeGroups[3]?.value
                val serial21 = codeGroups[4]?.value

                return gtin01prefix + gtin01 + serial21prefix + serial21
            } else {
                return null
            }
        }
        return code
    }

    private fun getCodeData(code: String) {
        val codeReadable = code.replace(fnc1char.toString(), "").replace(gs1char.toString(), "")
        val trimmedCode = code.trimStart(fnc1char, gs1char)

        val jsonObject = JSONObject()
        jsonObject.put("code", trimmedCode)

        val jsonMediaType: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body: RequestBody = jsonObject.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("https://mobile.api.crpt.ru/mobile/check")
            .post(body)
            .build()

        waitDialog.show()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    waitDialog.hide()
                    Toast.makeText(
                        context,
                        "Не удалось соединиться с ЧЗ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity!!.runOnUiThread { waitDialog.hide() }
                response.use {
                    if (!response.isSuccessful) {
                        activity!!.runOnUiThread {
                            waitDialog.hide()
                            Toast.makeText(
                                context,
                                "Не удалось получить ответ от ЧЗ (код ${response.code})",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val codeData = response.body!!.string()
                        val json = JSONObject(codeData)
                        val codeOK = json.getBoolean("codeFounded")
                        if (codeOK) {
                            val oldId = arguments!!.getLong("id", 0)
                            val dataId: Long

                            if (oldId > 0) {
                                dataId = insertCode(codeReadable, codeData, oldId)
                            } else {
                                dataId = insertCode(codeReadable, codeData)
                            }

                            // Fix return back from fullLog makes refresh
                            val args = Bundle()
                            args.putString("code", codeReadable)
                            args.putLong("id", dataId)
                            arguments = args

                            fillData(dataId)
                        } else {
                            activity!!.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "$codeReadable - код не найден!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun insertCode(code: String, data: String, id: Long = 0): Long {
        val db: AppDatabase = App.instance!!.getDatabase()!!
        val historyDao = db.historyDao()!!

        val historyRow: History = if (id > 0) {
            History(id = id, code = code, data = data)
        } else {
            History(code = code, data = data)
        }
        return historyDao.insertHistory(historyRow)
    }

    private fun fillData(id: Long) {
        val db: AppDatabase = App.instance!!.getDatabase()!!
        val historyDao = db.historyDao()!!

        val codeFromHistory = historyDao.getHistory(id)

        if (codeFromHistory == null) {
            Toast.makeText(context, "Запись не найдена в базе данных!", Toast.LENGTH_SHORT).show()
        } else {
            val codeReadable = codeFromHistory.code
            val codeData = codeFromHistory.data

            if (codeData != null) {
                var dateReadable = "-"
                var productName = "-"
                var category = "-"
                var producerName = "-"
                var ownerName = "-"
                var ownerInn = "-"
                var status = "-"

                try {
                    val json = JSONObject(codeData)

                    try {
                        val date = Date(json.getLong("checkDate"))
                        val format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                        dateReadable = format.format(date)
                    } catch (ignored: Exception) {
                    }

                    try {
                        productName = json.getString("productName")
                    } catch (ignored: Exception) {
                    }

                    try {
                        category = json.getString("category")
                    } catch (ignored: Exception) {
                    }

                    val categoryKey = category + "Data"
                    var categoryData: JSONObject? = null
                    try {
                        categoryData = json.getJSONObject(categoryKey)
                    } catch (ignored: Exception) {
                        Toast.makeText(
                            context,
                            "Ошибка парсинга категориальных признаков",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // For tobacco?
                    try {
                        producerName = json.getString("producerName")
                    } catch (ignored: Exception) {
                    }
                    try {
                        ownerName = json.getString("ownerName")
                    } catch (ignored: Exception) {
                    }
                    try {
                        ownerInn = json.getString("ownerInn")
                    } catch (ignored: Exception) {
                    }
                    try {
                        status = json.getString("status")
                    } catch (ignored: Exception) {
                    }

                    // For other groups
                    if (categoryData != null) {
                        try {
                            producerName = categoryData.getString("producerName")
                        } catch (ignored: Exception) {
                        }
                        try {
                            ownerName = categoryData.getString("ownerName")
                        } catch (ignored: Exception) {
                        }
                        try {
                            ownerInn = categoryData.getString("ownerInn")
                        } catch (ignored: Exception) {
                        }
                        try {
                            status = categoryData.getString("status")
                        } catch (ignored: Exception) {
                        }
                    }
                } catch (ignored: Exception) {
                    Toast.makeText(context, "Ошибка парсинга JSON", Toast.LENGTH_SHORT).show()
                }

                activity!!.runOnUiThread {
                    root?.tvCode?.text = codeReadable
                    root?.tvDate?.text = dateReadable
                    root?.tvProductName?.text = productName
                    root?.tvCategory?.text = category
                    root?.tvProducerName?.text = producerName
                    root?.tvOwnerName?.text = ownerName
                    root?.tvOwnerInn?.text = ownerInn
                    root?.tvStatus?.text = status

                    updateFullLogVisibility()
                }
            }
        }
    }

    private fun updateFullLogVisibility() {
        val fullLogMenuItem: MenuItem? = menuRoot?.findItem(R.id.menuFullResult)
        if (fullLogMenuItem != null) {
            fullLogMenuItem.isVisible = arguments!!.getLong("id") > 0
        }
    }
}