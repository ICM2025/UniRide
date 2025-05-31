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
import com.example.uniride.ui.driver.drawer.DriverDrawerFlowActivity
import com.example.uniride.ui.passenger.drawer.PassengerDrawerFlowActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d("FCM", "Notificación ignorada: usuario no autenticado")
            return
        }
        val title = remoteMessage.notification?.title ?: "Notificación"
        val body = remoteMessage.notification?.body ?: "Tienes una nueva notificación"
        val type = remoteMessage.data["type"] ?: "default"

        showNotification(title, body, type)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")

        val uid = FirebaseAuth.getInstance().currentUser?.uid
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

    private fun showNotification(title: String, message: String, type: String) {
        val channelId = "default_channel"

        // Determinar el destino en el NavGraph según el tipo de notificación
        val destination = when (type) {
            "aceptado", "rechazado" -> R.id.passengerRequestsFragment
            "solicitud_cupo" -> R.id.tripRequestsFragment
            else -> R.id.passengerHomeFragment
        }

        // Ir siempre a MainActivity con el destino deseado como extra
        val intent = Intent(this, com.example.uniride.ui.MainActivity::class.java).apply {
            putExtra("destinationFromNotification", destination)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_car)
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
