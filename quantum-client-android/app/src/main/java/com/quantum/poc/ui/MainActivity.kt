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
        
        setupObservers()
        setupButtons()
    }
    
    private fun setupObservers() {
        viewModel.plainText.observe(this) { binding.etPlainText.setText(it) }
        viewModel.cipherText.observe(this) { binding.etCipherText.setText(it) }
        viewModel.publicKey.observe(this) { binding.etPublicKey.setText(it) }
        viewModel.privateKey.observe(this) { binding.etPrivateKey.setText(it) }
        viewModel.signature.observe(this) { binding.etSignature.setText(it) }
        viewModel.verifyResult.observe(this) { binding.etVerifyResult.setText(it) }
        viewModel.logMessage.observe(this) { 
            binding.tvLog.text = it + "\n" + binding.tvLog.text
        }
        
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CryptoUiState.Loading -> {
                    binding.tvLog.text = "处理中..." + "\n" + binding.tvLog.text
                }
                is CryptoUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
    
    private fun setupButtons() {
        binding.btnInit.setOnClickListener { viewModel.reset() }
        binding.btnGenRandom.setOnClickListener { viewModel.genRandom() }
        
        binding.btnKyber512.setOnClickListener { viewModel.genPqcKeyPair("kyber512") }
        binding.btnKyber768.setOnClickListener { viewModel.genPqcKeyPair("kyber768") }
        binding.btnKyber1024.setOnClickListener { viewModel.genPqcKeyPair("kyber1024") }
        
        binding.btnKyber512Sm4.setOnClickListener { viewModel.genPqcKeyPair("kyber512") }
        binding.btnKyber768Sm4.setOnClickListener { viewModel.genPqcKeyPair("kyber768") }
        binding.btnKyber1024Sm4.setOnClickListener { viewModel.genPqcKeyPair("kyber1024") }
        
        binding.btnDilithium2Sign.setOnClickListener { viewModel.hmacSign() }
        binding.btnDilithium3Sign.setOnClickListener { viewModel.hmacSign() }
        binding.btnDilithium5Sign.setOnClickListener { viewModel.hmacSign() }
        
        binding.btnSm2KeyGen.setOnClickListener { viewModel.genPqcKeyPair("dilithium2") }
        binding.btnSm2Sign.setOnClickListener { viewModel.sm2Encrypt() }
        binding.btnSm2Verify.setOnClickListener { viewModel.sm2Decrypt() }
        
        binding.btnSm4Cbc.setOnClickListener { viewModel.sm4Encrypt("SM4/CBC/NoPadding") }
        binding.btnSm4Ecb.setOnClickListener { viewModel.sm4Encrypt("SM4/ECB/NoPadding") }
    }
}
