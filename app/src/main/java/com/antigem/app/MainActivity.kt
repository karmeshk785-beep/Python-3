// MainActivity.kt - वेलकम + परमिशन स्क्रीन
package com.antigem.app
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) { super.onCreate(b); render() }
    override fun onResume() { super.onResume(); render() }

    private fun has(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
    private fun notifOk() = Build.VERSION.SDK_INT < 33 || has(Manifest.permission.POST_NOTIFICATIONS)
    private fun micOk() = has(Manifest.permission.RECORD_AUDIO)
    private fun storageOk() = if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else has(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(60), dp(20), dp(20))
        }
        col.addView(TextView(this).apply {
            text = "Welcome to AntiGem"; textSize = 28f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(Color.BLACK)
        })
        col.addView(TextView(this).apply {
            text = "Configure your permissions to enable background execution, terminal tools, and local project storage."
            textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.GRAY); setPadding(0, dp(12), 0, dp(24))
        })
        card(col, "Notifications & Service", "Required for persistent background terminal service, live status alerts and installer progress.", notifOk()) {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        card(col, "Storage & /sdcard", "Allows AntiGem to access /sdcard to import packages, inspect files and manage code workspaces.", storageOk()) {
            if (Build.VERSION.SDK_INT >= 30)
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            else requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
        }
        card(col, "Microphone & Voice", "Enables voice-to-text dictation and voice commands directly in chat.", micOk()) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 3)
        }
        root.addView(ScrollView(this).apply { addView(col) }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(Button(this).apply {
            text = "Continue to AntiGem 🚀"; setTextColor(Color.WHITE); textSize = 17f; isAllCaps = false
            background = rounded(ORANGE, 0, 28)
            setOnClickListener { startActivity(Intent(this@MainActivity, ChatActivity::class.java)) }
        }, LinearLayout.LayoutParams(MATCH_PARENT, dp(58)).apply { setMargins(dp(20), dp(8), dp(20), dp(24)) })
        setContentView(root)
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) { render() }

    private fun card(parent: LinearLayout, title: String, desc: String, ok: Boolean, action: () -> Unit) {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(18))
            background = rounded(Color.WHITE, if (ok) GREEN_LIGHT else 0xFFD0D0D0.toInt(), 24)
            setOnClickListener { if (!ok) action() }
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply { text = title; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(TextView(this).apply {
            text = if (ok) "Enabled ✓" else "Tap to enable"; textSize = 13f; setPadding(dp(10), dp(6), dp(10), dp(6))
            setTextColor(if (ok) GREEN else ORANGE); background = rounded(if (ok) 0xFFE3F7EA.toInt() else 0xFFFDEBD3.toInt(), 0, 10)
        })
        c.addView(row)
        c.addView(TextView(this).apply { text = desc; textSize = 14f; setTextColor(Color.GRAY); setPadding(0, dp(10), 0, 0) })
        if (ok) c.addView(TextView(this).apply { text = "✓ Permission active"; setTextColor(GREEN); setPadding(0, dp(10), 0, 0) })
        parent.addView(c, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(16) })
    }
}
