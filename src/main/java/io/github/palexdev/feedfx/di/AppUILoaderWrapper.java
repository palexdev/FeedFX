package io.github.palexdev.feedfx.di;

import java.util.function.Supplier;

import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.architectfx.backend.resolver.DefaultResolver;
import io.github.palexdev.architectfx.backend.resolver.Resolver;
import io.github.palexdev.feedfx.AppSettings;
import io.github.palexdev.feedfx.model.AppModel;
import io.github.palexdev.feedfx.theming.ThemeEngine;
import io.github.palexdev.feedfx.ui.AppUILoader;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Wrapper;
import javafx.application.HostServices;
import javafx.stage.Stage;

@Bean
@Wrapper
public class AppUILoaderWrapper implements Supplier<JUIFXLoader> {
    private final JUIFXLoader loader = AppUILoader.instance();

    public AppUILoaderWrapper(AppSettings settings, AppModel appModel,
                              Stage mainWindow,
                              HostServices hostServices, ThemeEngine themeEngine
    ) {
        loader.config().setResolverFactory(uri -> {
            Resolver.Context ctx = new Resolver.Context(uri);
            ctx.setInjections(
                "appSettings", settings,
                "appModel", appModel,
                "mainWindow", mainWindow,
                "hostServices", hostServices,
                "themeEngine", themeEngine
            );
            return new DefaultResolver(ctx);
        });
    }

    @Override
    public JUIFXLoader get() {
        return loader;
    }
}
