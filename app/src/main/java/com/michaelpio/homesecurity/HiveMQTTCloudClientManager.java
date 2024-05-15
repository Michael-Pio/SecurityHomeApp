package com.michaelpio.homesecurity;



import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import android.util.Log;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class HiveMQTTCloudClientManager {

    private Mqtt5AsyncClient client;
    public Set<String> m_subscription = new ArraySet<>();
    public Boolean isAppConnected = false;
    public Boolean isDeviceConnected = false;


    public Boolean setupClient() {
        final String host = "5ba3cc9682464c34888197ac60a3b249.s1.eu.hivemq.cloud";
        final String username = "Michael";
        final String password = "Pio@2004";
        try {
            client = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost(host)
                    .serverPort(8883)
                    .sslWithDefaultConfig()
                    .buildAsync();

            client.connectWith()
                    .simpleAuth()
                    .username(username)
                    .password(UTF_8.encode(password))
                    .applySimpleAuth()
                    .send()
                    .whenComplete((ack, ex) -> {
                        if (ex != null) {
                            ex.printStackTrace();
                            // Handle connection failure
                            isAppConnected = false;
                        } else {
                            System.out.println("Connected successfully");
                            // Handle connection success'
                            isAppConnected = true;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("setupClient : Internet Failure");
            // Handle client creation failure
        }
        return isAppConnected;
    }

    public boolean reconnect() {
        boolean isConnected = setupClient();
        if (isConnected) {
            // Check if m_subscription is null
            if (m_subscription != null) {
                // Resubscribe to topics
                for (String topic : m_subscription) {
                    Log.d(TAG, "reconnect: "+topic);
                    subscribeToTopic(topic);
                }
            } else {
                // Handle case where m_subscription is null
                // Log an error or perform any other necessary action
                // For example:

                Log.e(TAG, "m_subscription is null");
            }
        }
        return isConnected;
    }
    public void subscribeToTopic(String topic) {
        client.subscribeWith()
                .topicFilter(topic)
                .send()
                .whenComplete((ack, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                        // Handle subscription failure
                    } else {
                        // Handle subscription success
                        m_subscription.add(topic);
                    }
                });

    }

    public void setMessageHandler(MessageHandler messageHandler) {
        client.toAsync().publishes(ALL, publish -> {
            String message = UTF_8.decode(publish.getPayload().get()).toString();
            String from = publish.getTopic().toString();
            messageHandler.handleMessage(message,from);
        });
    }

    public void publishMessage(String topic, String message, Boolean isRetained) {
        client.publishWith()
                .topic(topic)
                .payload(UTF_8.encode(message))
                .retain(isRetained)
                .send()
                .whenComplete((ack, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                        // Handle publish failure
                    } else {
                        // Handle publish success
                    }
                });
    }

    public void disconnect() {
        client.disconnect();
    }
}
