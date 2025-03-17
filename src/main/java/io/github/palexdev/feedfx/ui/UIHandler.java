package io.github.palexdev.feedfx.ui;

import io.github.palexdev.architectfx.backend.loaders.UILoader;
import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.events.AppEvent;
import io.github.palexdev.mfxcore.events.bus.IEventBus;
import io.inverno.core.annotation.Bean;
import java.io.IOException;
import java.net.URL;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.tinylog.Logger;

@Bean
public class UIHandler {
    //================================================================================
    // Properties
    //================================================================================
    private final Stage mainWindow;

    //================================================================================
    // Constructors
    //================================================================================
    public UIHandler(IEventBus bus, Stage mainWindow) {
        this.mainWindow = mainWindow;

        bus.subscribe(AppEvent.AppReadyEvent.class, e -> init());
    }

    //================================================================================
    // Methods
    //================================================================================
    private void init() {
        try {
            URL ui = Resources.loadURL("FeedFX.jui");
            JUIFXLoader loader = AppUILoader.instance();
            UILoader.Loaded<Node> res = loader.load(ui);

            Scene scene = new Scene((Parent) res.root());
            scene.setFill(Color.TRANSPARENT);

            mainWindow.setScene(scene);
            mainWindow.setTitle("FeedFX");
            mainWindow.initStyle(StageStyle.TRANSPARENT);
            mainWindow.getIcons().add(new Image(
                Resources.loadStream("assets/logo.png")
            ));
            mainWindow.show();
            mainWindow.centerOnScreen();
        } catch (IOException ex) {
            Logger.error("Failed to load UI because:\n{}", ex);
        }
    }
}
