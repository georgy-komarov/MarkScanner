package ml.komarov.markscanner.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import ml.komarov.markscanner.App
import ml.komarov.markscanner.BarcodeActivity
import ml.komarov.markscanner.R
import ml.komarov.markscanner.db.AppDatabase
import ml.komarov.markscanner.db.History
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class MainFragment : Fragment() {
    companion object {
        const val REQUEST_CODE = 101

        fun newInstance(): MainFragment {
            val args = Bundle()

            val fragment = MainFragment()
            fragment.arguments = args
            return fragment
        }
    }

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
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        activity!!.title = getString(R.string.app_name)

        view.btnScan.setOnClickListener {
            startActivityForResult(
                Intent(activity, BarcodeActivity::class.java),
                REQUEST_CODE
            )
        }
        view.btnCheck.setOnClickListener {
            openCodeData(editTextMark.text.toString())
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Toast.makeText(activity, item.title, Toast.LENGTH_SHORT).show()

        when (item.itemId) {
            R.id.menuScanHistory -> fragmentManager!!.beginTransaction()
                .replace(R.id.fragmentContainer, HistoryFragment.newInstance())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(HistoryFragment::class.qualifiedName)
                .commit()

            R.id.menuExit -> activity!!.finishAndRemoveTask()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val code = data!!.getStringExtra("DATA")!!
            openCodeData(code)
        }
    }

    private fun openCodeData(code: String) {
        val args = Bundle()
        args.putString("code", code)

        val newResultFragment = ResultFragment.newInstance()
        newResultFragment.arguments = args

        activity!!.supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, newResultFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack(ResultFragment::class.qualifiedName)
            .commit()
    }
}