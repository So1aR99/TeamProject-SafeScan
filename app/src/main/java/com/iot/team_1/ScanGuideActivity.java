package com.iot.team_1;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton; // ★ 1. ImageButton import 추가

import com.google.android.material.appbar.MaterialToolbar;

public class ScanGuideActivity extends AppCompatActivity {

    private Button GoScan;
    private Button GoHistory;
    private Button GoEx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_guide);

        // --- 뷰(View) 연결 ---
        GoScan = findViewById(R.id.GoScan);
        GoHistory = findViewById(R.id.GoHistory);
        GoEx = findViewById(R.id.GoEx);

        // --- 클릭 리스너 설정 ---

        // '성분 검사하기' 버튼
        GoScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // (주석의 설명대로 MainActivity로 이동)
                Intent intent = new Intent(ScanGuideActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // '스캔 이력 보기' 버튼
        GoHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // HistoryActivity로 이동
                Intent intent = new Intent(ScanGuideActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        GoEx.setOnClickListener(v -> {
            Intent intent = new Intent(ScanGuideActivity.this, ScanexActivity.class);
            startActivity(intent);
        });
        // 툴바 가져오기
        MaterialToolbar toolbar = findViewById(R.id.toolbar_scanex);
        setSupportActionBar(toolbar);

        // 뒤로가기 화살표 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 뒤로가기 화살표 클릭 시 뒤로가기
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}