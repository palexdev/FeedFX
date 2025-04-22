package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.events.AppEvenBus;
import io.github.palexdev.feedfx.events.ModelEvent;
import io.github.palexdev.feedfx.model.Feed;
import io.github.palexdev.feedfx.ui.components.FeedCardOverlay.OverlayOwner;
import io.github.palexdev.feedfx.ui.components.dialogs.TagMenu;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXIconButton;
import io.github.palexdev.mfxcomponents.window.MFXPlainContent;
import io.github.palexdev.mfxcomponents.window.popups.MFXTooltip;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import io.github.palexdev.mfxeffects.animations.motion.Motion;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

public class FeedCardOverlay<O extends Parent & OverlayOwner> extends Region {
    //================================================================================
    // Properties
    //================================================================================
    private O owner;
    private Supplier<Feed> feedSupplier;

    private final MFXIconButton browseBtn;
    private final MFXIconButton markAsBtn;
    private final MFXIconButton tagBtn;
    protected double H_GAP = 8.0;

    private final TagMenu tagMenu;

    // Animations
    private Animation showAnimation;
    private Animation hideAnimation;

    //================================================================================
    // Constructors
    //================================================================================
    public FeedCardOverlay() {
        browseBtn = new MFXIconButton().outlined();
        browseBtn.getStyleClass().add("browse");
        browseBtn.setOnAction(_ -> browse());
        MFXTooltip btp = new MFXTooltip(browseBtn).install();
        btp.setContent(new MFXPlainContent("Open in Browser"));

        markAsBtn = new MFXIconButton().outlined();
        markAsBtn.getStyleClass().add("read");
        markAsBtn.setOnAction(_ -> markAs());
        MFXTooltip mtp = new MFXTooltip(markAsBtn).install();
        mtp.setContent(new MFXPlainContent());

        tagBtn = new MFXIconButton().outlined();
        tagBtn.setIconDescription("fas-tag"); // Workaround for conflict in CSS with checkbox ¯\_(ツ)_/¯
        tagBtn.getStyleClass().add("tag");
        tagBtn.setOnAction(_ -> tag());
        MFXTooltip ttp = new MFXTooltip(tagBtn).install();
        ttp.setContent(new MFXPlainContent("Tag Feed"));

        tagMenu = new TagMenu();
        When.onInvalidated(tagMenu.contentBoundsProperty())
            .then(_ -> tagMenu.reposition())
            .listen();

        setManaged(false);
        getChildren().setAll(browseBtn, markAsBtn, tagBtn);
        getStyleClass().add("overlay");
    }

    //================================================================================
    // Methods
    //================================================================================
    public void show(O owner) {
        if (Animations.isPlaying(hideAnimation))
            hideAnimation.stop();

        // Update "Mark as "tooltip here
        Feed feed = getFeed();
        if (feed != null) {
            ((MFXPlainContent) markAsBtn.getMFXTooltip().getContent()).setText(
                feed.read() ? "Mark as Unread" : "Mark as Read"
            );
        }

        setOpacity(0.0);
        owner.addOverlay(this);
        this.owner = owner;
        owner.requestLayout();

        showAnimation = Animations.TimelineBuilder.build()
            .add(Animations.KeyFrames.of(M3Motion.SHORT4, opacityProperty(), 1.0, Motion.EASE_IN))
            .getAnimation();
        showAnimation.play();
    }

    public void hide(O owner) {
        if (Animations.isPlaying(showAnimation))
            showAnimation.stop();

        this.owner = null;
        hideAnimation = Animations.TimelineBuilder.build()
            .add(Animations.KeyFrames.of(M3Motion.SHORT4, opacityProperty(), 0.0, Motion.EASE_OUT))
            .getAnimation();
        Animations.onStopped(hideAnimation, () -> {
            owner.removeOverlay(this);
            owner.requestLayout();
        }, true);
        hideAnimation.play();
    }

    protected void browse() {
        AppEvenBus.instance().publish(new ModelEvent.OpenFeedEvent(getFeed()));
    }

    protected void markAs() {
        AppEvenBus.instance().publish(new ModelEvent.MarkFeedEvent(getFeed()));
    }

    protected void tag() {
        tagMenu.setFeed(getFeed());
        tagMenu.show(tagBtn, Pos.BOTTOM_CENTER);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected double computePrefWidth(double height) {
        double bW = LayoutUtils.boundWidth(browseBtn);
        double mW = LayoutUtils.boundWidth(markAsBtn);
        double tW = LayoutUtils.boundWidth(tagBtn);
        return snappedLeftInset() + snapSizeX(bW + mW + tW + H_GAP * 2) + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        double bH = LayoutUtils.boundHeight(browseBtn);
        double mH = LayoutUtils.boundHeight(markAsBtn);
        double tH = LayoutUtils.boundHeight(tagBtn);
        return snappedTopInset() + snapSizeY(Math.max(Math.max(bH, mH), tH)) + snappedBottomInset();
    }

    @Override
    protected void layoutChildren() {
        double w = Math.max(computeMinWidth(-1), getWidth());
        double h = Math.max(computeMinHeight(-1), getHeight());

        browseBtn.autosize();
        markAsBtn.autosize();
        tagBtn.autosize();
        double yOffset = 4.0;

        positionInArea(
            tagBtn,
            0, yOffset, w, h, 0,
            HPos.RIGHT, VPos.CENTER
        );
        positionInArea(
            markAsBtn,
            0, yOffset, w - tagBtn.getWidth() - H_GAP, h, 0,
            HPos.RIGHT, VPos.CENTER
        );
        positionInArea(
            browseBtn,
            0, yOffset, w - tagBtn.getWidth() - markAsBtn.getWidth() - H_GAP * 2, h, 0,
            HPos.RIGHT, VPos.CENTER
        );
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public O getOwner() {
        return owner;
    }

    public boolean isShowingFor(O owner) {
        return this.owner == owner;
    }

    public boolean isShowingPopup() {
        return tagMenu.isShowing();
    }

    public Feed getFeed() {
        return Optional.ofNullable(feedSupplier)
            .map(Supplier::get)
            .orElse(null);
    }

    public Supplier<Feed> getFeedSupplier() {
        return feedSupplier;
    }

    public void setFeedSupplier(Supplier<Feed> feedSupplier) {
        this.feedSupplier = feedSupplier;
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    public interface OverlayOwner {
        void addOverlay(Node overlay);

        void removeOverlay(Node overlay);
    }
}
