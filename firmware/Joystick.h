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
#include "datatypes.h"
#include "Response.h"
#include <stdio.h>

// AVR includes
#include <avr/power.h> /* clock_prescale_set() */
#include <util/crc16.h> /* _crc8_ccitt_update() */

// LUFA includes
#include <LUFA/Drivers/Board/LEDs.h>

// Setup all necessary hardware, including USB initialization.
void SetupHardware(void);

// Process and deliver data from IN and OUT endpoints.
void HID_Task(void);

#endif
