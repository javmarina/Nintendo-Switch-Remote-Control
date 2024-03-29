# Introduction

This Java project contains a client and server module, which can be compiled, and additional utility modules: 'util' module that acts as a dependency of the other two and the 'WebRTC' module with WebRTC-specific code. I used IntelliJ Idea.

In order to use this project, you need both `client.jar` and `server.jar`. You can download them [here](https://github.com/javmarina/Nintendo-Switch-Remote-Control/releases/tag/latest). You can also modify the code and compile the JARs by running `gradle buildAll`. Both JARs will appear in the root folder.

The signaling server is provided and running on the cloud. Its code is available [here](https://github.com/javmarina/Switch-Signaling-Server). You can also deploy your own server and edit the `SIGNALING_SERVER` field in [SignalingPeer.java](https://github.com/javmarina/Nintendo-Switch-Remote-Control/blob/master/gui/WebRTC/src/main/java/com/javmarina/webrtc/signaling/SignalingPeer.java).

The [Jamepad](https://github.com/williamahartman/Jamepad) library is used for reading controller input. The Discord bot is implemented using [Discord4J](https://github.com/Discord4J/Discord4J).

Note that the server and client can run on the same PC.

# Known issues

- When a connection (session) is finished, both the client and server programs have to be restarted in order to connect again. Connection will refuse if programs are not closed before.

# Instructions

Please follow the instructions in the exact same order as below.
 1. Turn on Switch and the computers that will run the server and the client. Can be the same PC.
 2. Connect the Arduino/Teensy board to the serial adapter. Note that RX->RX and TX->TX (not the other way around). Also connect VCC to 5V and GND.
 3. Connect USB side of the serial adapter to the server computer.
 4. Connect Arduino/Teensy board to Switch via USB A to B cable.
 5. Open server program, configure options and click on "Open server". Note the session ID and wait for the MCU to sync.
 6. Open client program, configure options (particularly, write the same session ID), and click on "Start". Connection should be established soon.
 
If you can't get the setup running, [this issue](https://github.com/javmarina/Nintendo-Switch-Remote-Control/issues/2) might be helpful. Remember to set the correct baudrate for serial communication, which is 1 Mbps by default and is saved inside the compiled firmware. You can edit `BAUD` in [avr.h](/firmware/include/avr.h), recompile and reflash.

# Discord bot configuration
 
If you want to configure your own Discord bot, go to gui/client/src/main/resources and create a `discord.properties` file. The format is as follows:

    # Discord Switch key (see DiscordService.java)
    DiscordBotToken=YOUR_BOT_TOKEN