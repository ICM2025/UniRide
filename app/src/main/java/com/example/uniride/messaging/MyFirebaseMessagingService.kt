package com.example.uniride.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.uniride.R
import com.example.uniride.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: "Notificación"
        val body = remoteMessage.notification?.body ?: "Tienes una nueva notificación"

        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).update("token", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token actualizado en Firestore")
                }
                .addOnFailureListener {
                    Log.e("FCM", "Error actualizando token: ${it.message}")
                }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "default_channel"

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_car) // Usa un ícono válido
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones generales",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(0, notification)
    }
}

