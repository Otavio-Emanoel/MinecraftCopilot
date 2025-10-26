# MinecraftCopilot (protótipo)

Um protótipo de mundo voxel 3D em Java inspirado no Minecraft, gerado do zero usando jMonkeyEngine 3.6.

Este projeto é didático e focado em iniciar rápido: renderiza um mundo simples de blocos com malha por chunk (faces expostas), câmera livre (WASD + mouse) e cores por bloco (sem texturas).

## Requisitos

- Java 17+ (OpenJDK recomendado)
- Gradle instalado (se você não tiver, veja como instalar abaixo)
- GPU/driver com OpenGL 3.0+ (a maioria dos PCs atende)

### Instalar Java (Ubuntu/Debian)
```sh
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version
```

### Instalar Gradle
- Via SDKMAN (recomendado por funcionar em várias distros)
```sh
curl -s "https://get.sdkman.io" | bash
. "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.10.2
gradle -v
```
- Ou via apt (pode estar desatualizado em algumas distros):
```sh
sudo apt update
sudo apt install -y gradle
gradle -v
```

## Como rodar

Dentro da pasta do projeto:
```sh
gradle run
```
A primeira execução baixa as dependências (pode demorar alguns minutos). Uma janela se abrirá com um pequeno mundo de blocos.

Controles:
- Mouse: olhar
- W/A/S/D: mover
- Espaço/Shift: subir/descer
- Roda do mouse: zoom

## Estrutura

- `build.gradle` e `settings.gradle`: configuração do Gradle e dependências do jMonkeyEngine.
- `src/main/java/com/minecraftcopilot/Main.java`: inicialização do app, câmera e criação do mundo.
- `src/main/java/com/minecraftcopilot/BlockType.java`: tipos de bloco e cores.
- `src/main/java/com/minecraftcopilot/Noise2D.java`: ruído 2D (value noise + FBM) para o relevo.
- `src/main/java/com/minecraftcopilot/Chunk.java`: geração de malha (faces expostas) por chunk 16x64x16.

## Próximos passos sugeridos

- Colisões do jogador (CharacterControl) ao invés da câmera livre.
- Texturas com atlas (substituir cores por UVs e material `Lighting.j3md`).
- Otimização com mesclagem "greedy" de faces e carregamento sob demanda de chunks.
- Edição de blocos (quebrar/colocar) e salvamento simples do mundo.

## Problemas comuns

- "gradle: comando não encontrado": instale o Gradle (veja acima).
- Tela preta/sem janela: verifique drivers de vídeo (atualize drivers e pacotes `mesa`).
- Erro de versão do Java: use Java 17+.

### Crash em drivers NVIDIA (SIGSEGV em libnvidia-glcore)
Alguns drivers podem ter instabilidades com LWJGL3/OpenGL3. Neste projeto já deixei configurado o backend LWJGL2 e o renderer OpenGL2, que são mais compatíveis:

- Em `build.gradle`: usa `jme3-lwjgl` (LWJGL2) ao invés de `jme3-lwjgl3`.
- Em `Main.java`: `settings.setRenderer(AppSettings.LWJGL_OPENGL2);`

Se desejar voltar ao LWJGL3 (mais recente), troque para `jme3-lwjgl3` na `build.gradle` e remova/ajuste a linha do renderer para `AppSettings.LWJGL_OPENGL3`. Se o crash voltar, mantenha LWJGL2.

## Licença

Uso livre para fins educacionais. Não é afiliado à Mojang/Microsoft.
