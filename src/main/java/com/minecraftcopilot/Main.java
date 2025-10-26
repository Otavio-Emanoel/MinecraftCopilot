package com.minecraftcopilot;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import com.minecraftcopilot.state.MainMenuState;

public class Main extends SimpleApplication {

    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("MinecraftCopilot - Protótipo (jMonkeyEngine)");
        settings.setResolution(1280, 720);
        // Força o renderer OpenGL2 (compatibilidade elevada com LWJGL2/NVIDIA)
        settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Cor de fundo padrão
        viewPort.setBackgroundColor(new ColorRGBA(0.15f, 0.18f, 0.22f, 1f));
        // No menu, desabilitamos a câmera fly para não mover
        flyCam.setEnabled(false);
        // Inicia no menu principal
        stateManager.attach(new MainMenuState());
    }
}
