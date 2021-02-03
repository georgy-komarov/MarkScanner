package ml.komarov.markscanner

import androidx.fragment.app.Fragment
import ml.komarov.markscanner.fragments.MainFragment


class MainActivity : SingleFragmentActivity() {
    override fun getFragment(): Fragment {
        return MainFragment.newInstance()
    }
}