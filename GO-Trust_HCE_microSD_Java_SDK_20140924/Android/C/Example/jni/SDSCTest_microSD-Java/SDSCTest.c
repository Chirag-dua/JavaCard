#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../../../SDSCInclude/SDSCDev.h"
#include "../../../SDSCInclude/SDSCErr.h"

/**
 * USE_FIXED_DEVICE_PATH.
 *  0: Let SDSCListDevs() search devices automatically
 *  1: Use SDSCInitParams() to assign an fixed device path.
 */
#define USE_FIXED_DEVICE_PATH	0

int main()
{
	int  hDevice = -1;
	char *p=NULL;
	int count,i;

	// SDSC IO Type
	unsigned long ulSCIOType;
	
	// init params
	char *pszParams = NULL;
    char szIoFileName[20] = {0};
	
	// list dev
	char *pszDrive=NULL;
	unsigned long ulDrivesLen,ulDriveNum=0;

	// firmware ver
	unsigned char bFirmwareVer[SDSC_FIRMWARE_VER_LEN];
	unsigned long ulFirmwareVerLen;

	// device info
	unsigned char bDeviceInfo[SDSC_DEVICE_INFO_LEN];
	unsigned long ulDeviceInfoLen;

	// SDK ver
	char szSDKVersion[SDSC_MAX_VERSION_LEN];
	unsigned long ulVersionLen;

	// command
	unsigned char bRetBuf[512],bCommand[256];
	unsigned long ulRet,ulCommandLen,ulRetBufLen,ulCosState;

    //init SDK
    printf("Please input Io file name: ");
    scanf("%s", szIoFileName);

	// get SDK ver.
	ulVersionLen = sizeof(szSDKVersion);
	if(ulRet=SDSCGetSDKVersion(szSDKVersion, &ulVersionLen))
	{
		printf("SDSCGetSDKVersion failure! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
	printf("SDK Ver: %s\n", szSDKVersion);

	// get all dev
	pszDrive = (char*)malloc(SDSC_MAX_DEV_NUM*SDSC_MAX_DEV_NAME_LEN);
	if(pszDrive == NULL)
	{
		printf("malloc error.\n");
		goto err;
	}

#if USE_FIXED_DEVICE_PATH
	// set params, for example: /dev/block/mmcblk0p1,/sdcard
	pszParams = (char*)malloc(strlen("/dev/block/mmcblk0p1,/sdcard")+2);// "+2" for multi-string
	if(pszParams == NULL)
	{
		printf("malloc error.\n");
		goto err;
	}
	memset(pszParams, 0x00, strlen("/dev/block/mmcblk0p1,/sdcard")+2);
	strcpy(pszParams, "/dev/block/mmcblk0p1,/sdcard");
	if(ulRet=SDSCInitParams(SDSC_DEVICE_PATH_NAME, pszParams))
	{
		printf("SDSCInitParams failure! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
#endif

	// list dev
	ulDrivesLen = SDSC_MAX_DEV_NUM*SDSC_MAX_DEV_NAME_LEN;
	memset(pszDrive, 0x00, ulDrivesLen);
	if(ulRet=SDSCListDevs(pszDrive, &ulDrivesLen, &ulDriveNum, szIoFileName))
	{
		printf("SDSCListDevs failure! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
	if(ulDriveNum == 0)
	{
		printf("There are 0 SD Card.\n");
		goto err;
	}
	printf("There are %ld SD Card:\n", ulDriveNum);
	p = pszDrive;
	for(i=0; i<ulDriveNum; i++)
	{
		printf("%d: %s\n", i+1, p);
		p += strlen(p)+1;
	}
	printf("Select Dev:\n");
	scanf("%d",&count);
	if(count<0 || count>ulDriveNum)
	{
		printf("input error, exit.\n");
		goto err;
	}
	p = pszDrive;
	count -= 1;
	while(count --)
		p += strlen(p)+1;

	// connect dev
	if(ulRet=SDSCConnectDev(pszDrive, &hDevice))
	{
		printf("SDSCConnectDev failure! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
	printf("SDSCConnectDev ok.\n");
	
	ulRet = SDSCGetSCIOType(hDevice, &ulSCIOType);
	if(ulRet != 0)
	{
		printf("SDSCGetSCIOType error! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
	if(ulSCIOType == SDSC_SCIO_7816)
		printf("SDSC_SCIO_7816.\n");
	else if(ulSCIOType == SDSC_SCIO_SPI_V1)
		printf("SDSC_SCIO_SPI_V1.\n");
	else if(ulSCIOType == SDSC_SCIO_SPI_V2)
		printf("SDSC_SCIO_SPI_V2.\n");
	else if(ulSCIOType == SDSC_SCIO_SPI_V3)
		printf("SDSC_SCIO_SPI_V3.\n");
	else
		printf("SDSC_SCIO_UNKNOWN.\n");
	
	// get Firmware Ver 
	ulFirmwareVerLen=sizeof(bFirmwareVer);
	if(ulRet=SDSCGetFirmwareVer(hDevice,bFirmwareVer,&ulFirmwareVerLen))
	{
		printf("SDSCGetFirmwareVer failure! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
	printf ("The firmware version is : ");
	for(i=0; i<ulFirmwareVerLen; i++)
		printf("%02x ",bFirmwareVer[i]);
	printf("\n");

	// get device info
	ulDeviceInfoLen=sizeof(bDeviceInfo);
	if(ulRet=SDSCGetDeviceInfo(hDevice,bDeviceInfo,&ulDeviceInfoLen))
	{
		printf("SDSCGetDeviceInfo failure! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
	printf ("The device info is : ");
	for(i=0; i<ulDeviceInfoLen; i++)
		printf("%02x ",bDeviceInfo[i]);
	printf("\n");

	// Send APDU (Select Card Manager)
	ulCommandLen=13;
	memcpy(bCommand,"\x00\xA4\x04\x00\x08\xA0\x00\x00\x00\x03\x00\x00\x00",ulCommandLen);
	
	printf("Send APDU: ");
	for(i = 0; i < ulCommandLen; ++i)
		printf("%02X ", bCommand[i]);
	printf("\n");
	
	ulRetBufLen=sizeof(bRetBuf);
	if(ulRet=SDSCTransmit(hDevice,bCommand,ulCommandLen,SDSC_DEV_DEFAULT_TIME_OUT,bRetBuf,&ulRetBufLen,&ulCosState))
	{
		printf("SDSCTransmit failure! Error code= 0x%08x.\n",ulRet);
		goto err;
	}
	printf("Get APDU Response Status: %x\n",ulCosState);
	printf("Get APDU Response data: %d bytes\n", ulRetBufLen);
	for(i = 0; i < ulRetBufLen; ++i)
	{
		printf("%02X ", bRetBuf[i]);
	}
	printf("\n");

	SDSCDisconnectDev(hDevice);
	
#if USE_FIXED_DEVICE_PATH
	SDSCDestroyParams();
#endif

	free(pszDrive);
	free(pszParams);

	return 0;
err:
	if(hDevice>=0)
	{
		SDSCDisconnectDev(hDevice);
	}
	SDSCDestroyParams();

	if(pszDrive)
		free(pszDrive);
	if(pszParams)
		free(pszParams);

	return 1;
}
