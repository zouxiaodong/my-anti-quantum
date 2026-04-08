package com.quantum.poc.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.quantum.poc.databinding.ActivityMainBinding
import com.quantum.poc.viewmodel.CryptoUiState
import com.quantum.poc.viewmodel.CryptoViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CryptoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etPlainText.setText("12345678")

        setupObservers()
        setupButtons()
    }

    private fun setupObservers() {
        viewModel.sessionData.observe(this) { session ->
            binding.etPlainText.setText(session.plainText)
            binding.tvLog.text = session.logMessages.joinToString("\n")
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CryptoUiState.Idle -> {
                    binding.tvStatus.text = "Ready"
                    binding.btnGenerateKeys.isEnabled = false
                    binding.btnEncryptUpload.isEnabled = false
                }
                is CryptoUiState.SessionCreated -> {
                    binding.tvStatus.text = "Session Created"
                    binding.btnGenerateKeys.isEnabled = true
                    binding.btnEncryptUpload.isEnabled = false
                }
                is CryptoUiState.KeysGenerated -> {
                    binding.tvStatus.text = "Keys Generated"
                    binding.btnEncryptUpload.isEnabled = true
                }
                is CryptoUiState.Encrypted -> {
                    binding.tvStatus.text = "Encrypted Locally"
                }
                is CryptoUiState.Uploaded -> {
                    binding.tvStatus.text = "Uploaded Successfully"
                }
                is CryptoUiState.Error -> {
                    binding.tvStatus.text = "Error: ${state.message}"
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnNewSession.setOnClickListener { viewModel.newSession() }
        binding.btnGenerateKeys.setOnClickListener { viewModel.generateKeys() }
        binding.btnEncryptUpload.setOnClickListener {
            val text = binding.etPlainText.text.toString()
            viewModel.setPlainText(text)
            viewModel.encryptAndUpload()
        }
        binding.btnReset.setOnClickListener { viewModel.reset() }
    }
}
