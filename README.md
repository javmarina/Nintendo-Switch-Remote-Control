[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://travis-ci.com/javmarina/Nintendo-Switch-Remote-Control.svg?branch=master)](https://travis-ci.com/javmarina/Nintendo-Switch-Remote-Control)

# Nintendo-Switch-Remote-Control

Remote control for the Nintendo Switch via UDP or with a single computer. **Pro Controller** emulation in AVR firmware

This project uses the LUFA library and reverse-engineering of the Pro Controller for Nintendo Switch for remote control of the console. It consists on three main parts:
* GUI: Java project with two programs, client and server.

  The client takes input from a real controller, keyboard or a Discord bot and sends it to the server over WebRTC.

  The server receives the input and sends it to an AVR microcontroller which acts as a controller. The MCU is plugged into the Switch dock and the console recognizes it as a Pro Controller. If you have a USB-C adapter, you should also be able to use this in handheld mode/Switch Lite.

  Finally, the server sends a real-time video and audio stream acquired from an HDMI capture card.

* Firmware: firmware that runs on the microcontroller. The server PC sends the received commands to the microcontroller via UART. When the Switch requests HID reports, the microcontroller sends them. It uses the LUFA library.

* Small Python code for fast prototyping (`python` folder).

The goal of this project is to provide a way to play Switch games remotely (two computers) or control the console locally (one computer).

A complete diagram is shown below:
![Hardware diagram](/images/diagram.png)

__NOTE__: if you're using a FTDI-based serial adapter, yo need to [reduce the latency timer value](https://projectgus.com/2011/10/notes-on-ftdi-latency-with-arduino/).

Additional information can be found on the specific README files.
* The [README](/gui/README.md) inside the `gui` folder contains information about the Java project, required setup and steps, known issues and instructions for configuring a Discord bot.

* Inside `firmware` folder [there are](/firmware/README.md) instructions for compiling and flashing the firmware for different boards, as well as a list of changes made to the original code.

### Prerequisites
* A LUFA-compatible microcontroller such as the Teensy 2.0++, Arduino UNO R3, or the Arduino Micro
* A USB-to-UART (TTL) adapter. Popular ones are based on FTDI and CH340 chip. Can be easily found online.
* An HDMI capture card compatible with `libuvc` (in general, any with USB output). Device quality can dramatically affect streaming performance.
* A deployed [signaling server](https://github.com/javmarina/Switch-Signaling-Server).
* A PC with Java and JDK installed.

### TODO list

List of things that I might do in the future, no guarantee. Pull requests are welcome.

* Automatically decrease read timeout for FTDI-based serial adapters (see https://github.com/projectgus/hairless-midiserial/blob/master/src/PortLatency_win32.cpp).
* Power the board from the serial adapter and not USB (Switch), so that console can suspend without turning off the MCU. In that case, the Switch could be remotely turned on by long pressing the HOME button.
* Power reduction techniques (section 9.9 from ATmega16U2 datasheet).
* Refactoring of SerialAdapter.java. Blocking/non-blocking operations, use default procedures for syncing.
* Migrate from Jamepad to sdl2gdx (?).

### Acknowledgments

* __devopvoid__ for his work on the [webrtc-java](https://github.com/devopvoid/webrtc-java) project.
* __mzyy94__ for his [work](https://mzyy94.com/blog/2020/03/20/nintendo-switch-pro-controller-usb-gadget/) on Pro Controller emulation using a Raspberry Pi.
* __wchill__ for the [SwitchInputEmulator](https://github.com/wchill/SwitchInputEmulator) project. My firmware was initially based on his work.
* __progmem__ for the [Switch-Fightstick](https://github.com/progmem/Switch-Fightstick) repository, which itself is the base of __wchill__ work and created the opportunity to control the Switch with a LUFA-compatible MCU.
* __ItsDeidara__ author of the [CommunityController](https://github.com/ItsDeidara/CommunityController) repository and host of the namesake Twitch [channel](https://www.twitch.tv/communitycontroller). Gave some tips for hardware configuration and some of the serial port code is based on his work.
* __abcminiuser__, who created the [LUFA](https://github.com/abcminiuser/lufa) library (Lightweight USB Framework for AVRs).
