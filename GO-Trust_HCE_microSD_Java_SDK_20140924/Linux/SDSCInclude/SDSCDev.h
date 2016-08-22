/**
 * @file SDSCDev.h
 * @author GOTrust Technology Inc.
 *
 * @note The Linux API supports both Linux x86 and x64 platforms
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

 
 
#ifndef __SDSCDEV_H__
#define __SDSCDEV_H__

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

/** @brief The type of the smart card I/O interface is unknown. */
#define SDSC_SCIO_UNKNOWN                    0

/** @brief The type of the smart card I/O interface is 7816. */
#define SDSC_SCIO_7816                       1

/** @brief The type of the smart card I/O interface is SPI V1. */
#define SDSC_SCIO_SPI_V1                     2

/** @brief The type of the smart card I/O interface is SPI V2. */
#define SDSC_SCIO_SPI_V2                     3

/** @brief The type of the smart card I/O interface is SPI V3. */
#define SDSC_SCIO_SPI_V3                     4

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
 
/** @brief The maximum number of the devices supported in. */
#define SDSC_MAX_DEV_NUM                4

/** @brief The maximum length of the device drive name, including the trailing null characters. */
#define SDSC_MAX_DEV_NAME_LEN           128

/** @brief The length of the firmware of the device. */
#define SDSC_FIRMWARE_VER_LEN			8

/** @brief The maximum length of device info. */
#define SDSC_DEVICE_INFO_LEN			128

/** @brief The maximum length of the SDK version and the protocol version. */
#define SDSC_MAX_VERSION_LEN			16

/** @} */
#endif	// end: #define SDSC_DEV_DEFINE


#ifndef SDSC_SYS_PARAM_DEFINE
#define SDSC_SYS_PARAM_DEFINE
/**
 * @ingroup DefineOther
 * @{
 */

 /**
 * @brief Specifies the device file name and the mountpoint of the device in order, the device file name and the mountpoint must be separated by ','. \n
 *        All devices can be input by multi-string. It means there are two '/0' in the trail. \n
 * @note Before calling SDSCListDevs, the calling application can call SDSCInitParams to set SDSC_DEVICE_PATH_NAME. \n
 *       For example, if there are two devices: "/dev/sdb1,/media/disk", "/dev/sdc1,/media/disk-1", \n
 *       the two strings can be input by multi-string. \n
 *       If the calling application does not set SDSC_DEVICE_PATH_NAME, SDSCListDevs will search all devices in "/dev/".
 */
#define SDSC_DEVICE_PATH_NAME            1

/** @} */
#endif    // end: #define SDSC_SYS_PARAM_DEFINE

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
unsigned long SDSCSetProductType
(
	int ulProductType
);

/**
 * Set the initial parameters.
 * @param ulParamID Specifies the type of the parameters. It can be set to one of the following: \n
 *                  SDSC_DEVICE_PATH_NAME: Specifies the device file name and the mountpoint of the device in order, the device file name and the mountpoint must be separated by ','. \n
 *                  All devices can be input by multi-string. \n
 *                  It means there are two '/0' in the trail. Before calling SDSCListDevs, the calling application can call SDSCInitParams to set SDSC_DEVICE_PATH_NAME. \n
 *                  For example, if there are two devices: "/dev/sdb1,/media/disk", "/dev/sdc1,/media/disk-1", the two strings can be input by multi-string. \n
 *                  If the calling application does not set SDSC_DEVICE_PATH_NAME, SDSCListDevs will search all devices in "/dev/".
 * @param pszParam Pointer to a null-terminated string.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APISysSetFunc
 */
unsigned long SDSCInitParams
(
    unsigned long ulParamID,
    char *pszParam
);

/**
 * Destroy the initial parameters specified by SDSCInitParams, otherwise memory leak will occur.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APISysSetFunc
 */
unsigned long SDSCDestroyParams
(
);

/**
 * Get the list of all the devices according to SDSC_DEVICE_PATH_NAME.
 * If SDSC_DEVICE_PATH_NAME is not assigned, it will search available devices by checking all mounted devices.
 * 
 * @param pszDrives Receives a multi-string that lists all the devices. \n
 *                  If this value is NULL, SDSCListDevs ignores the buffer length supplied in pulDrivesLen, writes the length of the buffer that would have been returned if this parameter had not been NULL to pulDrivesLen, and returns a success code.
 * @param pulDrivesLen Supplies the length of the pszDrives buffer in bytes, and receives the actual length of the multi-string structure.
 * @param pulDriveNum Receives the number of devices.
 * @param pszIoFileName Supplies the I/O file name. The communication channel between host applications and the secure microSD will be established based on the specified I/O file on the microSD.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIConnectFunc
 */
unsigned long SDSCListDevs
(
    char *pszDrives,
    unsigned long *pulDrivesLen,
    unsigned long *pulDriveNum,
	char* pszIoFileName
);


/**
 * Establishe a connection between the calling application and the device.\n
 * If the smart card has not been reset, this function will do SDSCResetCard() automatically.
 *
 * @param pszDrive Pointer to a null-terminated string that specifies the device drive name.
 * @param phDevice Receives device file descriptor that identifies the connection to the device.
 * @param pszIoFileName Supplies the I/O file name. The communication channel between host applications and the secure microSD is established by using a special I/O file under the root directory of the microSD.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIConnectFunc
 */
unsigned long SDSCConnectDev
(
    char *pszDrive,
    int *phDevice,
	char* pszIoFileName
);

/**
 * Terminate a connection previously opened between the calling application and the device.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIConnectFunc
 */
unsigned long SDSCDisconnectDev
(
    int hDevice
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
unsigned long SDSCGetFirmwareVer
(
    int hDevice,
    unsigned char *pbFirmwareVer,
    unsigned long *pulFirmwareVerLen
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
unsigned long SDSCGetDeviceInfo
(
    int hDevice,
    unsigned char *pbDeviceInfo,
    unsigned long *pulDeviceInfoLen
);

/**
 * Reset smart card.
 * If the smart card has not been reset, this library will do SDSCResetCard() automatically when the calling application firstly calls SDSCConnectDev().
 *
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pbAtr Receives the ATR if resetting smart card successfully. The buffer must be big enough.
 * @param pulAtrLen Supplies the length of the pbAtr buffer in bytes, and receives the actual length of the received ATR.
 * @note If the calling application needn't get the ATR, the parameters pbAtr and pulAtrLen can be NULL.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APICommandFunc
 */
unsigned long SDSCResetCard
(
    int hDevice,
    unsigned char *pbAtr,
    unsigned long *pulAtrLen
);

/**
 * PPS exchange.
 * @param[in] hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param[in] bRate Supplies the baud rate(Fi/Di) in the PPS request.
 * @param[out] pbPPS Receives the actual Fi/Di applied by the device.
 * @param[in,out] pulPPSLen Supplies the length of the pbPPS buffer in bytes, and receives the length of returned data.
 * @return Returns 0 on success; otherwise, returns an error code.
 */
unsigned long SDSCPPS
(
	int hDevice,
	unsigned char bRate,
	unsigned char *pbPPS,
	unsigned long *pulPPSLen
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
unsigned long SDSCTransmit
(
    int hDevice,
    unsigned char *pbCommand,
    unsigned long ulCommandLen,
    unsigned long ulTimeOutMode, 
    unsigned char *pbOutData,
    unsigned long *pulOutDataLen,
    unsigned long *pulCosState
);


/**
 * Send an APDU command to the device, and expects to receive data back from the device.
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
unsigned long SDSCTransmitEx
(
    int hDevice,
    unsigned char *pbCommand,
    unsigned long ulCommandLen,
    unsigned long ulTimeOutMode, 
    unsigned char *pbOutData,
    unsigned long *pulOutDataLen
);


/**
 * Get the SDK version.
 * @param pszVersion Receives the SDK version. The buffer size must not be smaller than SDSC_MAX_VERSION_LEN.
 * @param pulVersionLen Supplies the length of the pszVersion buffer in bytes, and receives the actual length of the version string, including the trailing null character.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
unsigned long SDSCGetSDKVersion
(
    char *pszVersion,
    unsigned long *pulVersionLen
);

/**
 * Get the version of the underlying protocol implementation.
 * @param pszVersion Receives the protocol version. The buffer size must not be smaller than SDSC_MAX_VERSION_LEN.
 * @param pulVersionLen Supplies the length of the pszVersion buffer in bytes, and receives the actual length of the version string, including the trailing null character.
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
unsigned long SDSCGetProtocolVersion
(
    char *pszVersion,
    unsigned long *pulVersionLen
);

/**
 * Get the communication protocol type of the smart card.
 * @param hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param pulSCIOType Receives the communication protocol type of the smart card. It will be one of the following values:
                      - #SDSC_SCIO_7816
                      - #SDSC_SCIO_SPI_V1
                      - #SDSC_SCIO_SPI_V2
                      - #SDSC_SCIO_SPI_V3
 * @return Returns 0 on success; otherwise, returns an error code.
 * @see SDSCErr.h
 * @ingroup APIAuxFunc
 */
unsigned long SDSCGetSCIOType
(
	int hDevice,
	unsigned long *pulSCIOType
);


#ifdef __cplusplus
}
#endif

#endif // end #ifndef __SDSCDEV_H__
