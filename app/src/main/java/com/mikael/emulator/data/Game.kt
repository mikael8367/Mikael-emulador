package com.mikael.emulator.data

import java.util.UUID

data class Game(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val executableUri: String,
    val extension: String,
    val container: String = "Padrão",
    val lastPlayed: String = "Nunca iniciado",
    val runtime: String = "Ainda não implementado",
    val fileSize: String = "Tamanho desconhecido"
)

data class ContainerProfile(
    val name: String,
    val wineVersion: String = "Não instalado",
    val box64Version: String = "Não instalado",
    val box86Version: String = "Não instalado",
    val dxvkVersion: String = "Não instalado",
    val vkd3dVersion: String = "Não instalado",
    val resolution: String = "1280x720",
    val environment: Map<String, String> = emptyMap()
)
