// SDSCTest.h : main header file for the SDSCTEST application
//

#if !defined(AFX_SDSCTEST_H__ACE8CDFD_B754_46E4_8A69_448D442C048B__INCLUDED_)
#define AFX_SDSCTEST_H__ACE8CDFD_B754_46E4_8A69_448D442C048B__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#ifndef __AFXWIN_H__
	#error include 'stdafx.h' before including this file for PCH
#endif

#include "resource.h"		// main symbols

/////////////////////////////////////////////////////////////////////////////
// CSDSCTestApp:
// See SDSCTest.cpp for the implementation of this class
//

class CSDSCTestApp : public CWinApp
{
public:
	CSDSCTestApp();

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CSDSCTestApp)
	public:
	virtual BOOL InitInstance();
	//}}AFX_VIRTUAL

// Implementation

	//{{AFX_MSG(CSDSCTestApp)
		// NOTE - the ClassWizard will add and remove member functions here.
		//    DO NOT EDIT what you see in these blocks of generated code !
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};


/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SDSCTEST_H__ACE8CDFD_B754_46E4_8A69_448D442C048B__INCLUDED_)
