/**
 * @file SDSCDev.h
 * @author GOTrust Technology Inc.
 * 
 * @warning WinCE version is obsolete and no longer supported.
 */
 
 
/**
 * @defgroup APIFunctions API Functions
 */

/**
 * @defgroup APISysSetFunc System Setting functions
 * @ingroup APIFunctions
 */

/**
 * @defgroup APIConnectFunc Connection functions
 * @ingroup APIFunctions
 */

/**
 * @defgroup APICommandFunc Command functions
 * @ingroup APIFunctions
 */

/**
 * @defgroup APIAuxFunc Auxiliary Functions
 * @ingroup APIFunctions
 */

 
/**
 * @defgroup PredefinedConstants Pre-defined constants
 */

/**
 * @defgroup PredefinedConstantsProductType Defines the product types
 * @ingroup PredefinedConstants
 */

/**
 * @defgroup PredefinedConstantsSCIOType Defines the type of the SCIO
 * @ingroup PredefinedConstants
 */

/**
 * @defgroup PredefinedConstantsTimeoutMode Defines the timeout mode of the device
 * @ingroup PredefinedConstants
 */

/**
 * @defgroup PredefinedConstantsRelative Defines the relative definition of the device
 * @ingroup PredefinedConstants
 */

/**
 * @defgroup DefineOther Defines the other definition of the device
 * @ingroup PredefinedConstants
 */
 

#ifndef _SDSC_DEV_H
#define _SDSC_DEV_H

#include <windows.h>

#ifndef SDSC_PRODUCT_TYPE_DEFINE

/// Let the library detect the product type.
#define SDSC_PRODUCT_TYPE_AUTO_DETECT	0

/// NS product.
#define SDSC_PRODUCT_TYPE_NS				1

/// CUP product.
#define SDSC_PRODUCT_TYPE_CUP				2

#endif // SDSC_PRODUCT_TYPE_DEFINE

#ifndef SDSC_SCIO_TYPE_DEFINE
#define SDSC_SCIO_TYPE_DEFINE
/**
 * @ingroup PredefinedConstantsType
 * @{
 */

/** @brief The type of the SCIO is unknown. */
#define SDSC_SCIO_UNKNOWN          0

/** @brief The type of the SCIO is 7816. */
#define SDSC_SCIO_7816             1

/** @brief The type of the SCIO is SPI V1. */
#define SDSC_SCIO_SPI_V1           2

/** @brief The unexpected error has occurred. */
#define SDSC_SCIO_SPI_V2           3

/** @brief The unexpected error has occurred. */
#define SDSC_SCIO_SPI_V3           4

/** @} */
#endif	// end: #define SDSC_SCIO_TYPE_DEFINE


#ifndef SDSC_DEV_TIME_OUT_DEFINE
#define SDSC_DEV_TIME_OUT_DEFINE
/**
 * @ingroup PredefinedConstantsTimeoutMode
 * @{
 */

/**
 * The default timeout value of the device, about 1 minute.
 */
#define SDSC_DEV_DEFAULT_TIME_OUT			0

/**
 * The maximum timeout value of the device, about 3 minute.
 */
#define SDSC_DEV_MAX_TIME_OUT					1

/**
 * Use the custom timeout value of the device. The time unit is millisecond.
 * SDSC_DEV_CUSTOM_FLAG_TIME_OUT is a flag, it must be bitwise-or'd with the custom timeout value. \n
 *        Timeout value range is 0x00000000 ~ 0x7FFFFFFF.\n
 *        Example: If the expected timeout is 300 ms, the parameter should be (SDSC_DEV_CUSTOM_FLAG_TIME_OUT | 300).
 */
 #define SDSC_DEV_CUSTOM_FLAG_TIME_OUT           0x80000000

/** @} */
#endif	// end: #define SDSC_DEV_TIME_OUT_DEFINE


#ifndef SDSC_DEV_DEFINE
#define SDSC_DEV_DEFINE
/**
 * @ingroup PredefinedConstantsRelative
 * @{
 */

#ifdef _WIN32_WCE		// WinCE
/** @brief the maximum number of the devices supported in WinCE. */
#define SDSC_MAX_DEV_NUM				4
#else					// windows
/** @brief the maximum number of the devices supported in windows. */
#define SDSC_MAX_DEV_NUM				26
#endif

#ifdef _WIN32_WCE		// WinCE
/** @brief the maximum number of the flash we will enumerate in WinCE */
#define SDSC_CE_MAX_FLASH_NUM			9
#endif

/** @brief the maximum length of the device drive name, including the trailing null characters. */
#define SDSC_MAX_DEV_NAME_LEN			33

/** @brief The device is present. */
#define SDSC_DEV_PRESENT				1

/** @brief The device is absent. */
#define SDSC_DEV_ABSENT					0

/** @brief When monitoring the state of devices, the intervals (ms) we recommend. */
#define SDSC_DEV_MONITOR_INTERVAL		1000

/** @brief 200ms for PNP delay. */
#define SDSC_PNP_SLEEP_TIME				200	

/** @brief The length of the firmware of the device. */
#define SDSC_FIRMWARE_VER_LEN			8

/** @brief The maximum length of device info. */
#define SDSC_DEVICE_INFO_LEN			128

/** The maximum length of the SDK version and the protocol version. */
#define SDSC_MAX_VERSION_LEN            16

/** @} */
#endif	// end: #define SDSC_DEV_DEFINE


#ifdef __cplusplus
extern "C" {
#endif

/**
 * Specify the product type. 
 * If users do not specify the product type, the default type is SDSC_PRODUCT_TYPE_AUTO_DETECT. In this case, the library will try to detect the product type in SDSCListDevs().
 * SDSCListDevs() can have better performance if the product type is specified explicitly.
 * 
 * @param[in] ulProductType The product type. It can be one of the following values:
				- #SDSC_PRODUCT_TYPE_AUTO_DETECT
				- #SDSC_PRODUCT_TYPE_NS
				- #SDSC_PRODUCT_TYPE_CUP
 * @return Returns 0 on success; otherwise, returns an error code.
 * @ingroup APISysSetFunc
 */
DWORD __stdcall SDSCSetProductType
(
	IN DWORD ulProductType
);

/**
 * Get the list of all the removable drives. This function won't send GO-Trust commands to all the removable devices.
 * @param pszRemovableDrives Receives a multi-string that lists all the removable drives. \n
 *                           If this value is NULL, SDSCListRemovableDrives ignores the buffer length supplied in pulRemovableDrivesLen, writes the length of the buffer that would have been returned if this parameter had not been NULL to pulRemovableDrivesLen, and returns a success code. 
 * @param pulRemovableDrivesLen Supplies the length of the pszRemovableDrives buffer in characters, and receives the actual length of the multi-string structure, including the trailing null character.
 * @param pulRemovableDriveNum Receives the number of all the removable drives.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
DWORD __stdcall SDSCListRemovableDrives
(
	OUT char *pszRemovableDrives,
	IN OUT DWORD *pulRemovableDrivesLen,
	OUT DWORD *pulRemovableDriveNum
);


/**
 * Get the list of all the devices without been connected, and returns the number of devices.
 * @param pszDrives Receives a multi-string that lists all the devices. \n
 * @param pulDrivesLen Supplies the length of the pszDrives buffer in bytes, and receives the actual length of the multi-string structure.
 * @param pulDriveNum Receives the number of devices.
 * @param pszIoFileName Supplies the I/O file name. The communication channel between host applications and the secure microSD will be established based on the specified I/O file on the microSD.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIConnectFunc
 */
DWORD __stdcall SDSCListDevs
(
	OUT char *pszDrives,
	IN OUT DWORD *pulDrivesLen,
	OUT DWORD *pulDriveNum,
	IN char* pszIoFileName
);

/**
 * Establish a connection between the calling application and the device.
 * If the smart card has not been reset, this function will do SDSCResetCard() automatically.
 * @param pszDrive Pointer to a null-terminated string that specifies the device drive name.
 * @param phDevice Receives a handle that identifies the connection to the device.
 * @param pszIoFileName Supplies the I/O file name. The communication channel between host applications and the secure microSD is established by using a special I/O file under the root directory of the microSD.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIConnectFunc
 */
DWORD __stdcall SDSCConnectDev
(
	IN char *pszDrive,
	OUT HANDLE *phDevice,
	IN char* pszIoFileName
);

/**
 * Terminate a connection previously opened between the calling application and the device.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIConnectFunc
 */
DWORD __stdcall SDSCDisconnectDev
(
	IN HANDLE hDevice
);

/**
 * Get the device drive name by handle.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pszDrive Receives the device drive name.
 * @param pulDriveLen Supplies the length of the pszDrive buffer in bytes, and receives the actual length of the drive name.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
DWORD __stdcall SDSCGetDriveName
(
	IN HANDLE hDevice,
	OUT char *pszDrive,
	IN OUT DWORD *pulDriveLen
);


/**
 * Get the firmware version of the device.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pbFirmwareVer Receives the firmware version of the device. The buffer size must not be smaller than SDSC_FIRMWARE_VER_LEN.
 * @param pulFirmwareVerLen Supplies the length of the pbFirmwareVer buffer in bytes, and receives the actual length of the received firmware version.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APICommandFunc
 */
DWORD __stdcall SDSCGetFirmwareVer
(
	IN HANDLE hDevice,
	OUT BYTE *pbFirmwareVer,
	IN OUT DWORD *pulFirmwareVerLen
);


/**
 * Get the secure microSD device info. The content of the returned device info depends on product type.
 * For the device of the type SDSC_PRODUCT_TYPE_NS, the device info is its flash ID.
 * For the device of the type SDSC_PRODUCT_TYPE_CUP, the device info is the response of the SCIF_INFO command.
 *
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pbDeviceInfo Receives the device info. The buffer must not be smaller than SDSC_DEVICE_INFO_LEN.
 * @param pulDeviceInfoLen Supplies the length of the pbDeviceInfo buffer in bytes, and receives the actual length of the received device info.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APICommandFunc
 */
DWORD __stdcall SDSCGetDeviceInfo
(
    IN HANDLE hDevice,
    OUT BYTE *pbDeviceInfo,
    IN OUT DWORD *pulDeviceInfoLen
);


/**
 * Reset smart card.
 * If the smart card has not been reset, this library will do SDSCResetCard() automatically when the calling application firstly calls SDSCConnectDev().
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pbAtr Receives the ATR if resetting smart card successfully. The buffer must be big enough.
 * @param pulAtrLen Supplies the length of the pbAtr buffer in bytes, and receives the actual length of the received ATR.
 * @note If the calling application needn't get the ATR, the parameters pbAtr and pulAtrLen can be NULL.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APICommandFunc
 */
DWORD __stdcall SDSCResetCard
(
	IN HANDLE hDevice,
	OUT BYTE *pbAtr,
	IN OUT DWORD *pulAtrLen
);

/**
 * PPS exchange.
 * @param[in] hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param[in] bRate Supplies the baud rate(Fi/Di) in the PPS request.
 * @param[out] pbPPS Receives the actual Fi/Di applied by the device.
 * @param[in,out] pulPPSLen Supplies the length of the pbPPS buffer in bytes, and receives the length of returned data.
 * @return Returns 0 on success; otherwise, returns an error code.
 */
DWORD __stdcall SDSCPPS
(
	IN HANDLE hDevice,
	IN BYTE bRate,
	OUT BYTE *pbPPS,
	IN OUT DWORD *pulPPSLen
);

/**
 * Send an APDU command to the device, and expect to receive data back from the device.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pbCommand Supplies the APDU command to be sent to the device.
 * @param ulCommandLen Supplies the length (in bytes) of the pbCommand parameter. 
 * @param ulTimeOutMode Specifies the timeout to wait for a response. The timeout can set to one of the following values:
 *                      - #SDSC_DEV_DEFAULT_TIME_OUT
 *                      - #SDSC_DEV_MAX_TIME_OUT
 *                      - #SDSC_DEV_CUSTOM_FLAG_TIME_OUT
 * @param pbOutData Receives the APDU response from the device (not including two-byte APDU status word).
 * @param pulOutDataLen Supplies the length of the pbOutData parameter (in bytes) and receives the actual number of bytes received from the device.
 * @param pulCosState Receives the APDU status word.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APICommandFunc
 */
DWORD __stdcall SDSCTransmit
(
	IN HANDLE hDevice,
	IN BYTE *pbCommand,
	IN DWORD ulCommandLen,
	IN DWORD ulTimeOutMode, 
	OUT BYTE *pbOutData,
	IN OUT DWORD *pulOutDataLen,
	OUT DWORD *pulCosState
);


/**
 * Send an APDU command to the device, and expect to receive data back from the device.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pbCommand Supplies the APDU command to be sent to the device.
 * @param ulCommandLen Supplies the length (in bytes) of the pbCommand parameter. 
 * @param ulTimeOutMode Specifies the timeout to wait for a response. The timeout can set to one of the following values:
 *                      - #SDSC_DEV_DEFAULT_TIME_OUT
 *                      - #SDSC_DEV_MAX_TIME_OUT
 *                      - #SDSC_DEV_CUSTOM_FLAG_TIME_OUT
 * @param pbOutData Receives the APDU response from the device (including two-byte APDU status word).
 * @param pulOutDataLen Supplies the length of the pbOutData parameter (in bytes) and receives the actual number of bytes received from the device.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APICommandFunc
 */
DWORD __stdcall SDSCTransmitEx
(
	IN HANDLE hDevice,
	IN BYTE *pbCommand,
	IN DWORD ulCommandLen,
	IN DWORD ulTimeOutMode, 
	OUT BYTE *pbOutData,
	IN OUT DWORD *pulOutDataLen
);

/**
 * Get the SDK version.
 * @param pszVersion Receives the SDK version. The buffer size must not be smaller than SDSC_MAX_VERSION_LEN.
 * @param pulVersionLen Supplies the length of the pszVersion buffer in bytes, and receives the actual length of the version string, including the trailing null character.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
DWORD __stdcall SDSCGetSDKVersion
(
    IN BYTE *pszVersion,
    IN OUT DWORD *pulVersionLen
);

/**
 * Get the version of the underlying protocol implementation.
 * @param pszVersion Receives the protocol version. The buffer size must not be smaller than SDSC_MAX_VERSION_LEN.
 * @param pulVersionLen Supplies the length of the pszVersion buffer in bytes, and receives the actual length of the version string, including the trailing null character.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
DWORD __stdcall SDSCGetProtocolVersion
(
    IN BYTE *pszVersion,
    IN OUT DWORD *pulVersionLen
);


/**
 * @brief SDSCGetSCIOType get the communication protocol type of the smart card.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pulSCIOType Receives the communication protocol type of the smart card. \n
                      It can be one of the following: \n
                      - SDSC_SCIO_7816
                      - SDSC_SCIO_SPI_V1
                      - SDSC_SCIO_SPI_V2
                      - SDSC_SCIO_SPI_V3
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
DWORD __stdcall SDSCGetSCIOType
(
	IN HANDLE hDevice,
	OUT DWORD *pulSCIOType
);


#ifdef _WIN32_WCE		// WinCE
/**
 * @brief SDSCSetSDIdleTime use to set maximum idle time of microSD, so that microSD will turn into Power Saving Mode when microSD is not in use. \n
          This function is only support Windows Mobile, is not support Windows system.
 * @param ulSeconds Supply the time in second. Default value is no Power Saving Mode, value as: SDSC_INFINITE_IDLE_TIME.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
DWORD __stdcall SDSCSetSDIdleTime
(
	IN DWORD ulSeconds
);
#endif



#ifdef __cplusplus
}
#endif

#endif

