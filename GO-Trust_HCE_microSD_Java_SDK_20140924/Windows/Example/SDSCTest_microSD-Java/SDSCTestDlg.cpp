// SDSCTestDlg.cpp : implementation file
//

#include "stdafx.h"
#include "SDSCTest.h"
#include "SDSCTestDlg.h"

#include "PNPUtil.h"
#include "..\..\..\SDSCInclude\SDSCDev.h"
#include "..\..\..\SDSCInclude\SDSCErr.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

#define WM_USER_SHNOTIFY	(WM_USER+0x100)

BOOL g_bSupportPNPMsg=FALSE;
DWORD g_ulRegisterID=0;

HANDLE g_hEvent=NULL;
HANDLE g_hMonitorHandle=NULL;
/////////////////////////////////////////////////////////////////////////////
// CAboutDlg dialog used for App About

class CAboutDlg : public CDialog
{
public:
	CAboutDlg();

// Dialog Data
	//{{AFX_DATA(CAboutDlg)
	enum { IDD = IDD_ABOUTBOX };
	//}}AFX_DATA

	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CAboutDlg)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	//{{AFX_MSG(CAboutDlg)
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

CAboutDlg::CAboutDlg() : CDialog(CAboutDlg::IDD)
{
	//{{AFX_DATA_INIT(CAboutDlg)
	//}}AFX_DATA_INIT
}

void CAboutDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CAboutDlg)
	//}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(CAboutDlg, CDialog)
	//{{AFX_MSG_MAP(CAboutDlg)
		// No message handlers
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CSDSCTestDlg dialog

CSDSCTestDlg::CSDSCTestDlg(CWnd* pParent /*=NULL*/)
	: CDialog(CSDSCTestDlg::IDD, pParent)
{
	//{{AFX_DATA_INIT(CSDSCTestDlg)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	// Note that LoadIcon does not require a subsequent DestroyIcon in Win32
	m_hIcon = AfxGetApp()->LoadIcon(IDR_MAINFRAME);
}

void CSDSCTestDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CSDSCTestDlg)
	DDX_Control(pDX, IDC_MSG_LIST, m_MsgList);
	DDX_Control(pDX, IDC_COMBO_DEV_LIST, m_DevList);
	//}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(CSDSCTestDlg, CDialog)
	//{{AFX_MSG_MAP(CSDSCTestDlg)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	ON_BN_CLICKED(IDC_BTN_TEST, OnBtnTest)
	ON_BN_CLICKED(IDC_BTN_REFRESH, OnBtnRefresh)
	ON_WM_DESTROY()
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CSDSCTestDlg message handlers


#ifdef _WIN32_WCE
#define MyHexToStr MyHexToStrW
#else
#define MyHexToStr MyHexToStrA
#endif

// Convert the hex data to hex ASCII string, the length of the hex ASCII string is 2*ulHexLen.
// for example: "\x01\x23\x45\x67\x89\xAB\xCD\xEF" --> "0123456789ABCDEF".
// Parameters:
// IN unsigned char *pbHex: Supplies the hex data.
// IN unsigned long ulHexLen: Supplies the length of the hex data.
// OUT char *pStr: Receives the converted hex ASCII string, supposed the pointer is legal and the buffer is big enough.
void MyHexToStrA
(
	unsigned char *pbHex,
	unsigned long ulHexLen,
	char *pStr
)
{
	unsigned long i;
	unsigned char ucHigh,ucLow;
	
	if(ulHexLen==0)
		return;
	if(!pbHex || !pStr)
		return;
	for(i=0;i<ulHexLen;i++)
	{
		ucHigh=(pbHex[i]&0xf0)>>4;		// the high half byte
		ucLow=pbHex[i]&0x0f;			// the low half byte
		
		if(ucHigh>=0 && ucHigh<=9)		// 0-9
			pStr[2*i]=ucHigh+0x30;
		else							// A-F
			pStr[2*i]=ucHigh+0x37;
		
		if(ucLow>=0 && ucLow<=9)
			pStr[2*i+1]=ucLow+0x30;
		else
			pStr[2*i+1]=ucLow+0x37;
	}
}

void MyHexToStrW
(
	unsigned char *pbHex,
	unsigned long ulHexLen,
	WCHAR *pStr
)
{
	unsigned long i;
	unsigned char ucHigh,ucLow;
	
	if(ulHexLen==0)
		return;
	if(!pbHex || !pStr)
		return;
	for(i=0;i<ulHexLen;i++)
	{
		ucHigh=(pbHex[i]&0xf0)>>4;		// the high half byte
		ucLow=pbHex[i]&0x0f;			// the low half byte
		
		if(ucHigh>=0 && ucHigh<=9)		// 0-9
			pStr[2*i]=ucHigh+0x30;
		else							// A-F
			pStr[2*i]=ucHigh+0x37;
		
		if(ucLow>=0 && ucLow<=9)
			pStr[2*i+1]=ucLow+0x30;
		else
			pStr[2*i+1]=ucLow+0x37;
	}
}


// monitor thread for device PNP
DWORD __stdcall MonitorProc
(
	IN void* pParam
)
{
	DWORD ulRet,ulMaxRemovableDrivesLen;
	DWORD ulRemovableDrivesLen,ulLastRemovableDrivesLen,ulRemovableDriveNum;
	TCHAR *pszRemovableDrives=NULL,*pszLastRemovableDrives=NULL;

#ifdef _WIN32_WCE
	// Though we just support SDSC_MAX_DEV_NUM safe SD devices,
	// we will list more removable drives.
	ulMaxRemovableDrivesLen=SDSC_CE_MAX_FLASH_NUM*MAX_PATH;
#else
	ulMaxRemovableDrivesLen=SDSC_MAX_DEV_NUM*MAX_PATH;
#endif

	if(!(pszRemovableDrives=(TCHAR*)malloc(ulMaxRemovableDrivesLen*sizeof(TCHAR))))
		return -1;
	if(!(pszLastRemovableDrives=(TCHAR*)malloc(ulMaxRemovableDrivesLen*sizeof(TCHAR))))
		return -1;
	memset(pszRemovableDrives,0x00,ulMaxRemovableDrivesLen*sizeof(TCHAR));
	memset(pszLastRemovableDrives,0x00,ulMaxRemovableDrivesLen*sizeof(TCHAR));
	
	// Initial value 
	ulLastRemovableDrivesLen=1;
	while(1)
	{
		if(g_hEvent)
		{
			if(WaitForSingleObject(g_hEvent,0)==WAIT_OBJECT_0)
				break;
		}
		ulRemovableDrivesLen=ulMaxRemovableDrivesLen;
		if(ulRet=SDSCListRemovableDrives(pszRemovableDrives,&ulRemovableDrivesLen,&ulRemovableDriveNum))
		{
			Sleep(SDSC_DEV_MONITOR_INTERVAL);	// sleep for SDSC_DEV_MONITOR_INTERVAL ms
			continue;
		}
		if(ulLastRemovableDrivesLen==ulRemovableDrivesLen && 
			_memicmp(pszLastRemovableDrives,pszRemovableDrives,ulRemovableDrivesLen*sizeof(TCHAR))==0)
		{
			Sleep(SDSC_DEV_MONITOR_INTERVAL);	// sleep for SDSC_DEV_MONITOR_INTERVAL ms
			continue;
		}
		ulLastRemovableDrivesLen=ulRemovableDrivesLen;
		memcpy(pszLastRemovableDrives,pszRemovableDrives,ulRemovableDrivesLen*sizeof(TCHAR));
		
		Sleep(SDSC_PNP_SLEEP_TIME);
		CSDSCTestDlg *dlg = NULL;
		dlg = (CSDSCTestDlg *)pParam;
		dlg->RefreshDev();

		Sleep(SDSC_DEV_MONITOR_INTERVAL);		// sleep for SDSC_DEV_MONITOR_INTERVAL ms
	}
	free(pszRemovableDrives);
	free(pszLastRemovableDrives);
	return 0;
}


BOOL CSDSCTestDlg::OnInitDialog()
{
	CDialog::OnInitDialog();

	// Add "About..." menu item to system menu.

	// IDM_ABOUTBOX must be in the system command range.
	ASSERT((IDM_ABOUTBOX & 0xFFF0) == IDM_ABOUTBOX);
	ASSERT(IDM_ABOUTBOX < 0xF000);

	CMenu* pSysMenu = GetSystemMenu(FALSE);
	if (pSysMenu != NULL)
	{
		CString strAboutMenu;
		strAboutMenu.LoadString(IDS_ABOUTBOX);
		if (!strAboutMenu.IsEmpty())
		{
			pSysMenu->AppendMenu(MF_SEPARATOR);
			pSysMenu->AppendMenu(MF_STRING, IDM_ABOUTBOX, strAboutMenu);
		}
	}

	// Set the icon for this dialog.  The framework does this automatically
	//  when the application's main window is not a dialog
	SetIcon(m_hIcon, TRUE);			// Set big icon
	SetIcon(m_hIcon, FALSE);		// Set small icon
	
	// TODO: Add extra initialization here

	GetDlgItem(IDC_IOFILE)->SetWindowText(TEXT("SMART_IO.CRD"));

    OSVERSIONINFO osvi;
	BOOL bIsWindowsXPorLater=FALSE;
	
	memset(&osvi, 0x00,sizeof(osvi));
    osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
    if(GetVersionEx(&osvi))
	{
		// osvi.dwMajorVersion == 6: vista/2008.
		// osvi.dwMajorVersion == 5: 2000/XP/2003 (osvi.dwMinorVersion=0: 2000/2000 server(exclude it))
		bIsWindowsXPorLater = ( (osvi.dwMajorVersion > 5) ||
			( (osvi.dwMajorVersion == 5) && (osvi.dwMinorVersion >= 1) ));
		if(bIsWindowsXPorLater)	// XP/2003/vista/2008
		{
			if(0==SDSCRegisterHWPNP(m_hWnd,WM_USER_SHNOTIFY,&g_ulRegisterID))
			{
				g_bSupportPNPMsg=TRUE;
				RefreshDev();
			}
		}
		// Maybe Windows 2000/2000 server do not support removable media message notifications, so we use monitor thread.
	}

	if(!g_bSupportPNPMsg)
	{
		DWORD ulThreadID;
		g_hEvent=CreateEvent(NULL,TRUE,FALSE,NULL);
		g_hMonitorHandle=CreateThread(NULL,0, MonitorProc,this,0,&ulThreadID);
	}

	return TRUE;  // return TRUE  unless you set the focus to a control
}

void CSDSCTestDlg::OnSysCommand(UINT nID, LPARAM lParam)
{
	if ((nID & 0xFFF0) == IDM_ABOUTBOX)
	{
		CAboutDlg dlgAbout;
		dlgAbout.DoModal();
	}
	else
	{
		CDialog::OnSysCommand(nID, lParam);
	}
}

// If you add a minimize button to your dialog, you will need the code below
//  to draw the icon.  For MFC applications using the document/view model,
//  this is automatically done for you by the framework.

void CSDSCTestDlg::OnPaint() 
{
	if (IsIconic())
	{
		CPaintDC dc(this); // device context for painting

		SendMessage(WM_ICONERASEBKGND, (WPARAM) dc.GetSafeHdc(), 0);

		// Center icon in client rectangle
		int cxIcon = GetSystemMetrics(SM_CXICON);
		int cyIcon = GetSystemMetrics(SM_CYICON);
		CRect rect;
		GetClientRect(&rect);
		int x = (rect.Width() - cxIcon + 1) / 2;
		int y = (rect.Height() - cyIcon + 1) / 2;

		// Draw the icon
		dc.DrawIcon(x, y, m_hIcon);
	}
	else
	{
		CDialog::OnPaint();
	}
}

// The system calls this to obtain the cursor to display while the user drags
//  the minimized window.
HCURSOR CSDSCTestDlg::OnQueryDragIcon()
{
	return (HCURSOR) m_hIcon;
}

DWORD CSDSCTestDlg::RefreshDev()
{
	DWORD ulRet;
	TCHAR szMsg[256];
	TCHAR *pszDrives=NULL,*pTemp;
	DWORD ulDrivesLen,ulDriveNum,ulDriveCount;
	
	m_MsgList.ResetContent();
	
	CString IoFileName;
	GetDlgItem(IDC_IOFILE)->GetWindowText(IoFileName);
	
	if(!(pszDrives=(TCHAR*)malloc(SDSC_MAX_DEV_NUM*SDSC_MAX_DEV_NAME_LEN*sizeof(TCHAR))))
	{
		wsprintf(szMsg,TEXT("Memory is not enough!"));
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		
		ulRet=ERROR_NOT_ENOUGH_MEMORY;
		goto EndOP;
	}
	memset(pszDrives,0x00,SDSC_MAX_DEV_NUM*SDSC_MAX_DEV_NAME_LEN*sizeof(TCHAR));
	
	ulDrivesLen=SDSC_MAX_DEV_NUM*SDSC_MAX_DEV_NAME_LEN;
	if(ulRet=SDSCListDevs(pszDrives,&ulDrivesLen,&ulDriveNum, IoFileName.GetBuffer()))
	{
		wsprintf(szMsg,TEXT("SDSCListDevs failed, error code: 0x%08x"),ulRet);
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		goto EndOP;
	}
	
	// clear all device in ComboBox
	m_DevList.ResetContent();
	
	ulDriveCount=0;
	pTemp=pszDrives;
	while(pTemp && *pTemp!=0x00)
	{
		m_DevList.AddString(pTemp);
		pTemp+=_tcslen(pTemp)+1;
		ulDriveCount++;
	}
	
	if(ulDriveCount!=ulDriveNum)	// device number error
	{
		wsprintf(szMsg,TEXT("The number of device error!"));
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		
		ulRet=SDSC_UNKNOWN_ERROR;
		goto EndOP;
	}
	m_DevList.SetCurSel(0);
	
	if(pszDrives)
		free(pszDrives);
	return 0;
EndOP:
	if(pszDrives)
		free(pszDrives);
	return ulRet;
}

DWORD CSDSCTestDlg::GetDriveName(TCHAR *pszDrive,DWORD *pulDriveLen)
{
	TCHAR szDrive[SDSC_MAX_DEV_NAME_LEN];
	DWORD ulDriveLen;
	
	memset(szDrive,0x00,sizeof(szDrive));
	m_DevList.GetWindowText(szDrive,sizeof(szDrive)/sizeof(TCHAR));
	ulDriveLen=_tcslen(szDrive)+1;
	
	if(!pszDrive)
	{
		*pulDriveLen=ulDriveLen;
		return 0;
	}
	if(*pulDriveLen<ulDriveLen)
	{
		*pulDriveLen=ulDriveLen;
		return ERROR_NOT_ENOUGH_MEMORY;
	}
	*pulDriveLen=ulDriveLen;
	_tcscpy(pszDrive,szDrive);
	return 0;
}

void CSDSCTestDlg::OnBtnTest() 
{
	// TODO: Add your control notification handler code here
	TCHAR szDrive[SDSC_MAX_DEV_NAME_LEN],szMsg[256];
	DWORD ulDriveLen;
	DWORD ulRet;
	HANDLE hDevice=NULL;
	BYTE bAtr[36],bCommand[256],bRetBuf[256];
	TCHAR szAtr[73],szCommand[512];	// 73=2*36+1
	DWORD ulAtrLen,ulCommandLen,ulRetBufLen,ulCOSState;
	CString IoFileName;
	
	GetDlgItem(IDC_IOFILE)->GetWindowText(IoFileName);
	m_MsgList.ResetContent();

	memset(szDrive,0x00,sizeof(szDrive));
	ulDriveLen=sizeof(szDrive)/sizeof(TCHAR);	
	if(ulRet=GetDriveName(szDrive,&ulDriveLen))
		goto EndOP;
	if(szDrive[0]=='\0')
	{
		wsprintf(szMsg,TEXT("This device is not exist!"));
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		goto EndOP;
	}
	
	// Connect the device
	if(ulRet=SDSCConnectDev(szDrive,&hDevice, IoFileName.GetBuffer()))
	{
		wsprintf(szMsg,TEXT("SDSCConnectDev error, error code: 0x%08x."),ulRet);
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		goto EndOP;
	}

	// Reset smart card.
	// In fact, our dll will do SDSCResetCard automatically when the calling application firstly calls SDSCConnectDev.
	// In the demo source codes, we call this function just for getting ATR.
	ulAtrLen=sizeof(bAtr);
	if(ulRet=SDSCResetCard(hDevice,bAtr,&ulAtrLen))
	{
		wsprintf(szMsg,TEXT("SDSCResetCard error, error code: 0x%08x."),ulRet);
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		goto EndOP;
	}
	if(ulAtrLen>0)
	{
		memset(szAtr,0x00,sizeof(szAtr));
		MyHexToStr(bAtr,ulAtrLen,szAtr);
		wsprintf(szMsg,TEXT("The ATR is 0x%s."),szAtr);
		m_MsgList.AddString(szMsg);
		UpdateWindow();
	}

	// select Card Manager
	ulCommandLen=13;
	memcpy(bCommand,"\x00\xA4\x04\x00\x08\xA0\x00\x00\x00\x03\x00\x00\x00",ulCommandLen);
	ulRetBufLen=sizeof(bRetBuf);

	memset(szCommand,0x00,sizeof(szCommand));
	MyHexToStr(bCommand,ulCommandLen,szCommand);
	wsprintf(szMsg,TEXT("APDU: %s (Select Card Manager)."),szCommand);
	m_MsgList.AddString(szMsg);
	UpdateWindow();
	
	if(ulRet=SDSCTransmit(hDevice,bCommand,ulCommandLen,SDSC_DEV_DEFAULT_TIME_OUT,bRetBuf,&ulRetBufLen,&ulCOSState))
	{
		wsprintf(szMsg,TEXT("SDSCTransmit error, error code: 0x%08x."),ulRet);
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		goto EndOP;
	}
	wsprintf(szMsg,TEXT("Response: %x"),ulCOSState);
	m_MsgList.AddString(szMsg);
	UpdateWindow();

	// Disconnect the device
	if(ulRet=SDSCDisconnectDev(hDevice))
	{
		wsprintf(szMsg,TEXT("SDSCDisconnectDev error, error code: 0x%08x."),ulRet);
		m_MsgList.AddString(szMsg);
		UpdateWindow();
		return;		// needn't goto EndOP
	}
	
	wsprintf(szMsg,TEXT("Test for device information end."));
	m_MsgList.AddString(szMsg);
	UpdateWindow();
	return;
EndOP:
	if(hDevice!=NULL && hDevice!=INVALID_HANDLE_VALUE)
	{
		SDSCDisconnectDev(hDevice);
	}
	return;
}

void CSDSCTestDlg::OnBtnRefresh() 
{
	// TODO: Add your control notification handler code here
	RefreshDev();
}


LRESULT CSDSCTestDlg::WindowProc(UINT message, WPARAM wParam, LPARAM lParam) 
{
	// TODO: Add your specialized code here and/or call the base class
	if(g_bSupportPNPMsg)
	{
		switch(message)
		{
		case  WM_USER_SHNOTIFY:
			if((lParam == SHCNE_DRIVEADD) || (lParam == SHCNE_DRIVEREMOVED) ||	// SD Reader PNP
				(lParam == SHCNE_MEDIAINSERTED) || (lParam == SHCNE_MEDIAREMOVED))	// SD Media PNP
			{
				Sleep(SDSC_PNP_SLEEP_TIME);
				RefreshDev();
			}
			break;
		default:
			break;
		}
	}
	return CDialog::WindowProc(message, wParam, lParam);
}

void CSDSCTestDlg::OnDestroy() 
{
	CDialog::OnDestroy();
	
	// TODO: Add your message handler code here

	if(g_bSupportPNPMsg)
		SDSCUnregisterHWPNP(g_ulRegisterID);
	else
	{
		// SetEvent
		if(g_hEvent)
			SetEvent(g_hEvent);
		if(g_hMonitorHandle)
		{
			int i;
			DWORD ulExitCode;
			// check the monitor thread is exited safely?
			for(i=0;i<5;i++)
			{
				if(GetExitCodeThread(g_hMonitorHandle,&ulExitCode) && ulExitCode==STILL_ACTIVE)
				{
					Sleep(SDSC_DEV_MONITOR_INTERVAL);
					continue;
				}
				break;
			}
			CloseHandle(g_hMonitorHandle);
			g_hMonitorHandle=NULL;
		}
	}
}
