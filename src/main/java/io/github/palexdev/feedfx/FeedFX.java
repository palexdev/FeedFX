package io.github.palexdev.feedfx;

import io.github.palexdev.architectfx.backend.utils.OSUtils;
import io.github.palexdev.feedfx.events.AppEvent;
import io.github.palexdev.feedfx.events.SettingsEvent;
import io.github.palexdev.feedfx.theming.ThemeEngine;
import io.github.palexdev.feedfx.utils.FileUtils;
import io.github.palexdev.mfxcore.events.bus.IEventBus;
import io.github.palexdev.mfxcore.settings.Settings;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Wrapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.tinylog.Logger;

public class FeedFX extends Application {
    //================================================================================
    // Static Properties
    //================================================================================

    // Extra Beans
    private static FeedFX app;
    private static Stage stage;
    private static Parameters parameters;
    private static HostServices hostServices;

    // Dependencies
    private static IEventBus bus;
    private static ThemeEngine themeEngine;
    private static AppSettings settings;

    //================================================================================
    // Startup/Shutdown
    //================================================================================
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // Init extra beans
        FeedFX.app = this;
        FeedFX.stage = stage;
        FeedFX.parameters = getParameters();
        FeedFX.hostServices = getHostServices();

        // Bootstrap
        bootstrap().ifPresentOrElse(
            f -> {
                Logger.info("Bootstrap completed successfully...");
                bus.publish(new AppEvent.AppReadyEvent()); // Start app, show main window
            },
            () -> {
                Logger.error("Bootstrap failed, closing app!");
                Platform.exit();
            }
        );
    }

    @Override
    public void stop() throws Exception {
        if (stage.getWidth() > 0.0) settings.windowWidth().set(stage.getWidth());
        if (stage.getHeight() > 0.0) settings.windowHeight().set(stage.getHeight());
        bus.publish(new AppEvent.AppCloseEvent());
        System.exit(0); // AWT tray icon keeps the app running, force close
    }

    private Optional<MainModule> bootstrap() {
        MainModule module = io.inverno.core.v1.Application
            .with(new MainModule.Builder())
            .run();

        // Ensure supported platform
        if (!OSUtils.isSupportedPlatform()) {
            Logger.error("Unsupported OS detected %s.%nApp will shutdown!".formatted(OSUtils.os()));
            return Optional.empty();
        }

        themeEngine.loadTheme();

        // Check if settings reset has been requested via arguments
        // Also add listener for ResetSettingEvents
        if (settings.isResetSettings()) Settings.resetAll();
        bus.subscribe(SettingsEvent.ResetSettingsEvent.class, e -> Settings.reset(e.data()));

        return Optional.of(module);
    }

    //================================================================================
    // Static Methods
    //================================================================================
    public static Path appBaseDir() {
        try {
            return FileUtils.createDirectory(
                Paths.get(System.getProperty("user.home"), ".feedfx")
            );
        } catch (IOException ex) {
            Logger.error("Could not get app base dir:\n{}", ex);
            bus.publish(new AppEvent.AppCloseEvent());
            throw new RuntimeException(ex);
        }
    }

    public static Path cacheDir() {
        try {
            return FileUtils.createDirectory(
                appBaseDir().resolve("cache")
            );
        } catch (IOException ex) {
            Logger.error("Could not get app cache dir:\n{}", ex);
            bus.publish(new AppEvent.AppCloseEvent());
            throw new RuntimeException(ex);
        }
    }

    //================================================================================
    // Sockets
    //================================================================================
    @Bean
    @Wrapper
    public static class App implements Supplier<FeedFX> {
        private final FeedFX app;

        public App(
            IEventBus events,
            ThemeEngine themeEngine,
            AppSettings settings
        ) {
            app = FeedFX.app;
            FeedFX.bus = events;
            FeedFX.themeEngine = themeEngine;
            FeedFX.settings = settings;
        }

        @Override
        public FeedFX get() {
            return app;
        }
    }

    @Bean
    @Wrapper
    public static class MainWindow implements Supplier<Stage> {
        @Override
        public Stage get() {
            return FeedFX.stage;
        }
    }

    @Bean
    @Wrapper
    public static class ParametersWrap implements Supplier<Parameters> {
        @Override
        public Parameters get() {
            return FeedFX.parameters;
        }
    }

    @Bean
    @Wrapper
    public static class HostServicesWrap implements Supplier<HostServices> {
        @Override
        public HostServices get() {
            return FeedFX.hostServices;
        }
    }
}
