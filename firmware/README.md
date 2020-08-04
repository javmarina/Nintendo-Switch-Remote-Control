# How it works

Based on valuable [reverse engineering information](https://github.com/dekuNukem/Nintendo_Switch_Reverse_Engineering), this AVR firmware emulates a Nintendo Switch Pro Controller.

The first version of this firmware emulated a HORIPAD S controller, and was inspired by [wchill's work](https://github.com/wchill/SwitchInputEmulator), which itself is an extension of the [Switch-Fightstick](https://github.com/progmem/Switch-Fightstick) project. __wchill__ added UART support to the original firmware, giving the ability to send packets to the microcontroller in a straightforward way, and used the original HORIPAD S descriptors instead of the Pokken Controller ones. While the original project had controller input pre-programmed, __wchill__ work enabled real-time controller input with packet synchronization and error detection (CRC).

However, emulating the HORIPAD S controller implies some limitations, as it's not a first-class Switch controller. For example, accelerometer and gyroscope aren't supported, nor are advanced features such as HD Rumble and [custom controller colors](https://github.com/CTCaer/jc_toolkit).

For that reason, I modified the original firmware so that it works as a Pro Controller instead of a HORIPAD S. Key changes made to the code:

* A sync protocol is used by the Switch. If Pro Controller doesn't success, it's not recognized as a valid controller. HORIPAD S worked fine from the moment it was plugged in. As a side note, the Pro Controller might not be added to the controller list instantly.

* USB communication is two-way. HORIPAD S doesn't receive requests from Switch.

* USB descriptors differ significantly (mostly HID report descriptors).

* Switch might request to read the SPI flash memory of the Pro Controller. This firmware doesn't include a full Pro Controller flash memory, but supports most common read requests. Write is not allowed.

* Controller input format is totally different.

  * Some unsupported buttons are added (such as SR and SL, only found in Joy-Cons), because all official controllers (Pro, Joy-Cons, Charging Grip) mostly share the same firmware.
  * Controller input includes information about connection status and battery level.
  * Joystick axes are 12 bits each instead of 1 byte.
  * IMU, NFC and IR data can be included (not supported in HORIPAD S).

As well as emulating a Pro Controller, I also made some changes to __wchill__'s firmware. The most important one is "connection lost detection". If the microcontroller doesn't receive new commands for a period of time, it sends custom commands to the console in order to pause the game and go to the HOME menu. Previously, the controller would stay blocked and the player could lost control of the game if the connection was unstable.

To the best of my knowledge, this is the first attempt to emulate a Pro Controller with an AVR microcontroller. Similar efforts have been made with Raspberry Pi, such as [mzyy94's work](https://mzyy94.com/blog/2020/03/20/nintendo-switch-pro-controller-usb-gadget/), which I found extremely helpful.

I have kept the original firmware [here](https://github.com/javmarina/Nintendo-Switch-Remote-Control/tree/HORIPAD) for future reference.

# Compilation instructions

The LUFA library is provided in this repository as a [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules). After `git clone`, you will need to run `git submodule update --init --recursive` in order to fetch the library folder. You can also clone this repository using `git clone --recurse-submodules`, which will clone and download all submodules in one step.

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
