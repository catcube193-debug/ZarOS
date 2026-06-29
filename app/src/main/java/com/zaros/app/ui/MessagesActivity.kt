package com.zaros.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zaros.app.R
import com.zaros.app.backend.SoundManager
import com.zaros.app.backend.ZarSmsManager
import com.zaros.app.databinding.ActivityMessagesBinding
import kotlinx.coroutines.*

class MessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessagesBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var hasLoadedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)
        binding.btnBack.setOnClickListener { finish() }

        requestPermissions()
    }

    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 3001)
        } else {
            loadThreads()
        }
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(req, perms, grants)
        if (req == 3001) loadThreads()
    }

    override fun onResume() {
        super.onResume()
        // Skip the redundant reload that would otherwise fire immediately
        // after onCreate()'s own loadThreads() call on first launch — only
        // refresh when the user is actually returning to an already-loaded
        // screen (e.g. coming back from a conversation after sending a text).
        if (hasLoadedOnce) loadThreads()
    }

    private fun loadThreads() {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.tvEmpty.visibility           = View.GONE
        binding.rvThreads.visibility         = View.GONE
        scope.launch {
            val threads = withContext(Dispatchers.IO) {
                ZarSmsManager.getThreads(this@MessagesActivity)
            }
            hasLoadedOnce = true
            binding.loadingContainer.visibility = View.GONE
            if (threads.isEmpty()) {
                binding.tvEmpty.visibility  = View.VISIBLE
                binding.rvThreads.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility  = View.GONE
                binding.rvThreads.visibility = View.VISIBLE
                setupAdapter(threads)
            }
        }
    }

    private fun setupAdapter(threads: List<ZarSmsManager.SmsThread>) {
        binding.rvThreads.layoutManager = LinearLayoutManager(this)
        binding.rvThreads.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val initials: TextView = v.findViewById(R.id.tvThreadInitials)
                val name:     TextView = v.findViewById(R.id.tvThreadName)
                val snippet:  TextView = v.findViewById(R.id.tvThreadSnippet)
                val date:     TextView = v.findViewById(R.id.tvThreadDate)
                val unread:   TextView = v.findViewById(R.id.tvThreadUnread)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                VH(LayoutInflater.from(p.context).inflate(R.layout.item_sms_thread, p, false))
            override fun getItemCount() = threads.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
                val t = threads[i]; h as VH
                h.initials.text  = t.initials.uppercase()
                h.name.text      = t.name
                h.snippet.text   = t.snippet
                h.date.text      = t.formattedDate()
                h.unread.visibility = if (t.unread > 0) View.VISIBLE else View.GONE
                h.unread.text    = t.unread.toString()
                h.itemView.setOnClickListener {
                    SoundManager.playTap(this@MessagesActivity)
                    startActivity(Intent(this@MessagesActivity, ConversationActivity::class.java)
                        .putExtra(ConversationActivity.EXTRA_ADDRESS, t.address)
                        .putExtra(ConversationActivity.EXTRA_NAME, t.name))
                }
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
}
