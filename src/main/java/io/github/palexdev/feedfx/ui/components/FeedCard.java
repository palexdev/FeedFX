package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.FeedFX;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.model.Feed;
import io.github.palexdev.feedfx.ui.components.FeedCardOverlay.OverlayOwner;
import io.github.palexdev.imcache.cache.DiskCache;
import io.github.palexdev.imcache.core.ImCache;
import io.github.palexdev.imcache.core.ImRequest;
import io.github.palexdev.imcache.transforms.CenterCrop;
import io.github.palexdev.mfxcomponents.controls.progress.MFXProgressIndicator;
import io.github.palexdev.mfxcomponents.controls.progress.ProgressDisplayMode;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.controls.Text;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.NodeUtils;
import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import io.github.palexdev.mfxcore.utils.fx.TextMeasurementCache;
import io.github.palexdev.mfxeffects.enums.ElevationLevel;
import io.github.palexdev.rectcut.Rect;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.cells.VFXCellBase;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Future;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.tinylog.Logger;

public class FeedCard extends VFXCellBase<Feed> implements OverlayOwner {
    //================================================================================
    // Static
    //================================================================================
    private static final ImCache IM_CACHE = new ImCache();

    static {
        IM_CACHE.cacheConfig(() -> {
            DiskCache storage = new DiskCache();
            storage.saveTo(FeedFX.cacheDir().resolve("feeds"));
            storage.setCapacity(Integer.MAX_VALUE);
            return storage;
        });
    }

    //================================================================================
    // Constructors
    //================================================================================
    public FeedCard(Feed item) {
        super(item);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public List<String> defaultStyleClasses() {
        return List.of("cell-base", "feed-card");
    }

    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new FeedCardSkin(this);
    }

    @Override
    public void addOverlay(Node overlay) {
        if (overlay.getParent() == this) return;
        getChildren().add(overlay);
    }

    @Override
    public void removeOverlay(Node overlay) {
        getChildren().remove(overlay);
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    static class FeedCardSkin extends SkinBase<VFXCellBase<Feed>, CellBaseBehavior<Feed>> {
        // One overlay shared across the cells
        private static final FeedCardOverlay<FeedCard> overlay = new FeedCardOverlay<>();

        private final Label title;
        private final Label date;
        private final StackPane iwContainer;
        private final ImageView iw;
        private static final Image FALLBACK = new Image(
            Resources.loadStream("assets/placeholder.png")
        );

        protected double IMAGE_RADIUS = 12.0;
        protected double V_GAP = 12.0;

        protected final TextMeasurementCache tCache;
        protected Future<?> imTask;

        public FeedCardSkin(FeedCard cell) {
            super(cell);
            cell.setEffect(ElevationLevel.LEVEL1.toShadow());

            title = new Label();
            title.setWrapText(true);
            title.getStyleClass().add("title");

            tCache = new TextMeasurementCache(title) {
                {
                    bind(cell.widthProperty());
                }

                @Override
                protected Size computeValue() {
                    Text helper = new Text(title.getText());
                    helper.setFont(title.getFont());
                    helper.setCssWrappingWidth(cell.getWidth());
                    return NodeUtils.getNodeSizes(helper);
                }
            };

            iw = new ImageView();
            iw.setManaged(false);

            MFXProgressIndicator indicator = new MFXProgressIndicator();
            indicator.setDisplayMode(ProgressDisplayMode.CIRCULAR);
            indicator.visibleProperty().bind(iw.imageProperty().isNull());

            iwContainer = new StackPane(iw, indicator);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(iw.fitWidthProperty());
            clip.heightProperty().bind(iw.fitHeightProperty());
            clip.setArcWidth(IMAGE_RADIUS);
            clip.setArcHeight(IMAGE_RADIUS);
            iwContainer.setClip(clip);

            date = new Label();
            date.getStyleClass().add("date");

            getChildren().setAll(title, iwContainer, date);

            // Force first layout computation for the overlay
            if (overlay.getParent() == null) {
                overlay.setOpacity(0.0);
                getChildren().addFirst(overlay);
            }

            /* Listeners */
            listeners(
                When.onInvalidated(cell.itemProperty())
                    .then(_ -> update())
                    .executeNow(),
                When.onInvalidated(cell.hoverProperty())
                    .then(v -> {
                        if (overlay.isShowingPopup()) return;
                        if (!v) {
                            overlay.hide(cell);
                            overlay.setFeedSupplier(null);
                        } else {
                            overlay.show(cell);
                            overlay.setFeedSupplier(cell::getItem);
                        }
                    })
                    .executeNow(cell::isHover),
                When.onInvalidated(iw.fitWidthProperty())
                    .condition(_ -> cell.getItem() != null)
                    .then(_ -> loadImage(cell.getItem()))
                    .invalidating(iw.fitHeightProperty())
            );
        }

        protected void update() {
            VFXCellBase<Feed> cell = getSkinnable();
            Feed feed = cell.getItem();
            iw.setImage(null);
            PseudoClasses.setOn(cell, "read", feed != null && feed.read());

            if (feed == null) {
                title.setText("");
                date.setText("");
                return;
            }

            title.setText(feed.title());
            date.setText(feed.dateToString());
            loadImage(feed);
        }

        protected void loadImage(Feed feed) {
            double fw = iw.getFitWidth();
            double fh = iw.getFitHeight();
            if (fw <= 0 || fh <= 0) return;

            if (imTask != null && !imTask.isDone()) imTask.cancel(true);
            imTask = IM_CACHE.request(feed.img())
                .transform(new CenterCrop(fw, fh))
                .onStateChanged(r -> {
                    ImRequest.RequestState state = r.state();
                    switch (state) {
                        case SUCCEEDED, CACHE_HIT -> {
                            BufferedImage bImg = r.unwrapOut().asImage();
                            WritableImage fxImage = SwingFXUtils.toFXImage(bImg, null);
                            Platform.runLater(() -> iw.setImage(fxImage));
                        }
                        case FAILED -> {
                            Throwable err = r.error().optional().orElse(null);
                            if (err != null) {
                                Logger.trace(
                                    "Failed to load image for feed {} because:\n{}",
                                    feed.title(), err
                                );
                                Logger.trace("Extension is: {}", feed.getImageExtension());
                            }
                            Platform.runLater(() -> iw.setImage(FALLBACK));
                        }
                    }
                })
                .executeAsync();
        }

        @Override
        protected void initBehavior(CellBaseBehavior<Feed> behavior) {
            behavior.init();
            FeedCard cell = (FeedCard) getSkinnable();
            events(
                // Workaround for hover property not working under specific conditions for whatever reason
                // Thank you very much JavaFX
                WhenEvent.intercept(cell, MouseEvent.MOUSE_MOVED)
                    .process(e -> {
                        if (overlay.isShowingPopup()) return;
                        if (!overlay.isShowingFor(cell)) {
                            overlay.show(cell);
                            overlay.setFeedSupplier(cell::getItem);
                            e.consume();
                        }
                    }),
                WhenEvent.intercept(cell, MouseEvent.MOUSE_CLICKED)
                    .condition(e ->
                        e.getButton() == MouseButton.PRIMARY &&
                        e.getClickCount() % 2 == 0
                    )
                    .process(_ -> overlay.browse())
            );
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            Rect area = Rect.of(x, y, w, h)
                .withVSpacing(V_GAP)
                .withInsets(new double[]{
                    snappedTopInset(),
                    snappedRightInset(),
                    snappedBottomInset(),
                    snappedLeftInset()
                });

            // Top
            area.cutTop(Math.min(tCache.getSnappedHeight(), snapSizeY(h * 0.3)))
                .layout(title::resizeRelocate);

            // Bottom & Overlay
            Size ovSizes = Size.of(
                LayoutUtils.snappedBoundWidth(overlay),
                LayoutUtils.snappedBoundHeight(overlay)
            );

            Rect bottom = area.cutBottom(Math.max(LayoutUtils.snappedBoundHeight(date), ovSizes.getHeight()));
            Rect ovArea = bottom.cutRight(ovSizes.getWidth())
                .withInsets(new double[] {
                    0.0,
                    snappedRightInset(),
                    snappedBottomInset() * 2.0,
                    0.0
                });
            if (overlay.isShowingFor(((FeedCard) getSkinnable()))) ovArea.layout(overlay::resizeRelocate);
            bottom.layout(date::resizeRelocate);

            // Middle
            iw.setFitWidth(w - snappedRightInset() - snappedLeftInset());
            iw.setFitHeight(area.height());
            area.resize((_, rh) -> iwContainer.resize(iw.getFitWidth(), rh))
                .position(iwContainer::relocate);
        }
    }
}
