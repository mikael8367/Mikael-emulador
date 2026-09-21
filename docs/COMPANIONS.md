# Mikael Game Hub — companions de execução

O código do Mikael Game Hub continua sendo o aplicativo principal. Ele organiza a biblioteca, importa arquivos, salva capas e preferências e escolhe o companion conforme a extensão do jogo.

## UZDoom Engine Companion

Arquivos `.WAD` e `.PK3` são encaminhados para um engine Doom real. O workflow `.github/workflows/doom-companion.yml` baixa a base open-source UZDoom Android 2026, remove o caminho Java específico do computador do autor, instala o SDK Android e gera o APK companion como artefato do GitHub Actions. O companion fornece renderização nativa, áudio e controles touch. O `DOOM1.WAD` continua sendo tratado como conteúdo do usuário e não como executável Windows.

## Windows Companion

Arquivos `.EXE` e `.MSI` são encaminhados pelo hub para um companion compatível com Wine, Box64/Box86 e DXVK quando um pacote desse tipo estiver instalado no Android. O hub não baixa runtimes silenciosamente. O projeto Winlator é uma referência open-source para esse companion, mas o repositório público consultado não contém uma árvore Android moderna diretamente compilável no estado atual; por isso o workflow não fabrica um APK de terceiro nem afirma que qualquer jogo Windows será compatível.

A compatibilidade de jogos Windows depende do processador ARM64, GPU Vulkan, memória, versão do Windows usada pelo jogo, DirectX, DRM e configuração do container. Portanto, o produto pode executar jogos compatíveis, mas não pode prometer “qualquer jogo” sem testes individuais.

## Fluxo de execução

O hub encaminha WAD/PK3 para o UZDoom Companion e EXE/MSI para um Windows Companion instalado. Se o pacote correspondente não estiver instalado, o usuário recebe uma mensagem explicando qual companion falta. Essa divisão evita manter um launcher que apenas exibe “iniciado” sem haver um engine real carregando o jogo.
