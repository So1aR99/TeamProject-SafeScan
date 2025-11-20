package com.iot.team_1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClient {
    private Socket socket;
    private PrintWriter out;
    private String host;
    private int port;

    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
