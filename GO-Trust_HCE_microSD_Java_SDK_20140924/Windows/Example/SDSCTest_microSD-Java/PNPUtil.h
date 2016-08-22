// PNPUtil.h : header file

#ifndef __PNP_UTIL_H__
#define __PNP_UTIL_H__

/**
 * Register PNP message.
 * @param hWnd Supplies the handle of the window for which to receive PNP notifications.
 * @param ulMessage Supplies the message the calling application defined.
 * @param pulRegisterID Receives the registration identifier.
 * @note Maybe Windows 2000/2000 server do not support removable media PNP notifications, this depends on the version of the windows shell(shell32.dll). \n
 *       Though register the PNP notifications can success under Windows 2000/2000 server, but maybe Windows 2000/2000 server won't send the removable media PNP messages to the calling applications. \n
 *       Please see the example about how to monitor hardware PNP under Windows 2000/2000 server. \n
 * @return Returns 0 on success; otherwise, returns an error code defined in SDSCErr.h.
 */
DWORD SDSCRegisterHWPNP
(
	IN HWND hWnd,
	IN DWORD ulMessage,
	OUT DWORD *pulRegisterID
);


/**
 * Unregister PNP message.
 * @param ulRegisterID Supplies the registration identifier returned by SDSCRegisterHWPNP.
 * @return Returns 0 on success; otherwise, returns an error code defined in SDSCErr.h.
 */
DWORD SDSCUnregisterHWPNP
(
	IN DWORD ulRegisterID
);


#endif // __PNP_UTIL_H__
