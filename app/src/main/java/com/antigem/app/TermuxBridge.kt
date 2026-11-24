// TermuxBridge.kt - Termux में कमांड चलाना और आउटपुट वापस लाना
package com.antigem.app
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast

val TERMUX_HINT = "The user's phone runs Termux (no root, no sudo). Give shell commands in fenced bash blocks, " +
    "ONE small step per block, never destructive, and wait for the output before the next step. " +
    "The user taps a button to run each block in Termux and the output is sent back to you. " +
    "For a full Linux: pkg install proot-distro, then proot-distro install debian, then proot-distro login debian -- <command>. " +
    "To build the APK, push the project to GitHub with git and let GitHub Actions build it."

object TermuxBridge {
    val callbacks = HashMap<Int, (Int, String) -> Unit>()
    private var next = 1
    const val PERM = "com.termux.permission.RUN_COMMAND"
    private const val HOME = "/data/data/com.termux/files/home"
    private const val BASH = "/data/data/com.termux/files/usr/bin/bash"

    fun run(a: Activity, cmd: String, cb: (Int, String) -> Unit) {
        if (a.checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED) {
            a.requestPermissions(arrayOf(PERM), 77)
            Toast.makeText(a, "परमिशन दो, फिर बटन दोबारा दबाओ", Toast.LENGTH_LONG).show()
            return
        }
        val id = next++
        callbacks[id] = cb
        val ri = Intent(a, TermuxResultReceiver::class.java).putExtra("id", id)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val pi = PendingIntent.getBroadcast(a, id, ri, flags)
        val i = Intent("com.termux.RUN_COMMAND").setClassName("com.termux", "com.termux.app.RunCommandService")
            .putExtra("com.termux.RUN_COMMAND_PATH", BASH)
            .putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", cmd))
            .putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME)
            .putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            .putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pi)
        try {
            if (Build.VERSION.SDK_INT >= 26) a.startForegroundService(i) else a.startService(i)
        } catch (e: Exception) {
            callbacks.remove(id)
            cb(-1, "Termux नहीं चला: ${e.message}")
        }
    }
}

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", 0)
        val cb = TermuxBridge.callbacks.remove(id) ?: return
        val b = i.getBundleExtra("result")
        if (b == null) { cb(-1, "Termux से जवाब नहीं आया"); return }
        val err = b.getString("stderr") ?: ""
        val msg = b.getString("errmsg") ?: ""
        val out = (if (msg.isNotEmpty()) msg + "\n" else "") + (b.getString("stdout") ?: "") +
            (if (err.isNotEmpty()) "\n[stderr]\n$err" else "")
        cb(b.getInt("exitCode", -1), out)
    }
}
