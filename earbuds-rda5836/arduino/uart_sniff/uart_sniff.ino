#include <SoftwareSerial.h>

// D2 = chip TX -> Nano RX (listen only, TX pin left unused/unconnected)
SoftwareSerial target(2, 3); // RX, TX (TX unused for now)

void setup() {
  Serial.begin(115200);      // USB side, to the Mac
  target.begin(115200);      // guess -- change if nothing/garbage shows up
  Serial.println("# UART sniff ready");
}

void loop() {
  if (target.available()) {
    int b = target.read();
    if (b >= 0x20 && b < 0x7F) Serial.write((char)b);
    else {
      Serial.print('[');
      if (b < 0x10) Serial.print('0');
      Serial.print(b, HEX);
      Serial.print(']');
    }
  }
}
