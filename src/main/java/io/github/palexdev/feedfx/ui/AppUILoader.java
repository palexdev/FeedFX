package io.github.palexdev.feedfx.ui;

import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;

public class AppUILoader extends JUIFXLoader {
    //================================================================================
    // Singleton
    //================================================================================
    private static final AppUILoader INSTANCE = new AppUILoader();

    public static AppUILoader instance() {
        return INSTANCE;
    }

    //================================================================================
    // Constructors
    //================================================================================
    private AppUILoader() {}
}
