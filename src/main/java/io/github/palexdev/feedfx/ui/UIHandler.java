package io.github.palexdev.feedfx.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import io.github.palexdev.architectfx.backend.loaders.UILoader;
import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.events.AppEvent;
import io.github.palexdev.feedfx.events.UIEvent;
import io.github.palexdev.feedfx.theming.ThemeEngine;
import io.github.palexdev.feedfx.theming.ThemeMode;
import io.github.palexdev.feedfx.utils.UIUtils;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcomponents.window.MFXPlainContent;
import io.github.palexdev.mfxcomponents.window.popups.MFXPopup;
import io.github.palexdev.mfxcore.events.bus.IEventBus;
import io.github.palexdev.mfxeffects.animations.ConsumerTransition;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import io.inverno.core.annotation.Bean;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.tinylog.Logger;

@Bean
public class UIHandler {
    //================================================================================
    // Properties
    //================================================================================
    private final Stage mainWindow;
    private final ThemeEngine themeEngine;
    private final Image ICON = new Image(
        Resources.loadStream("assets/logo.png")
    );

    private FXTrayIcon trayIcon;
    private ImageView transitionView;

    //================================================================================
    // Constructors
    //================================================================================
    public UIHandler(IEventBus bus, Stage mainWindow, ThemeEngine themeEngine) {
        this.mainWindow = mainWindow;
        this.themeEngine = themeEngine;
        bus.subscribe(AppEvent.AppReadyEvent.class, _ -> init());
        bus.subscribe(UIEvent.NotifyEvent.class, this::notify);
        bus.subscribe(UIEvent.ThemeSwitchEvent.class, _ -> onThemeSwitched());
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
            mainWindow.getIcons().add(ICON);
            mainWindow.show();
            mainWindow.centerOnScreen();

            initTrayIcon();
        } catch (IOException ex) {
            Logger.error("Failed to load UI because:\n{}", ex);
        }
    }

    private void initTrayIcon() {
        if (!FXTrayIcon.isSupported()) {
            Logger.warn("Tray icon is not supported on this system!");
            return;
        }

        trayIcon = new FXTrayIcon.Builder(mainWindow, ICON)
            .setIconSize(32)
            .applicationTitle("FeedFX")
            .addTitleItem(true)
            .addExitMenuItem("Exit", _ -> Platform.exit())
            .show()
            .build();
    }

    private void notify(UIEvent.NotifyEvent event) {
        if (trayIcon != null) {
            trayIcon.showMessage(event.data());
            return;
        }

        MFXPopup popup = new MFXPopup();
        popup.setContent(new MFXPlainContent(event.data()));
        mainWindow.setIconified(false);
        mainWindow.toFront();
        popup.show(mainWindow, Pos.CENTER);
    }

    private void onThemeSwitched() {
        Parent root = Optional.ofNullable(mainWindow.getScene())
            .map(Scene::getRoot)
            .orElse(null);
        if (root == null) return;
        PseudoClasses.setOn(root, "dark", themeEngine.getThemeMode() == ThemeMode.DARK);

        if (root instanceof StackPane sp) {
            // Disable window temporarily
            sp.setMouseTransparent(true);

            // Snapshot window
            double w = sp.getWidth();
            double h = sp.getHeight();
            WritableImage snap = UIUtils.snapshot(
                sp,
                w, h,
                new SnapshotParameters()
            );
            transitionView = new ImageView(snap);
            transitionView.setSmooth(false);
            transitionView.setFitWidth(w);
            transitionView.setFitHeight(h);
            transitionView.setPreserveRatio(false);
            transitionView.setOpacity(1.0);
            transitionView.setManaged(false);

            Rectangle clip = new Rectangle(w, h);
            clip.setArcWidth(24.0);
            clip.setArcHeight(24.0);
            transitionView.setClip(clip);

            sp.getChildren().add(transitionView);

            // Fade out and remove
            ConsumerTransition.of(
                frac -> transitionView.setOpacity(1.0 - (1.0 * frac)),
                M3Motion.EXTRA_LONG4,
                M3Motion.STANDARD
            ).setOnFinishedFluent(f -> {
                sp.getChildren().remove(transitionView);
                sp.setMouseTransparent(false);
            }).play();
        }
    }
}
