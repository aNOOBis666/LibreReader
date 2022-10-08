package com.denis.librereader.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.denis.librereader.MainActivity
import com.denis.librereader.R
import com.denis.librereader.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    private val viewModel: MainViewModel by viewModels()
    private val viewBinding by viewBinding(FragmentMainBinding::bind)

    companion object {
        private const val GLUCOSE_KEY = "glucose_key"
        private const val GLUCOSE_SECOND_KEY = "glucose_second_key"

        fun saveGlucose(currentGlucose: String, currentLectura: String): Bundle {
            val bundle = Bundle()
            bundle.putString(GLUCOSE_KEY, currentGlucose)
            bundle.putString(GLUCOSE_SECOND_KEY, currentLectura)
            return bundle
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = savedInstanceState ?: arguments
        val currentGlucose = args?.getString(GLUCOSE_KEY)
        val currentSecondGlucose = args?.getString(GLUCOSE_SECOND_KEY)
        viewBinding.message.text = currentGlucose.toString()
        viewBinding.message2.text = currentSecondGlucose.toString()
    }


}