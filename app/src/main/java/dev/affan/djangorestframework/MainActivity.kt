package dev.affan.djangorestframework

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startActivityForResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {
    init {
        isFullScreen = false
    }

    private var isAllowedToExit = false
    private var url = ""
    private val webChromeClient = ChromeClient(this)

    override fun initContentView() {
        setContentView(R.layout.activity_main)
    }

    override fun initComponents() {
        initWebView()
        swipeLayout.setOnRefreshListener {
            webview.reload()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webview.settings.javaScriptEnabled = true
        webview.settings.databaseEnabled = true
        webview.settings.domStorageEnabled = true
        webview.settings.builtInZoomControls = false
        webview.settings.displayZoomControls = false
        webview.settings.loadsImagesAutomatically = true
        webview.settings.useWideViewPort = true
        webview.settings.minimumFontSize = 1
        webview.settings.minimumLogicalFontSize = 1
        webview.settings.allowContentAccess = true
        webview.settings.allowFileAccess = true
        webview.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webview.setInitialScale(90)
        val client = WebClient()
        webview.addJavascriptInterface(WebAppInterface(this, this).apply {
            setOnUrlChangedListener(object : OnUrlChangedListener {
                override fun onUrlChanged(url: String?) {
                    Log.d("url", url)
                    this@MainActivity.url = url!!
                }
            })
        }, "android")
        client.setOnPageLoadedListener(object : OnPageLoaded {
            override fun onPageLoaded(url: String) {
                swipeLayout.isRefreshing = false
            }
        })

        client.setOnPageErrorListener(object : OnPageError {
            override fun onPageErrorListener(errorCode: Int, title: String, message: String) {
                webview.loadUrl("file:///android_asset/html/error.html")
                webview.clearHistory()
            }
        })
        webview.webViewClient = client
        webview.webChromeClient = webChromeClient
        webview.isScrollbarFadingEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true)
        }
        webview.loadUrl("file:///android_asset/html/www.django-rest-framework.org/index.html")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (isAllowedToExit)
            finish()
        else {
            if (webview.canGoBack() && !(this.url.contains("login", true) || this.url.contains("dashboard", true)))
                webview.goBack()
            else {
                isAllowedToExit = true
                Handler().postDelayed({ isAllowedToExit = false }, 2000)
                Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        webview.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        webview.onResume()
    }

    override fun onPause() {
        super.onPause()
        webview.onPause()
    }
}

class WebAppInterface(private val mContext: Context, private val activity: Activity) {
    private var onUrlChangedListener: OnUrlChangedListener? = null

    fun setOnUrlChangedListener(onUrlChangedListener: OnUrlChangedListener) {
        this.onUrlChangedListener = onUrlChangedListener
    }

    @JavascriptInterface
    fun changeUrl(url: String) {
        onUrlChangedListener?.onUrlChanged(url)
    }
}

interface OnUrlChangedListener {
    fun onUrlChanged(url: String?)
}

interface OnPageLoaded {
    fun onPageLoaded(url: String)
}

interface OnPageError {
    fun onPageErrorListener(errorCode: Int, title: String, message: String)
}

class WebClient : WebViewClient() {
    private var jsCode: String = ""
    private var cssCode: String = ""
    private var headers: MutableMap<String, String> = mutableMapOf()
    private var onPageLoaded: OnPageLoaded? = null
    private var onPageError: OnPageError? = null

    fun setOnPageLoadedListener(onPageLoaded: OnPageLoaded) {
        this.onPageLoaded = onPageLoaded
    }

    fun setOnPageErrorListener(onPageError: OnPageError) {
        this.onPageError = onPageError
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        onPageLoaded?.onPageLoaded(url!!)
        try {
            view?.loadUrl(
                "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('body').item(0);" +
                        "var script = document.createElement('script');" +
                        "script.type = 'application/javascript';" +
                        // Tell the browser to BASE64-decode the string into your script !!!
                        "script.innerHTML = '${jsCode.replace("'", "\"")}';" +
                        "parent.appendChild(script);" +
                        "})()"
            )
            view?.loadUrl(
                "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        // Tell the browser to BASE64-decode the string into your script !!!
                        "style.innerHTML = '${cssCode.replace("'", "\"")}';" +
                        "parent.appendChild(style);" +
                        "})()"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onPageFinished(view, url)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        Log.d("webclient error", "${error?.errorCode}: ${error?.description}")
        onPageError?.onPageErrorListener(error?.errorCode!!, "Kesalahan", error.description.toString())
        super.onReceivedError(view, request, error)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        Log.d("webclient error", "${errorResponse?.statusCode}: ${errorResponse?.reasonPhrase}")
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        Log.d("webclient error", "$errorCode: $description <$failingUrl>")
        super.onReceivedError(view, errorCode, description, failingUrl)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        view?.loadUrl(request?.url.toString(), headers)
        return true
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        view?.loadUrl(url, headers)
        return true
    }

    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        Log.d("dontResend", dontResend?.data.toString())
        Log.d("resend", resend?.data.toString())
        super.onFormResubmission(view, dontResend, resend)
    }

    fun injectScript(script: String) {
        this.jsCode += script
    }

    fun injectStyle(style: String) {
        this.cssCode += style
    }

    fun setHeaders(headers: Map<String, String>) {
        this.headers.putAll(headers)
    }

    fun addHeader(name: String, value: String) {
        this.headers[name] = value
    }
}

class ChromeClient(private val activity: BaseActivity) : WebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.d("console", consoleMessage?.message())
        return super.onConsoleMessage(consoleMessage)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequest(request: PermissionRequest?) {
        Log.d("permission", request?.resources.toString())
        request?.grant(request.resources)
        super.onPermissionRequest(request)
    }

}
