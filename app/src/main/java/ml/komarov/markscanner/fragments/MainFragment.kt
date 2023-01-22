package ml.komarov.markscanner.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import ml.komarov.markscanner.BarcodeActivity
import ml.komarov.markscanner.R


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

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        requireActivity().title = getString(R.string.app_name)

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
        when (item.itemId) {
            R.id.menuScanHistory -> requireFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, HistoryFragment.newInstance())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(HistoryFragment::class.qualifiedName)
                .commit()

            R.id.menuSettings -> requireFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, SettingsFragment.newInstance())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(SettingsFragment::class.qualifiedName)
                .commit()
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

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, newResultFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack(ResultFragment::class.qualifiedName)
            .commit()
    }
}