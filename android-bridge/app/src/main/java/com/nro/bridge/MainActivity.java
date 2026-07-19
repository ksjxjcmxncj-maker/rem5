package com.nro.bridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etWsUrl;
    private Button btnStart, btnStop;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etWsUrl = findViewById(R.id.et_ws_url);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        tvStatus = findViewById(R.id.tv_status);

        // Load saved URL
        String savedUrl = getSharedPreferences("nro", MODE_PRIVATE).getString("ws_url", "");
        if (!savedUrl.isEmpty()) etWsUrl.setText(savedUrl);

        btnStart.setOnClickListener(v -> {
            String url = etWsUrl.getText().toString().trim();
            if (url.isEmpty() || !url.startsWith("wss://")) {
                Toast.makeText(this, "URL phải bắt đầu bằng wss://", Toast.LENGTH_SHORT).show();
                return;
            }
            // Save URL
            getSharedPreferences("nro", MODE_PRIVATE).edit().putString("ws_url", url).apply();
            BridgeService.WS_URL = url;

            Intent intent = new Intent(this, BridgeService.class);
            intent.putExtra("WS_URL", url);
            startForegroundService(intent);

            tvStatus.setText("✅ Bridge đang chạy\n\nTrong game NRO:\n→ Chọn Custom Server\n→ Nhập: 127.0.0.1:14445");
            tvStatus.setTextColor(0xFF2E7D32);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, BridgeService.class));
            tvStatus.setText("⏹ Bridge đã dừng");
            tvStatus.setTextColor(0xFF757575);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        tvStatus.setText("⏸ Bridge chưa chạy\n\nNhập WebSocket URL của Codespace\nrồi bấm Start");
        btnStop.setEnabled(false);
    }
}
