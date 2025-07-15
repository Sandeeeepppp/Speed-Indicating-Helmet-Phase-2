import network
import socket
import time
import ujson
from machine import Pin, I2C

# WiFi Credentials
SSID = "Ranjith’s iPhone"
PASSWORD = "MyInt666"

# LED Setup
led = Pin(15, Pin.OUT)

# MPU6050 Setup
MPU6050_ADDR = 0x68
PWR_MGMT_1 = 0x6B
ACCEL_XOUT_H = 0x3B
GYRO_XOUT_H = 0x43
i2c = I2C(0, scl=Pin(17), sda=Pin(16), freq=400000)
i2c.writeto_mem(MPU6050_ADDR, PWR_MGMT_1, bytes([0x00]))
time.sleep(0.1)

# Connect to WiFi
def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    wlan.connect(SSID, PASSWORD)
    while not wlan.isconnected():
        print("Connecting to WiFi...")
        time.sleep(1)
    print("Connected:", wlan.ifconfig())

# Read 16-bit word
def read_word(reg):
    high = i2c.readfrom_mem(MPU6050_ADDR, reg, 1)[0]
    low = i2c.readfrom_mem(MPU6050_ADDR, reg + 1, 1)[0]
    val = (high << 8) | low
    return val - 65536 if val > 32767 else val

# Get sensor data
def get_sensor_data():
    ax = read_word(ACCEL_XOUT_H) / 16384.0
    ay = read_word(ACCEL_XOUT_H + 2) / 16384.0
    az = read_word(ACCEL_XOUT_H + 4) / 16384.0
    gx = read_word(GYRO_XOUT_H) / 131.0
    gy = read_word(GYRO_XOUT_H + 2) / 131.0
    gz = read_word(GYRO_XOUT_H + 4) / 131.0

    print("Gyro:", gx, gy, gz)# This line should align with other lines above
    print("accel:", ax, ay, az)
    return {
        "accel_x": round(ax, 2),
        "accel_y": round(ay, 2),
        "accel_z": round(az, 2),
        "gyro_x": round(gx, 2),
        "gyro_y": round(gy, 2),
        "gyro_z": round(gz, 2)
    }


# Blink LED based on level
def blink_led(level):
    duration = 3  # seconds
    if level == 1:
        for _ in range(3): led.on(); time.sleep(0.5); led.off(); time.sleep(0.5)
    elif level == 2:
        for _ in range(6): led.on(); time.sleep(0.25); led.off(); time.sleep(0.25)
    elif level == 3:
        for _ in range(9): led.on(); time.sleep(0.166); led.off(); time.sleep(0.166)

# Web server
def web_server():
    addr = socket.getaddrinfo('0.0.0.0', 80)[0][-1]
    s = socket.socket()
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(addr)
    s.listen(1)
    print("Listening on", addr)

    while True:
        cl, addr = s.accept()
        request = cl.recv(1024).decode('utf-8')
        response = ""
        print("Request:", request)

        if 'GET /data' in request:
            sensor_data = get_sensor_data()
            response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n"
            response += ujson.dumps(sensor_data)
        elif 'POST /speed' in request:
            try:
                body = request.split('\r\n\r\n')[1]
                data = ujson.loads(body)
                level = data.get('level', 0)
                print(f"Received level: {level}")
                blink_led(level)
                response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK"
            except:
                response = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/plain\r\n\r\nInvalid JSON"
        else:
            try:
                with open("index.html", "r") as file:
                    html = file.read()
                response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + html
            except:
                response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nFile not found"

        cl.send(response)
        cl.close()

# Main
connect_wifi()
web_server()
while True:
    data = get_sensor_data()
    print(data)
    time.sleep(1)