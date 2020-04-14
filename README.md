[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Nintendo-Switch-Remote-Control

Remote control for the Nintendo Switch via UDP or with a single computer.

This project uses the LUFA library and reverse-engineering of the HORIPAD for Nintendo Switch for remote control of the console. It consists on two main parts:
* GUI: Java project with two programs, client and server.

  The client takes input from a real controller, keyboard or a Discord bot and sends it to the server over UDP.
  
  The server receives the input and sends it to an AVR microcontroller which acts as a controller. The MCU is plugged into the Switch dock and the console recognizes it as a HORIPAD S controller.
  
* Firmware: firmware that runs on the microcontroller. The server PC sends the received commands to the microcontroller via UART. When the Switch requests HID reports, the microcontroller sends them. It uses the LUFA library.

The goal of this project is to provide a way to play Switch games remotely (two computers) or control the console locally (one computer). Keep in mind this project doesn't include any video streaming service.

A complete diagram is shown below:
![Hardware diagram](/images/diagram.png)

__NOTE__: if you're using a FTDI-based serial adapter, yo need to [reduce the latency timer value](https://projectgus.com/2011/10/notes-on-ftdi-latency-with-arduino/).

Additional information can be found on the specific README files.

### Prerequisites
* A LUFA-compatible microcontroller such as the Teensy 2.0++, Arduino UNO R3, or the Arduino Micro
* A USB-to-UART (TTL) adapter. Popular ones are based on FTDI and CH340 chip. Can be easily found online.
* A PC with Java and JDK installed.

### TODO list

List of things that I might do in the future, no guarantee. Pull requests are welcome.

* More flexible communication between server and client. Maybe requests to increase or decrease send rate (basic flow control).
* Automatically decrease read timeout for FTDI-based serial adapters (see https://github.com/projectgus/hairless-midiserial/blob/master/src/PortLatency_win32.cpp).
* Power the board from the serial adapter and not USB (Switch), so that console can suspend without turning off the MCU. In that case, the Switch could be remotely turned on by long pressing the HOME button.
* Power reduction techniques (section 9.9 from ATmega16U2 datasheet).
* Refactoring of SerialAdapter.java. Blocking/non-blocking operations, use default procedures for syncing.
* Migrate from Jamepad to sdl2gdx (?).
* Java i18n (ResourceBundle?).
* Remove absolute positioning in layouts.

### Acknowledgments

* __wchill__ for the [SwitchInputEmulator](https://github.com/wchill/SwitchInputEmulator) project. My firmware is heavily based on his work.
* __progmem__ for the [Switch-Fightstick](https://github.com/progmem/Switch-Fightstick) repository, which itself is the base of __wchill__ work and created the opportunity to control the Switch with a Lufa-compatible MCU.
* __ItsDeidara__ author of the [CommunityController](https://github.com/ItsDeidara/CommunityController) repository and host of the namesake Twitch [channel](https://www.twitch.tv/communitycontroller). Gave some tips for hardware configuration and some of the serial port code is based on his work.
* __abcminiuser__, who created the [LUFA](https://github.com/abcminiuser/lufa) library (Lightweight USB Framework for AVRs).
