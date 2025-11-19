package com.iot.team_1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;

public class MainActivity_Login extends AppCompatActivity {

    private Button btnStartScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_login); // XML 연결

        btnStartScan = findViewById(R.id.btnStartScan);

        btnStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity_Login.this, ScanGuideActivity.class);
                startActivity(intent);
            }
        });
    }
}