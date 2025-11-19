package com.iot.team_1; // 본인 패키지명

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // ★ 1. ImageButton import
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<ScanHistory> historyList;

    // ★★★ 2. '삭제' 클릭 이벤트를 위한 인터페이스 정의 ★★★
    public interface OnHistoryItemDeleteListener {
        void onDeleteClick(int position); // 몇 번째 항목이 눌렸는지 전달
    }

    private OnHistoryItemDeleteListener deleteListener; // ★ 리스너 변수

    // ★ 리스너를 Activity로부터 받아오는 메서드
    public void setOnDeleteListener(OnHistoryItemDeleteListener listener) {
        this.deleteListener = listener;
    }
    // ★★★ (인터페이스 추가 끝) ★★★

    public HistoryAdapter(List<ScanHistory> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ScanHistory historyItem = historyList.get(position);

        holder.tvResult.setText(historyItem.resultText);

        if (historyItem.foundIngredients == null || historyItem.foundIngredients.isEmpty()) {
            holder.tvIngredients.setText("발견된 주의 성분 없음");
        } else {
            holder.tvIngredients.setText(historyItem.foundIngredients);
        }

        holder.tvDate.setText(formatDate(historyItem.scanDate));

        // ★★★ 3. '삭제' 버튼에 클릭 리스너 설정 ★★★
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    // 현재 항목의 '위치(position)'를 가져옴
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        // 인터페이스를 통해 Activity에 "삭제!" 신호 전달
                        deleteListener.onDeleteClick(currentPosition);
                    }
                }
            }
        });
        // ★★★ (클릭 리스너 추가 끝) ★★★
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
        return sdf.format(new Date(timestamp));
    }

    /**
     * '뷰 홀더' 클래스
     */
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView tvResult;
        public TextView tvIngredients;
        public TextView tvDate;
        public ImageButton btnDelete; // ★ 4. 삭제 버튼 뷰 변수 추가

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvResult = itemView.findViewById(R.id.tv_history_result);
            tvIngredients = itemView.findViewById(R.id.tv_history_ingredients);
            tvDate = itemView.findViewById(R.id.tv_history_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_history); // ★ ID 연결
        }
    }
}