#include <Wire.h>
#include <TinyScreen.h>
#include <SPI.h>
#include <STBLE.h>
#include <TimeLib.h>

#define BLACK           0x00
#define WHITE           0xFF
#define RED             0x03
#define GREEN           0x1C

TinyScreen display = TinyScreen(TinyScreenDefault);

//Debug output adds extra flash and memory requirements!
#ifndef BLE_DEBUG
#define BLE_DEBUG true
#endif

#if defined (ARDUINO_ARCH_AVR)
#define SerialMonitorInterface Serial
#endif


uint8_t ble_rx_buffer[21];
uint8_t ble_rx_buffer_len = 0;
uint8_t ble_connection_state = false;
uint8_t ble_connection_displayed_state = true;

void setup() {
  SerialMonitorInterface.begin(9600);
  while (!SerialMonitorInterface);
  Wire.begin();
  display.begin();
  display.setFlip(1);
  BLEsetup();
  display.setFont(liberationSans_10ptFontInfo);
  display.setCursor(0,0);
  display.fontColor(WHITE,BLACK);
  display.print ("Bluetooth:");
  display.setCursor(0,20);
  display.fontColor(WHITE,BLACK);
  display.print("Disconnected");
}
void loop() {
  aci_loop();//Process any ACI commands or events from the NRF8001- main BLE handler, must run often. Keep main loop short.
    if (ble_rx_buffer_len)
    {
        display.clearScreen();
        displayprompt();
        display.clearScreen();
    }
  

}

void homescreen(){
    DateDisplay();
  TimeDisplay();
  updateBLEstatusDisplay();
  displayBattery();
}
void displayprompt()
{
  while (1){
    display.on();
     display.setFont(liberationSans_10ptFontInfo);
    display.setCursor(0,0);
    display.fontColor(WHITE,BLACK);
    display.print("Someone is at");
    display.setCursor(0,12);
    display.fontColor(WHITE,BLACK);
    display.print("the door");
    display.setCursor(0,25);
    display.fontColor(WHITE,BLACK);
    display.print("Open door?");
    display.setCursor(0,43);
    display.fontColor(GREEN,BLACK);
    display.print("Yes");
    display.setCursor(81,43);
    display.fontColor(RED,BLACK);
    display.print("No");
    if (display.getButtons(TSButtonLowerLeft)) {
        display.clearScreen();
        display.setFont(liberationSans_16ptFontInfo);
        display.fontColor(GREEN,BLACK);
        display.setCursor(5,20);
        display.print("Door open");
        lib_aci_send_data(0, "1", 2);
        ble_rx_buffer_len =0;
        delay(1000);
        GAP_DisconnectionComplete_CB();
        delay(1000);
        break;
      }
    else if (display.getButtons(TSButtonLowerRight)) {
        display.clearScreen();
        lib_aci_send_data(0, "0", 2);
        ble_rx_buffer_len =0;
        delay(1000);
        GAP_DisconnectionComplete_CB();
        delay(1000);
        break;
    }
    
  }
}





void updateBLEstatusDisplay() {
  if (ble_connection_state == ble_connection_displayed_state)
    return;
  ble_connection_displayed_state = ble_connection_state;
  int x = 62;
  int y = 6;
  int s = 2;
  uint8_t color = 0x03;
  if (ble_connection_state)
    color = 0xE0;
  display.drawLine(x, y + s + s, x, y - s - s, color);
  display.drawLine(x - s, y + s, x + s, y - s, color);
  display.drawLine(x + s, y + s, x - s, y - s, color);
  display.drawLine(x, y + s + s, x + s, y + s, color);
  display.drawLine(x, y - s - s, x + s, y - s, color);
}

void displayBattery() {
  int result = 0;
#if defined (ARDUINO_ARCH_AVR)
  //http://forum.arduino.cc/index.php?topic=133907.0
  const long InternalReferenceVoltage = 1100L;
  ADMUX = (0 << REFS1) | (1 << REFS0) | (0 << ADLAR) | (1 << MUX3) | (1 << MUX2) | (1 << MUX1) | (0 << MUX0);
  delay(10);
  ADCSRA |= _BV( ADSC );
  while ( ( (ADCSRA & (1 << ADSC)) != 0 ) );
  result = (((InternalReferenceVoltage * 1024L) / ADC) + 5L) / 10L;
  //SerialMonitorInterface.println(result);
  //if(result>440){//probably charging
  uint8_t charging = false;
  if (result > 450) {
    charging = true;
  }
  result = constrain(result - 300, 0, 120);
  uint8_t x = 70;
  uint8_t y = 3;
  uint8_t height = 5;
  uint8_t length = 20;
  uint8_t amtActive = (result * length) / 120;
  uint8_t red, green, blue;
  display.drawLine(x - 1, y, x - 1, y + height, 0xFF); //left boarder
  display.drawLine(x - 1, y - 1, x + length, y - 1, 0xFF); //top border
  display.drawLine(x - 1, y + height + 1, x + length, y + height + 1, 0xFF); //bottom border
  display.drawLine(x + length, y - 1, x + length, y + height + 1, 0xFF); //right border
  display.drawLine(x + length + 1, y + 2, x + length + 1, y + height - 2, 0xFF); //right border
  for (uint8_t i = 0; i < length; i++) {
    if (i < amtActive) {
      red = 63 - ((63 / length) * i);
      green = ((63 / length) * i);
      blue = 0;
    } else {
      red = 32;
      green = 32;
      blue = 32;
    }
    display.drawLine(x + i, y, x + i, y + height, red, green, blue);
  }
#endif
}


void DateDisplay(){
    int currentDay = day();
  int currentMonth = month();
  int currentYear = year();
    display.setFont(liberationSansNarrow_10ptFontInfo);
  display.fontColor(WHITE, BLACK);
  display.setCursor(2, 2);
   display.print(dayShortStr(weekday()));
  display.print(' ');
  display.print(month());
  display.print('/');
  display.print(day());
  display.print(F("  "));
}

void TimeDisplay(){
Hour();
Minute();
Second();
}

void Hour(){
    int currentHour = hourFormat12();
    display.fontColor(WHITE, BLACK);
    display.setFont(liberationSansNarrow_22ptFontInfo);
    display.setCursor(5, 25);
    if(currentHour < 10)
      display.print('0');
    display.print(currentHour);
    display.write(':');
  
}

void Minute(){
    int currentMinute = minute();
    display.fontColor(WHITE, BLACK);
    display.setFont(liberationSansNarrow_22ptFontInfo);
    display.setCursor(34, 25);
    if (currentMinute < 10)
      display.print('0');
    display.print(currentMinute);
    display.write(':');
}

void Second (){
    int currentSecond = second();
    display.fontColor(WHITE, BLACK);
    display.setFont(liberationSansNarrow_22ptFontInfo);
    display.setCursor(63, 25);
    if (currentSecond < 10)display.print('0');
    display.print(currentSecond);
}
