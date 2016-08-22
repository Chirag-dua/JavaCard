TARGET = SDSCTest

UNAME_P := $(shell uname -p)
ifeq ($(UNAME_P), x86_64)
	LIBS := ../../x64/libSESDAPI.a
else
	LIBS := ../../x86/libSESDAPI.a
endif

LIBS += -lrt

all: ${TARGET}

clean:
	rm -f *.o ${TARGET}

SDSCTest: SDSCTest.c
	gcc -o SDSCTest SDSCTest.c $(LIBS)

