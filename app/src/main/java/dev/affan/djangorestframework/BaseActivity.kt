package dev.affan.djangorestframework

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Window
import android.view.WindowManager
import androidx.annotation.IntRange
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    open var isFullScreen: Boolean = false
        protected set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
//            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
        }
        initContentView()
        setupToolbar()
        initComponents()
    }

    protected open fun initContentView() {}
    protected open fun setupToolbar() {}
    protected open fun initComponents() {}

    fun startActivityDelayed(intent: Intent, delay: Long) {
        Handler().postDelayed({ startActivity(intent);finish() }, delay)
    }

    fun checkAndRequestPermission(permission: Array<String>, @IntRange(from = 0) requestCode: Int): Boolean {
        val checkGranted = BooleanArray(permission.size)
        checkGranted.mapInPlace { index, _ ->
            ContextCompat.checkSelfPermission(
                this,
                permission[index]
            ) != PackageManager.PERMISSION_GRANTED

        }
        return if (checkGranted.contains(false)) {
            true
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    permission,
                    requestCode
                )
            }
            true
        }
    }
}