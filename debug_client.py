#!/bin/python
import socket
from time import sleep

HOST = "127.0.0.1"
PORT = 7926

sleep(1)
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    s.sendall(b"keyboard 46 1\n")
    data = s.recv(1024)
    print(data.decode("utf-8"))

    sleep(2)
    s.sendall(b"keyboard 45 0\n")
    data = s.recv(1024)
    print(data.decode("utf-8"))
    sleep(5)
    s.sendall(b"__KEEP_ALIVE\n")
    sleep(50)
#     for i in range(0, 10):
#         s.sendall(b"mouse start\n")
#         data = s.recv(1024)
#         print(data.decode("utf-8"))
#         sleep(1)
    s.close()