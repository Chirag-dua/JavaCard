#include "stdafx.h"
#include "PNPUtil.h"
#include "shlobj.h"

#include "..\..\..\SDSCInclude\SDSCErr.h"

typedef ULONG
(WINAPI* pfnSHChangeNotifyRegister)
(
	HWND hWnd,
	int fSource,
	LONG fEvents,
	UINT wMsg,
	int cEntries,
	SHChangeNotifyEntry* pfsne 
);

typedef BOOL (WINAPI* pfnSHChangeNotifyDeregister)(ULONG ulID);

DWORD SDSCRegisterHWPNP
(
	IN HWND hWnd,
	IN DWORD ulMessage,
	OUT DWORD *pulRegisterID
)
{
	SetErrorMode(SEM_FAILCRITICALERRORS|SEM_NOGPFAULTERRORBOX|SEM_NOOPENFILEERRORBOX);	// Don't display system error

	DWORD ulRet,ulRegisterID;
	SHChangeNotifyEntry shcne;  
	HMODULE hShell32=NULL;
	pfnSHChangeNotifyRegister pSHChangeNotifyRegister=NULL;
	LPITEMIDLIST pidl;
	
	if(!pulRegisterID)
		return SDSC_PARAM_ERROR;

	hShell32 = LoadLibrary("Shell32.dll");
	if(hShell32 == NULL)
	{
		ulRet=SDSC_UNKNOWN_ERROR;
		goto EndOP;
	}

	memset(&shcne,0x00,sizeof(SHChangeNotifyEntry));
	if   (NOERROR == SHGetSpecialFolderLocation(hWnd, CSIDL_DESKTOP, &pidl)) 
		shcne.pidl = pidl;   // Pointer to an item identifier list(PIDL) for which to receive notifications.  
	else
	{
		ulRet=SDSC_UNKNOWN_ERROR;
		goto EndOP;
	}

	// Flag indicating whether to post notifications for children of this PIDL.    
	// For example, if the PIDL points to a folder, then file notifications would come from the folder's children if this flag was TRUE. 
	shcne.fRecursive = TRUE;   	
	
	pSHChangeNotifyRegister = (pfnSHChangeNotifyRegister)GetProcAddress(hShell32,MAKEINTRESOURCE(2));
	if(!pSHChangeNotifyRegister)
	{
		ulRet=SDSC_UNKNOWN_ERROR;
		goto EndOP;
	}
	
	// SHCNF_TYPE
	ulRegisterID=pSHChangeNotifyRegister(hWnd,SHCNF_TYPE|SHCNF_IDLIST,SHCNE_MEDIAINSERTED|SHCNE_MEDIAREMOVED|SHCNE_DRIVEADD|SHCNE_DRIVEREMOVED,	// SHCNE_ALLEVENTS
		ulMessage,1,&shcne);
	if(ulRegisterID==0)
	{
		ulRet=SDSC_UNKNOWN_ERROR;
		goto EndOP;
	}

	*pulRegisterID=ulRegisterID;

EndOP:
	if(hShell32)
		FreeLibrary(hShell32);

	return ulRet;
}

DWORD SDSCUnregisterHWPNP
(
	IN DWORD ulRegisterID
)
{
	SetErrorMode(SEM_FAILCRITICALERRORS|SEM_NOGPFAULTERRORBOX|SEM_NOOPENFILEERRORBOX);	// Don't display system error

	DWORD ulRet;
	HMODULE hShell32=NULL;
	pfnSHChangeNotifyDeregister pSHChangeNotifyDeregister=NULL;

	hShell32 = LoadLibrary("Shell32.dll");
	if(hShell32 == NULL)
	{
		ulRet=SDSC_UNKNOWN_ERROR;
		goto EndOP;
	}
	
	pSHChangeNotifyDeregister = (pfnSHChangeNotifyDeregister)GetProcAddress(hShell32,MAKEINTRESOURCE(4));
	if(!pSHChangeNotifyDeregister)
	{
		ulRet=SDSC_UNKNOWN_ERROR;
		goto EndOP;
	}
	
	pSHChangeNotifyDeregister(ulRegisterID);
	
EndOP:
	if(hShell32)
		FreeLibrary(hShell32);

	return ulRet;
}
