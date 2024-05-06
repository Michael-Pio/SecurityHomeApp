package com.michaelpio.homesecurity;

public interface MessageHandler {
    void handleMessage(String message, String topic);
}