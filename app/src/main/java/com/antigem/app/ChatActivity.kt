// Markdown.kt - Groq के Markdown जवाब को सुंदर रूप में दिखाना (बोल्ड, बुलेट, कोड, टेबल)
package com.antigem.app
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

object Markdown {
    private const val SP = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    private val FENCE = "`".repeat(3)
    private val bulRe = Regex("^[-*•]\\s+(.*)")
    private val numRe = Regex("^(\\d+)[.)]\\s+(.*)")
    private val sepRe = Regex(":?-{2,}:?")

    fun inline(src: String): SpannableStringBuilder {
        val s = src.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
        val out = SpannableStringBuilder()
        var i = 0
        while (i < s.length) {
            if (s.startsWith("**", i)) {
                val e = s.indexOf("**", i + 2)
                if (e > 0) {
                    val st = out.length
                    out.append(inline(s.substring(i + 2, e)))
                    out.setSpan(StyleSpan(Typeface.BOLD), st, out.length, SP)
                    i = e + 2; continue
                }
            }
            if (s[i] == '`') {
                val e = s.indexOf('`', i + 1)
                if (e > 0) {
                    val st = out.length
                    out.append(" " + s.substring(i + 1, e) + " ")
                    out.setSpan(TypefaceSpan("monospace"), st, out.length, SP)
                    out.setSpan(ForegroundColorSpan(ORANGE), st, out.length, SP)
                    out.setSpan(BackgroundColorSpan(0xFFFDEBD3.toInt()), st, out.length, SP)
                    i = e + 1; continue
                }
            }
            out.append(s[i]); i++
        }
        return out
    }

    fun copy(c: Context, s: String) {
        (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("antigem", s))
        Toast.makeText(c, "Copied ✓", Toast.LENGTH_SHORT).show()
    }

    fun render(c: Context, text: String, box: LinearLayout) {
        box.removeAllViews()
        val lines = text.split("\n")
        var i = 0
        while (i < lines.size) {
            val t = lines[i].trim()
            val bm = bulRe.matchEntire(t)
            val nm = numRe.matchEntire(t)
            when {
                t.startsWith(FENCE) -> {
                    val sb = StringBuilder(); i++
                    while (i < lines.size && !lines[i].trim().startsWith(FENCE)) { sb.append(lines[i]).append("\n"); i++ }
                    code(c, box, sb.toString().trimEnd())
                }
                t.startsWith("|") -> {
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        val r = lines[i].trim().trim('|').split("|").map { it.trim() }
                        if (!r.all { it.matches(sepRe) }) rows.add(r)
                        i++
                    }
                    i--
                    table(c, box, rows)
                }
                t.startsWith("#") -> {
                    val lvl = t.takeWhile { it == '#' }.length
                    tv(c, box, inline(t.drop(lvl).trim()), if (lvl == 1) 22f else if (lvl == 2) 20f else 18f, 14, 0, true)
                }
                bm != null -> {
                    val b = SpannableStringBuilder("• ")
                    b.setSpan(ForegroundColorSpan(ORANGE), 0, 1, SP)
                    b.append(inline(bm.groupValues[1]))
                    tv(c, box, b, 16f, 4, 8, false)
                }
                nm != null -> {
                    val b = SpannableStringBuilder(nm.groupValues[1] + ". ")
                    b.setSpan(ForegroundColorSpan(ORANGE), 0, b.length, SP)
                    b.append(inline(nm.groupValues[2]))
                    tv(c, box, b, 16f, 4, 8, false)
                }
                t.isEmpty() -> {}
                else -> tv(c, box, inline(t), 16f, 6, 0, false)
            }
            i++
        }
    }

    private fun tv(c: Context, box: LinearLayout, s: CharSequence, size: Float, top: Int, left: Int, bold: Boolean) {
        box.addView(TextView(c).apply {
            text = s; textSize = size; setTextColor(Color.BLACK); setTextIsSelectable(true)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(c.dp(left), c.dp(top), 0, 0); setLineSpacing(0f, 1.15f)
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun code(c: Context, box: LinearLayout, s: String) {
        val w = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = c.rounded(0xFF1F2430.toInt(), 0, 12)
            setPadding(c.dp(12), c.dp(6), c.dp(12), c.dp(12))
        }
        w.addView(TextView(c).apply {
            text = "⧉ Copy"; textSize = 12f; setTextColor(0xFFB0B7C3.toInt()); gravity = Gravity.END
            setPadding(0, c.dp(4), 0, c.dp(6)); setOnClickListener { copy(c, s) }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        w.addView(HorizontalScrollView(c).apply {
            addView(TextView(c).apply {
                text = s; textSize = 13f; typeface = Typeface.MONOSPACE
                setTextColor(0xFFE6EDF3.toInt()); setTextIsSelectable(true)
            })
        })
        box.addView(w, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = c.dp(8); bottomMargin = c.dp(4) })
    }

    private fun table(c: Context, box: LinearLayout, rows: List<List<String>>) {
        val tl = TableLayout(c)
        rows.forEachIndexed { ri, r ->
            val tr = TableRow(c)
            r.forEach { cell ->
                tr.addView(TextView(c).apply {
                    text = inline(cell); textSize = 14f; setTextColor(Color.BLACK); maxWidth = c.dp(240)
                    setPadding(c.dp(10), c.dp(8), c.dp(10), c.dp(8))
                    background = c.rounded(if (ri == 0) 0xFFF1F3F5.toInt() else Color.WHITE, 0xFFDDDDDD.toInt(), 0)
                    if (ri == 0) typeface = Typeface.DEFAULT_BOLD
                })
            }
            tl.addView(tr)
        }
        box.addView(HorizontalScrollView(c).apply { addView(tl) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = c.dp(8); bottomMargin = c.dp(8) })
    }
}
