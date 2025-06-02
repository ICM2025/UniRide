package com.example.uniride.messaging

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object NotificationSender {

    private const val FUNCTION_URL = "https://us-central1-uniride-fed59.cloudfunctions.net/sendCustomNotification"
    private val client = OkHttpClient()

    fun enviar(tokenDestino: String, tipo: String, fromName: String, receiverId: String? = null, receiverName: String? = null, preview: String? = null, onResult: (success: Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("token", tokenDestino)
            put("type", tipo)
            put("fromName", fromName)
            receiverId?.let{ put("receiverId", it)}
            receiverName?.let{ put("receiverName", it)}
            preview?.let{ put("preview", it)}
            Log.e("datos", "tipo: $tipo, token: $tokenDestino, FromName: $fromName")
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(FUNCTION_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error:","Error al enviar notificación: ${e.message}")
                onResult(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("Correcto:","Notificación enviada correctamente")
                    onResult(true)
                } else {
                    Log.e("Error:","Error en la respuesta: ${response.code}")
                    onResult(false)
                }
            }
        })
    }
}

