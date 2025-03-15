package io.github.palexdev.feedfx;

import io.github.palexdev.feedfx.theming.ThemeMode;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.settings.BooleanSetting;
import io.github.palexdev.mfxcore.settings.NumberSetting;
import io.github.palexdev.mfxcore.settings.Settings;
import io.github.palexdev.mfxcore.settings.StringSetting;
import io.inverno.core.annotation.Bean;
import java.util.Map;
import javafx.application.Application;

@Bean
public class AppSettings extends Settings {
    //================================================================================
    // Settings
    //================================================================================
    // UI
    private final NumberSetting<Double> windowWidth = registerDouble("window.width", "", 1280.0);
    private final NumberSetting<Double> windowHeight = registerDouble("window.height", "", 720.0);
    private final StringSetting themeMode = registerString("theme.mode", "Theme variation, light/dark", ThemeMode.LIGHT.name());

    // Extra
    private final Application.Parameters parameters;
    private Boolean resetSettings = null;

    //================================================================================
    // Constructors
    //================================================================================
    public AppSettings(Application.Parameters parameters) {
        this.parameters = parameters;
    }

    //================================================================================
    // Methods
    //================================================================================
    public Size getWindowSize() {
        return Size.of(windowWidth.get(), windowHeight.get());
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected String node() {
        return "/io/github/palexdev/feedfx";
    }

    //================================================================================
    // Getters
    //================================================================================
    public NumberSetting<Double> windowWidth() {
        return windowWidth;
    }

    public NumberSetting<Double> windowHeight() {
        return windowHeight;
    }

    public StringSetting themeMode() {
        return themeMode;
    }

    public boolean isResetSettings() {
        if (resetSettings == null) {
            Map<String, String> named = parameters.getNamed();
            resetSettings = Boolean.parseBoolean(named.getOrDefault("reset-settings", "false"));
        }
        return resetSettings;
    }
}
