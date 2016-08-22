#include<stdio.h>
#include<stdlib.h>
#include<string.h>

#include<openssl/evp.h>

#include "SDSCInclude/SDSCDev.h"
#include "SDSCInclude/SDSCErr.h"

#define OK 0x9000

#define DEFAULT_SIZE 256
#define BLOCK_SIZE 0x80
#define RANDOM_LEN 0x08

unsigned char selectRAPDU[] = {0x00, 0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x01};
unsigned char selectCAPDU[] = {0x00, 0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x02};
unsigned char modulusAPDU[] = {0xA0, 0xB7, 0x00, 0x00, 0x80};
unsigned char verifyAPDU[] = {0xA0, 0xB6, 0x00, 0x00, 0x00};

/* Selects a device from a list of devices and establishes a connection.
   Returns the file descriptor for the device */
int selectAndConnectDevice(char *pszDrives, unsigned long pulDrivesLen, 
			   unsigned long pulDrivesNum, char *pszIoFileName) {
	int phDevice, i, count;
	char *p = pszDrives;
	/* List devices */
	printf("Following devices were detected\n");
	for(i = 0; i < pulDrivesNum; i++) {
		printf("%d: %s\n", i+1, p);
		p += strlen(p)+1;
	}
	/* Select device */
	printf("Select your device by entering the sno. of the device in above list: ");
	scanf("%d", &count);
	if(count < 0 || count > pulDrivesNum) {
		printf("Error: Invalid Input.\n");
		exit(1);
	}
	/* Connect to the selected device */
	p = pszDrives;
	count -= 1;
	while(count--)
		p += strlen(p) + 1;
	if(SDSCConnectDev(p, &phDevice, pszIoFileName)) {
		printf("Error: Cannot connect to the device\n");
		exit(1);
	}
	return phDevice;
}

/* Cleanup and exit the program if error has occured */
void cleanup(int phDevice) {
	unsigned long pulAtrLen = DEFAULT_SIZE;
	unsigned char pbAtr[pulAtrLen];

	printf("Cleaning Up\n");
	SDSCResetCard(phDevice, pbAtr, &pulAtrLen);
	SDSCDisconnectDev(phDevice);
	exit(1);
}

/* Retrieves the modulus from a file and build an APDU from it */
int getModulusAPDU(unsigned char APDU[]) {
	int i = 0, j = 0;
	char temp[300];
	APDU[i++] = 0xA0; APDU[i++] = 0xB2; APDU[i++] = 0x00;
	APDU[i++] = 0x00; APDU[i++] = 0x80;
	FILE *fp = fopen("keyapdu.txt", "r");
	fscanf(fp, "%s", temp);
	for(j = 0; temp[j] != '\0'; j += 2) {
		long num;		
		char s[3];
		s[0] = temp[j]; s[1] = temp[j+1];
		s[2] = '\0';
		num = strtol(s, NULL, 16);
		APDU[i++] = (unsigned char)(0x00000000000000FF & num);
	}
	fclose(fp);
	return i;
}

int getIdAPDU(unsigned char APDU[]) {
	int i = 0, j = 0, len;
	char temp[100];
	APDU[i++] = 0xA0; APDU[i++] = 0xB0; APDU[i++] = 0x00;
	APDU[i++] = 0x00;
	FILE *fp = fopen("se_id.txt", "r");
	fscanf(fp, "%s", temp);
	len = strlen(temp);
	APDU[i++] = (unsigned char)(len/2);
	for(j = 0; temp[j] != '\0'; j += 2) {
		long num;
		char s[3];
		s[0] = temp[j]; s[1] = temp[j+1];
		s[2] = '\0';
		num = strtol(s, NULL, 16);
		APDU[i++] = (unsigned char)(0x00000000000000FF & num);
	}
	fclose(fp);
	return i;
}

int createCertificate(unsigned char modulus[], unsigned char APDU[]) {
	int i = 0, j = 0;
	unsigned int siglen;	
	unsigned char temp[30];
	char s[100];

	EVP_PKEY *key;
	EVP_MD_CTX *ctx = (EVP_MD_CTX *)malloc(sizeof(EVP_MD_CTX));

	FILE *fp = fopen("se_id.txt", "r");
	fscanf(fp, "%s", s);
	for(j = 0; s[j] != '\0'; j += 2) {
		long num;
		char t[3];
		t[0] = s[j]; t[1] = s[j+1];
		t[2] = '\0';
		num = strtol(t, NULL, 16);
		temp[i++] = (unsigned char)(0x00000000000000FF & num);
	}
	fclose(fp);

	/* Retrieve the private key of the server from a PEM file */
	fp = fopen("private_key.pem", "r");
	key = PEM_read_PrivateKey(fp, NULL, NULL, NULL);
	fclose(fp);
	
	/* Sign the data using RSA_SHA1_PKCS1 algorithm */
	EVP_SignInit(ctx, EVP_sha1());
	j = EVP_SignUpdate(ctx, modulus, 128);
	j = EVP_SignUpdate(ctx, temp, i);
	j = EVP_SignFinal(ctx, APDU+5, &siglen, key);
	APDU[0] = 0xA0; APDU[1] = 0xB4; APDU[2] = 0x00;
	APDU[3] = 0x00; APDU[4] = (unsigned char)siglen;
	return (int)siglen+5;
}

int main(int argc, char **argv) {

	/* The list of the devices present on the machine */
	char pszDrives[SDSC_MAX_DEV_NUM * SDSC_MAX_DEV_NAME_LEN];
	/* The length of the pszDrives list */
	unsigned long pulDrivesLen = (unsigned long)sizeof(pszDrives);

	/* The number of devices */
	unsigned long pulDrivesNum;

	/* The I/O file name */
	char pszIoFileName[300];

	/* The file descriptor of the device */
	int phDevice, i, j, len;

	/* The length of the ATR APDU */
	unsigned long pulAtrLen = DEFAULT_SIZE;
	/* The buffer for the ATR APDU */
	unsigned char pbAtr[pulAtrLen];
	
	/* The length of the APDU response buffer */
	unsigned long pbOutDataLen = DEFAULT_SIZE;
	/* The buffer for the APDU response */
	unsigned char pbOutData[pbOutDataLen];

	/* The APDU status word */
	unsigned long pulCosState;

	/* Buffer used for temporary storage */
	unsigned char APDU[DEFAULT_SIZE];

	/* Size of temporary storage */
	unsigned long APDULen;

	if(SDSCListDevs(pszDrives, &pulDrivesLen, &pulDrivesNum, "SMART_IO.CRD")) {
		printf("Error: Cannot retrieve list of devices\n");		
		exit(1);
	}
	if(pulDrivesNum == 1) {
		/* There is only one device, connect to it */
		if(SDSCConnectDev(pszDrives, &phDevice, "SMART_IO.CRD"))
			cleanup(phDevice);
	}
	else 
		phDevice = selectAndConnectDevice(pszDrives, pulDrivesLen, pulDrivesNum, "SMART_IO.CRD");

	/* Reset the device */
	printf("Resetting the card\n");
	if(SDSCResetCard(phDevice, pbAtr, &pulAtrLen)) {
		printf("Error, card cannot be resetted\n");
		cleanup(phDevice);
	}
	printf("ATR is: ");
	for(i = 0; i < pulAtrLen; i++)
		printf("%02x ", pbAtr[i]);
	putchar('\n');
	
	/* Select the applet */
	if(strcmp(argv[1], "reader") == 0) {
		if(SDSCTransmit(phDevice, selectRAPDU, sizeof(selectRAPDU), SDSC_DEV_DEFAULT_TIME_OUT, pbOutData, &pbOutDataLen, &pulCosState))
			cleanup(phDevice);
		if(pulCosState != OK) {
			printf("Error: applet not present on the card %x\n", pulCosState);
			cleanup(phDevice);
		}
	}
	else if(strcmp(argv[1], "card") == 0) {
		if(SDSCTransmit(phDevice, selectCAPDU, sizeof(selectCAPDU), SDSC_DEV_DEFAULT_TIME_OUT, pbOutData, &pbOutDataLen, &pulCosState))
			cleanup(phDevice);
		if(pulCosState != OK) {
			printf("Error: applet not present on the card %x\n", pulCosState);
			cleanup(phDevice);
		}
	}
	else {
		printf("Invalid command\n");
		exit(0);
	}
	
	/* Set the public key of the server */
	APDULen = (unsigned long)getModulusAPDU(APDU);
	pbOutDataLen = DEFAULT_SIZE;
	if(SDSCTransmit(phDevice, APDU, APDULen, SDSC_DEV_DEFAULT_TIME_OUT, pbOutData, &pbOutDataLen, &pulCosState))
		cleanup(phDevice);
	if(pulCosState != OK) {
		printf("Error: cannot store the public key of the server %x\n", pulCosState);
		cleanup(phDevice);
	}

	/* Set the ID of the secure device */
	APDULen = (unsigned long)getIdAPDU(APDU);
	/* for(i = 0; i < j; i++)
		printf("%02x ", APDU[i]);
	putchar('\n'); */
	pbOutDataLen = DEFAULT_SIZE;
	if(SDSCTransmit(phDevice, APDU, APDULen, SDSC_DEV_DEFAULT_TIME_OUT, pbOutData, &pbOutDataLen, &pulCosState))
		cleanup(phDevice);
	if(pulCosState != OK) {
		printf("Error: cannot store the ID of the secure element %x\n", pulCosState);
		cleanup(phDevice);
	}

	/* Get modulus of the public key of secure element */
	pbOutDataLen = DEFAULT_SIZE;
	if(SDSCTransmit(phDevice, modulusAPDU, sizeof(modulusAPDU), SDSC_DEV_DEFAULT_TIME_OUT, pbOutData, &pbOutDataLen, &pulCosState))
		cleanup(phDevice);
	if(pulCosState != OK) {
		printf("Error: cannot obtain the modulus of the public key of secure element %x\n", pulCosState);
		cleanup(phDevice);
	}
	for(i = 0; i < pbOutDataLen; i++)
		printf("%02x ", pbOutData[i]);
	putchar('\n');

	/* Create a certificate for the secure element */
	APDULen = (unsigned long)createCertificate(pbOutData, APDU);
	pbOutDataLen = DEFAULT_SIZE;
	if(SDSCTransmit(phDevice, APDU, APDULen, SDSC_DEV_DEFAULT_TIME_OUT, pbOutData, &pbOutDataLen, &pulCosState))
		cleanup(phDevice);
	if(pulCosState != OK) {
		printf("Error: cannot store the signature of the secure element %x\n", pulCosState);
		cleanup(phDevice);
	}
	pbOutDataLen = DEFAULT_SIZE;
	if(SDSCTransmit(phDevice, verifyAPDU, sizeof(verifyAPDU), SDSC_DEV_DEFAULT_TIME_OUT, pbOutData, &pbOutDataLen, &pulCosState))
		cleanup(phDevice);
	printf("%s\n", pulCosState == 0x6984 ? "INCORRECT" : "CORRECT");
	if(pulCosState != OK) {
		printf("Error: cannot verify the signature of the secure element %x\n", pulCosState);
		cleanup(phDevice);
	}
	SDSCDisconnectDev(phDevice);
	return 0;
}
