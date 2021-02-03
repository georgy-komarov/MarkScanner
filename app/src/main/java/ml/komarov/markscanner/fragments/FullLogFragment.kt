package ml.komarov.markscanner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yuyh.jsonviewer.library.JsonRecyclerView
import kotlinx.android.synthetic.main.fragment_full_log.view.*
import ml.komarov.markscanner.R


class FullLogFragment : Fragment() {
    companion object {
        fun newInstance(): FullLogFragment {
            val args = Bundle()

            val fragment = FullLogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_full_log, container, false)

        activity!!.title = getString(R.string.fullResult)

        view.rvJson.bindJson(arguments!!.getString("json", "{}"))

        return view
    }
}