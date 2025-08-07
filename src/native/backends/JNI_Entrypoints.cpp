#include <jni.h>

#include "classes_cache.hpp"
#include "logger_manager.hpp"

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
        JNIEnv *env = nullptr;
        vm->GetEnv((void**) &env, JNI_VERSION_1_6);

        printf("JNI_OnLoad called. (If you see this, please remove printing)\n");
        getClassesCache(env); // initialize classes cache

        return JNI_VERSION_1_6;
    }

    JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
        JNIEnv *env = nullptr;
        vm->GetEnv((void**) &env, JNI_VERSION_1_6);

        printf("JNI_OnUnload called. (If you see this, please remove printing)\n");

        getClassesCache(env)->release(env);

        getLoggerManager()->releaseAll(env);
    }
}