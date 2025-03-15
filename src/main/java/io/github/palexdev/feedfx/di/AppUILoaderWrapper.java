package io.github.palexdev.feedfx.di;

import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.architectfx.backend.resolver.DefaultResolver;
import io.github.palexdev.architectfx.backend.resolver.Resolver;
import io.github.palexdev.feedfx.model.AppModel;
import io.github.palexdev.feedfx.ui.AppUILoader;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Wrapper;
import java.util.function.Supplier;
import javafx.stage.Stage;

@Bean
@Wrapper
public class AppUILoaderWrapper implements Supplier<JUIFXLoader> {
    private final JUIFXLoader loader = AppUILoader.instance();

    public AppUILoaderWrapper(AppModel appModel, Stage mainWindow) {
        loader.config().setResolverFactory(uri -> {
            Resolver.Context ctx = new Resolver.Context(uri);
            ctx.setInjections(
                "appModel", appModel,
                "mainWindow", mainWindow
            );
            return new DefaultResolver(ctx);
        });
    }

    @Override
    public JUIFXLoader get() {
        return loader;
    }
}
