package io.github.palexdev.feedfx.ui.controllers;

import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.StageUtils;
import io.github.palexdev.mfxcore.utils.resize.StageResizer;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class HeaderController {
    //================================================================================
    // Properties
    //================================================================================
    private final Stage window;
    private Size prevWindowSize = Size.empty();

    //================================================================================
    // Constructors
    //================================================================================
    public HeaderController(Stage window) {
        this.window = window;
    }

    //================================================================================
    // Methods
    //================================================================================
    public void handleWindow(Region root, Region dndArea) {
        StageUtils.makeDraggable(window, dndArea);
        StageResizer resizer = new StageResizer(root, window);
        resizer.setMinWidthFunction(r -> r.prefWidth(-1));
        resizer.setMinHeightFunction(r -> r.prefHeight(-1));
        resizer.makeResizable();
    }

    public void handleAot(Node aot) {
        aot.setOnMouseClicked(e -> window.setAlwaysOnTop(!window.isAlwaysOnTop()));
        When.onInvalidated(window.alwaysOnTopProperty())
            .then(v -> PseudoClasses.setOn(aot, "aot", v))
            .executeNow()
            .listen();
    }

    public void handleMinimize(Node minimize) {
        minimize.setOnMouseClicked(e -> window.setIconified(true));
    }

    public void handleMaximize(Node maximize) {
        maximize.setOnMouseClicked(e -> handleMaximize());
    }

    public void handleClose(Node close) {
        close.setOnMouseClicked(e -> window.hide());
    }

    private void handleMaximize() {
        Parent root = Optional.ofNullable(window.getScene())
            .map(Scene::getRoot)
            .orElse(null);
        if (root == null) return;

        if (!window.isMaximized()) {
            prevWindowSize = Size.of(
                root.getLayoutBounds().getWidth(),
                root.getLayoutBounds().getHeight()
            );
            window.setMaximized(true);
        } else {
            window.setMaximized(false);
            window.setWidth(prevWindowSize.getWidth());
            window.setHeight(prevWindowSize.getHeight());
            window.centerOnScreen();
        }
    }
}
