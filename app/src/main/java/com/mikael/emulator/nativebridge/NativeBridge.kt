package com.mikael.emulator.nativebridge

object NativeBridge {
    val isLoaded: Boolean
    init {
        isLoaded = runCatching { System.loadLibrary("mikael_native") }.isSuccess
    }

    external fun nativeInitialize(): Int
    external fun nativeLaunch(executable: String, prefix: String, arguments: Array<String>): Int
    external fun nativeStop(): Int
}

enum class NativeResult(val code: Int, val message: String) {
    OK(0, "OK"),
    RUNTIME_NOT_INSTALLED(-100, "Wine/Box64/Box86 ainda não foram instalados"),
    INVALID_ARGUMENT(-101, "Argumento inválido"),
    NOT_INITIALIZED(-102, "Bridge nativa não inicializada")
}
