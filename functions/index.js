const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendTestNotification = functions.https.onRequest(async (req, res) => {
  const token = req.query.token;

  if (!token) {
    return res.status(400).send("Falta el token en la query");
  }

  const message = {
    notification: {
      title: "Notificación de prueba",
      body: "¡Hola! Esta es una prueba FCM 📲",
    },
    token: token,
  };

  try {
    const response = await admin.messaging().send(message);
    console.log("Notificación enviada:", response);
    res.send("Notificación enviada con éxito");
  } catch (error) {
    console.error("Error enviando notificación:", error);
    res.status(500).send("Error al enviar notificación");
  }
});
