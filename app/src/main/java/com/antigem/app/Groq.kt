// Groq.kt - Groq API से बात + जवाब और टोकन की जानकारी
package com.antigem.app
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
const val DEFAULT_SYS = "You are AntiGem, a helpful assistant. Reply in the user's language. Use simple Markdown only: **bold**, bullet lists starting with '-', short headings with '#', and fenced code blocks. Never use HTML tags like <br>."

class Reply(val text: String, val pt: Int, val ct: Int)

object Groq {
    fun chat(key: String, model: String, sys: String, msgs: JSONArray): Reply {
        val all = JSONArray().put(JSONObject().put("role", "system").put("content", sys))
        for (i in 0 until msgs.length()) all.put(msgs.getJSONObject(i))
        val c = URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.connectTimeout = 20000; c.readTimeout = 90000; c.doOutput = true
        c.setRequestProperty("Authorization", "Bearer $key")
        c.setRequestProperty("Content-Type", "application/json")
        val body = JSONObject().put("model", model).put("messages", all).toString()
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val s = (if (code < 400) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code >= 400) throw Exception("HTTP $code: $s")
        val j = JSONObject(s)
        val text = j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "")
        val u = j.optJSONObject("usage")
        return Reply(text, u?.optInt("prompt_tokens") ?: 0, u?.optInt("completion_tokens") ?: 0)
    }
}
