import socket
import serial
import time

# -------------------------
# AVR 시리얼 설정 (이미 작성하신 그대로)
# -------------------------
ser = serial.Serial("COM5", 115200, timeout=1)
time.sleep(2)

# -------------------------
# TCP 서버 설정
# -------------------------
HOST = "0.0.0.0"
PORT = 4000

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((HOST, PORT))
server_socket.listen(1)

print("서버 대기 중...")

while True:
    client_socket, addr = server_socket.accept()
    print("클라이언트 연결됨:", addr)

    while True:
        try:
            data = client_socket.recv(1024)
            if not data:
                break
            message = data.decode().strip()
            print("받은 데이터:", message)

            # -------------------------
            # AVR로 데이터 전송
            # -------------------------
            if message in ['0', '1']:
                ser.write(message.encode())
                print(f"AVR로 전송: {message}")
                print()
            else:
                print("알 수 없는 메시지")

        except ConnectionResetError:
            break

    client_socket.close()
    print("클라이언트 연결 종료")
