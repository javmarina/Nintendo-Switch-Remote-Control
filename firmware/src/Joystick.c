/*
Nintendo Switch Remote Control

Based on the LUFA library's Low-Level Joystick Demo
    (C) Dean Camera
Based on the HORI's Pokken Tournament Pro Pad design
    (C) HORI

This project implements a modified version of HORI's HORIPAD S
USB descriptors to allow for the creation of custom controllers for the
Nintendo Switch. This also works to a limited degree on the PS3.

Since System Update v3.0.0, the Nintendo Switch recognizes third-party
controllers as a Pro Controller. Physical design limitations prevent
the HORIPAD S from functioning at the same level as the Pro Controller.
However, by default most of the descriptors are there. Descriptor
modification allows us to unlock these buttons for our use.
*/

#include "Joystick.h"
#include <stdio.h>
#include <avr.h>

typedef enum {
    SYNCED,
    SYNC_START,
    SYNC_1,
    OUT_OF_SYNC
} State_t;

typedef struct {
    uint8_t input[8];
    uint8_t crc8_ccitt;
    uint8_t received_bytes;
} USB_Input_Packet_t;

static USB_Input_Packet_t usbInput;
static USB_JoystickReport_Input_t buffer;
static USB_JoystickReport_Input_t defaultBuf;
static USB_JoystickReport_Input_t homeBuf;
static State_t state = OUT_OF_SYNC;
static uint16_t count = 0;

// Delay since last valid packet from UART:
// 0                          120 ms                        800 ms               1 second
// |  Send last packet received  |  Send empty packet (pause)  |  Send HOME command  |  Send empty packet
// Connection lost detection: if 120 milliseconds elapse without a new packet from UART, stop Switch input (send empty
// commands). If delay grows to 800 ms, a HOME command is sent during 200 ms in order to open the HOME menu and pause the
// game. Note that the Switch might not accept the HOME command if the duration is less than 200 ms.
// Formula: macro = period/8
#define FAILS_UNTIL_PAUSE 15 // 120 ms
#define FAILS_UNTIL_HOME 100 // 0.8 sec
#define HOME_PACKET_TIMES 25 // 200 ms (from 0.8 seconds to 1 second)

/*
 * Receive byte from UART. Reads the byte and acts depending on the current state. Includes synchronization logic and
 * reading packets.
 * When the state is SYNCED, the Arduino is constantly reading from serial and making up arrays of 9 bytes. 8 bytes
 * contain actual information about the controller state, and the last one is a CRC checksum. If CRC is OK, the buffer
 * variable is updated. This variable is read every time the Arduino has to send a HID report to the Switch. That means
 * if something went wrong, the buffer variable is not updated.
 */
// USART Interrupt Service Routine
ISR(USART1_RX_vect) {
	uint8_t b = recv_byte();
	// Most probable case on top
	if (state == SYNCED) {
		if (usbInput.received_bytes < 8) {
			// Still filling up the buffer
			usbInput.input[usbInput.received_bytes++] = b;
			usbInput.crc8_ccitt = _crc8_ccitt_update(usbInput.crc8_ccitt, b);
		} else {
			// We have 9 bytes ready in usbInput
			if (usbInput.crc8_ccitt != b) {
				// Last bytes is not valid CRC. It could be because host wants to re-sync
				if (b == COMMAND_SYNC_START) {
					// Start sync
					state = SYNC_START;
					send_byte(RESP_SYNC_START);
				} else {
					// Mismatched CRC
					send_byte(RESP_UPDATE_NACK);
				}
			} else {
				// Everything is ok
				// Send ACK as soon as possible so that client is not waiting forever for a response if it uses blocking-mode
				send_byte(RESP_UPDATE_ACK);
				// Populate buffer values
				buffer.Button = (usbInput.input[0] << 8) | usbInput.input[1];
				buffer.HAT = usbInput.input[2];
				buffer.LX = usbInput.input[3];
				buffer.LY = usbInput.input[4];
				buffer.RX = usbInput.input[5];
				buffer.RY = usbInput.input[6];
				buffer.VendorSpec = usbInput.input[7];
				// Reset counter
				count = 0;
			}
			usbInput.received_bytes = 0;
			usbInput.crc8_ccitt = 0;
		}
	} else if (state == SYNC_START) {
        if (b == COMMAND_SYNC_1) {
            // Synchronization process continues to second step
            state = SYNC_1;
            send_byte(RESP_SYNC_1);
        }
        else state = OUT_OF_SYNC;
        // Unsuccessful synchronization
    } else if (state == SYNC_1) {
        if (b == COMMAND_SYNC_2) {
            // Synchronization process has completed successfully
            state = SYNCED;
            send_byte(RESP_SYNC_OK);
        }
        else state = OUT_OF_SYNC;
        // Unsuccessful synchronization
    }

    if (state == OUT_OF_SYNC) {
        if (b == COMMAND_SYNC_START) {
            // Synchronization process starts with first step
            state = SYNC_START;
            send_byte(RESP_SYNC_START);
        }
    }
}

// Main entry point.
int main(void) {
    // We also need to initialize the initial input reports.

    // Assign default controller state values (no buttons pressed and centered sticks)
    memset(&defaultBuf, 0, sizeof(USB_JoystickReport_Input_t));
    defaultBuf.LX = STICK_CENTER;
    defaultBuf.LY = STICK_CENTER;
    defaultBuf.RX = STICK_CENTER;
    defaultBuf.RY = STICK_CENTER;
    defaultBuf.HAT = HAT_CENTER;

    // Create homeBuf as a copy of defaultBuf with HOME button pressed
    memcpy(&homeBuf, &defaultBuf, sizeof(USB_JoystickReport_Input_t));
    homeBuf.Button = SWITCH_HOME;

    // For now, copy defaultBuf into buffer
    memcpy(&buffer, &defaultBuf, sizeof(USB_JoystickReport_Input_t));

    memset(&usbInput, 0, sizeof(USB_Input_Packet_t));

    // We'll start by performing hardware and peripheral setup.
    SetupHardware();
    // We'll then enable global interrupts for our use.
    GlobalInterruptEnable();
    // Once that's done, we'll enter an infinite loop.
    for (;;) {
        // We need to run our task to process and deliver data for our IN and OUT endpoints.
        HID_Task();
        // We also need to run the main USB management task.
        USB_USBTask();
    }
}

// Configures hardware and peripherals, such as the USB peripherals. Only run once.
void SetupHardware(void) {
    // We need to disable watchdog if enabled by bootloader/fuses.
    disable_watchdog();

    // We need to disable clock division before initializing the USB hardware.
    clock_prescale_set(clock_div_1);
    // We can then initialize our hardware and peripherals, including the USB stack.
    USART_Init(); // Change the baudrate in the function itself (BAUD macro)

    // Initialize the board LED driver before first use
	LEDs_Init();

    // The USB stack should be initialized last.
    USB_Init();
}

// Fired to indicate that the device is enumerating.
void EVENT_USB_Device_Connect(void) {
    // We can indicate that we're enumerating here (via status LEDs, sound, etc.).
}

// Fired to indicate that the device is no longer connected to a host.
void EVENT_USB_Device_Disconnect(void) {
    // We can indicate that our device is not ready (via status LEDs, sound, etc.).
}

// Fired when the host set the current configuration of the USB device after enumeration.
void EVENT_USB_Device_ConfigurationChanged(void) {
    bool ConfigSuccess = true;

    // We setup the HID report endpoints.
    ConfigSuccess &= Endpoint_ConfigureEndpoint(JOYSTICK_OUT_EPADDR, EP_TYPE_INTERRUPT, JOYSTICK_EPSIZE, 1);
    ConfigSuccess &= Endpoint_ConfigureEndpoint(JOYSTICK_IN_EPADDR, EP_TYPE_INTERRUPT, JOYSTICK_EPSIZE, 1);

    // We can read ConfigSuccess to indicate a success or failure at this point.
}

// Process control requests sent to the device from the USB host.
void EVENT_USB_Device_ControlRequest(void) {
    // We can handle two control requests: a GetReport and a SetReport.

    // Not used here, it looks like we don't receive control request from the Switch.
}

// Process and deliver data from IN and OUT endpoints.
void HID_Task(void) {
    // If the device isn't connected and properly configured, we can't do anything here.
    if (USB_DeviceState != DEVICE_STATE_Configured) {
        return;
    }

    // We'll start with the OUT endpoint.
    Endpoint_SelectEndpoint(JOYSTICK_OUT_EPADDR);
    // We'll check to see if we received something on the OUT endpoint.
    if (Endpoint_IsOUTReceived()) {
        // If we did, and the packet has data, we'll react to it.
        if (Endpoint_IsReadWriteAllowed()) {
            // We'll create a place to store our data received from the host.
            USB_JoystickReport_Output_t JoystickOutputData;

            // We'll then take in that data, setting it up in our storage.
            while (Endpoint_Read_Stream_LE(&JoystickOutputData, sizeof(JoystickOutputData), NULL) != ENDPOINT_RWSTREAM_NoError);

            // At this point, we can react to this data.

            // However, since we're not doing anything with this data, we abandon it.
        }
        // Regardless of whether we reacted to the data, we acknowledge an OUT packet on this endpoint.
        Endpoint_ClearOUT();
    }

    // We'll then move on to the IN endpoint.
    Endpoint_SelectEndpoint(JOYSTICK_IN_EPADDR);
    // We first check to see if the host is ready to accept data.
    if (Endpoint_IsINReady()) {
        // We'll create an empty report.
        USB_JoystickReport_Input_t JoystickInputData;

        // We'll then populate this report with what we want to send to the host.
        disable_rx_isr();
        if (state == SYNCED) {
            if (count >= FAILS_UNTIL_PAUSE) {
                // Trigger "connection lost" mode
				// The same controller state was sent too many times to the Switch. That means we're not
				// receiving from UART or the CRC is incorrect (host not synced).
                if (count >= FAILS_UNTIL_HOME && count < FAILS_UNTIL_HOME + HOME_PACKET_TIMES) {
					// Send HOME command and change LEDs
					LEDs_SetAllLEDs(LEDMASK_PAUSE_HOME_BUFFER);
                    memcpy(&JoystickInputData, &homeBuf, sizeof(USB_JoystickReport_Input_t));
                } else {
					// Send empty packet (no input)
					LEDs_SetAllLEDs(LEDMASK_PAUSE_EMPTY_BUFFER);
                    memcpy(&JoystickInputData, &defaultBuf, sizeof(USB_JoystickReport_Input_t));
                }
				
				// Prevent overflow, there is no need to increment count if we're in the last state
                if (count <= FAILS_UNTIL_HOME + HOME_PACKET_TIMES) {
                    count++;
                }                
            } else {
				LEDs_SetAllLEDs(LEDMASK_SYNCED);
                // Send last controller state (from UART) to console and increment "times sent" counter
                memcpy(&JoystickInputData, &buffer, sizeof(USB_JoystickReport_Input_t));
                count++;
            }
			//send_byte(RESP_USB_ACK);
        } else {
			LEDs_SetAllLEDs(LEDMASK_NOT_SYNCED);
            // If not synced, use default buffer
            memcpy(&JoystickInputData, &defaultBuf, sizeof(USB_JoystickReport_Input_t));
			// Turn off all LEDs
        }
        // Re-enable interrupt, will call ISR if a transmission finished while populating the report
        enable_rx_isr();

        // Once populated, we can output this data to the host. We do this by first writing the data to the control stream.
        while(Endpoint_Write_Stream_LE(&JoystickInputData, sizeof(JoystickInputData), NULL) != ENDPOINT_RWSTREAM_NoError);
        
        // We then send an IN packet on this endpoint.
        Endpoint_ClearIN();
    }
}
