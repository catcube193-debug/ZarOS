package com.zaros.app.backend;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.core.app.NotificationCompat;

import com.zaros.app.R;
import com.zaros.app.ui.ConversationActivity;

public class SmsReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "zaros_sms";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        StringBuilder body = new StringBuilder();
        String sender = "";

        for (Object pdu : pdus) {
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
            sender = msg.getDisplayOriginatingAddress();
            body.append(msg.getDisplayMessageBody());
        }

        // Get contact name
        String name = ContactsManager.getNameForNumber(context, sender);

        // Play message sound
        SoundManager.init(context);
        SoundManager.playMessage(context);

        // Show notification
        showNotification(context, sender, name, body.toString());
    }

    private void showNotification(Context ctx, String address, String name, String body) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        // Create channel
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "ZarOS Messages", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(ch);

        // Open conversation on tap
        Intent intent = new Intent(ctx, ConversationActivity.class);
        intent.putExtra(ConversationActivity.EXTRA_ADDRESS, address);
        intent.putExtra(ConversationActivity.EXTRA_NAME, name);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, address.hashCode(),
            intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notif = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle(name)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_app_messages)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        nm.notify(address.hashCode(), notif.build());
    }
}
