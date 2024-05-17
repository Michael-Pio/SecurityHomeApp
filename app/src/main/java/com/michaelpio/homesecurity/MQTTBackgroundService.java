package com.michaelpio.homesecurity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MQTTBackgroundService extends Service {
    private HiveMQTTCloudClientManager mqttClientManager;
    DeviceState deviceState;
    Handler handler = new Handler();
    private static final String CHANNEL_ID = "MQTTServiceChannel";
    private static final long NOTIFICATION_INTERVAL = 1 * 60 * 1000; // 1 minutes in milliseconds

    @Override
    public void onCreate() {
        super.onCreate();
        deviceState = new DeviceState();

        mqttClientManager = new HiveMQTTCloudClientManager();
        mqttClientManager.setupClient();
        mqttClientManager.subscribeToTopic("MainGate001/Device");
        mqttClientManager.subscribeToTopic("MainGate001/Device/State");
        mqttClientManager.subscribeToTopic("MainGate001/Device/Intruder");
        mqttClientManager.setMessageHandler(getMessageHandler());

        createNotificationChannel();
        startPeriodicNotifications();
    }
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "MQTT Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the MQTT client
        mqttClientManager.reconnect();
        mqttClientManager.setMessageHandler(getMessageHandler());

        // Return START_STICKY to ensure the service is restarted if it is killed
        return START_STICKY;
    }
    private void showNotification(String title, String message) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.intruder_alert)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }
    private void showNotification(String title, String message, String additionalInfo) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n" + additionalInfo))
                .setSmallIcon(R.drawable.intruder_alert)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    private void startPeriodicNotifications() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String additionalInfo = "Temperature: " + deviceState.getTemperature().getValue() + "°C\n" +
                        "Humidity: " + deviceState.getHumidity().getValue() + "%\n" +
                        "System Armed: " + (deviceState.getIsArmed().getValue() ? "Yes" : "No") + "\n" +
                        "Intruder Detected: " + (deviceState.getIsIntruderDetected().getValue() ? "Yes" : "No");

                showNotification("Device State Updated", "Device State", additionalInfo);
                handler.postDelayed(this, NOTIFICATION_INTERVAL);
            }
        }, NOTIFICATION_INTERVAL);
    }

    @NonNull
    private MessageHandler getMessageHandler() {
        return new MessageHandler() {
            @Override
            public void handleMessage(String message, String topic) {
                System.out.println("TOPIC IN : " + topic + "\nMESSAGE IN : " + message);
                Handler mainLoophandler = new Handler(Looper.getMainLooper());//To Update UI elements in MainActivity

                switch (topic) {
                    case "MainGate001/Device":
                        DeviceMessageHandler(message, mainLoophandler);
                        break;
                    case "MainGate001/Device/State":
                        DeviceStateMessageHandler(message, mainLoophandler);
                        break;
                    case "MainGate001/Device/Intruder":
                        DeviceIntruderMessageHandler(message, mainLoophandler);
                        break;
                }
            }

            private void DeviceIntruderMessageHandler(String message, Handler mainLoophandler) {

                mainLoophandler.post(() -> {
                    if (message.equals("Threat Detected")) {
                        deviceState.setIsIntruderDetected(true);
                        showNotification("Security Alert", "Possible Intruder Detected at Main Gate");
                    } else {
                        deviceState.setIsIntruderDetected(false);
                        showNotification("Security Update", "No Intruder Detected at Main Gate");
                    }
                });

            }

            private void DeviceStateMessageHandler(String message, Handler mainLoophandler) {
                String[] elements = message.split(",");
                mainLoophandler.post(() -> {
                    if (elements.length >= 4) {
                        deviceState.setTemperature(Integer.parseInt(elements[0]));
                        deviceState.setHumidity(Integer.parseInt(elements[1]));
                        deviceState.setIsArmed(Integer.parseInt(elements[2]) != 0);
                        deviceState.setIsIntruderDetected(Integer.parseInt(elements[3]) != 0);


                    } else if (message.equals("Alive")) {
                        deviceState.setIsDeviceConnected(true);
                    }
                });

            }

            private void DeviceMessageHandler(String message, Handler mainLoophandler) {
                mainLoophandler.post(() -> {
                    if (message.equals("Alive")) {
                        deviceState.setIsDeviceConnected(true);
                    }
                });
            }
        };
    }

    public void onDestroy() {
        super.onDestroy();

        // Disconnect from the MQTT broker
        mqttClientManager.disconnect();
    }
}
