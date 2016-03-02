//
// Created by joren on 1/21/16.
//

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include "eventor.h"
#include <unistd.h>
#include <pthread.h>
#include <sys/time.h>
#include <errno.h>
#include <signal.h>
#ifdef __MACH__
    #include <mach/mach_time.h>
#endif

//typedef long long int  int64_t;

static int running = 0;
static double* beatsArray = NULL;
static int beatsLength = 0;
static jobject  obj;//global ref
static JavaVM* jvm = 0;
static pthread_t tapper_thread;

#ifdef __MACH__
static uint64_t start = 0;
static mach_timebase_info_data_t    sTimebaseInfo;
#endif

int64_t getTimeNsec() {
    #ifdef __MACH__
    if(start==0){
        start=mach_absolute_time();
    }
       // If this is the first time we've run, get the timebase.
    // We can use denom == 0 to indicate that sTimebaseInfo is 
    // uninitialised because it makes no sense to have a zero 
    // denominator is a fraction.

    if ( sTimebaseInfo.denom == 0 ) {
        (void) mach_timebase_info(&sTimebaseInfo);
    }


    //https://developer.apple.com/library/mac/qa/qa1398/_index.html
    uint64_t end = mach_absolute_time();
    uint64_t elapsed = end - start;
    // Calculate the duration.
    return elapsed * sTimebaseInfo.numer / sTimebaseInfo.denom;
    #else
        struct timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);
        return (int64_t) now.tv_sec*1000000000LL + now.tv_nsec;
    #endif
}



void *sendTaps(){
    int64_t referenceNs = getTimeNsec();
    JNIEnv* env;
    (*jvm)->AttachCurrentThread(jvm, &env, NULL);

    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid = (*env)->GetMethodID(env, cls, "tap", "()V");

    //__android_log_print(ANDROID_LOG_VERBOSE, "Tapper", "Started pthread, will tap %d beats", beatsLength);
    

    int index = 1;
    while(beatsLength!= 0 && running == 1 && index < beatsLength){
        
        int64_t nsTapEvent = 0;
        int64_t microsecondsToTapEvent = beatsArray[index] * 1000000;
        nsTapEvent = microsecondsToTapEvent * 1000 + referenceNs;
        int64_t currentNs = getTimeNsec();
        int64_t nsToSleep = nsTapEvent - currentNs;

        int secondsToSleep = 0;
        if(nsToSleep >= 1000000000){
            secondsToSleep = nsToSleep / 1000000000;
            nsToSleep = nsToSleep - secondsToSleep * 1000000000;
        }
        //printf("Sleep %d seconds, %d ns\n",secondsToSleep,nsToSleep);
        int val = nanosleep((const struct timespec[]){{secondsToSleep, nsToSleep}}, NULL);
        if(EINTR == val){
            //interrupted
            break;
        }else{
            (*env)->CallVoidMethod(env, obj, mid);
            //__android_log_print(ANDROID_LOG_VERBOSE, "Tapper", "Slept for for  %d seconds and %d nanoseconds", secondsToSleep, (int) nsToSleep);
            //__android_log_print(ANDROID_LOG_VERBOSE, "Tapper", "Called tap method!");
            index++;
        }
        
    }
    (*jvm)->DetachCurrentThread(jvm);
}

JNIEXPORT void JNICALL Java_be_panako_android_Tapper_startTapper(JNIEnv *env, jobject obje) {
    (*env)->GetJavaVM(env, &jvm);
    running = 1;

    obj = (*env)->NewGlobalRef(env, obje);
    printf("Start native tapping \n");
    pthread_create(&tapper_thread, NULL, sendTaps,env);
    int prevType;
    pthread_setcanceltype(PTHREAD_CANCEL_ASYNCHRONOUS, &prevType);
}



JNIEXPORT void JNICALL Java_be_panako_android_Tapper_setBeatList(JNIEnv *env, jobject instance, jdoubleArray beats_) {
    printf("Set beat list native\n");
    jdouble *beats = (*env)->GetDoubleArrayElements(env, beats_, NULL);
    jsize len = (*env)->GetArrayLength(env,beats_);
    beatsLength = (int) len;
    beatsArray = malloc(len * sizeof *beatsArray);
    int i;
    for(i = 0 ; i < beatsLength ; i++){
        beatsArray[i]=(double) beats[i];
    }
    (*env)->ReleaseDoubleArrayElements(env, beats_, beats, 0);
}

JNIEXPORT void JNICALL Java_be_panako_android_Tapper_stopTapper(JNIEnv *env, jobject instance){
    pthread_cancel(tapper_thread);

    pthread_join(tapper_thread, NULL);

    running=0;
    free((void*) beatsArray);
    beatsArray=NULL;
    beatsLength=0;
}