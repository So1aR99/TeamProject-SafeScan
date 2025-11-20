package com.iot.team_1;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 간단한 TCP 연결 매니저
 * - 백그라운드 쓰레드에서 소켓 연결/전송/종료 처리
 * - 앱 전체에서 단일 인스턴스처럼 사용 (static 메서드)
 */
public class TCPConnectionManager {
    private static final String TAG = "TCPConnectionManager";
    private static Socket socket;
    private static PrintWriter out;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private static volatile boolean connected = false;

    /**
     * 비동기 연결 (타임아웃 포함)
     */
    public static void connect(String host, int port) {
        executor.execute(() -> {
            try {
                if (socket != null && socket.isConnected() && !socket.isClosed()) {
                    Log.d(TAG, "이미 연결되어 있음");
                    connected = true;
                    return;
                }
                socket = new Socket();
                // 타임아웃 3초
                socket.connect(new InetSocketAddress(host, port), 3000);
                out = new PrintWriter(socket.getOutputStream(), true);
                connected = true;
                Log.d(TAG, "TCP 연결 성공: " + host + ":" + port);
            } catch (IOException e) {
                connected = false;
                Log.e(TAG, "TCP 연결 실패", e);
                // 정리
                closeInternal();
            }
        });
    }

    /**
     * 안전하게 연결 종료
     */
    public static void disconnect() {
        executor.execute(TCPConnectionManager::closeInternal);
    }

    private static void closeInternal() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = null;
            connected = false;
            Log.d(TAG, "TCP 연결 종료 완료");
        } catch (IOException e) {
            Log.e(TAG, "TCP 종료 중 오류", e);
        }
    }

    /**
     * 비동기 전송. 연결되어 있지 않다면 시도하지 않음.
     * (보내는 내용은 반드시 짧은 문자열("0" 또는 "1") 권장)
     */
    public static void send(String message) {
        if (message == null) return;
        executor.execute(() -> {
            if (!connected || out == null) {
                Log.w(TAG, "전송 실패: 연결 없음. 메시지: " + message);
                return;
            }
            try {
                out.println(message);
                out.flush();
                Log.d(TAG, "전송: " + message);
            } catch (Exception e) {
                Log.e(TAG, "전송 실패", e);
            }
        });
    }

    /**
     * 연결 상태 조회 (동기)
     */
    public static boolean isConnected() {
        return connected;
    }
}
