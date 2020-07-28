#ifndef JOYSTICK_RESPONSE_H
#define JOYSTICK_RESPONSE_H

#include "datatypes.h"
#include <LUFA/Drivers/USB/USB.h>

#define COUNTER_INCREMENT 3

void prepare_standard_report(USB_StandardReport_t *standardReport);

static uint8_t replyBuffer[JOYSTICK_EPSIZE];
static uint8_t counter = 0;

static bool nextPacketReady = false;

void (*before_send)(void) = 0;
static USB_StandardReport_t **selectedReportPtr;

void setup_response_manager(void (*before_callback)(void), USB_StandardReport_t **ptr) {
    before_send = before_callback;
    selectedReportPtr = ptr;
}

void send_buffer(void) {
    before_send();

    if (!nextPacketReady) {
        // No requests from Switch, use standard report
        prepare_standard_report(*selectedReportPtr);
    }

    Endpoint_SelectEndpoint(JOYSTICK_IN_EPADDR);
    while (!Endpoint_IsINReady()); // Wait until IN endpoint is ready
    while (Endpoint_Write_Stream_LE(&replyBuffer, sizeof(replyBuffer), NULL) != ENDPOINT_RWSTREAM_NoError);
    Endpoint_ClearIN(); // We then send an IN packet on this endpoint.

    nextPacketReady = false;
}

void prepare_reply(uint8_t code, uint8_t command, uint8_t data[], uint8_t length) {
    if (nextPacketReady) return;
    memset(replyBuffer, 0, sizeof(replyBuffer));
    replyBuffer[0] = code;
    replyBuffer[1] = command;
    memcpy(&replyBuffer[2], &data[0], length);
    nextPacketReady = true;
}

void prepare_uart_reply(uint8_t code, uint8_t subcommand, uint8_t data[], uint8_t length) {
    if (nextPacketReady) return;
    memset(replyBuffer, 0, sizeof(replyBuffer));
    replyBuffer[0] = 0x21;

    counter += COUNTER_INCREMENT;
    replyBuffer[1] = counter;

    USB_StandardReport_t *selectedReport = *selectedReportPtr;
    size_t n = sizeof(USB_StandardReport_t);
    memcpy(&replyBuffer[2], selectedReport, n);
    replyBuffer[n + 2] = code;
    replyBuffer[n + 3] = subcommand;
    memcpy(&replyBuffer[n + 4], &data[0], length);
    nextPacketReady = true;
}

void prepare_standard_report(USB_StandardReport_t *standardReport) {
    if (nextPacketReady) return;
    counter += COUNTER_INCREMENT;
    prepare_reply(0x30, counter, (uint8_t *) standardReport, sizeof(USB_StandardReport_t));
}

void prepare_8101(void) {
    if (nextPacketReady) return;
    size_t n = sizeof(mac_address); // = 6
    uint8_t buf[n + 2];
    buf[0] = 0x00;
    buf[1] = 0x03; // Pro Controller
    memcpy(&buf[2], &mac_address[0], n);
    prepare_reply(0x81, 0x01, buf, sizeof(buf));
}

#endif // JOYSTICK_RESPONSE_H
