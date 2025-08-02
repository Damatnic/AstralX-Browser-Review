package com.astralx.browser.presentation.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.astralx.browser.R
import com.astralx.browser.databinding.FragmentBrowserModernBinding
import com.astralx.browser.domain.manager.BookmarkManager
import com.astralx.browser.domain.manager.DownloadManager
import com.astralx.browser.domain.manager.HistoryManager
import com.astralx.browser.media.UniversalVideoPlayer
import com.astralx.browser.media.ThumbnailPreviewEngine
import com.astralx.browser.performance.MediaOptimizer
import com.astralx.browser.privacy.PrivacyShield
import com.astralx.browser.presentation.main.MainViewModel
import com.astralx.browser.video.AdultContentVideoDetector
import com.astralx.browser.video.VideoThumbnailPreviewEngine
import com.astralx.browser.video.AdultContentVideoCodecs
import com.astralx.browser.video.ModernVideoControlsOverlay
import com.google.android.material.transition.platform.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ModernBrowserFragment : Fragment() {

    private var _binding: FragmentBrowserModernBinding? = null
    private val binding get() = _binding!!
    
    private val mainViewModel: MainViewModel by viewModels()
    
    @Inject lateinit var bookmarkManager: BookmarkManager
    @Inject lateinit var historyManager: HistoryManager
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var universalVideoPlayer: UniversalVideoPlayer
    @Inject lateinit var thumbnailPreviewEngine: ThumbnailPreviewEngine
    @Inject lateinit var mediaOptimizer: MediaOptimizer
    @Inject lateinit var privacyShield: PrivacyShield
    @Inject lateinit var adultVideoDetector: AdultContentVideoDetector
    @Inject lateinit var videoThumbnailEngine: VideoThumbnailPreviewEngine
    @Inject lateinit var adultVideoCodecs: AdultContentVideoCodecs

    private lateinit var webViewClient: EnhancedWebViewClient
    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var isPrivateMode = false
    private var fabExpanded = false
    private var videoControlsOverlay: ModernVideoControlsOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up shared element transitions
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserModernBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge()
        setupWebView()
        setupVideoHandling()
        setupToolbar()
        setupBottomNavigation()
        setupFAB()
        observeViewModel()
        
        // Animate entrance
        animateEntrance()
        
        // Setup back press handling
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        )
        
        // Load homepage or restore state
        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        } else {
            loadUrl("https://www.google.com")
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupWebView() {
        webViewClient = EnhancedWebViewClient(
            requireContext(),
            universalVideoPlayer,
            thumbnailPreviewEngine,
            mediaOptimizer,
            privacyShield
        )
        
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                allowFileAccess = true
                allowContentAccess = true
                setGeolocationEnabled(true)
                
                // Optimizations for adult content video playback
                mediaPlaybackRequiresUserGesture = false
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            webViewClient = this@ModernBrowserFragment.webViewClient
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    updateProgress(newProgress)
                }
                
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    currentTitle = title
                    updateUrlDisplay(currentUrl ?: "")
                }
                
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    handleFullscreenVideo(view, callback)
                }
                
                override fun onHideCustomView() {
                    super.onHideCustomView()
                    exitFullscreenVideo()
                }
            }
        }
    }
    
    private fun setupVideoHandling() {
        // Initialize adult content video detection
        adultVideoDetector.detectedVideos.observe(viewLifecycleOwner) { videos ->
            if (videos.isNotEmpty()) {
                Timber.d("Detected ${videos.size} videos on current page")
                // Optionally show video detection indicator
                showVideoDetectionIndicator(videos.size)
            }
        }
        
        // Initialize video thumbnail previews
        videoThumbnailEngine.hoverPreviews.observe(viewLifecycleOwner) { previews ->
            // Handle hover preview updates
            Timber.d("Updated hover previews for ${previews.size} videos")
        }
        
        // Log codec capabilities for debugging
        adultVideoCodecs.logCodecCapabilities()
        
        // Setup video detection on page load
        binding.webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    // Detect videos on adult content sites
                    adultVideoDetector.detectVideosOnAdultSite(binding.webView, it)
                    
                    // Inject video thumbnail preview functionality
                    videoThumbnailEngine.injectHoverPreviewScript(binding.webView)
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.apply {
            // Tab counter click
            tabCounter.setOnClickListener {
                animateTabCounter()
                // TODO: Open tab switcher
            }
            
            // Omnibox click
            omnibox.setOnClickListener {
                animateOmniboxFocus()
                // TODO: Open search/URL input
            }
            
            // Reload button
            reloadButton.setOnClickListener {
                animateReload()
                binding.webView.reload()
            }
            
            // Menu button
            menuButton.setOnClickListener {
                animateMenuPress()
                // TODO: Open menu
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_back -> {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    }
                    true
                }
                R.id.nav_forward -> {
                    if (binding.webView.canGoForward()) {
                        binding.webView.goForward()
                    }
                    true
                }
                R.id.nav_home -> {
                    loadUrl("https://www.google.com")
                    true
                }
                R.id.nav_downloads -> {
                    // TODO: Open downloads
                    true
                }
                R.id.nav_settings -> {
                    // TODO: Open settings
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFAB() {
        binding.fabMain.setOnClickListener {
            toggleFABMenu()
        }
    }

    private fun observeViewModel() {
        mainViewModel.apply {
            // Observe privacy mode changes
            lifecycleScope.launch {
                // TODO: Observe privacy mode state
            }
        }
    }

    private fun loadUrl(url: String) {
        currentUrl = url
        binding.webView.loadUrl(url)
        updateUrlDisplay(url)
        showLoadingState()
    }

    private fun updateProgress(progress: Int) {
        binding.progressBar.apply {
            if (progress < 100) {
                isVisible = true
                setProgress(progress, true)
            } else {
                // Animate progress completion
                setProgress(100, true)
                lifecycleScope.launch {
                    delay(200)
                    isVisible = false
                    hideLoadingState()
                }
            }
        }
    }

    private fun updateUrlDisplay(url: String) {
        binding.urlText.text = when {
            url.startsWith("https://") -> url.removePrefix("https://")
            url.startsWith("http://") -> url.removePrefix("http://")
            else -> url
        }
        
        // Update security icon
        binding.securityIcon.setImageResource(
            if (url.startsWith("https://")) R.drawable.ic_lock
            else R.drawable.ic_warning
        )
        
        binding.securityIcon.setColorFilter(
            if (url.startsWith("https://")) 
                resources.getColor(R.color.astral_success, null)
            else 
                resources.getColor(R.color.astral_warning, null)
        )
    }

    private fun showLoadingState() {
        binding.loadingView.apply {
            alpha = 0f
            isVisible = true
            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun hideLoadingState() {
        binding.loadingView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.loadingView.isVisible = false
            }
            .start()
    }

    private fun animateEntrance() {
        binding.apply {
            // Animate app bar
            appBarLayout.translationY = -200f
            appBarLayout.animate()
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            // Animate bottom navigation
            bottomNavigation.translationY = 200f
            bottomNavigation.animate()
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            // Animate FAB
            fabMain.apply {
                scaleX = 0f
                scaleY = 0f
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
        }
    }

    private fun animateTabCounter() {
        binding.tabCounter.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.tabCounter.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateOmniboxFocus() {
        binding.omnibox.animate()
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(100)
            .withEndAction {
                binding.omnibox.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateReload() {
        binding.reloadButton.animate()
            .rotation(360f)
            .setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun animateMenuPress() {
        binding.menuButton.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.menuButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun toggleFABMenu() {
        fabExpanded = !fabExpanded
        
        val rotationAngle = if (fabExpanded) 45f else 0f
        binding.fabMain.animate()
            .rotation(rotationAngle)
            .setDuration(200)
            .start()
        
        // TODO: Implement mini FABs animation
    }
    
    private fun handleFullscreenVideo(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        if (view != null) {
            // Create video controls overlay
            videoControlsOverlay = ModernVideoControlsOverlay(requireContext()).apply {
                onPlayPauseClick = {
                    // Handle play/pause for fullscreen video
                    val script = "if (document.querySelector('video')) { " +
                                "var video = document.querySelector('video'); " +
                                "if (video.paused) video.play(); else video.pause(); }"
                    binding.webView.evaluateJavascript(script, null)
                }
                
                onSeekTo = { positionMs ->
                    // Handle seek in fullscreen video
                    val seconds = positionMs / 1000.0
                    val script = "if (document.querySelector('video')) { " +
                                "document.querySelector('video').currentTime = $seconds; }"
                    binding.webView.evaluateJavascript(script, null)
                }
                
                onVolumeChange = { volume ->
                    // Handle volume change
                    val script = "if (document.querySelector('video')) { " +
                                "document.querySelector('video').volume = $volume; }"
                    binding.webView.evaluateJavascript(script, null)
                }
                
                onFullscreenClick = {
                    // Exit fullscreen
                    callback?.onCustomViewHidden()
                }
            }
            
            // Add fullscreen video view and controls
            val fullscreenContainer = binding.coordinatorLayout
            fullscreenContainer.addView(view, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            
            videoControlsOverlay?.let { overlay ->
                fullscreenContainer.addView(overlay, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }
            
            // Hide browser UI
            binding.appBarLayout.isVisible = false
            binding.bottomNavigation.isVisible = false
            binding.fabMain.isVisible = false
        }
    }
    
    private fun exitFullscreenVideo() {
        // Remove fullscreen video view and controls
        val fullscreenContainer = binding.coordinatorLayout
        
        // Find and remove video view
        for (i in fullscreenContainer.childCount - 1 downTo 0) {
            val child = fullscreenContainer.getChildAt(i)
            if (child != binding.webviewContainer && 
                child != binding.appBarLayout && 
                child != binding.bottomNavigation && 
                child != binding.fabMain) {
                fullscreenContainer.removeView(child)
            }
        }
        
        videoControlsOverlay = null
        
        // Show browser UI
        binding.appBarLayout.isVisible = true
        binding.bottomNavigation.isVisible = true
        binding.fabMain.isVisible = true
    }
    
    private fun showVideoDetectionIndicator(videoCount: Int) {
        // Show a subtle indicator that videos were detected
        val message = if (videoCount == 1) "Video detected" else "$videoCount videos detected"
        
        // Create temporary indicator
        lifecycleScope.launch {
            // You could show a snackbar or toast here
            // For now, just log
            Timber.d("Video detection: $message")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ModernBrowserFragment {
            return ModernBrowserFragment()
        }
    }
}