package com.quantum.poc.ui

import android.os.Bundle
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.quantum.poc.R
import com.quantum.poc.databinding.ActivityMainBinding
import com.quantum.poc.viewmodel.CryptoUiState
import com.quantum.poc.viewmodel.CryptoViewModel
import com.quantum.poc.viewmodel.SessionState

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: CryptoViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSpinners()
        setupObservers()
        setupButtons()
    }
    
    private fun setupSpinners() {
        binding.spinnerKyber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val algorithm = when (position) {
                    0 -> "Kyber512"
                    1 -> "Kyber768"
                    2 -> "Kyber1024"
                    else -> "Kyber512"
                }
                viewModel.setKyberAlgorithm(algorithm)
                updateFullFlowText()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.spinnerDilithium.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val algorithm = when (position) {
                    0 -> "Dilithium2"
                    1 -> "Dilithium3"
                    2 -> "Dilithium5"
                    else -> "Dilithium2"
                }
                viewModel.setDilithiumAlgorithm(algorithm)
                updateFullFlowText()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun updateFullFlowText() {
        val kyber = binding.spinnerKyber.selectedItem ?: "Kyber512"
        val dilithium = binding.spinnerDilithium.selectedItem ?: "Dilithium2"
        binding.tvFullFlow.text = "$kyber + SM4-CBC + $dilithium"
    }
    
    private fun setupObservers() {
        viewModel.sessionData.observe(this) { session ->
            binding.etPublicKey.setText(session.publicKey)
            binding.etPrivateKey.setText(session.privateKey)
            binding.etRandom.setText(session.random)
            binding.etSessionKey.setText(session.sessionKey)
            binding.etPlainText.setText(session.plainText)
            binding.etCipherText.setText(session.cipherText)
            binding.etSignature.setText(session.signature)
            binding.etVerifyResult.setText(session.verifyResult)
            
            updateSessionStatus(session.state)
        }
        
        viewModel.logMessage.observe(this) { log ->
            binding.tvLog.text = log + "\n" + binding.tvLog.text
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
    
    private fun updateSessionStatus(state: SessionState) {
        when (state) {
            SessionState.IDLE -> {
                binding.tvSessionStatus.text = "🔴 未初始化"
                binding.tvSessionStatus.setTextColor(getColor(R.color.red))
                binding.tvKeyGenStatus.text = "⏳ 待生成"
                binding.tvRandomStatus.text = "⏳ 待生成"
            }
            SessionState.KEY_READY -> {
                binding.tvSessionStatus.text = "🟡 密钥就绪"
                binding.tvKeyGenStatus.text = "✅ 已生成"
                binding.tvRandomStatus.text = "⏳ 待生成"
            }
            SessionState.SESSION_KEY -> {
                binding.tvSessionStatus.text = "🟢 会话密钥就绪"
                binding.tvKeyGenStatus.text = "✅ 已生成"
                binding.tvRandomStatus.text = "✅ 已生成"
            }
            SessionState.ENCRYPTED -> {
                binding.tvSessionStatus.text = "🟢 数据已加密"
            }
            SessionState.SIGNED -> {
                binding.tvSessionStatus.text = "🟢 已签名"
            }
        }
    }
    
    private fun setupButtons() {
        binding.btnNewSession.setOnClickListener { viewModel.newSession() }
        
        binding.btnGenKeyPair.setOnClickListener { viewModel.genKeyPair() }
        
        binding.btnGenRandom.setOnClickListener { viewModel.genRandom() }
        
        binding.btnEncrypt.setOnClickListener {
            val sm4Mode = if (binding.rbSm4Cbc.isChecked) "SM4/CBC/NoPadding" else "SM4/ECB/NoPadding"
            viewModel.encrypt(sm4Mode)
        }
        
        binding.btnDecrypt.setOnClickListener {
            val sm4Mode = if (binding.rbSm4Cbc.isChecked) "SM4/CBC/NoPadding" else "SM4/ECB/NoPadding"
            viewModel.decrypt(sm4Mode)
        }
        
        binding.btnSign.setOnClickListener { viewModel.sign() }
        
        binding.btnVerify.setOnClickListener { viewModel.verify() }
        
        binding.btnFullFlow.setOnClickListener { viewModel.fullFlow() }
    }
}
