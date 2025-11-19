package com.iot.team_1;

// --- 기본 import ---
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// ★ 1. 툴바(Toolbar) import 추가
import androidx.appcompat.widget.Toolbar;

// --- ML Kit import ---
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import com.iot.team_1.utils.CSVUtils;

// --- 파일/날짜 import ---
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// --- 스캔 이력 저장 import ---
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iot.team_1.ScanHistory;
import java.lang.reflect.Type;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    // --- 뷰(View) 변수 ---
    private ImageView imageView;
    private TextView textView;
    private Button selectButton, captureButton;
    private View GuideBtn;

    // ★ 2. 툴바 및 스캔 이력 버튼 변수 추가 ★
    private Toolbar toolbar;
    private Button ScanlistBtn; // (기존 코드에서 View -> Button으로 수정됨)

    // --- 런처(Launcher) 변수 ---
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private Uri photoUri;

    // --- 데이터 변수 ---
    private List<String> cautionIngredients = new ArrayList<>();
    private Gson gson;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // (activity_main.xml 사용)

        // ★ 3. 툴바 설정 (onCreate 상단) ★
        toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 화살표
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // --- 뷰 연결 ---
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        selectButton = findViewById(R.id.selectButton);
        captureButton = findViewById(R.id.cameraButton);
        GuideBtn = findViewById(R.id.GuideBtn);

        // ★ 4. ScanlistBtn 연결 ★
        ScanlistBtn = findViewById(R.id.ScanlistBtn);

        // --- Gson 객체 초기화 ---
        gson = new Gson();

        // --- CSV 로드 ---
        CSVUtils.importCSVToDB(this, R.raw.ingredients, list -> cautionIngredients = list);

        // --- 권한 확인 ---
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        // --- 런처 초기화: 갤러리 ---
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Uri imageUri = result.getData().getData();
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            bitmap = rotateImageIfRequired(bitmap, imageUri);
                            processImageFromBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        // --- 런처 초기화: 카메라 ---
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK){
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
                            bitmap = rotateImageIfRequired(bitmap, photoUri);
                            processImageFromBitmap(bitmap);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
        );

        // --- 버튼 클릭 리스너 ---
        selectButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        captureButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                File photoFile = createImageFile();
                photoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePhotoLauncher.launch(intent);
            } catch (IOException e){
                e.printStackTrace();
                Toast.makeText(this, "사진 파일 생성 실패", Toast.LENGTH_SHORT).show();
            }
        });

        GuideBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanGuideActivity.class);
            startActivity(intent);
        });

        // ★ 5. ScanlistBtn 클릭 리스너 추가 ★
        ScanlistBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    // ★ 6. 툴바 클릭 처리 메서드 추가 ★
    // (onCreate 메서드 '밖', 클래스 '안'에 추가)
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // 뒤로가기 실행
        return true;
    }

    // (createImageFile - 기존 코드와 동일)
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // =====================================
    // OCR 처리
    // (기존 코드와 동일)
    // =====================================
    private void processImageFromBitmap(Bitmap bitmap){
        try {
            List<String> detectedIngredients = new ArrayList<>();
            textView.setText("");

            // ★ (수정) 전처리 비활성화 (OCR 인식률 향상) ★
            // Bitmap preprocessed = preprocessImageEnhanced(bitmap);
            imageView.setImageBitmap(bitmap);

            // ★ (수정) 원본 비트맵(bitmap)을 AI에 전달 ★
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            TextRecognizer recognizer = TextRecognition.getClient(
                    new KoreanTextRecognizerOptions.Builder().build()
            );

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String recognizedText = result.getText();

                        String filteredText = recognizedText.replaceAll("[0-9]", "")
                                .replaceAll("\\s+", " ").trim();

                        if(filteredText.isEmpty()) {
                            textView.setText("텍스트가 감지되지 않았습니다.");
                            return;
                        }

                        List<String> words = splitWords(filteredText);
                        for(String word : words){
                            if(word.length()<2) continue;

                            String corrected = correctWord(word, cautionIngredients);
                            if(cautionIngredients.contains(corrected) && !detectedIngredients.contains(corrected)){
                                detectedIngredients.add(corrected);
                            }
                        }

                        String fullLine = makeFullLine(textView);
                        textView.setText("📝 인식된 텍스트\n" + fullLine + "\n" + filteredText + "\n" + fullLine);

                        showDetectedIngredients(detectedIngredients);

                    })
                    .addOnFailureListener(e -> textView.setText("OCR 실패: " + e.getMessage()));

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // (makeFullLine - 기존 코드와 동일)
    private String makeFullLine(TextView textView) {
        Paint paint = textView.getPaint();
        int width = textView.getWidth();
        if(width == 0){
            width = getResources().getDisplayMetrics().widthPixels;
        }
        String dash = "-";
        float dashWidth = paint.measureText(dash);
        int count = (int) (width / dashWidth);
        StringBuilder line = new StringBuilder();
        for(int i = 0; i < count; i++){
            line.append(dash);
        }
        return line.toString();
    }

    // (preprocessImageEnhanced - 기존 코드와 동일)
    // (참고: 이 함수는 현재 processImageFromBitmap에서 호출되지 않도록 비활성화했습니다.)
    private Bitmap preprocessImageEnhanced(Bitmap bitmap){
        int width = bitmap.getWidth() * 2;
        int height = bitmap.getHeight() * 2;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        Bitmap processed = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(processed);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrix contrast = new ColorMatrix();
        float scale = 1.6f;
        float translate = -15f;
        contrast.set(new float[]{
                scale,0,0,0,translate,
                0,scale,0,0,translate,
                0,0,scale,0,translate,
                0,0,0,1,0
        });
        cm.postConcat(contrast);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(scaledBitmap,0,0,paint);
        return processed;
    }

    // (rotateImageIfRequired - 기존 코드와 동일)
    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = getContentResolver().openInputStream(selectedImage);
        ExifInterface ei = new ExifInterface(input);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180: return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270: return rotateImage(img, 270);
            default: return img;
        }
    }

    // (rotateImage - 기존 코드와 동일)
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img,0,0,img.getWidth(),img.getHeight(),matrix,true);
    }

    // (splitWords - 기존 코드와 동일)
    private List<String> splitWords(String text){
        List<String> words = new ArrayList<>();
        text = text.replaceAll("[,;]", " ");
        Matcher matcher = Pattern.compile("([가-힣]+|[A-Za-z]+)").matcher(text);
        while(matcher.find()){
            String w = matcher.group().trim();
            if(!w.isEmpty()) words.add(w);
        }
        return words;
    }

    // (decomposeHangul - 기존 코드와 동일)
    private String decomposeHangul(String s) {
        StringBuilder result = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int base = ch - 0xAC00;
                char cho = (char) (base / (21 * 28));
                char jung = (char) ((base % (21 * 28)) / 28);
                char jong = (char) (base % 28);
                result.append(cho).append(jung);
                if (jong != 0) result.append(jong);
            } else if (Character.isLetter(ch)) {
                result.append(Character.toLowerCase(ch));
            }
        }
        return result.toString();
    }

    // (levenshteinDistance - 기존 코드와 동일)
    private int levenshteinDistance(String a, String b){
        int[][] dp = new int[a.length()+1][b.length()+1];
        for(int i=0;i<=a.length();i++) dp[i][0]=i;
        for(int j=0;j<=b.length();j++) dp[0][j]=j;
        for(int i=1;i<=a.length();i++){
            for(int j=1;j<=b.length();j++){
                int cost = a.charAt(i-1)==b.charAt(j-1)?0:1;
                dp[i][j]=Math.min(Math.min(dp[i-1][j]+1, dp[i][j-1]+1), dp[i-1][j-1]+cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    // (correctWord - 기존 코드와 동일)
    private String correctWord(String word, List<String> dictionary) {
        // (기존의 복잡한 유사도 분석 로직... 생략)
        if (word == null || word.trim().length() < 2) return word;
        if (!word.matches("^[가-힣A-Za-z]+$")) return word;
        String normalizedWord = word.toLowerCase();
        String decomposedWord = decomposeHangul(normalizedWord);
        String bestMatch = word;
        double bestScore = 0.0;
        for (String dict : dictionary) {
            if (dict == null || dict.length() < 2) continue;
            String normalizedDict = dict.toLowerCase();
            String decomposedDict = decomposeHangul(normalizedDict);
            if (normalizedWord.equals(normalizedDict)) {
                return dict;
            }
            int distance = levenshteinDistance(decomposedWord, decomposedDict);
            double score = 1.0 - ((double) distance / Math.max(decomposedWord.length(), decomposedDict.length()));
            int lengthDiff = Math.abs(word.length() - dict.length());
            boolean similarLength = lengthDiff <= 2;
            boolean hasEnglish = word.matches(".*[A-Za-z].*");
            boolean firstTwoMatch = false;
            if (normalizedWord.length() >= 2 && normalizedDict.length() >= 2) {
                firstTwoMatch = normalizedWord.charAt(0) == normalizedDict.charAt(0) &&
                        normalizedWord.charAt(1) == normalizedDict.charAt(1);
            }
            boolean middleEndSimilar = true;
            if (normalizedWord.length() >= 5 && normalizedDict.length() >= 5) {
                int wordLen = normalizedWord.length();
                int dictLen = normalizedDict.length();
                char wordLast1 = normalizedWord.charAt(wordLen - 1);
                char wordLast2 = normalizedWord.charAt(wordLen - 2);
                char dictLast1 = normalizedDict.charAt(dictLen - 1);
                char dictLast2 = normalizedDict.charAt(dictLen - 2);
                boolean lastTwoMatch = (wordLast1 == dictLast1) || (wordLast2 == dictLast2);
                boolean middleMatch = true;
                if (hasEnglish && wordLen >= 8 && dictLen >= 8) {
                    int midStart = wordLen / 3;
                    int midEnd = (wordLen * 2) / 3;
                    String wordMiddle = normalizedWord.substring(midStart, midEnd);
                    String dictMiddle = normalizedDict.substring(midStart, Math.min(midEnd, dictLen));
                    int midDistance = levenshteinDistance(wordMiddle, dictMiddle);
                    double midScore = 1.0 - ((double) midDistance / Math.max(wordMiddle.length(), dictMiddle.length()));
                    middleMatch = midScore >= 0.70;
                }
                middleEndSimilar = (lastTwoMatch || score >= 0.92) && middleMatch;
            }
            double minScore;
            if (hasEnglish) {
                if (word.length() <= 4) { minScore = 0.72; }
                else if (word.length() <= 6) { minScore = 0.68; }
                else { minScore = 0.65; }
            } else {
                if (word.length() <= 3) { minScore = 0.85; }
                else if (word.length() <= 5) { minScore = 0.82; }
                else { minScore = 0.78; }
            }
            if (score > bestScore &&
                    score >= minScore &&
                    similarLength &&
                    firstTwoMatch &&
                    middleEndSimilar) {
                bestScore = score;
                bestMatch = dict;
            }
        }
        if (!bestMatch.equals(word)) {
            android.util.Log.d("WordCorrection",
                    String.format("교정: '%s' → '%s' (유사도: %.2f)", word, bestMatch, bestScore));
        }
        return bestMatch;
    }

    // =====================================
    // 주의 성분 표시 + 스캔 이력 저장
    // (기존 코드와 동일)
    // =====================================
    private void showDetectedIngredients(List<String> detectedIngredients){
        LinearLayout layout = findViewById(R.id.mainLayout);

        // --- (A. 저장할 데이터 준비) ---
        String resultStatus;
        String ingredientsText;

        if(!detectedIngredients.isEmpty()){
            // [경고]
            resultStatus = "🚨 [경고!]";
            ingredientsText = String.join(", ", detectedIngredients);

            // --- (기존 UI 로직 - 경고) ---
            StringBuilder warningHtml = new StringBuilder("⚠️ <b>주의 성분 발견:</b> ");
            for(int i=0;i<detectedIngredients.size();i++){
                if(i>0) warningHtml.append(", ");
                warningHtml.append("<font color='#FF0000'><b>")
                        .append(detectedIngredients.get(i))
                        .append("</b></font>");
            }
            textView.append(Html.fromHtml("<br>" + warningHtml.toString(), Html.FROM_HTML_MODE_COMPACT));

            TextView messageView = new TextView(this);
            messageView.setText(Html.fromHtml("아래 성분이 포함되어 있습니다.<br><br>"
                    + warningHtml.toString(), Html.FROM_HTML_MODE_COMPACT));
            messageView.setPadding(50, 40, 50, 40);
            messageView.setTextSize(16);
            messageView.setTextColor(Color.BLACK);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("⚠️ 위험 성분 감지")
                    .setView(messageView)
                    .setPositiveButton("확인", null)
                    .show();

            Toast.makeText(this, "성분표 재확인 필수!!!!!!!!!!!!!", Toast.LENGTH_LONG).show();

            int originalColor = Color.parseColor("#F4D7E8");
            int alertColor = Color.parseColor("#FF3B30");
            int flashColor = Color.WHITE;

            ValueAnimator animator = ValueAnimator.ofArgb(flashColor, alertColor, flashColor);
            animator.setDuration(700);
            animator.setRepeatCount(6);
            animator.setRepeatMode(ValueAnimator.RESTART);

            animator.addUpdateListener(animation ->
                    layout.setBackgroundColor((int) animation.getAnimatedValue())
            );
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    layout.setBackgroundColor(originalColor);
                }
            });
            animator.start();

        } else {
            // [안심]
            resultStatus = "✅ [안심]";
            ingredientsText = ""; // 저장할 성분 없음

            // --- (기존 UI 로직 - 안심) ---
            textView.append("\n\n ※ 텍스트 인식이 제대로 되지 않았을 수 있으니 재촬영 후 확인 ※");

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("✅ 안전한 성분")
                    .setMessage("주의가 필요한 성분이 감지되지 않았습니다.\n\n그래도 성분표를 실제로 한번 확인해보는 습관은 좋습니다! 😊")
                    .setPositiveButton("확인", null)
                    .show();

            Toast.makeText(this, "안전한 성분으로 확인되었습니다 :)", Toast.LENGTH_LONG).show();

            int originalColor = Color.parseColor("#F4D7E8");
            int safeColor = Color.parseColor("#4CAF50");
            int flashColor = Color.WHITE;

            ValueAnimator animator = ValueAnimator.ofArgb(flashColor, safeColor, flashColor);
            animator.setDuration(900);
            animator.setRepeatCount(4);
            animator.setRepeatMode(ValueAnimator.RESTART);

            animator.addUpdateListener(animation ->
                    layout.setBackgroundColor((int) animation.getAnimatedValue())
            );
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    layout.setBackgroundColor(originalColor);
                }
            });
            animator.start();
        }

        // --- (B. SharedPreferences에 스캔 이력 저장) ---

        // 1. 새 이력 객체 생성
        ScanHistory newHistory = new ScanHistory(
                resultStatus,
                ingredientsText,
                System.currentTimeMillis() // 현재 시간(날짜)
        );

        // 2. 기존 이력 목록 불러오기 (파일 이름 "ScanHistoryPrefs"로 지정)
        SharedPreferences prefs = getSharedPreferences("ScanHistoryPrefs", MODE_PRIVATE);
        String jsonHistory = prefs.getString("history_list", null);

        List<ScanHistory> historyList;

        if (jsonHistory == null) {
            historyList = new ArrayList<>(); // 저장된 목록이 없으면 새 목록 생성
        } else {
            // JSON 문자열을 List<ScanHistory>로 변환
            Type type = new TypeToken<ArrayList<ScanHistory>>() {}.getType();
            historyList = gson.fromJson(jsonHistory, type);
        }

        // 3. 새 이력을 목록 맨 앞에 추가 (최신순)
        historyList.add(0, newHistory);

        // 4. 새 이력이 추가된 목록을 다시 JSON 문자열로 변환
        String newJsonHistory = gson.toJson(historyList);

        // 5. SharedPreferences에 덮어쓰기 (저장!)
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("history_list", newJsonHistory);
        editor.apply();

        Log.d("HISTORY_SAVE", "새 이력 저장 완료: " + resultStatus);
    }
}