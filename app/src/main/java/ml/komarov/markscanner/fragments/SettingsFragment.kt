package ml.komarov.markscanner.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ml.komarov.markscanner.R
import ml.komarov.markscanner.databinding.FragmentSettingsBinding

enum class APIType {
    MOBILE_API,
    TRUE_API,
}

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private lateinit var sharedPreferences: SharedPreferences

    private val binding get() = _binding!!

    companion object {
        fun newInstance(): SettingsFragment {
            val args = Bundle()

            val fragment = SettingsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.app_settings),
            Context.MODE_PRIVATE
        )

        setup()

        return binding.root
    }

    private fun setTitle() {
        requireActivity().title = getString(R.string.settings)
    }

    private fun setup() {
        setTitle()
        loadSettings()

        binding.rbMobileApi.setOnClickListener { binding.layoutTokenSettings.visibility = INVISIBLE }
        binding.rbTrueApi.setOnClickListener { binding.layoutTokenSettings.visibility = VISIBLE }

        binding.buttonTokenRefresh.setOnClickListener {
            updateToken()
        }
        binding.buttonSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun updateToken() {
        Toast.makeText(requireContext(), "Получение токена...", Toast.LENGTH_SHORT).show()
    }

    private fun loadSettings() {
        val apiType = sharedPreferences.getString("apiType", APIType.MOBILE_API.name)
        if (apiType == APIType.MOBILE_API.name) {
            binding.rbMobileApi.isChecked = true
            binding.layoutTokenSettings.visibility = INVISIBLE
        } else {
            binding.rbTrueApi.isChecked = true
        }

        binding.checkboxAuthNeeded.isChecked = sharedPreferences.getBoolean("tokenAuthNeeded", false)
        binding.editTextTokenUrl.setText(
            sharedPreferences.getString("tokenUrl", "")
        )
        binding.editTextUsername.setText(
            sharedPreferences.getString("tokenAuthUsername", "")
        )
        binding.editTextPassword.setText(
            sharedPreferences.getString("tokenAuthPassword", "")
        )
    }

    private fun saveSettings() {
        val apiType = if (binding.rbTrueApi.isChecked) APIType.TRUE_API else APIType.MOBILE_API
        sharedPreferences.edit()
            .putString("apiType", apiType.name)
            .putBoolean("tokenAuthNeeded", binding.checkboxAuthNeeded.isChecked)
            .putString("tokenUrl", binding.editTextTokenUrl.text.toString())
            .putString("tokenAuthUsername", binding.editTextUsername.text.toString())
            .putString("tokenAuthPassword", binding.editTextPassword.text.toString())
            .apply()
    }
}