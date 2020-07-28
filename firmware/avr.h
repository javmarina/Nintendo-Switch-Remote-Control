#ifndef _JOYSTICK_AVR_H
#define _JOYSTICK_AVR_H

/* Includes: */
#include <avr/wdt.h> /* wdt_disable() */

// Macro for calculating the baud value from a given baud rate when the U2X (double speed) bit is
// not set.
// Taken from lufa/LUFA/Drivers/Peripheral/AVR8/Serial_AVR8.h
// #define SERIAL_UBBRVAL(Baud)    ((((F_CPU / 16) + (Baud / 2)) / (Baud)) - 1)

// Initializes the USART (USART1 module), note that the RX/TX interrupts need to be enabled manually.
// The baudrate must be defined as a macro instead of a function parameter. This utility will compute
// the best values for the UBRR register and U2X bit (will choose the combination with less error).
void USART_Init(void) {
#define BAUD        1000000
#define BAUD_TOL    0 // 0.0% error. A warning will appear if not achievable
#include <util/setbaud.h>
    UBRR1 = UBRR_VALUE; // set baud rate
#if USE_2X
    UCSR1A |= (1 << U2X1); // enable double speed mode
#else
    UCSR1A &= ~(1 << U2X1); // double speed mode is not needed
#endif

// TODO: implement with a library instead of manually writing to registers
    UCSR1C = _BV(UCSZ11) | _BV(UCSZ10); // no parity, 8 data bits, 1 stop bit, asynchronous USART
    UCSR1D = 0;                         // no cts, no rts
    UCSR1B = _BV(RXEN1) | _BV(TXEN1);   // enable RX and TX
    DDRD  |= _BV(3);                    // set TX pin as output
    PORTD |= _BV(2);                    // set RX pin as input
}

void disable_watchdog(void) {
    MCUSR &= ~(1 << WDRF);
    wdt_disable();
}

inline void disable_rx_isr(void) {
    // Disable USART receive complete interrupt
    UCSR1B &= ~_BV(RXCIE1);
}

inline void enable_rx_isr(void) {
    // When the receive complete interrupt enable (RXCIEn) in UCSRnB is set, the USART receive complete interrupt will be
    // executed as long as the RXCn flag is set
    // Here, n=1
    UCSR1B |= _BV(RXCIE1);
}

// Section 19.6.1 of datasheet
inline void send_byte(uint8_t c) {
    // Wait for empty transmit buffer
    while (!(UCSR1A & _BV(UDRE1)));
    // Put data into buffer, sends the data
    UDR1 = c;
}

/*inline void send_string(const char *str) {
    while (*str) {
        send_byte(*str++);
    }
}*/

// Section 19.7.1 of datasheet
inline uint8_t recv_byte(void) {
    // Wait for data to be received
    while (!(UCSR1A & _BV(RXC1)));
    // Get and return received data from buffer
    return UDR1;
}

#endif
