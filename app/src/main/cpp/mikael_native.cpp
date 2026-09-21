#include <jni.h>
#include <string>
#include <sys/stat.h>

namespace {
constexpr jint OK = 0;
constexpr jint RUNTIME_NOT_INSTALLED = -100;
constexpr jint INVALID_ARGUMENT = -101;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mikael_emulator_nativebridge_NativeBridge_nativeInitialize(JNIEnv*, jobject) {
    // The native bridge is intentionally runtime-neutral until licensed Wine/Box binaries are provided.
    return OK;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mikael_emulator_nativebridge_NativeBridge_nativeLaunch(
        JNIEnv* env, jobject, jstring executable, jstring, jobjectArray) {
    if (executable == nullptr) return INVALID_ARGUMENT;
    const char* executableChars = env->GetStringUTFChars(executable, nullptr);
    const std::string executablePath = executableChars != nullptr ? executableChars : "";
    if (executableChars != nullptr) env->ReleaseStringUTFChars(executable, executableChars);
    if (executablePath.empty()) return INVALID_ARGUMENT;
    struct stat fileInfo{};
    if (stat(executablePath.c_str(), &fileInfo) != 0 || !S_ISREG(fileInfo.st_mode)) return INVALID_ARGUMENT;
    // A real Wine/Box runtime is intentionally required before spawning any process.
    return RUNTIME_NOT_INSTALLED;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mikael_emulator_nativebridge_NativeBridge_nativeStop(JNIEnv*, jobject) {
    return OK;
}
