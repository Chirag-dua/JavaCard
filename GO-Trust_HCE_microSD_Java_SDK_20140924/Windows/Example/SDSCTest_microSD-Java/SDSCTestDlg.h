// SDSCTestDlg.h : header file
//

#if !defined(AFX_SDSCTESTDLG_H__653E1E40_CC48_4EBE_823C_D80A219295B9__INCLUDED_)
#define AFX_SDSCTESTDLG_H__653E1E40_CC48_4EBE_823C_D80A219295B9__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

/////////////////////////////////////////////////////////////////////////////
// CSDSCTestDlg dialog

class CSDSCTestDlg : public CDialog
{
// Construction
public:
	CSDSCTestDlg(CWnd* pParent = NULL);	// standard constructor
	DWORD RefreshDev();
	DWORD GetDriveName(TCHAR *pszDrive,DWORD *pulDriveLen);
		

// Dialog Data
	//{{AFX_DATA(CSDSCTestDlg)
	enum { IDD = IDD_SDSCTEST_DIALOG };
	CListBox	m_MsgList;
	CComboBox	m_DevList;
	//}}AFX_DATA

	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CSDSCTestDlg)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:
	HICON m_hIcon;

	// Generated message map functions
	//{{AFX_MSG(CSDSCTestDlg)
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	afx_msg void OnBtnTest();
	afx_msg void OnBtnRefresh();
	afx_msg void OnDestroy();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SDSCTESTDLG_H__653E1E40_CC48_4EBE_823C_D80A219295B9__INCLUDED_)
