//
// Created by joren on 1/22/16.
//

#ifndef PANAKOANDROID_EVENTOR_H
#define PANAKOANDROID_EVENTOR_H

#endif //PANAKOANDROID_EVENTOR_H

JNIEXPORT void JNICALL Java_be_panako_android_Tapper_startTapper(JNIEnv *env, jobject instance);

JNIEXPORT void JNICALL Java_be_panako_android_Tapper_setBeatList(JNIEnv *env, jobject instance, jdoubleArray beats_);

JNIEXPORT void JNICALL Java_be_panako_android_Tapper_stopTapper(JNIEnv *env, jobject instance);