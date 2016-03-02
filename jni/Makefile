all : eventor.mac

eventor.linux : eventor.c eventor.h 
	gcc -O3 -shared -fPIC -I"/opt/jdk1.8.0_45/include/" -I"/opt/jdk1.8.0_45/include/linux/"  eventor.c -o libeventor.so 

eventor.mac : eventor.c eventor.h 
	gcc -O3 -shared -fPIC -I"/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/include/" -I"/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/include/darwin/"  eventor.c -o libeventor.jnilib
