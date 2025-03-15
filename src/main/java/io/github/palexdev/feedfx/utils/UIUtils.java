package io.github.palexdev.feedfx.utils;

import io.github.palexdev.mfxcomponents.window.MFXPlainContent;
import io.github.palexdev.mfxcomponents.window.popups.MFXTooltip;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
}
