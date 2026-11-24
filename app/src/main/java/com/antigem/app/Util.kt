// Util.kt - रंग और छोटे helper (dp, गोल कोने वाला बैकग्राउंड)
package com.antigem.app
import android.content.Context
import android.graphics.drawable.GradientDrawable

val ORANGE = 0xFFD97706.toInt()
val BG = 0xFFF8F9FA.toInt()
val GREEN = 0xFF22C55E.toInt()
val GREEN_LIGHT = 0xFFA7E8C3.toInt()
val GRAY_BUBBLE = 0xFFE4E6EA.toInt()

fun Context.dp(i: Int) = (i * resources.displayMetrics.density).toInt()
fun Context.rounded(fill: Int, stroke: Int = 0, r: Int = 16) = GradientDrawable().apply {
    setColor(fill); cornerRadius = dp(r).toFloat()
    if (stroke != 0) setStroke(dp(1), stroke)
}
