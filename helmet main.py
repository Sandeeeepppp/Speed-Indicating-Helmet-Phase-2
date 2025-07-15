# MAIN CODE for HELMET
import network
import socket
import time
import json
from machine import Pin
# LED setup
led = Pin(4, Pin.OUT)#gp4

#WiFi Credentials
#SSID = "Rahul's S23"
#PASSWORD = "11111111"

SSID = "Ranjith’s iPhone"
PASSWORD = "MyInt666"

# WiFi credentials
# SSID = "<A12>"
# PASSWORD = "<MyInt666>"

def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    wlan.connect(SSID, PASSWORD)
    while not wlan.isconnected():
        print("Connecting to WiFi...")
        time.sleep(1)
    print("WiFi connected:", wlan.ifconfig())
    led.on()
    time.sleep(0.5)
    led.off()
            
            
def blink_led(level):
   # time.sleep(5)
    if level == 1:  # >60 km/h, ≤80 km/h
        for _ in range(1):  # 3 seconds
            led.on()
            time.sleep(0.5)
            led.off()
            time.sleep(0.5)
    elif level == 2:  # >80 km/h, ≤100 km/h
        for _ in range(2):  # 3 seconds, 2 blinks per second
            led.on()
            time.sleep(0.25)
            led.off()
            time.sleep(0.25)
    elif level == 3:  # >100 km/h
        for _ in range(3):  # 3 seconds, 3 blinks per second
            led.on()
            time.sleep(0.166)
            led.off()
            time.sleep(0.166)

    time.sleep(10)


def http_server():
    addr = socket.getaddrinfo('0.0.0.0', 80)[0][-1]
    s = socket.socket()
    s.bind(addr)
    s.listen()
    time.sleep(0.5)
    print('Listening on', addr)
    led.on()
    time.sleep(0.5)
    led.off()
    while True:
        cl, addr = s.accept()
        print('Client connected from', addr)
        request = cl.recv(1024).decode()
        
        # Parse POST request
        if 'POST /speed' in request:
            try:
                body = request.split('\r\n\r\n')[1]
                data = json.loads(body)
                level = data.get('level', 0)
                print(f"Received speed level: {level}")
                blink_led(level)
               # time.sleep(5)
                response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK"
            except:
                response = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/plain\r\n\r\nInvalid JSON"
        else:
            response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found"

        cl.send(response.encode())
        cl.close()

def main():
    connect_wifi()
    http_server()

if _name_ == '_main_':
    main()