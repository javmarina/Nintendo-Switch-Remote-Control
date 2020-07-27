/*
Nintendo Switch Remote Control

Based on the LUFA library's Low-Level Joystick Demo
    (C) Dean Camera
Based on the HORI's Pokken Tournament Pro Pad design
    (C) HORI
Based on the Nintendo Pro Controller design
    (C) Nintendo

This project implements a modified version of the Nintendo Pro Controller
USB descriptors to allow for the creation of custom controllers for the
Nintendo Switch.

By using the same USB descriptors as the Pro Controller and its
reverse-engineered protocol, the Switch can recognize the AVR
microcontroller as a real Pro Controller. We can therefore overcome
the limitations of the HORIPAD S or other USB controllers, such as
vibration, IMU data and controller color in the Switch UI.
*/

#include "Joystick.h"

typedef enum {
    SYNCED,
    SYNC_START,
    SYNC_1,
    OUT_OF_SYNC
} State_t;

static Serial_Input_Packet_t serialInput;

static USB_StandardReport_t controllerReport;
static USB_StandardReport_t idleReport;
static USB_StandardReport_t *selectedReport; // Either &controllerReport or &idleReport

// Messages from Switch
static uint8_t switchResponseBuffer[JOYSTICK_EPSIZE];

// Current sync state
static State_t state = OUT_OF_SYNC;
static uint16_t millis = 0;

// Delay since last valid packet from UART:
// 0                          120 ms                        800 ms               1 second
// |  Send last packet received  |  Send empty packet (pause)  |  Send HOME command  |  Send empty packet
// Connection lost detection: if 120 milliseconds elapse without a new packet from UART, stop Switch input (send empty
// commands). If delay grows to 800 ms, a HOME command is sent during 200 ms in order to open the HOME menu and pause the
// game. Note that the Switch might not accept the HOME command if the duration is less than 200 ms.
#define MILLIS_UNTIL_PAUSE		120 // ms
#define MILLIS_UNTIL_HOME		800 // 0.8 sec
#define MILLIS_HOME_PRESSED		200 // ms (from 0.8 seconds to 1 second)

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
		if (serialInput.received_bytes < 8) {
			// Still filling up the buffer
			serialInput.input[serialInput.received_bytes++] = b;
			serialInput.crc8_ccitt = _crc8_ccitt_update(serialInput.crc8_ccitt, b);
		} else {
			// We have 9 bytes ready in serialInput
			if (serialInput.crc8_ccitt != b) {
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
				// Populate report values and select it
				populate_report_from_serial(&serialInput, &controllerReport);
				selectedReport = &controllerReport;
				// Reset and resume timer
				millis = 0;
			}
			serialInput.received_bytes = 0;
			serialInput.crc8_ccitt = 0;
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

/*
 * Take UART input and put values in the controller report
 */
void populate_report_from_serial(Serial_Input_Packet_t *serialInputPacket, USB_StandardReport_t *standardReport) {
    // Populate buffer values

    standardReport->connection_info = 1; // Pro Controller + USB connected
    standardReport->battery_level = BATTERY_FULL | BATTERY_CHARGING;

    uint16_t button = (serialInput.input[0] << 8) | serialInput.input[1];
    uint8_t dpad = serialInput.input[2];

    standardReport->button_y = (button & SWITCH_Y) == SWITCH_Y;
    standardReport->button_x = (button & SWITCH_X) == SWITCH_X;
    standardReport->button_b = (button & SWITCH_B) == SWITCH_B;
    standardReport->button_a = (button & SWITCH_A) == SWITCH_A;
    standardReport->button_right_sr = false;
    standardReport->button_right_sl = false;
    standardReport->button_r = (button & SWITCH_R) == SWITCH_R;
    standardReport->button_zr = (button & SWITCH_ZR) == SWITCH_ZR;
    standardReport->button_minus = (button & SWITCH_MINUS) == SWITCH_MINUS;
    standardReport->button_plus = (button & SWITCH_PLUS) == SWITCH_PLUS;
    standardReport->button_thumb_r = (button & SWITCH_RCLICK) == SWITCH_RCLICK;
    standardReport->button_thumb_l = (button & SWITCH_LCLICK) == SWITCH_LCLICK;
    standardReport->button_home = (button & SWITCH_HOME) == SWITCH_HOME;
    standardReport->button_capture = (button & SWITCH_CAPTURE) == SWITCH_CAPTURE;
    standardReport->dummy = 0;
    standardReport->charging_grip = true;
    standardReport->dpad_down = dpad == HAT_BOTTOM_RIGHT || dpad == HAT_BOTTOM || dpad == HAT_BOTTOM_LEFT;
    standardReport->dpad_up = dpad == HAT_TOP || dpad == HAT_TOP_RIGHT || dpad == HAT_TOP_LEFT;
    standardReport->dpad_right = dpad == HAT_TOP_RIGHT || dpad == HAT_RIGHT || dpad == HAT_BOTTOM_RIGHT;
    standardReport->dpad_left = dpad == HAT_BOTTOM_LEFT || dpad == HAT_LEFT || dpad == HAT_TOP_LEFT;
    standardReport->button_left_sr = false;
    standardReport->button_left_sl = false;
    standardReport->button_l = (button & SWITCH_L) == SWITCH_L;
    standardReport->button_zl = (button & SWITCH_ZL) == SWITCH_ZL;

    // Left stick
    uint16_t lx = (serialInput.input[3] << 4) | 0x08;
    uint16_t ly = (serialInput.input[4] << 4) | 0x08;
    standardReport->analog[0] = lx & 0xFF;
    standardReport->analog[1] = ((ly & 0x0F) << 4) | ((lx & 0xF00) >> 8);
    standardReport->analog[2] = (ly & 0xFF0) >> 4;

    // Right stick
    uint16_t rx = (serialInput.input[5] << 4) | 0x08;
    uint16_t ry = (serialInput.input[6] << 4) | 0x08;
    standardReport->analog[3] = rx & 0xFF;
    standardReport->analog[4] = ((ry & 0x0F) << 4) | ((rx & 0xF00) >> 8);
    standardReport->analog[5] = (ry & 0xFF0) >> 4;

    standardReport->vibrator_input_report = 0;
}

/*
 * Assign default values to the controller report (full battery, charging, centered sticks and no buttons pressed).
 */
void initialize_idle_report(USB_StandardReport_t *standardReport) {
    memset(standardReport, 0, sizeof(USB_StandardReport_t));

    standardReport->connection_info = 1; // Pro Controller + USB connected
    standardReport->battery_level = BATTERY_FULL | BATTERY_CHARGING;

    /*standardReport->button_y = false;
    standardReport->button_x = false;
    standardReport->button_b = false;
    standardReport->button_a = false;
    standardReport->button_right_sr = false;
    standardReport->button_right_sl = false;
    standardReport->button_r = false;
    standardReport->button_zr = false;
    standardReport->button_minus = false;
    standardReport->button_plus = false;
    standardReport->button_thumb_r = false;
    standardReport->button_thumb_l = false;
    standardReport->button_home = false;
    standardReport->button_capture = false;
    standardReport->dummy = 0;
    standardReport->charging_grip = false;
    standardReport->dpad_down = false;
    standardReport->dpad_up = false;
    standardReport->dpad_right = false;
    standardReport->dpad_left = false;
    standardReport->button_left_sr = false;
    standardReport->button_left_sl = false;
    standardReport->button_l = false;
    standardReport->button_zl = false;*/
    standardReport->charging_grip = true;

    // Left stick
    uint16_t lx = 0x0800;
    uint16_t ly = 0x0800;
    standardReport->analog[0] = lx & 0xFF;
    standardReport->analog[1] = ((ly & 0x0F) << 4) | ((lx & 0xF00) >> 8);
    standardReport->analog[2] = (ly & 0xFF0) >> 4;

    // Right stick
    uint16_t rx = 0x0800;
    uint16_t ry = 0x0800;
    standardReport->analog[3] = rx & 0xFF;
    standardReport->analog[4] = ((ry & 0x0F) << 4) | ((rx & 0xF00) >> 8);
    standardReport->analog[5] = (ry & 0xFF0) >> 4;

    standardReport->vibrator_input_report = 0x0c;
}

// Main entry point.
int main(void) {
    // We also need to initialize the initial input reports.

    // Assign default controller state values (no buttons pressed and centered sticks)
    initialize_idle_report(&controllerReport); // Will be populated later with values received from UART
    initialize_idle_report(&idleReport); // Idle report (no buttons pressed)
    selectedReport = &idleReport; // Use idle report until data is received from UART

    // Memory allocated for UART inputs, initially zeroed
    memset(&serialInput, 0, sizeof(Serial_Input_Packet_t));

    // Initial value for IN endpoint buffer
    prepare_8101();

    setup_response_manager(CALLBACK_beforeSend, &selectedReport);

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
    // Order doesn't matter if ORDERED_EP_CONFIG is not set.
    // ORDERED_EP_CONFIG reduces memory usage. If set, you must keep this order (ascending addresses, see Descriptors.h)
    ConfigSuccess &= Endpoint_ConfigureEndpoint(JOYSTICK_IN_EPADDR, EP_TYPE_INTERRUPT, JOYSTICK_EPSIZE, 1);
    ConfigSuccess &= Endpoint_ConfigureEndpoint(JOYSTICK_OUT_EPADDR, EP_TYPE_INTERRUPT, JOYSTICK_EPSIZE, 1);

    // We can read ConfigSuccess to indicate a success or failure at this point.
    if (!ConfigSuccess) {
        LEDs_SetAllLEDs(LEDMASK_NOT_SYNCED);
        for(;;); // die
    }
}

// Process control requests sent to the device from the USB host.
void EVENT_USB_Device_ControlRequest(void) {
    // Switch doesn't use GetReport or SetReport

    switch (USB_ControlRequest.bRequest) {
        case HID_REQ_SetIdle: { // Difference between old firmware and this #1 : Old FW Sends a Broken Pipe status.
            if (USB_ControlRequest.bmRequestType == (REQDIR_HOSTTODEVICE | REQTYPE_CLASS | REQREC_INTERFACE)) {
                Endpoint_ClearSETUP();
                Endpoint_ClearStatusStage();
                // Ignore idle period
            }
            break;
        }
            // No other HID requests are used
            // Standard control requests (such as SET_CONFIGURATION) are handled by LUFA
    }
}

void CALLBACK_beforeSend() {
    disable_rx_isr();
    if (state == SYNCED) {
        if (millis >= MILLIS_UNTIL_PAUSE) {
            // Trigger "connection lost" mode
            // The same controller state was sent too many times to the Switch. That means we're not
            // receiving from UART or the CRC is incorrect (host not synced).
            if (millis >= MILLIS_UNTIL_HOME && millis < MILLIS_UNTIL_HOME + MILLIS_HOME_PRESSED) {
                // Send HOME command and change LEDs
                LEDs_SetAllLEDs(LEDMASK_PAUSE_HOME_BUFFER);
                idleReport.button_home = true;
                selectedReport = &idleReport;
            } else {
                // Send empty packet (no input)
                LEDs_SetAllLEDs(LEDMASK_PAUSE_EMPTY_BUFFER);
                idleReport.button_home = false;
                selectedReport = &idleReport;
            }

            // Prevent overflow, there is no need to increment timer if we're in the last state
            if (millis < MILLIS_UNTIL_HOME+MILLIS_HOME_PRESSED) {
                millis += 8;
            }
        } else {
            LEDs_SetAllLEDs(LEDMASK_SYNCED);
            // Send last controller state (from UART) to console and increment timer
            selectedReport = &controllerReport;
            millis += 8;
        }
        //send_byte(RESP_USB_ACK);
    } else {
        // Turn off all LEDs
        LEDs_SetAllLEDs(LEDMASK_NOT_SYNCED);
        // If not synced, use default buffer
        idleReport.button_home = false;
        selectedReport = &idleReport;
    }

    // Re-enable interrupt, will call ISR if a transmission finished while populating the report
    enable_rx_isr();
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
        // Clear input buffer before every read
        memset(switchResponseBuffer, 0, sizeof(switchResponseBuffer));

        uint8_t* ReportData = switchResponseBuffer;
        uint16_t ReportSize = 0;
        // If we received something, and the packet has data, we'll store it.
        while (Endpoint_IsReadWriteAllowed()) {
            uint8_t b = Endpoint_Read_8();
            if (ReportSize < sizeof(switchResponseBuffer)) {
                // avoid over filling of the buffer
                *ReportData = b;
                ReportSize++;
                ReportData++;
            }
        }

        // We acknowledge an OUT packet on this endpoint.
        Endpoint_ClearOUT();

        // At this point, we can react to this data.
        // https://github.com/dekuNukem/Nintendo_Switch_Reverse_Engineering/blob/master/bluetooth_hid_subcommands_notes.md
        if (switchResponseBuffer[0] == 0x80) {
            switch (switchResponseBuffer[1]) {
                case 0x01: {
                    prepare_8101();
                    break;
                }
                case 0x02:
                case 0x03: {
                    prepare_reply(0x81, switchResponseBuffer[1], NULL, 0);
                    break;
                }
                case 0x04: {
                    //reportType = NONE;
                    prepare_standard_report(selectedReport);
                    break;
                }
                default: {
                    // TODO
                    prepare_reply(0x81, switchResponseBuffer[1], NULL, 0);
                    break;
                }
            }
        } else if (switchResponseBuffer[0] == 0x01 && ReportSize > 16) {
            Switch_Subcommand_t subcommand = switchResponseBuffer[10];
            switch (subcommand) {
                case SUBCOMMAND_BLUETOOTH_MANUAL_PAIRING: {
                    prepare_uart_reply(0x81, subcommand, (uint8_t*) 0x03, 1);
                    break;
                }
                case SUBCOMMAND_REQUEST_DEVICE_INFO: {
                    size_t n = sizeof(mac_address); // = 6
                    uint8_t buf[n+6];
                    buf[0] = 0x03; buf[1] = 0x48; // Firmware version
                    buf[2] = 0x03; // Pro Controller
                    buf[3] = 0x02; // Unkown
                    // MAC address is flipped (big-endian)
                    for (int i = 0; i < n; i++) {
                        buf[(n+3)-i] = mac_address[i];
                    }
                    buf[n+4] = 0x03; // Unknown
                    buf[n+5] = 0x02; // Use colors in SPI memory, and use grip colors (added in Switch firmware 5.0)
                    prepare_uart_reply(0x82, subcommand, buf, sizeof(buf));
                    break;
                }
                case SUBCOMMAND_SET_INPUT_REPORT_MODE:
                case SUBCOMMAND_SET_SHIPMENT_LOW_POWER_STATE:
                case SUBCOMMAND_SET_PLAYER_LIGHTS:
                case SUBCOMMAND_SET_HOME_LIGHTS:
                case SUBCOMMAND_ENABLE_IMU:
                case SUBCOMMAND_ENABLE_VIBRATION: {
                    prepare_uart_reply(0x80, subcommand, NULL, 0);
                    break;
                }
                case SUBCOMMAND_TRIGGER_BUTTONS_ELAPSED_TIME: {
                    prepare_uart_reply(0x83, subcommand, NULL, 0);
                    break;
                }
                case SUBCOMMAND_SET_NFC_IR_MCU_CONFIG: {
                    uint8_t buf[] = {0x01, 0x00, 0xFF, 0x00, 0x03, 0x00, 0x05, 0x01};
                    prepare_uart_reply(0xA0, subcommand, buf, sizeof(buf));
                    break;
                }
                case SUBCOMMAND_SPI_FLASH_READ: {
                    // SPI
                    // Addresses are little-endian, so 80 60 means address 0x6080
                    SPI_Address_t address = (switchResponseBuffer[12] << 8) | switchResponseBuffer[11];
                    size_t size = (size_t) switchResponseBuffer[15];
                    spi_read(address, size);
                    break;
                }
                default: {
                    // TODO
                    prepare_uart_reply(0x80, subcommand, NULL, 0);
                    break;
                }
            }
        }
    }

    // We'll then move on to the IN endpoint.
    Endpoint_SelectEndpoint(JOYSTICK_IN_EPADDR);
    // We first check to see if the host is ready to accept data.
    if (Endpoint_IsINReady()) {
        // Received IN interrupt. Switch wants a new packet.
        send_buffer();
    }
}
