const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.onItemStatusChanged = onDocumentUpdated("items/{itemId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();

  // If status changed to GIVEN
  if (before.status !== "GIVEN" && after.status === "GIVEN") {
    const db = getFirestore();
    const ownerId = after.ownerId;

    await db.doc(`users/${ownerId}`).update({
      givenCount: FieldValue.increment(1),
      karmaPoints: FieldValue.increment(10) // 10 points per item
    });
  }
});

exports.sendMessageNotification = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const message = event.data.data();
    const chatId = event.params.chatId;

    const db = getFirestore();
    const chatDoc = await db.doc(`chats/${chatId}`).get();
    const chat = chatDoc.data();
    if (!chat) return;

    const recipientIds = (chat.participants || []).filter(
      (p) => p !== message.senderId
    );
    if (recipientIds.length === 0) return;

    const senderDoc = await db.doc(`users/${message.senderId}`).get();
    const senderName = senderDoc.data()?.name || "Someone";

    const messaging = getMessaging();
    await Promise.all(
      recipientIds.map(async (recipientId) => {
        const userDoc = await db.doc(`users/${recipientId}`).get();
        const fcmToken = userDoc.data()?.fcmToken;
        if (!fcmToken) return;

        await messaging.send({
          token: fcmToken,
          notification: {
            title: senderName,
            body: message.content,
          },
          data: {
            chatId: chatId,
            type: "message",
          },
          android: {
            priority: "high",
            notification: { channelId: "messages" },
          },
        });
      })
    );
  }
);
