package com.zaros.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.zaros.app.backend.CallLogManager
import com.zaros.app.backend.ContactsManager
import com.zaros.app.backend.SoundManager
import com.zaros.app.databinding.ActivityDialerBinding
import kotlinx.coroutines.*

class DialerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDialerBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var dialedNumber = ""
    private var currentTab = TAB_KEYPAD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding = ActivityDialerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)
        requestPermissions()
        setupTabs()
        setupKeypad()
        showTab(TAB_KEYPAD)
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ_PERMS)
        } else {
            loadData()
        }
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<String>, grants: IntArray) {
        super.onRequestPermissionsResult(req, perms, grants)
        if (req == REQ_PERMS) loadData()
    }

    private fun loadData() {
        scope.launch {
            val calls = withContext(Dispatchers.IO) {
                CallLogManager.getRecentCalls(this@DialerActivity, 50)
            }
            val contacts = withContext(Dispatchers.IO) {
                ContactsManager.getContacts(this@DialerActivity)
            }
            setupRecent(calls)
            setupContacts(contacts)
        }
    }

    private fun setupTabs() {
        binding.tabKeypad.setOnClickListener   { showTab(TAB_KEYPAD);   SoundManager.playTap(this) }
        binding.tabRecent.setOnClickListener   { showTab(TAB_RECENT);   SoundManager.playTap(this) }
        binding.tabContacts.setOnClickListener { showTab(TAB_CONTACTS); SoundManager.playTap(this) }
    }

    private fun showTab(tab: Int) {
        currentTab = tab
        binding.panelKeypad.visibility = if (tab == TAB_KEYPAD)   View.VISIBLE else View.GONE
        binding.rvRecent.visibility    = if (tab == TAB_RECENT)   View.VISIBLE else View.GONE
        binding.rvContacts.visibility  = if (tab == TAB_CONTACTS) View.VISIBLE else View.GONE
        val active = 0xFF38BDF8.toInt(); val inactive = 0x88FFFFFF.toInt()
        binding.tabKeypad.setTextColor(if (tab == TAB_KEYPAD)   active else inactive)
        binding.tabRecent.setTextColor(if (tab == TAB_RECENT)   active else inactive)
        binding.tabContacts.setTextColor(if (tab == TAB_CONTACTS) active else inactive)
    }

    private fun setupKeypad() {
        val keys = mapOf(
            binding.key0 to "0", binding.key1 to "1", binding.key2 to "2",
            binding.key3 to "3", binding.key4 to "4", binding.key5 to "5",
            binding.key6 to "6", binding.key7 to "7", binding.key8 to "8",
            binding.key9 to "9", binding.keyStar to "*", binding.keyHash to "#"
        )
        keys.forEach { (btn, digit) ->
            btn.setOnClickListener {
                dialedNumber += digit
                binding.tvDialed.text = dialedNumber
                SoundManager.playTap(this)
            }
        }
        binding.keyDelete.setOnClickListener {
            if (dialedNumber.isNotEmpty()) {
                dialedNumber = dialedNumber.dropLast(1)
                binding.tvDialed.text = dialedNumber
                SoundManager.playBackspace(this)
            }
        }
        binding.keyDelete.setOnLongClickListener {
            dialedNumber = ""; binding.tvDialed.text = ""; true
        }
        binding.btnCall.setOnClickListener {
            if (dialedNumber.isNotEmpty()) placeCall(dialedNumber)
        }
    }

    private fun placeCall(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) {
            SoundManager.playTap(this)
            try {
                // ACTION_CALL (not ACTION_DIAL) places the call directly,
                // with ZarOS itself as the initiating app — this is what
                // gives InCallActivity any legitimate control over ending
                // the call later via TelecomManager. ACTION_DIAL would
                // hand off to the system dialer's own UI instead, leaving
                // ZarOS with no hook to show a custom screen at all.
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                val callIntent = Intent(this, InCallActivity::class.java).apply {
                    putExtra(InCallActivity.EXTRA_NUMBER, number)
                    putExtra(InCallActivity.EXTRA_INCOMING, false)
                }
                startActivity(callIntent)
            } catch (e: SecurityException) {
                // Some OEM skins still reject ACTION_CALL even with
                // CALL_PHONE granted (seen historically on certain
                // Samsung/Motorola builds) — fall back to ACTION_DIAL so
                // the user can still complete the call manually rather
                // than the tap silently doing nothing.
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CALL_PHONE), REQ_PERMS)
        }
    }

    private fun setupRecent(calls: List<CallLogManager.CallEntry>) {
        binding.rvRecent.layoutManager = LinearLayoutManager(this)
        binding.rvRecent.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val emoji:   TextView = v.findViewById(R.id.tvCallEmoji)
                val name:    TextView = v.findViewById(R.id.tvCallName)
                val info:    TextView = v.findViewById(R.id.tvCallInfo)
                val btnCall: TextView = v.findViewById(R.id.btnCallBack)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                VH(LayoutInflater.from(p.context).inflate(R.layout.item_call_entry, p, false))
            override fun getItemCount() = calls.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
                val e = calls[i]; h as VH
                h.emoji.text  = e.typeEmoji()
                h.name.text   = e.name
                h.info.text   = "${e.formattedDate()} · ${e.formattedDuration()}"
                h.btnCall.setOnClickListener { placeCall(e.number) }
                h.itemView.setOnClickListener {
                    dialedNumber = e.number
                    binding.tvDialed.text = dialedNumber
                    showTab(TAB_KEYPAD)
                }
            }
        }
    }

    private fun setupContacts(contacts: List<ContactsManager.Contact>) {
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val initials: TextView = v.findViewById(R.id.tvContactInitials)
                val name:     TextView = v.findViewById(R.id.tvContactName)
                val phone:    TextView = v.findViewById(R.id.tvContactPhone)
                val btnCall:  TextView = v.findViewById(R.id.btnContactCall)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                VH(LayoutInflater.from(p.context).inflate(R.layout.item_contact, p, false))
            override fun getItemCount() = contacts.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
                val c = contacts[i]; h as VH
                h.initials.text = c.initials.uppercase()
                h.name.text     = c.name
                h.phone.text    = c.phone
                h.btnCall.setOnClickListener { placeCall(c.phone) }
                h.itemView.setOnClickListener {
                    dialedNumber = c.phone
                    binding.tvDialed.text = dialedNumber
                    showTab(TAB_KEYPAD)
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

    companion object {
        private const val TAB_KEYPAD   = 0
        private const val TAB_RECENT   = 1
        private const val TAB_CONTACTS = 2
        private const val REQ_PERMS    = 2001
    }
}
