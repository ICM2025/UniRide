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
    case "mensaje":
      title = `${fromName} te envió un mensaje`;
      body = req.body.preview?.substring(0, 100) || "Nuevo mensaje";
      break;
    case "viaje_iniciado":
      title = "¡Tu viaje ha comenzado!";
      body = `${fromName} ha iniciado el viaje.`;
      break;
    case "viaje_terminado":
      title = "¡Tu viaje ha finalizado!";
      body = `${fromName} ha finalizado el viaje. ¡Gracias por viajar con UniRide!`;
      break;
    case "viaje_cancelado":
      title = "¡Tu viaje ha sido cancelado!";
      body = `${fromName} canceló el viaje que tenias asignado`;
      break;
    default:
      title = "Notificación";
      body = `Tienes una nueva notificación de ${fromName}`;
  }
  const message = {
  notification: { title, body },
  data: {
    type,
    receiverId: req.body.receiverId || "",
    receiverName: req.body.fromName || "",
    preview: req.body.preview || ""
  },
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
