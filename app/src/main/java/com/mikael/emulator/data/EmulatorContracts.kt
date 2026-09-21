package com.mikael.emulator.data

interface EmulatorEngine {
    val id: String
    fun isAvailable(): Boolean
    fun launch(executable: String, prefix: String, arguments: List<String>): Int
    fun stop(): Int
}

interface GraphicsBackend {
    val name: String
    fun isAvailable(): Boolean
    fun supportedDirectX(): Set<Int>
}

interface InputBackend {
    fun devices(): List<String>
    fun sendKey(key: String): Boolean
}

interface AudioBackend {
    val name: String
    fun setVolume(value: Float)
    fun setMuted(muted: Boolean)
}

interface ContainerManager {
    fun list(): List<ContainerProfile>
    fun create(profile: ContainerProfile): Boolean
    fun delete(name: String): Boolean
}

interface RuntimeManager {
    fun installedVersions(): Map<String, String>
    fun availableVersions(): Map<String, List<String>>
    fun install(component: String, version: String): Result<Unit>
}
