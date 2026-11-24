// ChatActivity.kt - चैट स्क्रीन + Settings (API key और MODEL यहीं बदलो)
package com.antigem.app
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : Activity() {
    private val history = JSONArray()
    private lateinit var list: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var chip: TextView
    private val prefs by lazy { getSharedPreferences("antigem", 0) }
    private fun model() = prefs.getString("model", "llama-3.3-70b-versatile")!!

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); setPadding(dp(8), dp(36), dp(8), dp(8)) }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        bar.addView(TextView(this).apply { text = "AntiGem"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        bar.addView(TextView(this).apply { text = "⚙"; textSize = 24f; setPadding(dp(12), 0, dp(12), 0); setOnClickListener { showSettings() } })
        root.addView(bar)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        chip = TextView(this).apply {
            textSize = 13f; setTextColor(ORANGE); setPadding(dp(14), dp(6), dp(14), dp(6))
            background = rounded(Color.WHITE, 0xFFE0E0E0.toInt(), 20); setOnClickListener { showSettings() }
        }
        root.addView(chip, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { setMargins(dp(8), dp(4), 0, dp(4)) })
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = rounded(Color.WHITE, 0xFFE0E0E0.toInt(), 28); setPadding(dp(12), dp(4), dp(8), dp(4)) }
        input = EditText(this).apply { hint = "Reply to Groq…"; background = null; maxLines = 5 }
        row.addView(input, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(Button(this).apply { text = "➤"; setTextColor(Color.WHITE); background = rounded(Color.BLACK, 0, 24); setOnClickListener { send() } },
            LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)
        refreshChip()
    }

    private fun refreshChip() { chip.text = "✦ Groq · ${model()} ⌄" }

    private fun add(text: String, user: Boolean): TextView {
        val t = TextView(this).apply {
            this.text = text; textSize = 16f; setTextColor(Color.BLACK); setTextIsSelectable(true)
            if (user) { background = rounded(GRAY_BUBBLE, 0, 20); setPadding(dp(16), dp(12), dp(16), dp(12)) }
            else setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        list.addView(t, LinearLayout.LayoutParams(if (user) WRAP_CONTENT else MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = dp(10); if (user) gravity = Gravity.END
        })
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return t
    }

    private fun send() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        val key = prefs.getString("key", "") ?: ""
        if (key.isEmpty()) { Toast.makeText(this, "पहले Settings में Groq API key डालो", Toast.LENGTH_LONG).show(); showSettings(); return }
        input.setText(""); add(text, true)
        history.put(JSONObject().put("role", "user").put("content", text))
        val reply = add("…", false)
        val m = model()
        Thread {
            val r = try { Groq.chat(key, m, history) } catch (e: Exception) { "Error: ${e.message}" }
            runOnUiThread {
                reply.text = r
                if (!r.startsWith("Error: ")) history.put(JSONObject().put("role", "assistant").put("content", r))
                else history.remove(history.length() - 1)
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }.start()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), 0) }
        val key = EditText(this).apply { hint = "Groq API key (gsk_...)"; setText(prefs.getString("key", "")); setSingleLine() }
        val mdl = EditText(this).apply { hint = "Model नाम"; setText(model()); setSingleLine() }
        box.addView(TextView(this).apply { text = "API key" }); box.addView(key)
        box.addView(TextView(this).apply { text = "Model (जैसे llama-3.3-70b-versatile)"; setPadding(0, dp(12), 0, 0) }); box.addView(mdl)
        AlertDialog.Builder(this).setTitle("Settings").setView(box)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("key", key.text.toString().trim())
                    .putString("model", mdl.text.toString().trim().ifEmpty { "llama-3.3-70b-versatile" }).apply()
                refreshChip()
            }.setNegativeButton("Cancel", null).show()
    }
}
