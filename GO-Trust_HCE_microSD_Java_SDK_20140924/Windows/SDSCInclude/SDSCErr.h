/**
 * @file SDSCErr.h
 * @author GOTrust Technology Inc.
 * @brief Specifies error codes for communication.
 * @defgroup ErrorCodes Error Codes
 * @{
 */

#ifndef _SDSC_ERROR_H
#define _SDSC_ERROR_H


//------------------------------------------------------------
// Specifies error codes for communication.
//------------------------------------------------------------
#ifndef SDSC_COMMUNICATION_ERROR_DEFINE
#define SDSC_COMMUNICATION_ERROR_DEFINE


/** @brief The base error code. */
#define SDSC_ERROR_BASE					0x0F000000

/** @brief The parameter is incorrect. */
#define SDSC_PARAM_ERROR				(SDSC_ERROR_BASE | 0x1)

/** @brief The unexpected error has occurred. */
#define SDSC_UNKNOWN_ERROR				(SDSC_ERROR_BASE | 0x2)

/** 
 * @brief There is no enough memory to process this command. \n
 *        It is the same as ERROR_NOT_ENOUGH_MEMORY in Windows API. 
 */
#define SDSC_NO_MEMORY_ERROR			(SDSC_ERROR_BASE | 0x3)

/**
 * @brief The output of the function is too large to fit in the supplied buffer.\n
 *        It is the same as ERROR_MORE_DATA in Windows API.
 */
#define SDSC_BUFFER_SMALL_ERROR			(SDSC_ERROR_BASE | 0x4)

/** @brief Some problem has occurred to the device, and the accurate error code can't be given. */
#define SDSC_DEV_OP_ERROR				(SDSC_ERROR_BASE | 0x5)

/**
 * @brief The device is not ready.
 * The error happens when the device is ever powered off, and users have to call SDSCResetCard() to reinitialize the device again.
 */
#define SDSC_DEV_NOT_READY_ERROR		(SDSC_ERROR_BASE | 0x6)

/** @brief The header that is read from the device is incorrect, it means the device is not our device. */
#define SDSC_DEV_HEADER_ERROR			(SDSC_ERROR_BASE | 0x7)

/** @brief The device is in the state of run time issue.The failed APDU (COS) command needs to be re-transmitted. */
#define SDSC_DEV_RUN_TIME_ERROR			(SDSC_ERROR_BASE | 0x8)

/** @brief The specified data is incorrect. */
#define SDSC_DATA_ERROR					(SDSC_ERROR_BASE | 0x9)

/** @brief The length of the specified data is incorrect. */
#define SDSC_DATA_LEN_ERROR				(SDSC_ERROR_BASE | 0xA)

/** @brief This function is not supported. */
#define SDSC_NOT_SUPPORT_FUN_ERROR		(SDSC_ERROR_BASE | 0xB)

/** @brief The state of the device is incorrect, the system file of the device is not exist. */
#define SDSC_DEV_STATE_ERROR			(SDSC_ERROR_BASE | 0xC)

/** @brief The device is set as writing protection. */
#define SDSC_DEV_WRITE_PROTECT_ERROR	(SDSC_ERROR_BASE | 0xD)

/** @brief The device does not exist. */
#define SDSC_DEV_NOT_EXIST_ERROR		(SDSC_ERROR_BASE | 0xE)

/** @brief The received response format is invalid. */
#define SDSC_DEV_RESPONSE_FORMAT_ERROR	(SDSC_ERROR_BASE | 0xF)

/** @brief The device did not return a valid response till timeout. */
#define SDSC_DEV_TIMEOUT_ERROR			(SDSC_ERROR_BASE | 0x10)

/** @brief The device is been connected. One device only one connect. */
#define SDSC_DEV_OCCUPIED_ERROR         (SDSC_ERROR_BASE | 0x11)

/** @brief The length or format of the I/O file name is incorrect. */
#define SDSC_IO_FILE_NAME_FORMAT_ERROR  (SDSC_ERROR_BASE | 0x12)

/** @brief The type or format of the time out mode is incorrect. */
#define SDSC_TIME_OUT_MODE_FORMAT_ERROR (SDSC_ERROR_BASE | 0x13)

// The error codes that have not been listed before:

/**
 * @brief The base error code for internal communication errors occur between the application and the device.\n
 *        More specifically, an error code is composed of (SDSC_DEV_COMMUNICATION_BASE_ERROR | two-byte abnormal communication state code).\n
 */
#define SDSC_DEV_COMMUNICATION_BASE_ERROR		0x0FF00000

/** @} */	// end of ErrorCodes
#endif	// end: #define SDSC_COMMUNICATION_ERROR_DEFINE

#endif