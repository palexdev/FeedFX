package io.github.palexdev.feedfx.ui.components.dialogs;

import io.github.palexdev.mfxcomponents.window.popups.MFXPopup;
import io.github.palexdev.mfxcore.enums.Zone;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.StageUtils;
import io.github.palexdev.mfxcore.utils.resize.RegionDragResizer;
import io.github.palexdev.mfxeffects.MFXScrimEffect;
import java.util.Optional;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

public abstract class MFXDialog<T> extends MFXPopup {
    //================================================================================
    // Properties
    //================================================================================
    private boolean inNestedEventLoop = false;

    private MFXScrimEffect scrim;
    private boolean scrimOwner = false;
    private double scrimStrength = 0.25;
    private When<?> ownerPosWhen;
    private boolean draggable = false;

    private RegionDragResizer resizer;

    //================================================================================
    // Constructors
    //================================================================================
    protected MFXDialog() {
        setAutoHide(false);
        setConsumeAutoHidingEvents(true);
        setHideOnEscape(false);

        /*
         * Workaround: popups can be focused only if the owner window is also focused
         */
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Window owner = getOwnerWindow();
            if (owner != null && !owner.isFocused())
                owner.requestFocus();
        });
    }

    //================================================================================
    // Abstract Methods
    //================================================================================
    protected abstract T getResult();

    //================================================================================
    // Methods
    //================================================================================
    public T showAndWait(Window window, Pos anchor) {
        if (!Platform.canStartNestedEventLoop()) {
            throw new IllegalStateException("showAndWait is not allowed during animation or layout processing");
        }

        assert !inNestedEventLoop;

        show(window, anchor);
        inNestedEventLoop = true;
        Platform.enterNestedEventLoop(this);
        return getResult();
    }

    public Optional<T> showAndWaitOpt(Window window, Pos anchor) {
        return Optional.ofNullable(showAndWait(window, anchor));
    }

    protected void autoReposition() {
        if (getOwnerWindow() != null) {
            windowReposition();
        } else {
            reposition();
        }
    }

    protected void applyScrim() {
        if (scrim == null) scrim = new MFXScrimEffect() {
            final EventHandler<Event> eh = Event::consume;

            @Override
            public void scrimWindow(Window window, double opacity) {
                Parent root = Optional.ofNullable(window)
                    .map(Window::getScene)
                    .map(Scene::getRoot)
                    .orElse(null);
                if (root == null) return;
                super.scrimWindow(window, opacity);
                root.addEventFilter(Event.ANY, eh);
            }

            @Override
            public void removeEffect(Window window) {
                Parent root = Optional.ofNullable(window)
                    .map(Window::getScene)
                    .map(Scene::getRoot)
                    .orElse(null);
                if (root == null) return;
                super.removeEffect(window);
                root.removeEventFilter(Event.ANY, eh);
            }
        };
        Window window = getOwnerWindow();
        if (window != null) {
            scrim.scrimWindow(window, scrimStrength);
        }
    }

    protected void unscrim() {
        Window window = getOwnerWindow();
        if (scrim != null && window != null) {
            scrim.removeEffect(window);
        }
    }

    protected void makeDraggable(Node byNode) {
        if (!draggable) return;
        Window w = getOwnerWindow();
        if (w instanceof Stage s)
            StageUtils.makeDraggable(s, byNode);
    }

    protected void makeResizable() {
        if (getContent() instanceof Region r) {
            if (resizer != null) resizer.dispose();
            resizer = new RegionDragResizer(r);
            resizer.setAllowedZones(Zone.CENTER_RIGHT, Zone.BOTTOM_CENTER);
            resizer.makeResizable();
        }
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected void show() {
        Optional.ofNullable(getOwnerWindow())
            .filter(Stage.class::isInstance)
            .map(Stage.class::cast)
            .ifPresent(s -> {
                ownerPosWhen = When.onInvalidated(s.xProperty())
                    .then(v -> autoReposition())
                    .invalidating(s.yProperty())
                    .invalidating(s.widthProperty())
                    .invalidating(s.heightProperty())
                    .listen();
                applyScrim();
            });
        super.show();
    }

    @Override
    public void hide() {
        Optional.ofNullable(getOwnerWindow())
            .filter(Stage.class::isInstance)
            .map(Stage.class::cast)
            .ifPresent(_ -> unscrim());
        super.hide();
        if (inNestedEventLoop) {
            inNestedEventLoop = false;
            Platform.exitNestedEventLoop(this, null);
        }
    }

    public void dispose() {
        if (ownerPosWhen != null) {
            ownerPosWhen.dispose();
            ownerPosWhen = null;
        }
        setOwner(null);
        setContent(null);
        scrim = null;
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public boolean isScrimOwner() {
        return scrimOwner;
    }

    public void setScrimOwner(boolean scrimOwner) {
        this.scrimOwner = scrimOwner;
    }

    public double getScrimStrength() {
        return scrimStrength;
    }

    public void setScrimStrength(double scrimStrength) {
        this.scrimStrength = scrimStrength;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }
}
