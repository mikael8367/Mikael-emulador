package com.mikael.emulator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mikael.emulator.data.Game
import com.mikael.emulator.diagnostics.AndroidDeviceDiagnostics
import com.mikael.emulator.diagnostics.DeviceDiagnostics
import com.mikael.emulator.nativebridge.NativeBridge

private val Ink = Color(0xFF08070E)
private val Panel = Color(0xFF12101B)
private val PanelSoft = Color(0xFF191626)
private val Violet = Color(0xFF8B6CFF)
private val VioletDark = Color(0xFF4B2FA5)
private val Mint = Color(0xFF65E6B5)
private val Amber = Color(0xFFFFC857)
private val Muted = Color(0xFFA9A2B9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (NativeBridge.isLoaded) NativeBridge.nativeInitialize()
        setContent { MikaelTheme { MikaelApp() } }
    }
}

@Composable
private fun MikaelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Violet,
            secondary = Mint,
            background = Ink,
            surface = Panel,
            surfaceVariant = PanelSoft,
            onSurface = Color(0xFFF6F2FF)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MikaelApp() {
    var tab by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val games = remember { mutableStateListOf<Game>().apply { addAll(loadGames(context)) } }
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    var pendingGame by remember { mutableStateOf<Game?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var playMessage by remember { mutableStateOf<String?>(null) }
    var launchingGame by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val diagnostics = remember { AndroidDeviceDiagnostics(context).read() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = getDisplayName(context, uri)
        val extension = name.substringAfterLast('.', "arquivo").uppercase()
        if (extension !in setOf("EXE", "MSI")) {
            importMessage = "Escolha um arquivo .exe ou .msi do seu próprio jogo."
            return@rememberLauncherForActivityResult
        }
        val persisted = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        }.getOrDefault(false)
        if (!persisted) {
            importMessage = "Não foi possível manter acesso ao arquivo. Escolha-o novamente em um provedor compatível."
            return@rememberLauncherForActivityResult
        }
        pendingGame = Game(name = name.substringBeforeLast('.'), executableUri = uri.toString(), extension = extension, fileSize = formatFileSize(getFileSize(context, uri)))
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            Surface(
                color = Color(0xFF100E18),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 12.dp
            ) {
                NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                    listOf("Início", "Meus Jogos", "Diagnóstico", "Componentes", "Config").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(listOf("⌂", "🎮", "✓", "⚙", "☷")[index], style = MaterialTheme.typography.titleMedium) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet,
                                selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .14f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> DashboardScreen(games.size, diagnostics, { picker.launch(arrayOf("application/octet-stream", "application/x-msdownload")) }, Modifier.padding(padding))
            1 -> LibraryScreen(games, selectedGame, { selectedGame = it }, { picker.launch(arrayOf("application/octet-stream", "application/x-msdownload")) }, importMessage, playMessage, diagnostics, { game ->
                if (launchingGame == null) {
                    launchingGame = game.name
                    playMessage = "${game.name}: preparando o arquivo e verificando o runtime..."
                    Thread {
                        val result = launchGame(context, game)
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            playMessage = result.message
                            if (!result.success) errorMessage = result.message
                            launchingGame = null
                        }
                    }.start()
                }
            }, Modifier.padding(padding))
            2 -> DiagnosticsScreen(diagnostics, Modifier.padding(padding))
            3 -> ComponentsScreen(Modifier.padding(padding))
            else -> SettingsScreen(context, diagnostics, Modifier.padding(padding))
        }
    }

    pendingGame?.let { game ->
        GameConfirmationDialog(
            game = game,
            onConfirm = {
                if (games.any { it.executableUri == game.executableUri }) {
                    importMessage = "Esse arquivo já está na biblioteca."
                    pendingGame = null
                    return@GameConfirmationDialog
                }
                games.add(game)
                saveGames(context, games)
                pendingGame = null
                importMessage = "${game.name}.${game.extension.lowercase()} adicionado à biblioteca."
            },
            onDismiss = { pendingGame = null }
        )
    }
    errorMessage?.let { message ->
        ErrorDialog(context, message, onDismiss = { errorMessage = null })
    }
}

@Composable
private fun SettingsScreen(context: android.content.Context, diagnostics: DeviceDiagnostics, modifier: Modifier) {
    val preferences = remember { context.getSharedPreferences("mikael_settings", android.content.Context.MODE_PRIVATE) }
    val activityManager = remember { context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager }
    val memoryInfo = remember { android.app.ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) } }
    val safeMaxRam = ((memoryInfo.totalMem / (1024L * 1024L)) * 0.75).toInt().coerceIn(1024, 4096).let { it - (it % 256) }
    var resolution by remember { mutableStateOf(preferences.getString("resolution", "1280x720") ?: "1280x720") }
    var ramMb by remember { mutableIntStateOf(preferences.getInt("ram_mb", 1536).coerceIn(512, safeMaxRam)) }
    var saved by remember { mutableStateOf(false) }
    val resolutions = listOf("1280x720", "1600x900", "1920x1080")

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)) {
        item { Header(); Text("Configurações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Preferências do container e do runtime", color = Muted) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Resolução", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Escolha a resolução inicial dos jogos compatíveis.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { resolutions.forEach { option -> FilterChip(selected = resolution == option, onClick = { resolution = option; saved = false }, label = { Text(option) }) } }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Memória do container", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("${ramMb} MB", color = Violet, fontWeight = FontWeight.Bold) }
                    Text("RAM total detectada: ${diagnostics.ram} • limite seguro: ${safeMaxRam} MB", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Slider(value = ramMb.toFloat(), onValueChange = { ramMb = (it.toInt() / 256) * 256 }, valueRange = 512f..safeMaxRam.toFloat(), steps = ((safeMaxRam - 512) / 256 - 1).coerceAtLeast(0))
                    Text("O limite evita configurações que podem encerrar o app por falta de memória. A escolha não altera a RAM do Android.", color = Amber, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Button(onClick = { preferences.edit().putString("resolution", resolution).putInt("ram_mb", ramMb).apply(); saved = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Violet), shape = RoundedCornerShape(13.dp)) { Text(if (saved) "✓  Configurações salvas" else "Salvar configurações", fontWeight = FontWeight.Bold) }
        }
        item { Text("Essas preferências ficam prontas para o Container Manager. A execução Windows real ainda depende da instalação licenciada de Wine, Box64/Box86 e dos backends gráficos.", color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DashboardScreen(gameCount: Int, diagnostics: DeviceDiagnostics, onImport: () -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)) {
        item { Header() }
        item { HeroCard(onImport) }
        item { QuickStats(gameCount, diagnostics) }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acesso rápido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Seus jogos importados ficam organizados na aba Meus Jogos. Toque em um título para configurar ou iniciar o fluxo de execução.", color = Muted)
                    Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = Violet), shape = RoundedCornerShape(12.dp)) { Text("＋  Adicionar jogo", fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { LegalNotice() }
    }
}

@Composable
private fun GameConfirmationDialog(game: Game, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Panel, shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, Violet.copy(alpha = .35f))) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Confirmar jogo", color = Violet, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(VioletDark, Color(0xFF15112A), Color(0xFF102A37)))), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(game.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                        Text("CAPA DO JOGO", color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Text(game.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Arquivo", color = Muted, style = MaterialTheme.typography.labelSmall); Text(".${game.extension.lowercase()}", fontWeight = FontWeight.SemiBold) }
                    Column(horizontalAlignment = Alignment.End) { Text("Tamanho", color = Muted, style = MaterialTheme.typography.labelSmall); Text(game.fileSize, fontWeight = FontWeight.SemiBold) }
                }
                Text("O jogo será apenas adicionado à biblioteca. O Mikael não baixa jogos, arquivos adicionais ou conteúdo protegido automaticamente.", color = Amber, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar", color = Muted) }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Violet), shape = RoundedCornerShape(11.dp)) { Text("Adicionar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ErrorDialog(context: android.content.Context, message: String, onDismiss: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    val component = when {
        message.contains("runtime", ignoreCase = true) -> "Wine / Box64 / Box86"
        message.contains("bridge", ignoreCase = true) -> "Bridge JNI / C++"
        message.contains("permissão", ignoreCase = true) -> "Armazenamento Android"
        else -> "Process Manager"
    }
    val solution = when {
        message.contains("runtime", ignoreCase = true) -> "Instale um runtime Windows compatível e tente novamente. O app não baixa binários automaticamente."
        message.contains("permissão", ignoreCase = true) -> "Reimporte o arquivo usando um provedor de arquivos que permita acesso persistente."
        message.contains("bridge", ignoreCase = true) -> "Verifique se a biblioteca nativa arm64 foi compilada e incluída no APK."
        else -> "Verifique o arquivo selecionado e consulte os logs do container."
    }
    val report = "Mikael Emulator\nComponente: $component\nErro: $message\nSolução sugerida: $solution"

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Panel, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Amber.copy(alpha = .45f))) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Não foi possível iniciar", color = Amber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Componente responsável", color = Muted, style = MaterialTheme.typography.labelSmall)
                Text(component, fontWeight = FontWeight.SemiBold)
                Text("Erro", color = Muted, style = MaterialTheme.typography.labelSmall)
                Surface(color = Color.Black.copy(alpha = .22f), shape = RoundedCornerShape(12.dp)) { Text(message, Modifier.padding(12.dp), color = Color(0xFFFFD9D0), style = MaterialTheme.typography.bodySmall) }
                Text("Solução sugerida", color = Muted, style = MaterialTheme.typography.labelSmall)
                Text(solution, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { copyError(context, report); copied = true }, modifier = Modifier.weight(1f)) { Text(if (copied) "✓ Copiado" else "Copiar erro", color = Violet) }
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Violet), shape = RoundedCornerShape(11.dp)) { Text("Fechar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(games: List<Game>, selectedGame: Game?, onSelect: (Game) -> Unit, onImport: () -> Unit, importMessage: String?, playMessage: String?, diagnostics: DeviceDiagnostics, onPlay: (Game) -> Unit, modifier: Modifier) {
    var search by remember { mutableStateOf("") }
    val filteredGames = games.filter { it.name.contains(search, ignoreCase = true) }
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(.9f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Header()
                    HeroCard(onImport)
                    QuickStats(games.size, diagnostics)
                    LegalNotice()
                }
                LazyColumn(Modifier.weight(1.35f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 18.dp)) {
                    item { LibrarySearch(search, { search = it }) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text("Minha biblioteca", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Seus executáveis, seus containers", color = Muted, style = MaterialTheme.typography.bodySmall) }
                            Text("${filteredGames.size} jogos", color = Violet, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    importMessage?.let { message -> item { StatusBanner(message, message.contains("adicionado")) } }
                    playMessage?.let { message -> item { StatusBanner(message, false) } }
                    if (games.isEmpty()) item { EmptyLibrary(onImport) }
                    else if (filteredGames.isEmpty()) item { NoSearchResults(search) }
                    items(filteredGames, key = { it.id }) { game -> GameCard(game, onSelect, selectedGame?.id == game.id, onPlay) }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Header() }
                item { HeroCard(onImport) }
                item { QuickStats(games.size, diagnostics) }
                item { LibrarySearch(search, { search = it }) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Minha biblioteca", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Seus executáveis, seus containers", color = Muted, style = MaterialTheme.typography.bodySmall) }
                        Text("${filteredGames.size} jogos", color = Violet, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                importMessage?.let { message -> item { StatusBanner(message, message.contains("adicionado")) } }
                playMessage?.let { message -> item { StatusBanner(message, false) } }
                if (games.isEmpty()) item { EmptyLibrary(onImport) }
                else if (filteredGames.isEmpty()) item { NoSearchResults(search) }
                items(filteredGames, key = { it.id }) { game -> GameCard(game, onSelect, selectedGame?.id == game.id, onPlay) }
                item { LegalNotice() }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("MIKAEL", color = Violet, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text("Game Hub", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Surface(color = Mint.copy(alpha = .12f), shape = RoundedCornerShape(50.dp), border = BorderStroke(1.dp, Mint.copy(alpha = .28f))) {
            Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Mint))
                Text("DISPOSITIVO PRONTO", color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibrarySearch(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar jogo ou executável", color = Muted) },
        leadingIcon = { Text("⌕", color = Violet, style = MaterialTheme.typography.titleLarge) },
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violet,
            unfocusedBorderColor = Color.White.copy(alpha = .1f),
            focusedContainerColor = Panel,
            unfocusedContainerColor = Panel
        )
    )
}

@Composable
private fun NoSearchResults(query: String) {
    Surface(color = Violet.copy(alpha = .08f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Violet.copy(alpha = .2f))) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⌕", color = Violet, style = MaterialTheme.typography.headlineSmall)
            Column { Text("Nenhum jogo encontrado", fontWeight = FontWeight.Bold); Text("Não encontramos resultados para “$query”.", color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun HeroCard(onImport: () -> Unit) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = VioletDark), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF3A237D), Color(0xFF15112A), Color(0xFF101827))))) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PLAY YOUR WAY", color = Mint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text("Seu PC.\nNo seu bolso.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text("Organize seus jogos e prepare containers compatíveis com seu Android.", color = Color(0xFFD8D1F2), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VioletDark), shape = RoundedCornerShape(12.dp)) { Text("＋  Importar jogo", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun QuickStats(gameCount: Int, diagnostics: DeviceDiagnostics) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("JOGOS", gameCount.toString(), "na biblioteca", Violet, Modifier.weight(1f))
        StatCard("RAM", diagnostics.ram, "detectada", Mint, Modifier.weight(1f))
        StatCard("GPU API", diagnostics.openGl, "gráficos", Amber, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, detail: String, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(detail, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EmptyLibrary(onImport: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⌁", color = Violet, style = MaterialTheme.typography.displaySmall)
            Text("Sua biblioteca está vazia", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Comece adicionando um executável que você possui. O Mikael não baixa jogos automaticamente.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onImport) { Text("Adicionar meu primeiro jogo", color = Violet, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun StatusBanner(message: String, success: Boolean) {
    Surface(color = (if (success) Mint else Amber).copy(alpha = .12f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, (if (success) Mint else Amber).copy(alpha = .3f))) {
        Text(message, color = if (success) Mint else Amber, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GameCard(game: Game, onSelect: (Game) -> Unit, selected: Boolean, onPlay: (Game) -> Unit) {
    Card(onClick = { onSelect(game) }, colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF211B3A) else Panel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (selected) Violet.copy(alpha = .65f) else Color.White.copy(alpha = .06f))) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(62.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Violet, Color(0xFF2D1C68)))), contentAlignment = Alignment.Center) {
                Text(game.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("${game.extension}  •  ${game.container}", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("${game.fileSize}  •  ${game.lastPlayed}", color = Color(0xFF777188), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = Amber.copy(alpha = .12f), shape = RoundedCornerShape(6.dp)) { Text("RUNTIME PENDENTE", color = Amber, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                }
            }
            Button(onClick = { onPlay(game) }, colors = ButtonDefaults.buttonColors(containerColor = Violet), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp), shape = RoundedCornerShape(11.dp)) { Text("Jogar", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun DiagnosticsScreen(info: DeviceDiagnostics, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        val wide = maxWidth >= 700.dp
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)) {
            item { Header(); Text("Diagnóstico", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Leitura em tempo real do seu Android", color = Muted) }
            if (wide) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DiagnosticCard("Dispositivo", listOf("CPU" to "${info.cpuAbi} • ${info.cores} núcleos", "RAM" to info.ram, "Android" to info.android, "Tela" to info.resolution, "GPU" to info.gpu), Modifier.weight(1f))
                        DiagnosticCard("Gráficos", listOf("Vulkan" to info.vulkan, "OpenGL ES" to info.openGl), Modifier.weight(1f))
                    }
                }
                item { DiagnosticCard("Runtime Windows", listOf("Wine" to info.wine, "Box64" to info.box64, "Box86" to info.box86, "DXVK" to info.dxvk, "VKD3D-Proton" to info.vkd3d)) }
            } else {
                item { DiagnosticCard("Dispositivo", listOf("CPU" to "${info.cpuAbi} • ${info.cores} núcleos", "RAM" to info.ram, "Android" to info.android, "Tela" to info.resolution, "GPU" to info.gpu)) }
                item { DiagnosticCard("Gráficos", listOf("Vulkan" to info.vulkan, "OpenGL ES" to info.openGl)) }
                item { DiagnosticCard("Runtime Windows", listOf("Wine" to info.wine, "Box64" to info.box64, "Box86" to info.box86, "DXVK" to info.dxvk, "VKD3D-Proton" to info.vkd3d)) }
            }
            item { Surface(color = Violet.copy(alpha = .1f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Violet.copy(alpha = .25f))) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Mikael Auto Config", color = Violet, fontWeight = FontWeight.Bold); Text("A recomendação automática será ativada quando um runtime compatível for instalado. Nenhuma configuração será alterada silenciosamente.", color = Muted) } } }
        }
    }
}

@Composable
private fun DiagnosticCard(title: String, rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Muted)
                    Text(value, color = if (value.contains("Ausente") || value.contains("não")) Amber else Mint, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ComponentsScreen(modifier: Modifier) {
    val components = listOf("Wine" to "API Windows", "Box64 / Box86" to "Tradução x64/x86 para ARM64", "DXVK" to "DirectX 9/10/11 via Vulkan", "VKD3D-Proton" to "DirectX 12 via Vulkan", "Mesa / Vulkan" to "Backend gráfico")
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        val wide = maxWidth >= 700.dp
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)) {
            item { Header(); Text("Componentes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Instalação controlada e compatível com licenças.", color = Muted) }
            if (wide) {
                items(components.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { pair.forEach { (name, detail) -> ComponentCard(name, detail, Modifier.weight(1f)) }; if (pair.size == 1) Spacer(Modifier.weight(1f)) }
                }
            } else {
                items(components) { (name, detail) -> ComponentCard(name, detail) }
            }
            item { TextButton(onClick = { android.util.Log.i("Mikael", "Licenças serão exibidas na próxima etapa") }) { Text("Ver licenças e fontes", color = Violet) } }
        }
    }
}

@Composable
private fun ComponentCard(name: String, detail: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(name, fontWeight = FontWeight.SemiBold); Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall) }
            FilterChip(selected = false, onClick = { android.util.Log.i("Mikael", "$name ainda não implementado") }, label = { Text("Pendente", style = MaterialTheme.typography.labelSmall) })
        }
    }
}

@Composable
private fun LegalNotice() { Text("Sem root • sem downloads automáticos • seus arquivos permanecem sob seu controle", color = Color(0xFF777188), modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.labelSmall) }

private data class LaunchOutcome(val success: Boolean, val message: String)

private fun launchGame(context: android.content.Context, game: Game): LaunchOutcome {
    if (game.extension.uppercase() == "MSI") return LaunchOutcome(false, "${game.name}: instaladores MSI precisam do fluxo de instalação do Wine; importe o executável principal do jogo para iniciar.")
    if (!NativeBridge.isLoaded) return LaunchOutcome(false, "${game.name}: bridge nativa indisponível. Compile o módulo C++ antes de executar.")
    val executablePath = materializeExecutable(context, game)
        ?: return LaunchOutcome(false, "${game.name}: não foi possível ler o arquivo selecionado. Verifique a permissão do provedor Android.")
    val prefixPath = java.io.File(context.filesDir, "containers/${safeFilePart(game.id)}/prefix").apply { mkdirs() }.absolutePath
    val result = runCatching {
        val initCode = NativeBridge.nativeInitialize()
        if (initCode != 0) initCode else NativeBridge.nativeLaunch(executablePath, prefixPath, emptyArray())
    }.getOrElse { return LaunchOutcome(false, "${game.name}: erro na bridge nativa — ${it.message ?: "falha desconhecida"}.") }
    return when (result) {
        0 -> LaunchOutcome(true, "${game.name}: execução iniciada.")
        -100 -> LaunchOutcome(false, "${game.name}: runtime ausente. Wine + Box64/Box86 precisam ser instalados antes de executar.")
        -101 -> LaunchOutcome(false, "${game.name}: caminho do executável inválido.")
        -102 -> LaunchOutcome(false, "${game.name}: bridge não inicializada.")
        else -> LaunchOutcome(false, "${game.name}: falha ao iniciar (código $result). Consulte os logs.")
    }
}

private fun materializeExecutable(context: android.content.Context, game: Game): String? {
    val destination = java.io.File(context.filesDir, "games/${safeFilePart(game.id)}.exe")
    return runCatching {
        destination.parentFile?.mkdirs()
        val source = Uri.parse(game.executableUri)
        context.contentResolver.openInputStream(source)?.use { input ->
            java.io.FileOutputStream(destination, false).use { output -> input.copyTo(output) }
        } ?: return null
        destination.absolutePath
    }.getOrNull()
}

private fun safeFilePart(value: String): String = value.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "game" }

private fun copyError(context: android.content.Context, report: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Mikael Emulator error", report))
}

private fun getDisplayName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "jogo.exe"
}

private fun getFileSize(context: android.content.Context, uri: Uri): Long {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getLong(0)
    }
    return -1L
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 0) return "Desconhecido"
    if (bytes < 1024L * 1024L) return "${bytes / 1024L} KB"
    if (bytes < 1024L * 1024L * 1024L) return "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    return "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

private fun saveGames(context: android.content.Context, games: List<Game>) {
    val json = org.json.JSONArray()
    games.forEach { game ->
        json.put(org.json.JSONObject().apply {
            put("id", game.id)
            put("name", game.name)
            put("uri", game.executableUri)
            put("extension", game.extension)
            put("container", game.container)
            put("lastPlayed", game.lastPlayed)
            put("runtime", game.runtime)
            put("fileSize", game.fileSize)
        })
    }
    context.getSharedPreferences("mikael_games", android.content.Context.MODE_PRIVATE).edit().putString("library", json.toString()).apply()
}

private fun loadGames(context: android.content.Context): List<Game> {
    val raw = context.getSharedPreferences("mikael_games", android.content.Context.MODE_PRIVATE).getString("library", null) ?: return emptyList()
    return runCatching {
        val json = org.json.JSONArray(raw)
        List(json.length()) { index ->
            val item = json.getJSONObject(index)
            Game(
                id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                name = item.optString("name", "Jogo sem nome"),
                executableUri = item.optString("uri"),
                extension = item.optString("extension", "EXE"),
                container = item.optString("container", "Padrão"),
                lastPlayed = item.optString("lastPlayed", "Nunca iniciado"),
                runtime = item.optString("runtime", "Ainda não implementado"),
                fileSize = item.optString("fileSize", "Tamanho desconhecido")
            )
        }
    }.getOrDefault(emptyList())
}
