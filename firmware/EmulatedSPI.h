#ifndef EMULATED_SPI_H
#define EMULATED_SPI_H

#include "datatypes.h"
#include <string.h>

void spi_read(SPI_Address_t address, size_t size, uint8_t buf[]);

#endif // EMULATED_SPI_H
