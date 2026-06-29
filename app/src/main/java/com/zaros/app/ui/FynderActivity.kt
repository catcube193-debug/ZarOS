package com.zaros.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.zaros.app.R
import com.zaros.app.backend.AiSearchEngine
import com.zaros.app.backend.FynderSearchEngine
import com.zaros.app.backend.SoundManager
import com.zaros.app.databinding.ActivityFynderBinding
import kotlinx.coroutines.*

class FynderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFynderBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentUrl = ""
    private var isShowingResults = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = ActivityFynderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupAddressBar()
        setupButtons()
        showHomePage()

        val query = intent.getStringExtra(EXTRA_QUERY)
        if (!query.isNullOrBlank()) performAiSearch(query)
    }

    // ── WebView ───────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(binding.webView.settings) {
            javaScriptEnabled        = true
            domStorageEnabled        = true
            loadsImagesAutomatically = true
            setSupportZoom(true)
            builtInZoomControls      = true
            displayZoomControls      = false
            useWideViewPort          = true
            loadWithOverviewMode     = true
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                currentUrl = url
                binding.urlInput.setText(url)
                binding.progressBar.isVisible = true
                updateNavButtons()
            }
            override fun onPageFinished(view: WebView, url: String) {
                currentUrl = url
                binding.urlInput.setText(url)
                binding.progressBar.isVisible = false
                updateNavButtons()
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.progressBar.progress = newProgress
                binding.progressBar.isVisible = newProgress < 100
            }
            override fun onReceivedTitle(view: WebView, title: String) {
                binding.tvPageTitle.text = title
            }
        }
    }

    // ── AI Search ─────────────────────────────────────────────────────────

    private fun performAiSearch(query: String) {
        binding.urlInput.setText(query)
        hideKeyboard()

        // Show loading state
        showResultsPage()
        binding.resultsContainer.removeAllViews()
        addLoadingCard()
        FynderSearchEngine.addToHistory(this, query)
        SoundManager.playTap(this)

        scope.launch {
            val response = withContext(Dispatchers.IO) {
                AiSearchEngine.search(query)
            }

            binding.resultsContainer.removeAllViews()

            if (response.error != null && response.results.isEmpty()) {
                // Fallback to WebView
                addErrorCard(response.error)
                return@launch
            }

            // AI Summary card
            if (response.aiSummary.isNotBlank()) {
                addAiSummaryCard(response.aiSummary)
            }

            // Result cards
            response.results.forEach { result ->
                addResultCard(result)
            }

            // If no API keys set, fall back to WebView search
            if (response.results.isEmpty()) {
                binding.webView.isVisible = true
                binding.scrollResults.isVisible = false
                binding.webView.loadUrl(FynderSearchEngine.resolve(query))
            }
        }
    }

    private fun addLoadingCard() {
        val v = layoutInflater.inflate(R.layout.item_search_loading, binding.resultsContainer, false)
        binding.resultsContainer.addView(v)
    }

    private fun addAiSummaryCard(summary: String) {
        val v = layoutInflater.inflate(R.layout.item_ai_summary, binding.resultsContainer, false)
        v.findViewById<TextView>(R.id.tvSummary).text = summary
        binding.resultsContainer.addView(v)
    }

    private fun addResultCard(result: AiSearchEngine.SearchResult) {
        val v = layoutInflater.inflate(R.layout.item_search_result, binding.resultsContainer, false)
        v.findViewById<TextView>(R.id.tvResultTitle).text = result.title
        v.findViewById<TextView>(R.id.tvResultUrl).text = result.url
        v.findViewById<TextView>(R.id.tvResultSnippet).text = result.snippet
        v.setOnClickListener {
            SoundManager.playTap(this)
            loadInWebView(result.url)
        }
        binding.resultsContainer.addView(v)
    }

    private fun addErrorCard(msg: String) {
        val v = layoutInflater.inflate(R.layout.item_search_result, binding.resultsContainer, false)
        v.findViewById<TextView>(R.id.tvResultTitle).text = "⚠️ Search unavailable"
        v.findViewById<TextView>(R.id.tvResultUrl).text = ""
        v.findViewById<TextView>(R.id.tvResultSnippet).text = msg
        binding.resultsContainer.addView(v)
    }

    private fun loadInWebView(url: String) {
        binding.scrollResults.isVisible = false
        binding.webView.isVisible = true
        currentUrl = url
        binding.urlInput.setText(url)
        binding.webView.loadUrl(url)
        isShowingResults = false
    }

    private fun showHomePage() {
        binding.webView.isVisible = false
        binding.scrollResults.isVisible = false
        binding.fynderHomePage.isVisible = true
        isShowingResults = false
    }

    private fun showResultsPage() {
        binding.fynderHomePage.isVisible = false
        binding.webView.isVisible = false
        binding.scrollResults.isVisible = true
        isShowingResults = true
    }

    // ── Address bar ───────────────────────────────────────────────────────

    private fun setupAddressBar() {
        binding.urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                commitInput(); true
            } else false
        }
        binding.urlInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.urlInput.selectAll()
        }
        binding.btnGo.setOnClickListener { commitInput() }
    }

    private fun commitInput() {
        val input = binding.urlInput.text.toString().trim()
        if (input.isBlank()) return
        hideKeyboard()

        // If it looks like a URL, load directly; otherwise AI search
        if (input.contains(".") && !input.contains(" ") ||
            input.startsWith("http")) {
            loadInWebView(FynderSearchEngine.resolve(input))
        } else {
            performAiSearch(input)
        }
    }

    // ── Nav buttons ───────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            when {
                binding.webView.isVisible && binding.webView.canGoBack() ->
                    binding.webView.goBack()
                binding.webView.isVisible -> showHomePage()
                isShowingResults -> showHomePage()
                else -> finish()
            }
        }
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
        binding.btnRefresh.setOnClickListener {
            if (binding.webView.isVisible) binding.webView.reload()
        }
        binding.btnHome.setOnClickListener { showHomePage(); SoundManager.playTap(this) }
        binding.btnShare.setOnClickListener {
            val url = if (binding.webView.isVisible) currentUrl
                      else binding.urlInput.text.toString()
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(share, "Share via"))
        }
        binding.btnOpenExternal.setOnClickListener {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))) }
            catch (e: Exception) { Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show() }
        }
        binding.btnClose.setOnClickListener { finish() }

        // Home page search button
        binding.btnHomeSearch.setOnClickListener {
            val q = binding.etHomeSearch.text.toString().trim()
            if (q.isNotBlank()) performAiSearch(q)
        }
        binding.etHomeSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = binding.etHomeSearch.text.toString().trim()
                if (q.isNotBlank()) performAiSearch(q)
                true
            } else false
        }
    }

    private fun updateNavButtons() {
        binding.btnBack.alpha    = if (binding.webView.canGoBack())    1f else 0.35f
        binding.btnForward.alpha = if (binding.webView.canGoForward()) 1f else 0.35f
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.urlInput.windowToken, 0)
        binding.urlInput.clearFocus()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            binding.webView.isVisible && binding.webView.canGoBack() ->
                binding.webView.goBack()
            binding.webView.isVisible || isShowingResults -> showHomePage()
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    companion object { const val EXTRA_QUERY = "extra_query" }
}
