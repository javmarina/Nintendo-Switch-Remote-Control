#include "Descriptors.h"

// Pro Controller HID descriptor
// https://gist.github.com/ToadKing/b883a8ccfa26adcc6ba9905e75aeb4f2
const USB_Descriptor_HIDReport_Datatype_t PROGMEM JoystickReport[] = {
    HID_RI_USAGE_PAGE(8,1),                         // Generic desktop controls
    HID_RI_LOGICAL_MINIMUM(8,0),                    // Logical Minimum (0)
    HID_RI_USAGE(8,4),                              // Joystick
    HID_RI_COLLECTION(8,1),                         // Application

    HID_RI_REPORT_ID(8,48),                         // Report ID (48)
    HID_RI_USAGE_PAGE(8,1),                         // Generic desktop controls
    HID_RI_USAGE_PAGE(8,9),                         // Button
    HID_RI_USAGE_MINIMUM(8,1),                      // Usage Minimum (0x01)
    HID_RI_USAGE_MAXIMUM(8,0x0A),                   // Usage Maximum (0x0A)
    HID_RI_LOGICAL_MINIMUM(8,0),                    // button off state
    HID_RI_LOGICAL_MAXIMUM(8,1),                    // button on state
    HID_RI_REPORT_SIZE(8,1),                        // 1 bit per report field
    HID_RI_REPORT_COUNT(8,10),                      // 10 report fields (10 buttons)
    HID_RI_UNIT_EXPONENT(8,0),                      //
    HID_RI_UNIT(8,0),                               // no unit
    HID_RI_INPUT(8,2),                              // Variable input
    HID_RI_USAGE_PAGE(8,9),                         // Button
    HID_RI_USAGE_MINIMUM(8,0x0B),                   // Usage Minimum (0x0B)
    HID_RI_USAGE_MAXIMUM(8,0x0E),                   // Usage Maximum (0x0E)
    HID_RI_LOGICAL_MINIMUM(8,0),                    // button off state
    HID_RI_LOGICAL_MAXIMUM(8,1),                    // button on state
    HID_RI_REPORT_SIZE(8,1),                        // 1 bit per report field
    HID_RI_REPORT_COUNT(8,4),                       // 4 report fields (4 buttons)
    HID_RI_INPUT(8,2),                              // Variable input
    HID_RI_REPORT_SIZE(8,1),                        //
    HID_RI_REPORT_COUNT(8,2),                       // 2 empty bits?
    HID_RI_INPUT(8,3),                              // Abs input?

    /*
     * Probably joystick data (16 bits per axis)
     */
    HID_RI_USAGE(32,0x010001),                      // Generic Desktop: Pointer
    HID_RI_COLLECTION(8,0),                         // Physical
    HID_RI_USAGE(32,0x010030),                      // Axis 0
    HID_RI_USAGE(32,0x010031),                      // Axis 1
    HID_RI_USAGE(32,0x010032),                      // Axis 2
    HID_RI_USAGE(32,0x010035),                      // Axis 3
    HID_RI_LOGICAL_MINIMUM(8,0),
    HID_RI_LOGICAL_MAXIMUM(32,65535),
    HID_RI_REPORT_SIZE(8,16),
    HID_RI_REPORT_COUNT(8,4),
    HID_RI_INPUT(8,2),                              // Variable input
    HID_RI_END_COLLECTION(0),

    /*
     * DPAD (HAT)
     */
    HID_RI_USAGE(32,0x010039),
    HID_RI_LOGICAL_MINIMUM(8,0),
    HID_RI_LOGICAL_MAXIMUM(8,7),                    // 8 valid HAT states, sending 0x08 = nothing pressed
    HID_RI_PHYSICAL_MINIMUM(8,0),
    HID_RI_PHYSICAL_MAXIMUM(16,315),                // HAT "rotation"
    HID_RI_UNIT(8,0x14),                            // System: English Rotation, Length: Centimeter
    HID_RI_REPORT_SIZE(8,4),                        // 4 bits per report field
    HID_RI_REPORT_COUNT(8,1),                       // 1 report field (a nibble containing entire HAT state)
    HID_RI_INPUT(8,2),                              // Variable input

    /*
     * Four unrecognized buttons
     */
    HID_RI_USAGE_PAGE(8,9),                         // Button
    HID_RI_USAGE_MINIMUM(8,0x0F),                   // Usage Minimum (0x0F)
    HID_RI_USAGE_MAXIMUM(8,0x12),                   // Usage Maximum (0x12)
    HID_RI_LOGICAL_MINIMUM(8,0),                    // button off state
    HID_RI_LOGICAL_MAXIMUM(8,1),                    // button on state
    HID_RI_REPORT_SIZE(8,1),                        // 1 bit per report field
    HID_RI_REPORT_COUNT(8,4),                       // 4 report fields (4 buttons)
    HID_RI_INPUT(8,2),                              // Variable input
    HID_RI_REPORT_SIZE(8,8),                        //
    HID_RI_REPORT_COUNT(8,52),                      //
    HID_RI_INPUT(8,3),                              // Input

    /*
     * Vendor defined
     */
    HID_RI_USAGE_PAGE(16,0xFF00),

    HID_RI_REPORT_ID(8,33),                         // Report ID (33)
    HID_RI_USAGE(8,1),                              // Vendor defined: 1
    HID_RI_REPORT_SIZE(8,8),
    HID_RI_REPORT_COUNT(8,63),
    HID_RI_INPUT(8,3),                              // Input

    HID_RI_REPORT_ID(8,0x81),                       // Report ID (-127)
    HID_RI_USAGE(8,2),                              // Vendor defined: 2
    HID_RI_REPORT_SIZE(8,8),
    HID_RI_REPORT_COUNT(8,63),
    HID_RI_INPUT(8,3),                              // Input

    HID_RI_REPORT_ID(8,1),                          // Report ID (1)
    HID_RI_USAGE(8,3),                              // Vendor defined: 3
    HID_RI_REPORT_SIZE(8,8),
    HID_RI_REPORT_COUNT(8,63),
    HID_RI_OUTPUT(8,0x83),                          // Output

    HID_RI_REPORT_ID(8,16),                         // Report ID (16)
    HID_RI_USAGE(8,4),                              // Vendor defined: 4
    HID_RI_REPORT_SIZE(8,8),
    HID_RI_REPORT_COUNT(8,63),
    HID_RI_OUTPUT(8,0x83),                          // Output

    HID_RI_REPORT_ID(8,0x80),                       // Report ID (-128)
    HID_RI_USAGE(8,5),                              // Vendor defined: 5
    HID_RI_REPORT_SIZE(8,8),
    HID_RI_REPORT_COUNT(8,63),
    HID_RI_OUTPUT(8,0x83),                          // Output

    HID_RI_REPORT_ID(8,0x82),                       // Report ID (-126)
    HID_RI_USAGE(8,6),                              // Vendor defined: 6
    HID_RI_REPORT_SIZE(8,8),
    HID_RI_REPORT_COUNT(8,63),
    HID_RI_OUTPUT(8,0x83),                          // Output

    HID_RI_END_COLLECTION(0),
};

// Device Descriptor Structure
const USB_Descriptor_Device_t PROGMEM DeviceDescriptor = {
        .Header                 = {.Size = sizeof(USB_Descriptor_Device_t), .Type = DTYPE_Device},

        .USBSpecification       = VERSION_BCD(2, 0, 0),
        .Class                  = USB_CSCP_NoDeviceClass,
        .SubClass               = USB_CSCP_NoDeviceSubclass,
        .Protocol               = USB_CSCP_NoDeviceProtocol,

        .Endpoint0Size          = FIXED_CONTROL_ENDPOINT_SIZE,

        .VendorID               = 0x057E,
        .ProductID              = 0x2009,
        .ReleaseNumber          = VERSION_BCD(2, 1, 0),

        .ManufacturerStrIndex   = STRING_ID_Manufacturer,
        .ProductStrIndex        = STRING_ID_Product,
        .SerialNumStrIndex      = STRING_ID_Serial,

        .NumberOfConfigurations = FIXED_NUM_CONFIGURATIONS
};

// Configuration Descriptor Structure
const USB_Descriptor_Configuration_t PROGMEM ConfigurationDescriptor = {
        .Config = {
                .Header                 = {.Size = sizeof(USB_Descriptor_Configuration_Header_t), .Type = DTYPE_Configuration},

                .TotalConfigurationSize = sizeof(USB_Descriptor_Configuration_t),
                .TotalInterfaces        = 1,

                .ConfigurationNumber    = 1,
                .ConfigurationStrIndex  = NO_DESCRIPTOR,

                .ConfigAttributes       = (USB_CONFIG_ATTR_RESERVED | USB_CONFIG_ATTR_REMOTEWAKEUP),

                .MaxPowerConsumption    = USB_CONFIG_POWER_MA(500)
        },

        .HID_Interface = {
                .Header                 = {.Size = sizeof(USB_Descriptor_Interface_t), .Type = DTYPE_Interface},

                .InterfaceNumber        = INTERFACE_ID_Joystick,
                .AlternateSetting       = 0x00,

                .TotalEndpoints         = 2,

                .Class                  = HID_CSCP_HIDClass,
                .SubClass               = HID_CSCP_NonBootSubclass,
                .Protocol               = HID_CSCP_NonBootProtocol,

                .InterfaceStrIndex      = NO_DESCRIPTOR
        },

        .HID_JoystickHID = {
                .Header                 = {.Size = sizeof(USB_HID_Descriptor_HID_t), .Type = HID_DTYPE_HID},

                .HIDSpec                = VERSION_BCD(1, 1, 1),
                .CountryCode            = 0x00,
                .TotalReportDescriptors = 1,
                .HIDReportType          = HID_DTYPE_Report,
                .HIDReportLength        = sizeof(JoystickReport)
        },

        .HID_ReportINEndpoint = {
                .Header                 = {.Size = sizeof(USB_Descriptor_Endpoint_t), .Type = DTYPE_Endpoint},

                .EndpointAddress        = JOYSTICK_IN_EPADDR,
                .Attributes             = (EP_TYPE_INTERRUPT | ENDPOINT_ATTR_NO_SYNC | ENDPOINT_USAGE_DATA),
                .EndpointSize           = JOYSTICK_EPSIZE,
                .PollingIntervalMS      = 0x08
        },

        .HID_ReportOUTEndpoint = {
                .Header                 = {.Size = sizeof(USB_Descriptor_Endpoint_t), .Type = DTYPE_Endpoint},

                .EndpointAddress        = JOYSTICK_OUT_EPADDR,
                .Attributes             = (EP_TYPE_INTERRUPT | ENDPOINT_ATTR_NO_SYNC | ENDPOINT_USAGE_DATA),
                .EndpointSize           = JOYSTICK_EPSIZE,
                .PollingIntervalMS      = 0x08
        },
};

// Language Descriptor Structure
const USB_Descriptor_String_t PROGMEM LanguageString = USB_STRING_DESCRIPTOR_ARRAY(LANGUAGE_ID_ENG);

// Manufacturer, Product and Serial number Descriptor Strings
const USB_Descriptor_String_t PROGMEM ManufacturerString = USB_STRING_DESCRIPTOR(L"Nintendo Co., Ltd.");
const USB_Descriptor_String_t PROGMEM ProductString      = USB_STRING_DESCRIPTOR(L"Pro Controller");
const USB_Descriptor_String_t PROGMEM SerialNumberString = USB_STRING_DESCRIPTOR(L"000000000001");

// USB Device Callback - Get Descriptor
uint16_t CALLBACK_USB_GetDescriptor(
        const uint16_t wValue,
        const uint16_t wIndex,
        const void **const DescriptorAddress
) {
    const uint8_t  DescriptorType   = (wValue >> 8);
    const uint8_t  DescriptorNumber = (wValue & 0xFF);

    const void *Address = NULL;
    uint16_t    Size    = NO_DESCRIPTOR;

    switch (DescriptorType) {
        case DTYPE_Device:
            Address = &DeviceDescriptor;
            Size    = sizeof(USB_Descriptor_Device_t);
            break;
        case DTYPE_Configuration:
            Address = &ConfigurationDescriptor;
            Size    = sizeof(USB_Descriptor_Configuration_t);
            break;
        case DTYPE_String:
            switch (DescriptorNumber) {
                case STRING_ID_Language:
                    Address = &LanguageString;
                    Size    = pgm_read_byte(&LanguageString.Header.Size);
                    break;
                case STRING_ID_Manufacturer:
                    Address = &ManufacturerString;
                    Size    = pgm_read_byte(&ManufacturerString.Header.Size);
                    break;
                case STRING_ID_Product:
                    Address = &ProductString;
                    Size    = pgm_read_byte(&ProductString.Header.Size);
                    break;
                case STRING_ID_Serial:
                    Address = &SerialNumberString;
                    Size    = pgm_read_byte(&SerialNumberString.Header.Size);
            }

            break;
        case DTYPE_HID:
            Address = &ConfigurationDescriptor.HID_JoystickHID;
            Size    = sizeof(USB_HID_Descriptor_HID_t);
            break;
        case DTYPE_Report:
            Address = &JoystickReport;
            Size    = sizeof(JoystickReport);
            break;
    }

    *DescriptorAddress = Address;
    return Size;
}
