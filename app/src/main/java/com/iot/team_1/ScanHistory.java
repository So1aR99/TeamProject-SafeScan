// 이 패키지 이름이 HistoryActivity.java와 동일한지 확인하세요
package com.iot.team_1;

/**
 * '스캔 이력' 한 건의 데이터를 담는 클래스 (데이터 모델)
 * SharedPreferences + Gson을 위해 사용됩니다.
 */
public class ScanHistory {

    // 1. 결과 (예: "✅ [안심]" 또는 "🚨 [경고!]")
    public String resultText;

    // 2. 발견된 성분 (예: "레티놀, 벤조산" 또는 "")
    public String foundIngredients;

    // 3. 스캔한 날짜 (System.currentTimeMillis()로 저장될 숫자)
    public long scanDate;

    /**
     * 기본 생성자
     * (Gson이 JSON을 자바 객체로 변환할 때 필요합니다)
     */
    public ScanHistory() {
    }

    /**
     * ScanActivity에서 새 이력을 만들 때 사용할 생성자
     * @param resultText 결과 텍스트
     * @param foundIngredients 발견된 성분
     * @param scanDate 스캔한 날짜 (long)
     */
    public ScanHistory(String resultText, String foundIngredients, long scanDate) {
        this.resultText = resultText;
        this.foundIngredients = foundIngredients;
        this.scanDate = scanDate;
    }
}