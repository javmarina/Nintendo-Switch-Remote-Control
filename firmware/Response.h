#ifndef JOYSTICK_RESPONSE_H
#define JOYSTICK_RESPONSE_H

#include "datatypes.h"
#include "Descriptors.h"
#include "EmulatedSPI.h"
#include <LUFA/Drivers/USB/USB.h>

void setup_response_manager(void (*before_callback)(void), USB_StandardReport_t **ptr);
void process_OUT_report(uint8_t* ReportData, uint8_t ReportSize);
void send_IN_report(void);

#endif // JOYSTICK_RESPONSE_H
