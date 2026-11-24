// ChatActivity.kt - चैट स्क्रीन: सेव होने वाली चैट, ☰ चैट सूची, फ़ॉर्मैटेड जवाब, Settings
package com.antigem.app
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var chip: TextView
    private lateinit var titleView: TextView
    private lateinit var cur: JSONObject
    private var chats = JSONArray()
    private var busy = false
    private val prefs by lazy { getSharedPreferences("antigem", 0) }
    private fun model() = prefs.getString("model", DEFAULT_MODEL)!!

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); setPadding(dp(8), dp(36), dp(8), dp(8)) }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(8), dp(4), dp(8)) }
        fun icon(t: String, f: () -> Unit) = TextView(this).apply {
            text = t; textSize = 24f; setPadding(dp(12), 0, dp(12), 0); setOnClickListener { f() }
        }
        bar.addView(icon("☰") { showChats() })
        titleView = TextView(this).apply {
            textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK)
            isSingleLine = true; ellipsize = TextUtils.TruncateAt.END
        }
        bar.addView(titleView, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        bar.addView(icon("✚") { cur = newChat(); prefs.edit().putLong("cur", cur.getLong("id")).apply(); renderAll() })
        bar.addView(icon("⚙") { showSettings() })
        root.addView(bar)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        chip = TextView(this).apply {
            textSize = 13f; setTextColor(ORANGE); setPadding(dp(14), dp(6), dp(14), dp(6))
            background = rounded(Color.WHITE, 0xFFE0E0E0.toInt(), 20); setOnClickListener { showSettings() }
        }
        root.addView(chip, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { setMargins(dp(8), dp(4), 0, dp(4)) })
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; background = rounded(Color.WHITE, 0xFFE0E0E0.toInt(), 28); setPadding(dp(12), dp(4), dp(8), dp(4))
        }
        input = EditText(this).apply { hint = "Reply to Groq…"; background = null; maxLines = 5 }
        row.addView(input, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(Button(this).apply { text = "➤"; setTextColor(Color.WHITE); background = rounded(Color.BLACK, 0, 24); setOnClickListener { send() } },
            LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)

        chats = try { JSONArray(prefs.getString("chats", "[]")) } catch (e: Exception) { JSONArray() }
        val id = prefs.getLong("cur", 0L)
        cur = (0 until chats.length()).map { chats.getJSONObject(it) }.firstOrNull { it.getLong("id") == id } ?: newChat()
        renderAll(); refreshChip()
    }

    private fun refreshChip() { chip.text = "✦ Groq · ${model()} ⌄" }
    private fun newChat() = JSONObject().put("id", System.currentTimeMillis()).put("title", "New chat").put("msgs", JSONArray())
    private fun down() { scroll.post { scroll.fullScroll(View.FOCUS_DOWN) } }

    private fun save() {
        val has = (0 until chats.length()).any { chats.getJSONObject(it).getLong("id") == cur.getLong("id") }
        if (!has && cur.getJSONArray("msgs").length() > 0) chats.put(cur)
        prefs.edit().putString("chats", chats.toString()).putLong("cur", cur.getLong("id")).apply()
    }

    private fun renderAll() {
        list.removeAllViews(); titleView.text = cur.getString("title")
        val m = cur.getJSONArray("msgs")
        for (i in 0 until m.length()) {
            val o = m.getJSONObject(i)
            if (o.getString("role") == "user") user(o.getString("content")) else bot(o.getString("content"), o.optString("meta", ""))
        }
        down()
    }

    private fun user(text: String) {
        val t = TextView(this).apply {
            this.text = text; textSize = 16f; setTextColor(Color.BLACK); setTextIsSelectable(true)
            background = rounded(GRAY_BUBBLE, 0, 20); setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        list.addView(t, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { bottomMargin = dp(12); gravity = Gravity.END })
        down()
    }

    private fun bot(text: String, meta: String): Pair<LinearLayout, TextView> {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        Markdown.render(this, text, box)
        val st = TextView(this).apply {
            textSize = 12f; setTextColor(Color.GRAY); setPadding(dp(10), dp(6), dp(10), dp(6))
            background = rounded(0xFFEDEFF2.toInt(), 0, 10); this.text = meta; tag = text
            visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE
            setOnClickListener { Markdown.copy(this@ChatActivity, tag as String) }
        }
        wrap.addView(box)
        wrap.addView(st, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        list.addView(wrap, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(16) })
        down()
        return Pair(box, st)
    }

    private fun send() {
        if (busy) return
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        val key = prefs.getString("key", "") ?: ""
        if (key.isEmpty()) { Toast.makeText(this, "पहले Settings में Groq API key डालो", Toast.LENGTH_LONG).show(); showSettings(); return }
        input.setText("")
        val msgs = cur.getJSONArray("msgs")
        msgs.put(JSONObject().put("role", "user").put("content", text))
        if (msgs.length() == 1) cur.put("title", text.take(28))
        save(); titleView.text = cur.getString("title")
        user(text)
        val (box, st) = bot("…", "")
        val hist = JSONArray()
        for (i in 0 until msgs.length()) {
            val m = msgs.getJSONObject(i)
            hist.put(JSONObject().put("role", m.getString("role")).put("content", m.getString("content")))
        }
        busy = true
        val mdl = model(); val sys = prefs.getString("sys", DEFAULT_SYS)!!; val t0 = System.currentTimeMillis()
        Thread {
            var r: Reply? = null; var err = ""
            try { r = Groq.chat(key, mdl, sys, hist) } catch (e: Exception) { err = e.message ?: "error" }
            val res = r
            val sec = (System.currentTimeMillis() - t0) / 100 / 10.0
            runOnUiThread {
                busy = false
                if (res != null) {
                    val meta = "↑${res.pt}  ↓${res.ct}  ⏱ ${sec}s   ⧉ Copy"
                    Markdown.render(this, res.text, box)
                    st.tag = res.text; st.text = meta; st.visibility = View.VISIBLE
                    msgs.put(JSONObject().put("role", "assistant").put("content", res.text).put("meta", meta))
                    save()
                } else Markdown.render(this, "⚠️ $err", box)
                down()
            }
        }.start()
    }

    private fun deleteChat(c: JSONObject) {
        for (i in 0 until chats.length()) if (chats.getJSONObject(i).getLong("id") == c.getLong("id")) { chats.remove(i); break }
        if (cur.getLong("id") == c.getLong("id")) cur = newChat()
        save(); renderAll()
    }

    private fun showChats() {
        val items = (chats.length() - 1 downTo 0).map { chats.getJSONObject(it) }
        val lv = ListView(this)
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.map { it.getString("title") })
        val dlg = AlertDialog.Builder(this).setTitle("चैट्स (देर तक दबाओ = मिटाओ)").setView(lv)
            .setPositiveButton("✚ नई चैट") { _, _ -> cur = newChat(); prefs.edit().putLong("cur", cur.getLong("id")).apply(); renderAll() }
            .setNegativeButton("बंद", null).create()
        lv.setOnItemClickListener { _, _, p, _ ->
            cur = items[p]; prefs.edit().putLong("cur", cur.getLong("id")).apply(); renderAll(); dlg.dismiss()
        }
        lv.setOnItemLongClickListener { _, _, p, _ ->
            AlertDialog.Builder(this).setMessage("यह चैट मिटाएँ?")
                .setPositiveButton("मिटाओ") { _, _ -> deleteChat(items[p]); dlg.dismiss() }
                .setNegativeButton("नहीं", null).show()
            true
        }
        dlg.show()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), 0) }
        val key = EditText(this).apply { hint = "Groq API key (gsk_...)"; setText(prefs.getString("key", "")); setSingleLine() }
        val mdl = EditText(this).apply { hint = "Model नाम"; setText(model()); setSingleLine() }
        val sys = EditText(this).apply {
            setText(prefs.getString("sys", DEFAULT_SYS)); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; maxLines = 4
        }
        box.addView(TextView(this).apply { text = "API key" }); box.addView(key)
        box.addView(TextView(this).apply { text = "Model (जैसे llama-3.3-70b-versatile)"; setPadding(0, dp(12), 0, 0) }); box.addView(mdl)
        box.addView(TextView(this).apply { text = "System prompt"; setPadding(0, dp(12), 0, 0) }); box.addView(sys)
        AlertDialog.Builder(this).setTitle("Settings").setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("key", key.text.toString().trim())
                    .putString("model", mdl.text.toString().trim().ifEmpty { DEFAULT_MODEL })
                    .putString("sys", sys.text.toString().trim().ifEmpty { DEFAULT_SYS }).apply()
                refreshChip()
            }.setNegativeButton("Cancel", null).show()
    }
}
