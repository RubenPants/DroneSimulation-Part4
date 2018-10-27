/**
 * Vision algorithms.
 *
 * @author Team Safier
 * @version 1.0
 */

void setupVision();
void cleanupVision();
JNIEXPORT jobjectArray JNICALL locateUnitCubes(JNIEnv *, jclass, jbyteArray, jint, jint, jboolean, jboolean);