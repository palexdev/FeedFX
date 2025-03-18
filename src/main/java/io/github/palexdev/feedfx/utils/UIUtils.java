package io.github.palexdev.feedfx.utils;

import io.github.palexdev.mfxcomponents.window.MFXPlainContent;
import io.github.palexdev.mfxcomponents.window.popups.MFXTooltip;
import io.github.palexdev.mfxcore.utils.fx.NodeUtils;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
import javafx.stage.Screen;
import javafx.util.Duration;

public class UIUtils {

    //================================================================================
    // Constructors
    //================================================================================
    private UIUtils() {}

    //================================================================================
    // Static Methods
    //================================================================================
    public static MFXTooltip installTooltip(Node node, String text, Pos position) {
        MFXTooltip tooltip = new MFXTooltip(node);
        tooltip.setContent(new MFXPlainContent(text));
        tooltip.setInDelay(M3Motion.LONG2);
        tooltip.setOutDelay(Duration.ZERO);
        tooltip.setAnchor(position);
        return tooltip.install();
    }

    public static WritableImage snapshot(Node node, double w, double h, SnapshotParameters parameters) {
        Screen screen = NodeUtils.getScreenFor(node);
        if (screen.getOutputScaleX() != 1.0) {
            double scale = screen.getOutputScaleX();
            int scaledW = (int) (w * scale);
            int scaledH = (int) (h * scale);
            WritableImage snapshot = new WritableImage(scaledW, scaledH);
            parameters.setTransform(Transform.scale(scale, scale));
            node.snapshot(parameters, snapshot);
            return snapshot;
        }
        return node.snapshot(parameters, null);
    }
}
