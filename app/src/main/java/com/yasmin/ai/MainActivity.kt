package com.yasmin.ai

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val messages = mutableListOf<Msg>()
    private lateinit var adapter: ChatAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var input: EditText
    private lateinit var send: Button

    private val prefs by lazy { getSharedPreferences("yasmin", MODE_PRIVATE) }
    private var apiKey: String
        get() = prefs.getString("key", "") ?: ""
        set(v) = prefs.edit().putString("key", v).apply()
    private var model: String
        get() = prefs.getString("model", "claude-sonnet-5-5") ?: "claude-sonnet-5-5"
        set(v) = prefs.edit().putString("model", v).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recycler)
        input = findViewById(R.id.input)
        send = findViewById(R.id.btnSend)
        adapter = ChatAdapter(messages)
        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettings() }
        send.setOnClickListener { onSend() }

        if (apiKey.isBlank()) showSettings()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 0) }
        val keyField = EditText(this).apply { hint = "API key"; setText(apiKey) }
        val modelField = EditText(this).apply { hint = "Model"; setText(model) }
        box.addView(keyField); box.addView(modelField)
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                apiKey = keyField.text.toString().trim()
                model = modelField.text.toString().trim().ifEmpty { "claude-sonnet-5-5" }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onSend() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        if (apiKey.isBlank()) { showSettings(); return }
        input.setText("")
        add(Msg("user", text))
        send.isEnabled = false

        val history = messages.toList()
        Thread {
            val reply = try { callApi(history) } catch (e: Exception) { "Error: ${e.message}" }
            runOnUiThread {
                add(Msg("assistant", reply))
                send.isEnabled = true
            }
        }.start()
    }

    private fun add(m: Msg) {
        messages.add(m)
        adapter.notifyItemInserted(messages.size - 1)
        recycler.scrollToPosition(messages.size - 1)
    }

    private fun callApi(history: List<Msg>): String {
        val arr = JSONArray()
        history.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.text)) }
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1024)
            .put("system", "You are Yasmin, a friendly and helpful AI assistant. Reply in the user's language.")
            .put("messages", arr)

        val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) {
            val msg = try { JSONObject(resp).getJSONObject("error").getString("message") } catch (e: Exception) { resp }
            return "API error ($code): $msg"
        }
        val content = JSONObject(resp).getJSONArray("content")
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val b = content.getJSONObject(i)
            if (b.optString("type") == "text") sb.append(b.getString("text"))
        }
        return sb.toString().ifEmpty { "(empty reply)" }
    }
}
