package com.example.data

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    suspend fun generateContent(
        systemInstruction: String,
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "{\"reply\": \"Error: Gemini API Key is missing. Please set it in the AI Studio Secrets panel as GEMINI_API_KEY.\"}"
        }

        val url = "$BASE_URL?key=$apiKey"

        val requestJson = JSONObject()

        // 1. System Instruction
        val systemInstructionObj = JSONObject()
        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", systemInstruction))
        systemInstructionObj.put("parts", partsArray)
        requestJson.put("systemInstruction", systemInstructionObj)

        // 2. Contents (History + Current Prompt)
        val contentsArray = JSONArray()

        // Add history turns
        for (turn in history) {
            val userTurn = JSONObject()
            userTurn.put("role", "user")
            userTurn.put("parts", JSONArray().put(JSONObject().put("text", turn.first)))
            contentsArray.put(userTurn)

            val modelTurn = JSONObject()
            modelTurn.put("role", "model")
            modelTurn.put("parts", JSONArray().put(JSONObject().put("text", turn.second)))
            contentsArray.put(modelTurn)
        }

        // Add current user prompt
        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        currentTurn.put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        contentsArray.put(currentTurn)

        requestJson.put("contents", contentsArray)

        // 3. Generation Config (Optional, force JSON output format)
        val generationConfig = JSONObject()
        generationConfig.put("temperature", 0.7)
        
        // Request structured JSON response
        val responseFormat = JSONObject()
        responseFormat.put("mimeType", "application/json")
        generationConfig.put("responseFormat", responseFormat)
        
        requestJson.put("generationConfig", generationConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return "{\"reply\": \"Error calling Gemini API: HTTP ${response.code} - $errBody\"}"
                }
                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return parts.getJSONObject(0).optString("text", "{\"reply\": \"Error: Empty response parts.\"}")
                        }
                    }
                }
                "{\"reply\": \"Error: No output from Gemini API.\"}"
            }
        } catch (e: Exception) {
            "{\"reply\": \"Network Error: ${e.message}\"}"
        }
    }
}
