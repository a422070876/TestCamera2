#include <jni.h>
#include <string>
#include <GLES3/gl3.h>
extern "C"
JNIEXPORT jstring

JNICALL
Java_com_example_openglesv3_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_openglesv3_GLPixelBuffer_glReadPixels(JNIEnv *env, jobject instance, jint x,
                                                       jint y, jint width, jint height, jint format,
                                                       jint type) {

    // TODO
    glReadPixels(x, y, width, height, (GLenum) format, (GLenum) type, 0);
}