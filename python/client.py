# !/usr/bin/env python3
import argparse
import math
import time
from typing import Tuple, List, Optional
import warnings

import serial.tools.list_ports

parser = argparse.ArgumentParser("Client for sending controller commands to a controller emulator")
parser.add_argument("baudrate", type=int, help="Serial port baudrate (in bps). "
                                               "Must have the same value as in the firmware.")
args = parser.parse_args()

STATE_OUT_OF_SYNC = 0
STATE_SYNC_START = 1
STATE_SYNC_1 = 2
STATE_SYNC_2 = 3
STATE_SYNC_OK = 4

# Actual Switch DPAD Values
A_DPAD_CENTER = 0x08
A_DPAD_U = 0x00
A_DPAD_U_R = 0x01
A_DPAD_R = 0x02
A_DPAD_D_R = 0x03
A_DPAD_D = 0x04
A_DPAD_D_L = 0x05
A_DPAD_L = 0x06
A_DPAD_U_L = 0x07

# Enum DIR Values
DIR_CENTER = 0x00
DIR_U = 0x01
DIR_R = 0x02
DIR_D = 0x04
DIR_L = 0x08
DIR_U_R = DIR_U + DIR_R
DIR_D_R = DIR_D + DIR_R
DIR_U_L = DIR_U + DIR_L
DIR_D_L = DIR_D + DIR_L

BTN_NONE = 0x0000000000000000
BTN_Y = 0x0000000000000001
BTN_B = 0x0000000000000002
BTN_A = 0x0000000000000004
BTN_X = 0x0000000000000008
BTN_L = 0x0000000000000010
BTN_R = 0x0000000000000020
BTN_ZL = 0x0000000000000040
BTN_ZR = 0x0000000000000080
BTN_MINUS = 0x0000000000000100
BTN_PLUS = 0x0000000000000200
BTN_LCLICK = 0x0000000000000400
BTN_RCLICK = 0x0000000000000800
BTN_HOME = 0x0000000000001000
BTN_CAPTURE = 0x0000000000002000

DPAD_CENTER = 0x0000000000000000
DPAD_U = 0x0000000000010000
DPAD_R = 0x0000000000020000
DPAD_D = 0x0000000000040000
DPAD_L = 0x0000000000080000
DPAD_U_R = DPAD_U + DPAD_R
DPAD_D_R = DPAD_D + DPAD_R
DPAD_U_L = DPAD_U + DPAD_L
DPAD_D_L = DPAD_D + DPAD_L

LSTICK_CENTER = 0x0000000000000000
LSTICK_R = 0x00000000FF000000  # 0 (000)
LSTICK_U_R = 0x0000002DFF000000  # 45 (02D)
LSTICK_U = 0x0000005AFF000000  # 90 (05A)
LSTICK_U_L = 0x00000087FF000000  # 135 (087)
LSTICK_L = 0x000000B4FF000000  # 180 (0B4)
LSTICK_D_L = 0x000000E1FF000000  # 225 (0E1)
LSTICK_D = 0x0000010EFF000000  # 270 (10E)
LSTICK_D_R = 0x0000013BFF000000  # 315 (13B)

RSTICK_CENTER = 0x0000000000000000
RSTICK_R = 0x000FF00000000000  # 0 (000)
RSTICK_U_R = 0x02DFF00000000000  # 45 (02D)
RSTICK_U = 0x05AFF00000000000  # 90 (05A)
RSTICK_U_L = 0x087FF00000000000  # 135 (087)
RSTICK_L = 0x0B4FF00000000000  # 180 (0B4)
RSTICK_D_L = 0x0E1FF00000000000  # 225 (0E1)
RSTICK_D = 0x10EFF00000000000  # 270 (10E)
RSTICK_D_R = 0x13BFF00000000000  # 315 (13B)

NO_INPUT = BTN_NONE + DPAD_CENTER + LSTICK_CENTER + RSTICK_CENTER

# Commands to send to MCU
COMMAND_NOP = 0x00
COMMAND_SYNC_1 = 0x33
COMMAND_SYNC_2 = 0xCC
COMMAND_SYNC_START = 0xFF

# Responses from MCU
# RESP_USB_ACK = 0x90
RESP_UPDATE_ACK = 0x91
RESP_UPDATE_NACK = 0x92
RESP_SYNC_START = 0xFF
RESP_SYNC_1 = 0xCC
RESP_SYNC_OK = 0x33


# Compute x and y based on angle and intensity


def angle(angle: float, intensity: float) -> Tuple[float, float]:
    """
    Compute x and y based on angle and intensity
    """
    # y is negative because on the Y input, UP = 0 and DOWN = 255
    x = int((math.cos(math.radians(angle)) * 0x7F) * intensity / 0xFF) + 0x80
    y = -int((math.sin(math.radians(angle)) * 0x7F) * intensity / 0xFF) + 0x80
    return x, y


def lstick_angle(angle: int, intensity: int) -> int:
    return (intensity + (angle << 8)) << 24


def rstick_angle(angle: int, intensity: int) -> int:
    return (intensity + (angle << 8)) << 44


# Precision wait
def p_wait(waitTime: float):
    t0 = time.perf_counter()
    t1 = t0
    while t1 - t0 < waitTime:
        t1 = time.perf_counter()


# Wait for data to be available on the serial port
def wait_for_data(sleepTime: float = 0.1):
    t0 = time.perf_counter()
    t1 = t0
    inWaiting = ser.in_waiting
    while (t1 - t0 < sleepTime) or (inWaiting == 0):
        time.sleep(sleepTime)
        inWaiting = ser.in_waiting
        t1 = time.perf_counter()


def read_bytes(size: int) -> List[int]:
    """
    Read X bytes from the serial port (returns list)
    """
    print("Reading {:d} byte(s) from serial port".format(size))
    bytes_in = ser.read(size)
    print("Read {:d} bytes: {:s}".format(len(bytes_in), str([hex(i) for i in bytes_in])))
    if len(bytes_in) < size:
        print("Warning! We received less bytes than expected")
    return list(bytes_in)


def read_byte() -> int:
    """
    Read 1 byte from the serial port (returns int)
    """
    bytes_in = read_bytes(1)
    if len(bytes_in) != 0:
        byte_in = bytes_in[0]
    else:
        print("[read_byte()] Error: requested one byte but received none!")
        byte_in = 0
    return byte_in


def read_byte_latest() -> int:
    """
    Discard all incoming bytes and read the last (latest) (returns int)
    """
    inWaiting = ser.in_waiting
    print("[read_byte_latest()] {:d} byte(s) in input buffer, reading last one and discarding the rest".format(inWaiting))
    if inWaiting == 0:
        inWaiting = 1
    bytes_in = read_bytes(inWaiting)
    if inWaiting > len(bytes_in) > 0:
        print("[read_byte_latest()] Error: requested {:d} byte(s) but received {:d}!".format(inWaiting, len(bytes_in)))
    if len(bytes_in) != 0:
        byte_in = bytes_in[-1]
        print("[read_byte_latest()] Input buffer contained: {:s}".format(str([hex(i) for i in bytes_in])))
    else:
        print("[read_byte_latest()] Error: requested {:d} byte(s) but received none!".format(inWaiting))
        byte_in = 0
    return byte_in


def write_bytes(bytes_out: List[int]):
    """
    # Write bytes to the serial port
    """
    print("Writing {:s} to serial port".format(str([hex(i) for i in bytes_out])))
    count = ser.write(bytearray(bytes_out))
    print("Wrote {:d} bytes out of {:d}".format(count, len(bytes_out)))


def write_byte(byte_out: int):
    """
    Write byte to the serial port
    """
    write_bytes([byte_out])


def crc8_ccitt(old_crc: int, new_data: int) -> int:
    """
    Compute CRC8
    https://www.microchip.com/webdoc/AVRLibcReferenceManual/group__util__crc_1gab27eaaef6d7fd096bd7d57bf3f9ba083.html
    """
    data = old_crc ^ new_data

    for i in range(8):
        if (data & 0x80) != 0:
            data = data << 1
            data = data ^ 0x07
        else:
            data = data << 1
        data = data & 0xff
    return data


def send_packet(packet: Optional[List[int]] = None, debug: bool = False) -> bool:
    """
    Send a raw packet and wait for a response (CRC will be added automatically)
    """
    if packet is None:
        packet = [0x00, 0x00, 0x08, 0x80, 0x80, 0x80, 0x80, 0x00]
    if not debug:
        bytes_out = []
        bytes_out.extend(packet)

        # Compute CRC
        crc = 0
        for d in packet:
            crc = crc8_ccitt(crc, d)
        bytes_out.append(crc)
        write_bytes(bytes_out)
        # print(bytes_out)

        # Wait for USB ACK or UPDATE NACK
        byte_in = read_byte()
        commandSuccess = (byte_in == RESP_UPDATE_ACK)
    else:
        commandSuccess = True
    return commandSuccess


def decrypt_dpad(dpad: int) -> int:
    """
    Convert DPAD value to actual DPAD value used by Switch
    """
    if dpad == DIR_U:
        dpadDecrypt = A_DPAD_U
    elif dpad == DIR_R:
        dpadDecrypt = A_DPAD_R
    elif dpad == DIR_D:
        dpadDecrypt = A_DPAD_D
    elif dpad == DIR_L:
        dpadDecrypt = A_DPAD_L
    elif dpad == DIR_U_R:
        dpadDecrypt = A_DPAD_U_R
    elif dpad == DIR_U_L:
        dpadDecrypt = A_DPAD_U_L
    elif dpad == DIR_D_R:
        dpadDecrypt = A_DPAD_D_R
    elif dpad == DIR_D_L:
        dpadDecrypt = A_DPAD_D_L
    else:
        dpadDecrypt = A_DPAD_CENTER
    return dpadDecrypt


def cmd_to_packet(command: int) -> List[int]:
    """
    Convert CMD to a packet
    """
    cmdCopy = command
    low = (cmdCopy & 0xFF)
    cmdCopy = cmdCopy >> 8
    high = (cmdCopy & 0xFF)
    cmdCopy = cmdCopy >> 8
    dpad = (cmdCopy & 0xFF)
    cmdCopy = cmdCopy >> 8
    lstick_intensity = (cmdCopy & 0xFF)
    cmdCopy = cmdCopy >> 8
    lstick_angle = (cmdCopy & 0xFFF)
    cmdCopy = cmdCopy >> 12
    rstick_intensity = (cmdCopy & 0xFF)
    cmdCopy = cmdCopy >> 8
    rstick_angle = (cmdCopy & 0xFFF)
    dpad = decrypt_dpad(dpad)
    left_x, left_y = angle(lstick_angle, lstick_intensity)
    right_x, right_y = angle(rstick_angle, rstick_intensity)

    packet = [high, low, dpad, left_x, left_y, right_x, right_y, 0x00]
    # print (hex(command), packet, lstick_angle, lstick_intensity, rstick_angle, rstick_intensity)
    return packet


def send_cmd(command: int = NO_INPUT) -> bool:
    """
    Send a formatted controller command to the MCU
    """
    commandSuccess = send_packet(cmd_to_packet(command))
    return commandSuccess


def testbench_btn():
    """
    Test all buttons except for home and capture
    """
    send_cmd(BTN_A)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(BTN_B)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(BTN_X)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(BTN_Y)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(BTN_PLUS)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(BTN_MINUS)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(BTN_LCLICK)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(BTN_RCLICK)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)


def testbench_dpad():
    """
    Test DPAD U / R / D / L
    """
    send_cmd(DPAD_U)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(DPAD_R)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(DPAD_D)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(DPAD_L)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)


def testbench_dpad_diagonals():
    """
    Test DPAD Diagonals - Does not register on switch due to dpad buttons
    """
    send_cmd(DPAD_U_R)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(DPAD_D_R)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(DPAD_D_L)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(DPAD_U_L)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)


def testbench_lstick():
    """
    Test Left Analog Stick
    """
    # Test U/R/D/L
    send_cmd(BTN_LCLICK)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(LSTICK_U)
    p_wait(0.5)
    send_cmd(LSTICK_R)
    p_wait(0.5)
    send_cmd(LSTICK_D)
    p_wait(0.5)
    send_cmd(LSTICK_L)
    p_wait(0.5)
    send_cmd(LSTICK_U)
    p_wait(0.5)
    send_cmd(LSTICK_CENTER)
    p_wait(0.5)

    # 360 Circle @ Full Intensity
    for i in range(0, 721):
        cmd = lstick_angle(i + 90, 0xFF)
        send_cmd(cmd)
        p_wait(0.001)
    send_cmd(LSTICK_CENTER)
    p_wait(0.5)

    # 360 Circle @ Partial Intensity
    for i in range(0, 721):
        cmd = lstick_angle(i + 90, 0x80)
        send_cmd(cmd)
        p_wait(0.001)
    send_cmd(LSTICK_CENTER)
    p_wait(0.5)


def testbench_rstick():
    """
    Test Right Analog Stick
    """
    # Test U/R/D/L
    send_cmd(BTN_RCLICK)
    p_wait(0.5)
    send_cmd()
    p_wait(0.001)
    send_cmd(RSTICK_U)
    p_wait(0.5)
    send_cmd(RSTICK_R)
    p_wait(0.5)
    send_cmd(RSTICK_D)
    p_wait(0.5)
    send_cmd(RSTICK_L)
    p_wait(0.5)
    send_cmd(RSTICK_U)
    p_wait(0.5)
    send_cmd(RSTICK_CENTER)
    p_wait(0.5)

    # 360 Circle @ Full Intensity
    for i in range(0, 721):
        cmd = rstick_angle(i + 90, 0xFF)
        send_cmd(cmd)
        p_wait(0.001)
    send_cmd(RSTICK_CENTER)
    p_wait(0.5)

    # 360 Circle @ Partial Intensity
    for i in range(0, 721):
        cmd = rstick_angle(i + 90, 0x80)
        send_cmd(cmd)
        p_wait(0.001)
    send_cmd(RSTICK_CENTER)
    p_wait(0.5)


def testbench_packet_speed(count: int = 100):
    """
    Test Packet Speed
    """
    sum = 0
    min = 999
    max = 0
    err = 0

    for i in range(0, count + 1):
        # Send packet and check time
        t0 = time.perf_counter()
        status = send_packet()
        t1 = time.perf_counter()

        # Count errors
        if not status:
            err += 1
            print("Packet Error!")

        # Compute times
        delta = t1 - t0
        if delta < min:
            min = delta
        if delta > max:
            max = delta
        sum += delta

    avg = sum / count
    print("Min = ", "{:.3f}".format(min),
          "Max = ", "{:.3f}".format(max),
          "Avg = ", "{:.3f}".format(avg),
          "Errors = ", err)


def testbench():
    testbench_btn()
    testbench_dpad()
    testbench_lstick()
    testbench_rstick()
    testbench_packet_speed()


def force_sync() -> bool:
    """
    Force MCU to sync
    """

    print("First, clear input buffer")
    ser.reset_input_buffer()

    # Send 9x 0xFF's to fully flush out buffer on device
    # Device will send back 0xFF (RESP_SYNC_START) when it is ready to sync
    print("Then, write nine 0xFF bytes to flush out buffer on device")
    write_bytes([0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF])

    # Wait for serial data and read the last byte sent
    timestamp = time.perf_counter()
    available = 0
    timeout = 0.018
    while time.perf_counter() - timestamp < timeout:
        now = ser.in_waiting
        if now > available:
            print("{:d} byte(s) in the input buffer, waiting {:.1f} ms for more bytes".format(now, 1000 * timeout))
            available = now
            timestamp = time.perf_counter()

    print("Eventually we received {:d} byte(s)".format(available))
    byte_in = read_byte_latest()

    if 1 <= available <= 9:
        print("Last received byte is", hex(byte_in))
        if byte_in == RESP_SYNC_START:
            print("Sending COMMAND_SYNC_1 command...")
            write_byte(COMMAND_SYNC_1)
            byte_in = read_byte()
            print("Received byte {:s} as response".format(hex(byte_in)))
            if byte_in == RESP_SYNC_1:
                print("Sending COMMAND_SYNC_2 command...")
                write_byte(COMMAND_SYNC_2)
                byte_in = read_byte()
                print("Received byte {:s} as response".format(hex(byte_in)))
                if byte_in == RESP_SYNC_OK:
                    print("RESP_SYNC_OK, sync completed!")
                    return True
                else:
                    print("[SYNC ERROR] We expected response to be 0x33")
            else:
                print("[SYNC ERROR] We expected response to be 0xCC")
        else:
            print("[SYNC ERROR] We expected last byte to be 0xFF")
    else:
        print("[SYNC ERROR] We expected between 1 and 9 bytes")
    return False


def sync() -> bool:
    """
    Start MCU syncing process
    """
    # Try sending a packet
    inSync = send_packet()
    if not inSync:
        # Not in sync: force re-sync and send a packet
        print("[sync()] Not synced, forcing sync...")
        inSync = force_sync()
        if inSync:
            inSync = send_packet()
    print("[sync()]", "Synced!" if inSync else "Not synced")
    return inSync


# -------------------------------------------------------------------------

if __name__ == "__main__":
    available_ports = serial.tools.list_ports.comports()

    if len(available_ports) == 0:
        print("No serial ports available")
        exit(-1)

    print("List of available ports:")
    for i, port in enumerate(available_ports):
        print("[{:d}] {:s}".format(i, port.name))

    index = -1
    while index < 0 or index >= len(available_ports):
        index = int(input("Introduce port index: "))

    ser = serial.Serial(port=available_ports[index].device, baudrate=args.baudrate, timeout=1)
    # In theory, an unsupported baud rate should raise an exception. However, these don't seem to
    # happen for some platforms/devices. Thus, we check if the selected baud rate is not standard,
    # and if the baudrate attribute was not set correctly in the object
    if args.baudrate not in ser.BAUDRATES:
        warnings.warn("Selected baud rate {:d} might be unsupported. Supported values are: {:s}".format(
            args.baudrate, str(ser.BAUDRATES)))
    if ser.baudrate != args.baudrate:
        warnings.warn("Could not set desired baud rate ({:d}), device was configured with {:d} instead".format(
            args.baudrate, ser.baudrate))

    # Attempt to sync with the MCU
    if not sync():
        print("Could not sync!")
        exit(-1)

    if not send_cmd(BTN_A + DPAD_U_R + LSTICK_U + RSTICK_D_L):
        print("Packet Error!")

    p_wait(0.05)

    if not send_cmd():
        print("Packet Error!")

    # testbench()
    # testbench_packet_speed(1000)
