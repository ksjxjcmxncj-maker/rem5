package com.nro.bridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * NRO WebSocket Bridge Service
 * Lắng nghe TCP 127.0.0.1:14445 → forward qua WebSocket đến Codespace/Replit
 *
 * CÁCH DÙNG:
 * 1. Cài APK này, mở app, bấm Start
 * 2. Trong game NRO → chọn Custom Server → nhập 127.0.0.1:14445
 * 3. Kết nối!
 */
public class BridgeService extends Service {
    private static final String TAG = "NROBridge";
    private static final int LOCAL_PORT = 14445;
    private static final String CHANNEL_ID = "nro_bridge";
    private static final int NOTIF_ID = 1;

    // ═══════════════════════════════════════════════════════
    // ⚙️  SỬA URL NÀY thành Codespace WebSocket URL của bạn
    // Ví dụ: wss://cautious-space-halibut-p7rwgqwxrg5gfrrqg-8080.app.github.dev
    // Hoặc Replit: wss://your-repl.replit.dev/ws
    // ═══════════════════════════════════════════════════════
    public static String WS_URL = "wss://CHANGE_THIS.app.github.dev";

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newCachedThreadPool();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("WS_URL")) {
            WS_URL = intent.getStringExtra("WS_URL");
        }

        if (!running) {
            running = true;
            startForeground(NOTIF_ID, buildNotification("Đang chạy — localhost:" + LOCAL_PORT + " → WebSocket"));
            executor.submit(this::acceptLoop);
            Log.i(TAG, "Bridge started: TCP " + LOCAL_PORT + " → " + WS_URL);
        }
        return START_STICKY;
    }

    private void acceptLoop() {
        try {
            InetAddress localhost = InetAddress.getByName("127.0.0.1");
            serverSocket = new ServerSocket(LOCAL_PORT, 50, localhost);
            Log.i(TAG, "Listening on 127.0.0.1:" + LOCAL_PORT);

            while (running) {
                Socket client = serverSocket.accept();
                Log.i(TAG, "Game connected from: " + client.getRemoteSocketAddress());
                executor.submit(() -> handleClient(client));
            }
        } catch (IOException e) {
            if (running) Log.e(TAG, "Accept error: " + e.getMessage());
        }
    }

    private void handleClient(Socket tcpSocket) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(WS_URL).build();

        // Shared state
        Object lock = new Object();
        boolean[] wsClosed = {false};
        WebSocket[] wsHolder = {null};

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.i(TAG, "WebSocket connected → " + WS_URL);
                synchronized (lock) {
                    wsHolder[0] = webSocket;
                    lock.notifyAll();
                }
                // Start reading from TCP → WebSocket
                executor.submit(() -> {
                    try {
                        InputStream in = tcpSocket.getInputStream();
                        byte[] buf = new byte[65536];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            webSocket.send(ByteString.of(buf, 0, len));
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "TCP→WS end: " + e.getMessage());
                    } finally {
                        webSocket.close(1000, "TCP closed");
                    }
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                // WebSocket → TCP
                try {
                    OutputStream out = tcpSocket.getOutputStream();
                    out.write(bytes.toByteArray());
                    out.flush();
                } catch (IOException e) {
                    Log.d(TAG, "WS→TCP error: " + e.getMessage());
                    webSocket.close(1000, "TCP write failed");
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.i(TAG, "WebSocket closed: " + reason);
                synchronized (lock) {
                    wsClosed[0] = true;
                    lock.notifyAll();
                }
                closeQuietly(tcpSocket);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error: " + t.getMessage());
                synchronized (lock) {
                    wsClosed[0] = true;
                    lock.notifyAll();
                }
                closeQuietly(tcpSocket);
            }
        };

        WebSocket ws = httpClient.newWebSocket(request, listener);

        // Wait for WS to close
        synchronized (lock) {
            while (!wsClosed[0]) {
                try {
                    lock.wait(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        closeQuietly(tcpSocket);
        ws.cancel();
        httpClient.dispatcher().executorService().shutdown();
        Log.i(TAG, "Session ended");
    }

    private void closeQuietly(Socket s) {
        try { if (s != null && !s.isClosed()) s.close(); }
        catch (IOException ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "NRO Bridge", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("TCP→WebSocket relay đang chạy");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setContentTitle("NRO Bridge")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }
}
