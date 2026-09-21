package com.mikael.emulator.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.opengl.GLES20
import android.os.Build
import android.view.WindowManager
import java.util.Locale

fun interface DiagnosticReader { fun read(): DeviceDiagnostics }

data class DeviceDiagnostics(
    val cpuAbi: String,
    val cores: Int,
    val ram: String,
    val android: String,
    val vulkan: String,
    val openGl: String,
    val gpu: String,
    val resolution: String,
    val wine: String = "Ausente — componente externo necessário",
    val box64: String = "Ausente — componente externo necessário",
    val box86: String = "Ausente — componente externo necessário",
    val dxvk: String = "Ausente — componente externo necessário",
    val vkd3d: String = "Ausente — componente externo necessário",
    val audio: String = "Pronto para integração Android",
    val input: String = "Pronto para integração Android"
)

class AndroidDeviceDiagnostics(private val context: Context) : DiagnosticReader {
    override fun read(): DeviceDiagnostics {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val ramGb = memory.totalMem / (1024.0 * 1024.0 * 1024.0)
        val glInfo = activityManager.deviceConfigurationInfo
        val glVersion = if (glInfo.reqGlEsVersion >= 0x30000) "OpenGL ES 3+" else "OpenGL ES 2"
        val vulkan = if (Build.VERSION.SDK_INT >= 24 && (
                context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) ||
                    context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
            )) "Disponível" else "Não detectado"
        val metrics = context.resources.displayMetrics
        val resolution = "${metrics.widthPixels} × ${metrics.heightPixels}"
        return DeviceDiagnostics(
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Desconhecida",
            cores = Runtime.getRuntime().availableProcessors(),
            ram = String.format(Locale.US, "%.1f GB", ramGb),
            android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            vulkan = vulkan,
            openGl = glVersion,
            gpu = "Não disponível sem contexto GL",
            resolution = resolution
        )
    }
}
