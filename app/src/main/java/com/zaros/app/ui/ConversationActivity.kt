package com.zaros.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zaros.app.R
import com.zaros.app.backend.SoundManager
import com.zaros.app.backend.ZarSmsManager
import com.zaros.app.databinding.ActivityConversationBinding
import kotlinx.coroutines.*

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var address = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)

        address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val name = intent.getStringExtra(EXTRA_NAME) ?: address

        binding.tvContactName.text = name
        binding.btnBack.setOnClickListener { finish() }

        // Send button
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotBlank() && address.isNotBlank()) {
                sendMessage(text)
            }
        }

        // Keyboard type sound
        binding.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > before) SoundManager.playType(this@ConversationActivity)
                else if (count < before) SoundManager.playBackspace(this@ConversationActivity)
            }
        })

        loadMessages()
    }

    override fun onResume() { super.onResume(); loadMessages() }

    private fun loadMessages() {
        if (address.isBlank()) return
        scope.launch {
            val msgs = withContext(Dispatchers.IO) {
                ZarSmsManager.getMessages(this@ConversationActivity, address)
            }
            setupAdapter(msgs)
        }
    }

    private fun setupAdapter(msgs: List<ZarSmsManager.SmsMessage>) {
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val body: TextView = v.findViewById(R.id.tvMessageBody)
                val time: TextView = v.findViewById(R.id.tvMessageTime)
            }
            override fun getItemViewType(i: Int) = if (msgs[i].outgoing) 1 else 0
            override fun onCreateViewHolder(p: ViewGroup, t: Int): RecyclerView.ViewHolder {
                val layout = if (t == 1) R.layout.item_message_sent else R.layout.item_message_recv
                return VH(LayoutInflater.from(p.context).inflate(layout, p, false))
            }
            override fun getItemCount() = msgs.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
                val m = msgs[i]; h as VH
                h.body.text = m.body
                h.time.text = m.formattedTime()
            }
        }
        binding.rvMessages.scrollToPosition(msgs.size - 1)
    }

    private fun sendMessage(text: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { ZarSmsManager.sendSms(address, text) }
                binding.etMessage.setText("")
                SoundManager.playMessage(this@ConversationActivity)
                loadMessages()
            } catch (e: Exception) {
                Toast.makeText(this@ConversationActivity,
                    "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    companion object {
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_NAME    = "extra_name"
    }
}
