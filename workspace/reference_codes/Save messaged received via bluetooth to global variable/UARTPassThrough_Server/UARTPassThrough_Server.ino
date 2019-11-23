//-------------------------------------------------------------------------------
//  TinyCircuits ST BLE TinyShield UART Example Sketch
//  Last Updated 2 March 2016
//
//  This demo sets up the BlueNRG-MS chipset of the ST BLE module for compatiblity 
//  with Nordic's virtual UART connection, and can pass data between the Arduino
//  serial monitor and Nordic nRF UART V2.0 app or another compatible BLE
//  terminal. This example is written specifically to be fairly code compatible
//  with the Nordic NRF8001 example, with a replacement UART.ino file with
//  'aci_loop' and 'BLEsetup' functions to allow easy replacement. 
//
//  Written by Ben Rose, TinyCircuits http://tinycircuits.com
//
//-------------------------------------------------------------------------------

#define MAX_WIDTH       96    /* 96 Pixels */
#define MAX_HEIGHT      64    /* 64 Pixels */
#define BLACK           0x00
#define BLUE            0xE0
#define RED             0x03
#define GREEN           0x1C
#define DGREEN          0x0C
#define YELLOW          0x1F
#define WHITE           0xFF
#define ALPHA           0xFE
#define BROWN           0x32

#include <SPI.h>
#include <STBLE.h>
#include <TinyScreen.h>
#include <Wire.h>

//Debug output adds extra flash and memory requirements!
#ifndef BLE_DEBUG
#define BLE_DEBUG true
#endif

#if defined (ARDUINO_ARCH_AVR)
#define SerialMonitorInterface Serial
#elif defined(ARDUINO_ARCH_SAMD)
#define SerialMonitorInterface SerialUSB
#endif

TinyScreen display = TinyScreen(0);
uint8_t ble_rx_buffer[21];
uint8_t ble_rx_buffer_len = 0;
uint8_t ble_connection_state = false;
#define PIPE_UART_OVER_BTLE_UART_TX_TX 0

void setup() {
  Wire.begin();
  display.begin();
  display.setFlip(true);
  SerialMonitorInterface.begin(9600);
  while (!SerialMonitorInterface); //This line will block until a serial monitor is opened with TinyScreen+!
  BLEsetup();
}

char *received_message = "";  /* Message received from bluetooth */
    
void loop() {
  display_prompt();
  while(1)
  {
    /* --> Removed: server-side, buttons disabled until have a required function
    if (display.getButtons(TSButtonLowerLeft)) {
      display.clearScreen();
      display.setFont(liberationSans_16ptFontInfo);
      display.fontColor(GREEN,BLACK);
      display.setCursor(5,20);
      display.print("Door open");
      lib_aci_send_data(0, "1", 2);
      delay(1000);
      display.clearScreen();
      display_prompt();
    }
    else if (display.getButtons(TSButtonLowerRight)) {
      display.clearScreen();
      lib_aci_send_data(0, "0", 2);
      delay(1000);
      display_prompt();
    }
    */
    aci_loop();//Process any ACI commands or events from the NRF8001- main BLE handler, must run often. Keep main loop short.
    if (ble_rx_buffer_len) {//Check if data is available
      SerialMonitorInterface.print(ble_rx_buffer_len);
      SerialMonitorInterface.print(" : ");
      /* Store message to global variable */
      received_message = (char*)ble_rx_buffer; 
      SerialMonitorInterface.println(received_message); /* Receive Message */
      if(strstr(received_message, "1"))
      {
        SerialMonitorInterface.println("DOOR OPEN"); /* Receive Message */
        display.clearScreen();
        display.setFont(liberationSans_16ptFontInfo);
        display.fontColor(GREEN,BLACK);
        display.setCursor(5,20);
        display.print("Door Open");
        delay(1000);
        display.clearScreen();
        display_prompt();
      }
      else
      {
        SerialMonitorInterface.println("DOOR REMAINS"); /* Receive Message */
        display.clearScreen();
        display.setFont(liberationSans_12ptFontInfo);
        display.fontColor(GREEN,BLACK);
        display.setCursor(3,20);
        display.print("Door Remains");
        delay(1000);
        display.clearScreen();
        display_prompt();
      }
      ble_rx_buffer_len = 0;//clear afer reading
    }
    if (SerialMonitorInterface.available()) {//Check if serial input is available to send
      delay(10);//should catch input
      uint8_t sendBuffer[21];
      uint8_t sendLength = 0;
      while (SerialMonitorInterface.available() && sendLength < 19) {
        sendBuffer[sendLength] = SerialMonitorInterface.read();
        sendLength++;
      }
      if (SerialMonitorInterface.available()) {
        SerialMonitorInterface.print(F("Input truncated, dropped: "));
        if (SerialMonitorInterface.available()) {
          SerialMonitorInterface.write(SerialMonitorInterface.read());
        }
      }
      sendBuffer[sendLength] = '\0'; //Terminate string
      sendLength++;
      if (!lib_aci_send_data(PIPE_UART_OVER_BTLE_UART_TX_TX, (uint8_t*)sendBuffer, sendLength))
      {
        SerialMonitorInterface.println(F("TX dropped!"));
      }
    }
  } 
}

void display_prompt()
{
//  display.setFont(liberationSans_10ptFontInfo);
//  display.setCursor(0,0);
//  display.fontColor(WHITE,BLACK);
//  display.print("Do you want to");
//  display.setCursor(0,16);
//  display.print("open door?");
//  display.setCursor(0,43);
//  display.fontColor(GREEN,BLACK);
//  display.print("Yes");
//  display.setCursor(81,43);
//  display.fontColor(RED,BLACK);
//  display.print("No");
  int width = 0;
  char *display_str = "";
  
  display_str = "Welcome to my home!";
  display.setFont(liberationSans_8ptFontInfo);
  display.setCursor((MAX_WIDTH/4),0);
  display.fontColor(BLUE,BLACK);
  display.print ("Welcome!");
  display.fontColor(WHITE,BLACK);
  display.setCursor(0,10);
  display.print("Please send a");
  display.setCursor(0,20);
  display.print("bluetooth request");
  display.setCursor(0,30);
  display.print("to the owner and");
  display.setCursor(0,40);
  display.print("wait for a reply.");
  display.setCursor((MAX_WIDTH/4),50);
  display.fontColor(GREEN,BLACK);
  display.print("Thank you!");
}
