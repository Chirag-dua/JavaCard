// SDSCDev.h
#ifndef __SDSCDEV_H__
#define __SDSCDEV_H__

//***********************************************************
// -----Specifies the type of the SCIO.
//***********************************************************
#ifndef SDSC_SCIO_TYPE_DEFINE
#define SDSC_SCIO_TYPE_DEFINE

#define SDSC_SCIO_UNKNOWN                    0         ///< The SCIO type is unknown.
#define SDSC_SCIO_7816                       1         ///< The SCIO type is ISO7816.
#define SDSC_SCIO_SPI_V1                     2         ///< The SCIO type is SPI V1.
#define SDSC_SCIO_SPI_V2                     3         ///< The SCIO type is SPI V2.
#define SDSC_SCIO_SPI_V3                     4         ///< The SCIO type is SPI V3

#endif	// end: #define SDSC_SCIO_TYPE_DEFINE
//***********************************************************
// Specifies the type of the SCIO.-----
//***********************************************************


//***********************************************************
// -----Specifies the timeout mode of the device.
//***********************************************************
#ifndef SDSC_DEV_TIME_OUT_DEFINE
#define SDSC_DEV_TIME_OUT_DEFINE

/**
 * @brief Use the default timeout value of the device, about 1 minute.
 */
#define SDSC_DEV_DEFAULT_TIME_OUT				0		

/**
 * @brief Use the maximum timeout value of the device, about 3 minute.
 */
#define SDSC_DEV_MAX_TIME_OUT					1		

/**
 * @brief Use the custom timeout value of the device. The time unit is millisecond. \n
 *        SDSC_DEV_CUSTOM_FLAG_TIME_OUT is a flag , the parameter flag must include custom timeout by bitwise-or'd. \n
 *        Timeout value range is 0x00000000 ~ 0x7FFFFFFF \n
 *        Example: If the time out is 300 ms, the parameter is SDSC_DEV_CUSTOM_FLAG_TIME_OUT | 0x12c.
 */
#define SDSC_DEV_CUSTOM_FLAG_TIME_OUT           0x80000000

#endif    // end: #define SDSC_DEV_TIME_OUT_DEFINE
//***********************************************************
// Specifies the timeout mode of the device.-----
//***********************************************************


//***********************************************************
// -----Specifies the relative definition of the device.
//***********************************************************
#ifndef SDSC_DEV_DEFINE
#define SDSC_DEV_DEFINE

#define SDSC_MAX_DEV_NUM                1        // The maximum number of the devices supported in.
#define SDSC_MAX_DEV_NAME_LEN           256      // The maximum length of the device drive name, including the
                                                 // trailing null characters.

//------------------------------------------------------------
// the length of some data 
//------------------------------------------------------------
#define SDSC_FIRMWARE_VER_LEN           8        // The length of the firmware of the device.

#define SDSC_DEVICE_INFO_LEN            128      // The maximum length of device info.

#define SDSC_MAX_VERSION_LEN            16       // The maximum length of the SDK version.

#endif    // end: #define SDSC_DEV_DEFINE
//***********************************************************
// Specifies the relative definition of the device.-----
//***********************************************************



//***********************************************************
// Specifies the other definition of the device.-----
//***********************************************************
#ifndef SDSC_SYS_PARAM_DEFINE
#define SDSC_SYS_PARAM_DEFINE

#define SDSC_DEVICE_PATH_NAME            1       // Specifies the device file name and the mountpoint of the device in order, the device file name 
                                                 // and the mountpoint must be separated by ','. All devices can be input by multi-string. It means there are two '/0' in the trail.
//             Before calling SDSCListDevs, the calling application can call SDSCInitParams to set SDSC_DEVICE_PATH_NAME.
//             For example, if there are two devices: "/dev/sdb1,/media/disk", "/dev/sdc1,/media/disk-1",
//             the two strings can be input by multi-string.
//             If the calling application does not set SDSC_DEVICE_PATH_NAME, SDSCListDevs will search all devices in "/dev/".

#endif    // end: #define SDSC_SYS_PARAM_DEFINE
//***********************************************************
// -----Specifies the other definition of the device.
//***********************************************************

#define SDSC_PRODUCT_TYPE_AUTO_DETECT	0
#define SDSC_PRODUCT_TYPE_NS			1
#define SDSC_PRODUCT_TYPE_CUP			2



#ifdef __cplusplus
extern "C" {
#endif

/**
 * Specify the product type. 
 * If users do not specify the product type, the default type is SDSC_PRODUCT_TYPE_AUTO_DETECT. In this case, the library will try to detect the product type in SDSCListDevs().
 * SDSCListDevs() can have better performance if the product type is specified explicitly.
 * 
 * @param[in] ulProductType The product type. It can be one of the following values:
 				- SDSC_PRODUCT_TYPE_AUTO_DETECT
 				- SDSC_PRODUCT_TYPE_NS
 				- SDSC_PRODUCT_TYPE_CUP
 * @return Returns 0 on success; otherwise, returns an error code.
 */
unsigned long SDSCSetProductType
(
	unsigned long ulProductType
);


/**
 * Create the I/O file under the specified directory.
 * The function blocks the caller until the file creation process completes.
 * 
 * @param[in] pszIoFileDirectory The directory path where the I/O file should be created.
 * @param[out] pulSize This parameter is valid if the following two errors occur:
			1) If the function fails on SDSC_DEV_NO_SPACE_ERROR, the parameter receives the extra disk space (in MB) needed to create the I/O file.
			2) If the function fails on SDSC_IO_FILE_FRAGMENTED_ERROR, the parameter receives the size (in MB) which cannot be used for I/O commands.
 * @return Returns 0 on success; otherwise, returns an error code.
 */
unsigned long SDSCCreateIoFile
(
	char *pszIoFileDirectory,
	unsigned long* pulSize
);

/**
 * Get the progress of I/O file creation.
 * 
 * @param[in] pszIoFileDirectory The directory path where the I/O file is created.
 * @param[out] psiProgress Receives the progress ranging from 0 to 100 of the I/O file creation.
 * @return Returns 0 on success; otherwise, returns an error code.
 */
unsigned long SDSCGetCreateIoFileProgress
(
	char *pszIoFileDirectory,
	short* psiProgress
);


// This function set the initial parameters.
// Parameters:
// IN unsigned long ulParamID: Specifies the type of parameter
//             It can be set to one of the following values:
//             SDSC_DEVICE_PATH_NAME: Specifies the device file name and the mountpoint of the device in order, the device file name 
//                  and the mountpoint must be separated by ','. All devices can be input by multi-string. It means there are two '/0' in the trail.
//                  Before calling SDSCListDevs, the calling application can call SDSCInitParams to set SDSC_DEVICE_PATH_NAME.
//                  For example, if there are two devices: "/dev/sdb1,/media/disk", "/dev/sdc1,/media/disk-1",
//                  the two strings can be input by multi-string.
//                  If the calling application does not set SDSC_DEVICE_PATH_NAME, SDSCListDevs will search all devices in "/dev/".
// IN char *pszParam: Pointer to a null-terminated string.
unsigned long SDSCInitParams
(
    unsigned long ulParam,
    char *pszParam
);


// Release the destroy parameters specified by SDSCInitParams, otherwise memory leak will occur.
unsigned long SDSCDestroyParams
(
);


// This function receives the list of all the devices according as SDSC_DEVICE_PATH_NAME, and returns the number of devices.
// Before calling SDSCListDevs, the calling application can call SDSCInitParams to set SDSC_DEVICE_PATH_NAME.
// The default value of SDSC_DEVICE_PATH_NAME is "/dev/".
// Parameters:
// OUT char *pszDrives: Receives a multi-string that lists all the devices.
//             If this value is NULL, SDSCListDevs ignores the buffer length supplied in pulDrivesLen, 
//             writes the length of the buffer that would have been returned if this parameter had not
//             been NULL to pulDrivesLen, and returns a success code. 
// IN OUT unsigned long *pulDrivesLen: Supplies the length of the pszDrives buffer in characters, 
//             and receives the actual length of the multi-string structure, including all trailing null characters. 
// OUT unsigned long *pulDriveNum: Receives the number of devices.
// OUT pszIoFilePath Supplies the complete I/O file path and file name. The communication channel between host applications and the secure microSD is established by using a special I/O file under the root directory of the microSD.
unsigned long SDSCListDevs
(
    char *pszDrives,
    unsigned long *pulDrivesLen,
    unsigned long *pulDriveNum,
	char* pszIoFilePath
);


// This function establishes a connection between the calling application and the device. If the smart card has not been reset,
// this library will do SDSCResetCard automatically when the calling application firstly calls SDSCConnectDev.
// Parameters:
// IN char *pszIoFilePath Supplies the complete I/O file path and file name.
// OUT int *phDevice: Receives device file descriptor that identifies the connection to the SD device.
unsigned long SDSCConnectDev
(
    char *pszIoFilePath,
    int *phDevice
);


// This function terminates a connection previously opened between the calling application and the device.
// Parameters:
// IN int hDevice: Supplies the reference value obtained from a previous call to SDSCConnectDev.
unsigned long SDSCDisconnectDev
(
    int hDevice
);


// Receives the firmware version of the device.
// Parameters:
// IN int hDevice: Supplies the reference value obtained from a previous call to SDSCConnectDev.
// OUT unsigned char *pbFirmwareVer: Receives the firmware version of the device.
//             If this value is NULL, SDSCGetFirmwareVer ignores the buffer length supplied in pulFirmwareVerLen, 
//             writes the length of the buffer that would have been returned if this parameter had not been NULL to pulFirmwareVerLen, and returns a success code.
// IN OUT unsigned long *pulFirmwareVerLen: Supplies the length of the pbFirmwareVer buffer in bytes, and receives the actual length of the received pbFirmwareVer.
//             In the current version, the length of the firmware version of the device is SDSC_FIRMWARE_VER_LEN.
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
 * @param[in] hDevice Supplies the device handle obtained by calling SDSCConnectDev().
 * @param[out] pbDeviceInfo Receives the device Info. If pbDeviceInfo is NULL, the function writes the actual length of received device info to pulDeviceInfoLen and returns success.
 * @param[in,out] pulDeviceInfoLen Supplies the length of the pbDeviceInfo buffer in bytes, and receives the actual length of the received device info.
 * @return Returns 0 on success; otherwise, returns an error code.
 */
unsigned long SDSCGetDeviceInfo
(
	int hDevice,
	unsigned char *pbDeviceInfo,
	unsigned long *pulDeviceInfoLen
);



// Reset smart card.
// Parameters:
// IN int hDevice: Supplies the reference value obtained from a previous call to SDSCConnectDev.
// OUT unsigned char *pbAtr: Receives the  ATR.
//             If this value is NULL, SDSCResetCard ignores the buffer length supplied in pulAtrLen, 
//             writes the length of the buffer that would have been returned if this parameter had not been NULL to pulAtrLen, and returns a success code. 
// IN OUT unsigned long *pulAtrLen: Supplies the length of the pbAtr buffer in bytes, and receives the actual length of the received ATR.
// Remarks:
// If the calling application needn't get the ATR, the parameters pbAtr and pulAtrLen can be NULL.
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

// This function sends a COS command to the device, and expects to receive data back from the device.
// Parameters:
// IN int hDevice: Supplies the reference value obtained from a previous call to SDSCConnectDev.
// IN unsigned char *pbCommand: Pointer to the COS command to be written to the device.
// IN unsigned long ulCommandLen: Supplies the length (in bytes) of the pbCommand parameter. 
// IN unsigned long ulTimeOutMode: Specify the timeout mode of the device. When sending a COS command to smart card, smart card must send back the response to host. 
//             The time period between the last character of command and first byte of response may exceed work waiting time.
//             The work waiting time can set to one of the following values:
//             SDSC_DEV_DEFAULT_TIME_OUT: Use the default timeout value of the device. If COS does not send any 
//                 response back to SD controller in default timeout, this function will return an error code. 
//                 If COS send a Null ACK to SD controller, the timeout counter will be restart. The library 
//                 will wait 1 minute in total for COS returning the status word (SW).
//             SDSC_DEV_MAX_TIME_OUT: Use maximum timeout value of the device. If COS does not send any response back 
//                 to SD controller in maximum timeout, this function will return an error code. If COS send a Null
//                 ACK to SD controller, the timeout counter will be restart. The library will wait 3 minute in total for COS returning the status word (SW).

//             When using the 7816, the default timeout value is around 1.5 seconds, the maximum timeout value is around 15 seconds.
//             When using the SPI, the default timeout value is around 4.5 seconds, the maximum timeout value is around 45 seconds.
//             The default value is SDSC_DEV_DEFAULT_TIME_OUT. To check errors and optimize performance, the application should not always use SDSC_DEV_MAX_TIME_OUT.
// OUT unsigned char *pbOutData: Pointer to any data returned from the device (not including two bytes COS status word). If no data will be returned, this parameter can be NULL.
// IN OUT unsigned long *pulOutDataLen: Supplies the length of the pbOutData parameter (in bytes) and receives the actual number of bytes received from the device.
//             This parameter cannot be NULL.
// OUT unsigned long *pulCosState: Receives the status word of the COS command.This parameter cannot be NULL.
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


// This function sends a COS command to the device, and expects to receive data back from the device.
// Parameters:
// IN int hDevice: Supplies the reference value obtained from a previous call to SDSCConnectDev.
// IN unsigned char *pbCommand: Pointer to the COS command to be written to the device.
// IN unsigned long ulCommandLen: Supplies the length (in bytes) of the pbCommand parameter. 
// IN unsigned long ulTimeOutMode: Specify the timeout mode of the device. When sending a COS command to smart card, smart card must send back the response to host. 
//             The time period between the last character of command and first byte of response may exceed work waiting time. 
//             The work waiting time can set to one of the following values:
//             SDSC_DEV_DEFAULT_TIME_OUT: Use the default timeout value of the device. If COS does not send any response back to 
//                 SD controller in default timeout, this function will return an error code. If COS send a Null ACK to SD controller, 
//                 the timeout counter will be restart. The library will wait 1 minute in total for COS returning the status word (SW).
//             SDSC_DEV_MAX_TIME_OUT: Use maximum timeout value of the device. If COS does not send any response back to SD controller in maximum timeout, 
//                 this function will return an error code. If COS send a Null ACK to SD controller, the timeout counter will be restart.
//                 The library will wait 3 minute in total for COS returning the status word (SW).
//             When using the 7816, the default timeout value is around 1.5 seconds, the maximum timeout value is around 15 seconds.
//             When using the SPI, the default timeout value is around 4.5 seconds, the maximum timeout value is around 45 seconds.
//             The default value is SDSC_DEV_DEFAULT_TIME_OUT. To check errors and optimize performance, the application should not always use SDSC_DEV_MAX_TIME_OUT.
// OUT unsigned char *pbOutData: Pointer to any data returned from the device (not including two bytes COS status word). If no data will be returned, this parameter can be NULL.
// IN OUT unsigned long *pulOutDataLen: Supplies the length of the pbOutData parameter (in bytes) and receives the actual number of bytes received from the device.
//             This parameter cannot be NULL.
unsigned long SDSCTransmitEx
(
    int hDevice,
    unsigned char *pbCommand,
    unsigned long ulCommandLen,
    unsigned long ulTimeOutMode, 
    unsigned char *pbOutData,
    unsigned long *pulOutDataLen
);


// Parameters:
// OUT char *pszVersion: Receives the current version of SDK, the maximum length of pszVersion is no larger than SDSC_MAX_VERSION_LEN.
// IN OUT unsigned long *pulVersionLen: Supplies the length of the pszVersion buffer in characters,and receives the actual 
//             length of the string, including the trailing null characters.
unsigned long SDSCGetSDKVersion
(
    char *pszVersion,
    unsigned long *pulVersionLen
);

/**
 * Get the underlying smart card protocol version.
 * @param[out] pszVersion Receives the protocol version.
 * @param[in,out] pulVersionLen Supplies the length of the pszVersion buffer in characters,and receives the actual length of the string, including the trailing null characters.
 * @return Returns 0 on success; otherwise, returns an error code.
 */
unsigned long SDSCGetProtocolVersion
(
	char *pszVersion,
	unsigned long *pulVersionLen
);


// Get the protocol type of the smart card
// Parameters:
// IN int hDevice:Supplies the reference value obtained from a previous call to SDSCConnectDev.
// OUT unsigned long *pulSCIOType:Receives the type of the smart card.
//                       It can be one of the following: SDSC_SCIO_7816, SDSC_SCIO_SPI_V1, SDSC_SCIO_SPI_V2, SDSC_SCIO_SPI_V3.
unsigned long SDSCGetSCIOType
(
	int hDevice,
	unsigned long *pulSCIOType
);

#ifdef __cplusplus
}
#endif

#endif // end #ifndef __SDSCDEV_H__
