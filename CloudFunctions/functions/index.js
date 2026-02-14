
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

setGlobalOptions({maxInstances: 20});
setGlobalOptions({region: "europe-west1"});


// Inizializza l'app una sola volta
admin.initializeApp();

// WHen new SOS document is created (in SOS collection)
// - check timestamp (if too old it will not be discarded)
// - check state (new)
// - get FCM token of destination user
// - send notification to destination user

exports.checkNewSOS = onDocumentCreated("SOS/{sosId}", async (event) => {
  const snapshot = event.data;
  logger.info("SOS da inviare");
  if (!snapshot) {
    logger.error("Nessun dato trovato nel documento.");
    return;
  }
  const data = snapshot.data();
  const sosId = event.params.sosId;

  const state = data.state;
  const issued = data.issued;
  const destinationUid = data.destinationUser;


  if (state !== "ON") {
    logger.info(`SOS ${sosId} ignorato: Stato è ${state}`);
    return;
  }

  if (issued) {
    const oraAttuale = Date.now();
    const unOraFa = oraAttuale - (60 * 60 * 1000);
    const issuedMillis = issued.toMillis();

    if (issuedMillis < unOraFa) {
      logger.info(`SOS ${sosId} ignorato: troppo vecchio.`);
      return;
    }
  }

  try {
    const userDoc = await admin.firestore().collection("Users")
        .doc(destinationUid).get();

    if (!userDoc.exists) {
      logger.error(`Utente ${destinationUid} non trovato.`);
      return;
    }

    const fcmToken = userDoc.data().fcmToken;

    if (!fcmToken) {
      logger.error(`L'utente ${destinationUid} non ha un token FCM.`);
      return;
    }


    const message = {
      data: {
        sosId: sosId,
      },
      token: fcmToken,
      android: {
        priority: "high",
        ttl: 90,
      },
    };

    await admin.messaging().send(message);
    logger.info(`Notifica inviata con successo per SOS: ${sosId}`);
  } catch (error) {
    logger.error("CheckNewSOS - Errore durante l'invio della notifica:", error);
  }
});


exports.fireRideAction = onDocumentCreated("RideActions/{actionId}",
    async (event) => {
      const snapshot = event.data;
      if (!snapshot) {
        logger.error("Nessun dato trovato nel documento.");
        return;
      }

      const data = snapshot.data();
      const actionId = event.params.actionId;
      const rideid = data.rideid;
      const action = data.action;
      const issued = data.timestamp;
      const passengers = data.passengers;


      if ((action !== "Imbarco") && (action !== "Terminato")) {
        logger.info(`Ride action ${actionId} ignorata: action è ${action}`);
        return;
      }

      if (issued) {
        const nowHour = Date.now();
        const tenMinutesAgo = nowHour - (10 * 60 * 1000);
        const issuedMillis = issued.toMillis();

        if (issuedMillis < tenMinutesAgo) {
          logger.info(`Azione ${actionId} ignorata più vecchia di 10 minuti.`);
          return;
        }
      }

      try {
        for (const destinationUid of passengers) {
          const userDoc = await admin.firestore().collection("Users")
              .doc(destinationUid).get();

          if (!userDoc.exists) {
            logger.error(`Utente ${destinationUid} non trovato.`);
            continue;
          }

          const fcmToken = userDoc.data().fcmToken;

          if (!fcmToken) {
            logger.error(`L'utente ${destinationUid} non ha un token FCM.`);
            continue;
          }


          const message = {
            data: {
              rideId: rideid,
              action: action,
            },
            token: fcmToken,
            android: {
              priority: "high",
              ttl: 90,
            },
          };

          await admin.messaging().send(message);
          logger.info(`Notifica inviata con successo per action: ${actionId}`);
        }
      } catch (error) {
        logger.error("FireRideAction - Errore invio notifica:", error);
      }
    });
