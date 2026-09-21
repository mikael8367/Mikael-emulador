# Mikael Emulator

Aplicativo Android nativo para organizar jogos Windows e preparar containers isolados para execução via Wine + Box64/Box86. O projeto segue ARM64 (`arm64-v8a`), Android 10+ e usa Kotlin, Jetpack Compose, Material 3, CMake e JNI.

## Estado atual

Esta primeira versão é compilável e funcional como aplicativo Android, mas **não inclui binários de Wine, Box64, Box86, DXVK ou VKD3D-Proton**. Esses componentes possuem licenças, builds e requisitos próprios e não devem ser falsamente declarados como instalados. A interface mostra essa situação como “Ainda não implementado” ou “Ausente — componente externo necessário”.

O app já inclui:

- biblioteca local de jogos;
- aba dedicada **Meus Jogos** para listar os executáveis importados;
- aba **Config** com resolução, memória do container e persistência local;
- limite de memória calculado a partir da RAM real do dispositivo para evitar configurações absurdas;
- biblioteca persistida localmente, com prevenção de duplicatas e validação de acesso persistente ao arquivo selecionado;
- fluxo de execução com cópia do arquivo para armazenamento privado, criação do prefixo e preflight fora da UI thread;
- validação JNI do caminho do executável antes do launch, com códigos de erro para arquivo inválido e runtime ausente;
- diálogo de erro com componente responsável, causa, solução sugerida e botão para copiar o relatório;
- importação de conteúdo `.wad` e `.pk3`, com preflight que exige um engine compatível em vez de tentar executá-los como `.exe`;
- orientação fixa em Landscape para uso confortável com controles e teclado;
- importação de arquivos `.exe` e `.msi` via Storage Access Framework;
- botão **Jogar** por arquivo importado, conectado à bridge JNI e com erro explícito quando Wine/Box64/Box86 estiverem ausentes;
- diálogo de confirmação antes da importação, mostrando capa visual, nome, extensão e tamanho do arquivo;
- nenhum download automático do jogo ou de conteúdo protegido;
- leitura real de ABI, núcleos, RAM, Android, resolução, Vulkan e OpenGL ES;
- contratos para `EmulatorEngine`, `GraphicsBackend`, `InputBackend`, `AudioBackend`, `ContainerManager` e `RuntimeManager`;
- bridge JNI/C++ com códigos de retorno explícitos;
- arquitetura preparada para containers, perfis, logs e gerenciadores de runtime;
- nenhuma exigência de root, nenhum download automático de jogos e nenhuma execução silenciosa de arquivo desconhecido.

## Compilação

O workflow `.github/workflows/android.yml` instala Android SDK, NDK, CMake e Gradle e gera automaticamente o APK ARM64 como artefato do GitHub Actions.

Para gerar o APK ARM64 manualmente em uma máquina Android configurada:

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`. Para um release assinado, configure um keystore externo e execute o fluxo de assinatura do Gradle; o projeto não inclui credenciais.

## Próximas integrações reais

1. adicionar um instalador legal de runtimes fornecidos pelo usuário, validando checksum e licença;
2. implementar `ContainerManager` com armazenamento privado e exportação/importação de prefix;
3. integrar Wine/Box64/Box86 por builds compatíveis com Android/ARM64;
4. implementar backend gráfico verificando Vulkan antes de habilitar DXVK/VKD3D;
5. adicionar input overlay, gamepad Bluetooth/USB, áudio, logs e gerenciamento de processos;
6. criar testes instrumentados no dispositivo Android.

Até essas etapas, o app não afirma executar programas Windows. Essa limitação é intencional e visível ao usuário.
