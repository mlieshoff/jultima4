package org.jultima4;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class DesktopLauncher {
    private DesktopLauncher() { }
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Ultima IV - Britannia");
        config.setWindowedMode(1000, 720);
        config.useVsync(true);
        new Lwjgl3Application(new UltimaGame(), config);
    }
}
