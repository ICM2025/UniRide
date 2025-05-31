const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendCustomNotification = functions.https.onRequest(async (req, res) => {
  const { token, type, fromName } = req.body;

  if (!token || !type || !fromName) {
    return res.status(400).json({ error: "Faltan campos requeridos" });
  }

  let title = "";
  let body = "";

  switch (type) {
    case "solicitud_cupo":
      title = "Nueva solicitud de cupo";
      body = `${fromName} te ha solicitado un cupo.`;
      break;
    case "aceptado":
      title = "Solicitud aceptada";
      body = `${fromName} aceptó tu solicitud de cupo.`;
      break;
    case "rechazado":
      title = "Solicitud rechazada";
      body = `${fromName} rechazó tu solicitud de cupo.`;
      break;
    default:
      title = "Notificación";
      body = `Tienes una nueva notificación de ${fromName}`;
  }

  const message = {
    notification: { title, body },
    data: { type },
    token,
  };

  try {
    const response = await admin.messaging().send(message);
    console.log("Notificación enviada:", response);
    return res.status(200).json({ success: true });
  } catch (error) {
    console.error("Error al enviar notificación:", error);
    return res.status(500).json({ error: "Error al enviar notificación" });
  }
});
