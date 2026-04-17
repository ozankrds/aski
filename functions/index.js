const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

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
