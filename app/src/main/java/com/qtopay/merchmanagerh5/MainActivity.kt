package com.qtopay.merchmanagerh5

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.*
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings.Secure.getString
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tbruyelle.rxpermissions2.RxPermissions
import java.io.File
import java.util.logging.Logger
import android.R.menu
import android.content.SharedPreferences
import android.view.MenuInflater
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.text.TextUtils
import android.view.MenuItem
import com.qtopay.litemall.CaptureActivity


class MainActivity : AppCompatActivity() {
    var myWebChromeClient: MyWebChromeClient? = null
    public val REQUEST_FILE_PICKER = 1
    var exitTime: Long = 0
    //http://192.168.2.248/merchant-app/#/login
    //http://192.168.36.236:8090/#/product/list
    //http://192.168.36.236:8090/#/home
    //https://dc.xmfstore.com/merchant-app/#/product/list
    var url = "http://192.168.36.236:8090/#/home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webview_main.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            allowFileAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            var appCachePath = cacheDir.absolutePath
            setAppCachePath(appCachePath)
            setAppCacheEnabled(true)
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        }
        myWebChromeClient = MyWebChromeClient(this)
        webview_main.webChromeClient = myWebChromeClient!!
        webview_main.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request!!.url.toString().contains("code=close")) {
                    this@MainActivity.finish()
                    return true
                }
                if (request!!.url.toString().contains("code=scanCode")) {
                    if (!lacksPermission(this@MainActivity, Manifest.permission.CAMERA))
                        startActivityForResult(
                            Intent(
                                this@MainActivity,
                                CaptureActivity::class.java
                            ), 2
                        )
                    return true
                }
                saveCookie(request.url.toString())
                CookieManager.getInstance().flush()
                webview_main.loadUrl(request.url.toString())
                return true
            }
        }
        var disp = RxPermissions(this).request(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
            .subscribe({ aBoolean ->
                if (!aBoolean) {
                    Toast.makeText(this, R.string.refuse_permission, Toast.LENGTH_SHORT).show()
                }
            }) { error ->
                Log.d("err", "error:" + error.message!!)
            }

        webview_main.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webview_main.canGoBack()) {
            webview_main.goBack()
        } else
            super.onBackPressed()
    }


    fun lacksPermission(mContexts: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            mContexts,
            permission
        ) == PackageManager.PERMISSION_DENIED
    }

    class MyWebChromeClient(var context: Activity) : WebChromeClient() {
        public var mFilePathCallback: ValueCallback<Uri>? = null
        public var mFilePathCallbacks: ValueCallback<Array<Uri>>? = null

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean {
            if (lacksPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                return false
            }
            mFilePathCallbacks = filePathCallback
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/bmp"
            context.startActivityForResult(
                Intent.createChooser(intent, "File Chooser"),
                1
            )
            return true
        }

        fun lacksPermission(mContexts: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                mContexts,
                permission
            ) == PackageManager.PERMISSION_DENIED
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (myWebChromeClient!!.mFilePathCallback != null) {
                val result = if (data == null || resultCode != Activity.RESULT_OK)
                    null
                else
                    data.data
                if (result != null) {
                    val path = UriUtil.getPath(this, result)
                    val uri = Uri.fromFile(File(path))
                    myWebChromeClient!!.mFilePathCallback!!
                        .onReceiveValue(uri)
                } else {
                    myWebChromeClient!!.mFilePathCallback!!
                        .onReceiveValue(null)
                }
            }
            if (myWebChromeClient!!.mFilePathCallbacks != null) {
                val result = if (data == null || resultCode != Activity.RESULT_OK)
                    null
                else
                    data.data
                if (result != null) {
                    val path = UriUtil.getPath(this, result)
                    val uri = Uri.fromFile(File(path))
                    myWebChromeClient!!.mFilePathCallbacks!!
                        .onReceiveValue(arrayOf(uri))
                } else {
                    myWebChromeClient!!.mFilePathCallbacks!!
                        .onReceiveValue(null)
                }
            }
            myWebChromeClient!!.mFilePathCallback = null
            myWebChromeClient!!.mFilePathCallbacks = null
        } else if (requestCode == 2) {
            if (data != null) {
                var url = data!!.getStringExtra("qrcode")
                webview_main.loadUrl("javascript:mobileSetCouponCode('$url')")
            }
        }
    }

    override fun onResume() {
        if (webview_main != null)
            setCookies(webview_main.url)
        super.onResume()
    }

    override fun onPause() {
        if (webview_main != null)
            saveCookie(webview_main.url)
        super.onPause()
    }

    override fun onDestroy() {
        if (webview_main != null)
            saveCookie(webview_main.url)
        super.onDestroy()
    }

    fun setCookies(url: String) {
        var cookieMap: Map<String, String> = HashMap()
        var cookie = getSharedPreferences("cookie", Context.MODE_PRIVATE).getString(
            "cookies",
            ""
        )
        if (!TextUtils.isEmpty(cookie)) {
            var cookieArray = cookie!!.split(";")
            for (item in cookieArray) {
                var keyValuesArray = item.split("=")
                if (keyValuesArray.size >= 2) {
                    var cookieName = keyValuesArray[0]
                    var cookieValue = keyValuesArray[1]
                    var value = cookieName + "=" + cookieValue
                    Log.i("cookie", value)
                    CookieManager.getInstance().setCookie(
                        getDomain(url),
                        value
                    )
                }
            }
        }
    }

    fun saveCookie(url: String) {
        var cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        var cookieStr = cookieManager.getCookie(url)
        var preferences = getSharedPreferences("cookie", Context.MODE_PRIVATE)
        var editor = preferences.edit()
        editor.putString("cookies", cookieStr)
        editor.apply()
    }

    fun getDomain(url: String): String {
        var url = url.replace("http://", "").replace("https://", "")
        if (url.contains("/")) {
            url = url.substring(0, url.indexOf('/'))
        }
        return url
    }
}
