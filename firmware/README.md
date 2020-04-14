# How it works

On June 20, 2017, Nintendo released System Update v3.0.0 for the Nintendo Switch. Along with a number of additional features that were advertised or noted in the changelog, additional hidden features were added. One of those features allows for the use of compatible USB controllers on the Nintendo Switch, such as the HORIPAD S.

Unlike the Wii U, which handles these controllers on a 'per-game' basis, the Switch treats the controllers as if they were a Switch Pro Controller. Along with having the icon for the Pro Controller, they function just like it in terms of using it in other games, apart from the lack of gyroscope, acceleremeter, HD Rumble and NFC.

This code is based on [wchill's work](https://github.com/wchill/SwitchInputEmulator). His work is based on the popular [Switch-Fightstick](https://github.com/progmem/Switch-Fightstick) repository, but he made some improvements such as using HORIPAD S descriptors instead of the Pokken Controller ones. Most importantly, he added the UART interface, so commands can be sent from a computer and don't need to be programmed inside the microcontroller memory. He also included packet syncronization and CRC check.

My code has some minor improvements. The most important one is "connection lost detection". If the microcontroller doesn't receive new commands for a period of time, it sends custom commands to the console in order to pause the game and go to the HOME menu. Previously, the controller would stay blocked and the player could lost control of the game if the connection was unstable.

# Compilation instructions

The first thing you need to do is grabing the LUFA library. You can download it in a zipped folder at the bottom of [this page](http://www.fourwalledcubicle.com/LUFA.php) or you can clone [this](https://github.com/abcminiuser/lufa) repository in your computer. Rename the folder to `LUFA`. Then, download or clone the contents of this repository onto your computer. Next, you'll need to make sure the `LUFA_PATH` inside of the `Makefile` points to the `LUFA` subdirectory inside your `LUFA` directory. I recommend putting the `LUFA` inside `firmware`, so the current `Makefile` (`LUFA_PATH = ../LUFA/LUFA`). You can also put the `LUFA` folder elsewhere, in which case you will need to write the complete path.

Then, edit the MCU value in `Makefile` according to the microcontroller (e.g. at90usb1286 for a Teensy 2.0++, atmega16u2 for an Arduino UNO R3 and atmega32u4 for Arduino Micro).

Next, download packages for your device:
* If using Teensy 2.0++, go to the Teensy website and download/install the [Teensy Loader application](https://www.pjrc.com/teensy/loader.html). For Linux, follow their instructions for installing the [GCC Compiler and Tools](https://www.pjrc.com/teensy/gcc.html). For Windows, you will need the [latest AVR toolchain](https://www.microchip.com/mplab/avr-support/avr-and-arm-toolchains-c-compilers) from the Microchip site. See [this issue](https://github.com/LightningStalker/Splatmeme-Printer/issues/10) and [this thread](http://gbatemp.net/threads/how-to-use-shinyquagsires-splatoon-2-post-printer.479497/) on GBAtemp for more information. (Note for Mac users - the AVR MacPack is now called AVR CrossPack. If that does not work, you can try installing `avr-gcc` with `brew`.)
* If using Arduino Uno R3, you will need the AVR GCC Compiler and Tools like Teensy 2.0++. (Again for Mac users - try brew, adding the [osx-cross/avr](osx-cross/avr) repository, all you need to do is to type `brew tap osx-cross/avr` and `brew install avr-gcc`).
* For Arduino Micro, the steps are the same as Arduino Uno R3.

Finally, open a terminal window in the `firmware` directory, type `make`, and hit enter to compile. If all goes well, the printout in the terminal will let you know it finished the build. A `Joystick.hex` file will appear.

__NOTE__ for Windows users: I haven't used the AVR toolchain, and instead follow the steps for Linux using Windows Subsystem for Linux (WSL).

# Flashing instructions

## Teensy 2.0++

Follow the directions on flashing `Joystick.hex` onto your Teensy, which can be found page where you downloaded the Teensy Loader application.

## Arduino Uno R3

You will need to set your [Arduino in DFU mode](https://www.arduino.cc/en/Hacking/DFUProgramming8U2), and flash its USB controller (not the main ATmega328P controller).

How to set the Arduino Uno R3 in DFU mode:
1. Connect the board to the computer with a USB cable.
2. Put the board so that the USB connector is on top. There is a group of 3x2 male pins, the ICSP connector, close to the USB connector and "AREF" label.
3. Connect the pin closer to the red reset button (on the top-left) to GND permanently.
4. See the capacitor on the right of the "RESET EN" label? Connect it to ground for a second and disconnect. The LEDs on the board should blink.
5. Disconnect the first wire. Now the Arduino is in DFU mode and the computer should recognize it correctly.

If you're stuck, I think [this tutorial](https://www.youtube.com/watch?v=fSXZMVdO5Sg) is very helpful.

Please note that once the board is flashed, you will need to flash it back with the original firmware to make it work again as a standard Arduino.

Follow the [DFU mode directions](https://www.arduino.cc/en/Hacking/DFUProgramming8U2) to flash `Joystick.hex` onto your Arduino UNO R3. To sum up, you'll need FLIP for Windows and commands for Linux and Mac.

__NOTE__ for Windows users: select device (Device > Select... > ATMega16U2) open a connection (Settings > Communication > USB > Open), then load the file (File > Load HEX File...), mark all checkboxes in Operations Flow and press Run. If you get a "AtLibUsbDfu.dll not found" error, follow the instructions [here](https://youtu.be/KQ9BjKjGnIc?t=180).

## Arduino Micro

The Arduino Micro is more like the Teensy in that it has a single microcontroller that communicates directly over USB. Most of the steps are the same as those for the Teensy, except do not download Teensy Loader program.

Once finished building, start up Arduino IDE. Under `File -> Preferences`, check `Show verbose output during: upload` and pick OK. With the Arduino plugged in and properly selected under `Tools`, upload any sketch. Find the line with `avrdude` and copy the entire `avrdude` command and all options into a terminal, replacing the `.hex` file and path to the location of the `Joystick.hex` created in the previous step. Also make sure the `-P/dev/??` port is the same as what Arduino IDE is currently reporting. Now double tap the reset button on the Arduino and quickly press Enter in the terminal. This may take several tries. You may need to press Enter first and then the reset button or try various timings. Eventually, `avrdude` should report success. Store the `avrdude` command in a text file or somewhere safe since you will need it every time you want to print a new image.

Sometimes, the Arduino will show up under a different port, so you may need to run Arduino IDE again to see the current port of your Micro.

If you ever need to use your Arduino Micro with Arduino IDE again, the process is somewhat similar. Upload your sketch in the usual way and double tap reset button on the Arduino. It may take several tries and various timings, but should eventually be successful.

The Arduino Leonardo is theoretically compatible, but has not been tested. It also has the ATmega32u4, and is layed out somewhat similar to the Micro.
