// Groq.kt - Groq API से बात (chat completions endpoint)
package com.antigem.app
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Groq {
    fun chat(key: String, model: String, msgs: JSONArray): String {
        val c = URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.connectTimeout = 20000; c.readTimeout = 60000; c.doOutput = true
        c.setRequestProperty("Authorization", "Bearer $key")
        c.setRequestProperty("Content-Type", "application/json")
        val body = JSONObject().put("model", model).put("messages", msgs).toString()
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val s = (if (code < 400) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code >= 400) throw Exception("HTTP $code: $s")
        return JSONObject(s).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
    }
}
