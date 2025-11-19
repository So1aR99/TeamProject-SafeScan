package com.iot.team_1;

// --- 기본 Android 및 Java ---
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Logcat 출력을 위해 import
import android.view.View;
import android.widget.Button; // ★ 1. Button import 추가
import android.widget.TextView;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// --- AndroidX (AppCompat, 툴바, RecyclerView) ---
import androidx.appcompat.app.AlertDialog; // ★ 2. AlertDialog import 추가
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// --- Gson (JSON 변환) ---
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * '스캔 이력' 목록을 보여주는 화면(Activity)입니다.
 * SharedPreferences에서 데이터를 불러와 RecyclerView에 표시하고,
 * 메인 화면으로 돌아가는 툴바(UP 버튼)를 포함합니다.
 *
 * ★ HistoryAdapter의 '삭제' 인터페이스를 구현(implements)합니다. ★
 */
public class HistoryActivity extends AppCompatActivity
        implements HistoryAdapter.OnHistoryItemDeleteListener { // (인터페이스 구현)

    // --- 뷰(View) 변수 ---
    private RecyclerView recyclerView;
    private TextView tvEmptyHistory;
    private Toolbar toolbar;
    private Button btnDeleteAll; // ★ 3. '전체 삭제' 버튼 변수 추가

    // --- 데이터 변수 ---
    private HistoryAdapter adapter;
    private List<ScanHistory> historyList;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 레이아웃 파일 연결
        setContentView(R.layout.activity_history);

        // 2. 뷰(View) 및 객체 초기화
        recyclerView = findViewById(R.id.recycler_view_history);
        tvEmptyHistory = findViewById(R.id.tv_empty_history);
        toolbar = findViewById(R.id.toolbar_history);
        btnDeleteAll = findViewById(R.id.btn_delete_all); // ★ 4. '전체 삭제' 버튼 ID 연결
        gson = new Gson();

        // 3. 기능 실행 (순서대로)
        setupToolbar();
        loadHistoryData();
        setupRecyclerView(); // (RecyclerView 설정 시 버튼 가시성도 처리됨)

        // ★ 5. '전체 삭제' 버튼 클릭 리스너 설정 ★
        btnDeleteAll.setOnClickListener(v -> {
            // (경고) 정말 삭제할지 물어보는 알림창 띄우기
            new AlertDialog.Builder(this)
                    .setTitle("전체 기록 삭제")
                    .setMessage("정말로 모든 스캔 기록을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
                    .setIcon(android.R.drawable.ic_dialog_alert) // 안드로이드 기본 경고 아이콘
                    .setPositiveButton("삭제", (dialog, which) -> {
                        // "삭제" 버튼을 누르면 실행
                        deleteAllHistory();
                    })
                    .setNegativeButton("취소", null) // "취소" 버튼은 아무것도 안 함
                    .show();
        });
    }

    /**
     * 툴바를 이 화면의 액션바로 설정하고,
     * 'UP' 버튼 (← 뒤로가기 화살표)을 활성화합니다.
     * (기존 코드와 동일)
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * 툴바의 'UP' 버튼(←)을 클릭했을 때 호출되는 메서드입니다.
     * (기존 코드와 동일)
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // 안드로이드의 기본 '뒤로가기' 동작을 실행
        return true;
    }

    /**
     * SharedPreferences에서 저장된 이력(JSON)을 불러와
     * 'historyList' 변수에 채워넣습니다.
     * (기존 코드와 동일)
     */
    private void loadHistoryData() {
        SharedPreferences prefs = getSharedPreferences("ScanHistoryPrefs", MODE_PRIVATE);
        String jsonHistory = prefs.getString("history_list", null);

        if (jsonHistory == null) {
            historyList = new ArrayList<>(); // 이력이 없으면 빈 리스트
        } else {
            // Gson을 사용해 JSON 문자열을 List<ScanHistory>로 변환
            Type type = new TypeToken<ArrayList<ScanHistory>>() {}.getType();
            historyList = gson.fromJson(jsonHistory, type);
        }
    }

    /**
     * ★ (수정됨) 'historyList'의 데이터 상태에 따라
     * RecyclerView 및 '전체 삭제' 버튼의 가시성을 설정합니다.
     */
    private void setupRecyclerView() {
        if (historyList == null || historyList.isEmpty()) {
            // 이력이 없으면
            recyclerView.setVisibility(View.GONE);
            tvEmptyHistory.setVisibility(View.VISIBLE);
            btnDeleteAll.setVisibility(View.GONE); // ★ 6. 이력이 없으면 '전체 삭제' 버튼 숨기기
        } else {
            // 이력이 있으면
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyHistory.setVisibility(View.GONE);
            btnDeleteAll.setVisibility(View.VISIBLE); // ★ 7. 이력이 있으면 '전체 삭제' 버튼 보이기

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new HistoryAdapter(historyList); // 어댑터 생성
            adapter.setOnDeleteListener(this); // (개별 삭제 리스너 설정)
            recyclerView.setAdapter(adapter); // 어댑터 연결
        }
    }

    /**
     * ★ (수정됨) (개별 삭제) 인터페이스 구현 메서드
     * (개별 삭제 후 리스트가 비었을 때의 처리 추가)
     */
    @Override
    public void onDeleteClick(int position) {
        // 1. 현재 메모리(List)에서 해당 항목 제거
        historyList.remove(position);

        // 2. 어댑터에게 알림
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, historyList.size());

        // 3. SharedPreferences에도 이 변경사항(항목이 삭제된 리스트)을 저장
        saveHistoryListToPrefs();

        // 4. (★추가됨★) 만약 리스트가 비게 되면 "이력 없음" 상태로 변경
        if (historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyHistory.setVisibility(View.VISIBLE);
            btnDeleteAll.setVisibility(View.GONE); // '전체 삭제' 버튼 숨기기
        }

        Log.d("HISTORY_DELETE", "Item at position " + position + " deleted. List size: " + historyList.size());
    }

    /**
     * (헬퍼 함수) 현재 'historyList'를 SharedPreferences에 저장합니다.
     * (기존 코드와 동일)
     */
    private void saveHistoryListToPrefs() {
        // 1. 현재(삭제가 반영된) 'historyList'를 JSON 문자열로 변환
        String newJsonHistory = gson.toJson(historyList);

        // 2. SharedPreferences에 덮어쓰기
        SharedPreferences prefs = getSharedPreferences("ScanHistoryPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("history_list", newJsonHistory);
        editor.apply();

        Log.d("HISTORY_SAVE", "History list saved after modification.");
    }

    /**
     * ★★★ 8. (새 헬퍼 함수) 모든 이력을 삭제하는 함수 ★★★
     * (이 함수를 HistoryActivity 클래스 안에 새로 추가하세요)
     */
    private void deleteAllHistory() {
        // 1. SharedPreferences에서 "history_list" 키 자체를 삭제
        SharedPreferences prefs = getSharedPreferences("ScanHistoryPrefs", MODE_PRIVATE);
        prefs.edit().remove("history_list").apply();

        // 2. 현재 화면의 리스트(historyList)도 비움
        historyList.clear();

        // 3. 어댑터에게 데이터가 모두 사라졌다고 알림
        adapter.notifyDataSetChanged();

        // 4. UI를 "이력 없음" 상태로 변경
        recyclerView.setVisibility(View.GONE);
        tvEmptyHistory.setVisibility(View.VISIBLE);
        btnDeleteAll.setVisibility(View.GONE); // '전체 삭제' 버튼 숨기기

        Log.d("HISTORY_DELETE", "All history deleted.");
    }
}