/*
             LUFA Library
     Copyright (C) Dean Camera, 2014.

  dean [at] fourwalledcubicle [dot] com
           www.lufa-lib.org
*/

/*
  Copyright 2014  Dean Camera (dean [at] fourwalledcubicle [dot] com)

  Permission to use, copy, modify, distribute, and sell this
  software and its documentation for any purpose is hereby granted
  without fee, provided that the above copyright notice appear in
  all copies and that both that the copyright notice and this
  permission notice and warranty disclaimer appear in supporting
  documentation, and that the name of the author not be used in
  advertising or publicity pertaining to distribution of the
  software without specific, written prior permission.

  The author disclaims all warranties with regard to this
  software, including all implied warranties of merchantability
  and fitness.  In no event shall the author be liable for any
  special, indirect or consequential damages or any damages
  whatsoever resulting from loss of use, data or profits, whether
  in an action of contract, negligence or other tortious action,
  arising out of or in connection with the use or performance of
  this software.
*/

/** \file
 *
 *  Header file for Joystick.c.
 */

#ifndef _JOYSTICK_H_
#define _JOYSTICK_H_

#include "avr.h"
#include "Descriptors.h"

// Type Defines
// Enumeration for joystick buttons.
typedef enum {
    SWITCH_Y       = 0x01,
    SWITCH_B       = 0x02,
    SWITCH_A       = 0x04,
    SWITCH_X       = 0x08,
    SWITCH_L       = 0x10,
    SWITCH_R       = 0x20,
    SWITCH_ZL      = 0x40,
    SWITCH_ZR      = 0x80,
    SWITCH_MINUS   = 0x100,
    SWITCH_PLUS    = 0x200,
    SWITCH_LCLICK  = 0x400,
    SWITCH_RCLICK  = 0x800,
    SWITCH_HOME    = 0x1000,
    SWITCH_CAPTURE = 0x2000,
} JoystickButtons_t;

#define HAT_TOP          0x00
#define HAT_TOP_RIGHT    0x01
#define HAT_RIGHT        0x02
#define HAT_BOTTOM_RIGHT 0x03
#define HAT_BOTTOM       0x04
#define HAT_BOTTOM_LEFT  0x05
#define HAT_LEFT         0x06
#define HAT_TOP_LEFT     0x07
#define HAT_CENTER       0x08

#define STICK_MIN      0
#define STICK_CENTER 128
#define STICK_MAX    255

/** LED mask for the library LED driver, to indicate that the UART is not synced. */
#define LEDMASK_NOT_SYNCED             LEDS_NO_LEDS
/** LED mask for the library LED driver, to indicate that UART is synced and we're receiving packets. */
#define LEDMASK_SYNCED                 LEDS_ALL_LEDS
/** LED mask for the library LED driver, to indicate that we're in "connection lost" mode (empty buffer). */
#define LEDMASK_PAUSE_EMPTY_BUFFER     LEDS_LED1
/** LED mask for the library LED driver, to indicate that we're in "connection lost" mode (home buffer). */
#define LEDMASK_PAUSE_HOME_BUFFER      LEDS_LED2

typedef enum {
    COMMAND_NOP         = 0,
    COMMAND_SYNC_1      = 0x33,
    COMMAND_SYNC_2      = 0xCC,
    COMMAND_SYNC_START  = 0xFF
} Command_t;

typedef enum {
    RESP_USB_ACK        = 0x90, // USB report sent to Switch: Not currently used.
    RESP_UPDATE_ACK     = 0x91, // Sent to host after a valid packet
    RESP_UPDATE_NACK    = 0x92, // Sent to host after an incorrect packet (CRC mismatch)
    RESP_SYNC_START     = 0xFF, // Sent to host after COMMAND_SYNC_START
    RESP_SYNC_1         = 0xCC, // Sent to host after COMMAND_SYNC_1
    RESP_SYNC_OK        = 0x33, // Sent to host after COMMAND_SYNC_2, synchronization finished
} Response_t;

// Joystick HID report structure. We have an input and an output.
typedef struct {
    uint16_t Button; // 16 buttons; see JoystickButtons_t for bit mapping
    uint8_t  HAT;    // HAT switch; one nibble w/ unused nibble
    uint8_t  LX;     // Left  Stick X
    uint8_t  LY;     // Left  Stick Y
    uint8_t  RX;     // Right Stick X
    uint8_t  RY;     // Right Stick Y
    uint8_t  VendorSpec;    // Normally, 0x00
} USB_JoystickReport_Input_t;

// The output is structured as a mirror of the input.
// This is based on initial observations of the Pokken Controller.
typedef struct {
    uint16_t Button; // 16 buttons; see JoystickButtons_t for bit mapping
    uint8_t  HAT;    // HAT switch; one nibble w/ unused nibble
    uint8_t  LX;     // Left  Stick X
    uint8_t  LY;     // Left  Stick Y
    uint8_t  RX;     // Right Stick X
    uint8_t  RY;     // Right Stick Y
} USB_JoystickReport_Output_t;

// Setup all necessary hardware, including USB initialization.
void SetupHardware(void);

// Process and deliver data from IN and OUT endpoints.
void HID_Task(void);

#endif
