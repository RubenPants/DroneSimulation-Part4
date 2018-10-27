/**
 * C implementation of autopilot_library (JNI).
 *  This class just forwards calls to the relevant classes.
 *
 * @author Team Safier
 * @version 1.0
 */

#include "autopilot_library.h"

#include "vision/vision.h"
#include "scheduling/scheduler.h"

#pragma mark General

JNIEXPORT void JNICALL Java_autopilot_1library_AutopilotLibrary_setup
(JNIEnv *env, jclass class) {
    setupPlanning();
    setupVision();
}

JNIEXPORT void JNICALL Java_autopilot_1library_AutopilotLibrary_cleanup
(JNIEnv *env, jclass class) {
    cleanupPlanning();
    cleanupVision();
}

#pragma mark Planning

JNIEXPORT void JNICALL Java_autopilot_1library_AutopilotLibrary_defineAirportParams
(JNIEnv *env, jclass class, jfloat length, jfloat width) {
    return defineAirportParameters(length, width);
}

JNIEXPORT void JNICALL Java_autopilot_1library_AutopilotLibrary_defineAirport
(JNIEnv *env, jclass class, jfloat centerX, jfloat centerZ, jfloat centerToRunway0X, jfloat centerToRunway0Z) {
    return defineAirport(centerX, centerZ, centerToRunway0X, centerToRunway0Z);
}

JNIEXPORT jobjectArray JNICALL Java_autopilot_1library_AutopilotLibrary_scheduleDrones
(JNIEnv *env, jclass class, jfloatArray data) {
    // Pass on to planning
    return NULL;
}

#pragma mark Vision

JNIEXPORT jobjectArray JNICALL Java_autopilot_1library_AutopilotLibrary_locateUnitCubes
(JNIEnv *env, jclass class, jbyteArray bytes, jint width, jint height, jboolean useHistory, jboolean hasGround) {
    return locateUnitCubes(env, class, bytes, width, height, useHistory, hasGround);
}