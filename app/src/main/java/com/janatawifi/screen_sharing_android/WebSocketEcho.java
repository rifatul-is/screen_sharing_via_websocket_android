package com.janatawifi.screen_sharing_android;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketEcho extends WebSocketListener {
    private OkHttpClient client;
    private WebSocket webSocket; // Hold a reference to the WebSocket

    public void run(String url) {
        client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        webSocket = client.newWebSocket(request, this);

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        //client.dispatcher().executorService().shutdown();
    }

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        // Connection opened, you can now send messages
        Log.d("TAG", "onOpen: Sending Test");
        //webSocket.send("Hello, World This is Socket Test!");

        //webSocket.send(ByteString.decodeHex("deadbeef"));
        //webSocket.close(1000, "Goodbye, World!");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        // Received a message from the server
        System.out.println("MESSAGE: " + text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        // Received a binary message from the server
        System.out.println("MESSAGE: " + bytes.hex());
    }

    public void sendMessage(String message) {
        if (this.webSocket != null) {
            Log.d("TAG", "sendMessage: " + message.length());
            this.webSocket.send(message);
        } else {
            System.err.println("WebSocket is not connected.");
        }
    }

    // Define a method to send messages
    // Overloaded method to send binary messages
    public void sendMessage(byte[] data) {
        if (this.webSocket != null) {
            //ByteString byteString = ByteString.of(data);
            this.webSocket.send(Arrays.toString(data));
        } else {
            System.err.println("WebSocket is not connected.");
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(1000, null);
        System.out.println("CLOSE: " + code + " " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        t.printStackTrace();
    }
}
