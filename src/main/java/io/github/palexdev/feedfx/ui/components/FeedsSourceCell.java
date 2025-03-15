package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.FeedFX;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.events.AppEvenBus;
import io.github.palexdev.feedfx.events.ModelEvent;
import io.github.palexdev.feedfx.model.FeedsSource;
import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.WithSelectionModel;
import io.github.palexdev.imcache.cache.DiskCache;
import io.github.palexdev.imcache.core.ImCache;
import io.github.palexdev.imcache.core.ImRequest;
import io.github.palexdev.mfxcomponents.controls.MaterialSurface;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXIconButton;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.mfxcore.utils.fx.SwingFXUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.cells.VFXLabeledCellSkin;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleCell;
import java.awt.image.BufferedImage;
import java.net.URI;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.tinylog.Logger;

public class FeedsSourceCell extends VFXSimpleCell<FeedsSource> {
    //================================================================================
    // Static
    //================================================================================
    private static final ImCache IM_CACHE = new ImCache();
    private static final Image FALLBACK_IMAGE = new Image(
        Resources.loadStream("assets/logo.png"),
        32.0,
        32.0,
        false,
        true
    );

    static {
        IM_CACHE.cacheConfig(() -> {
            DiskCache storage = new DiskCache();
            storage.saveTo(FeedFX.cacheDir().resolve("favicons"));
            storage.setCapacity(Integer.MAX_VALUE);
            return storage;
        });
    }

    //================================================================================
    // Properties
    //================================================================================
    private final BooleanProperty selected = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            PseudoClasses.SELECTED.setOn(FeedsSourceCell.this, get());
        }
    };

    //================================================================================
    // Constructors
    //================================================================================
    public FeedsSourceCell(FeedsSource item) {
        super(item, FunctionalStringConverter.to(FeedsSource::name));
    }

    //================================================================================
    // Methods
    //================================================================================
    private String getBaseUrl(String url) {
        if (url == null) return null;
        try {
            URI uri = URI.create(url);
            return uri.toURL().getProtocol() + "://" + uri.getHost();
        } catch (Exception ex) {
            Logger.error("Failed to get base URL because:\n{}", ex);
            return null;
        }
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public void onCreated(VFXContainer<FeedsSource> container) {
        super.onCreated(container);
        if (container instanceof WithSelectionModel<?> wsm) {
            ISelectionModel<FeedsSource> sm = (ISelectionModel<FeedsSource>) wsm.getSelectionModel();
            selected.bind(BooleanBindingBuilder.build()
                .setMapper(() -> sm.contains(getIndex()))
                .addSources(sm.selection(), indexProperty())
                .get()
            );
        }
    }

    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new VFXLabeledCellSkin<>(this) {
            MaterialSurface surface = new MaterialSurface(FeedsSourceCell.this);
            final MFXIconButton deleteBtn = new MFXIconButton();
            final ImageView logo = new ImageView(FALLBACK_IMAGE);

            {
                surface.getStates().add(new MaterialSurface.State(
                    0,
                    _ -> FeedsSourceCell.this.isSelected(),
                    _ -> 0.16
                ));
                surface.setManaged(false);
                getChildren().addFirst(surface);

                deleteBtn.visibleProperty().bind(hoverProperty().and(itemProperty().isNotEqualTo(FeedsSource.ALL)));
                deleteBtn.setManaged(false);
                deleteBtn.setOnAction(e ->
                    AppEvenBus.instance()
                        .publish(
                            new ModelEvent.DeleteSourceEvent(getItem())
                        )
                );
                getChildren().add(deleteBtn);

                logo.setFitWidth(32.0);
                logo.setFitHeight(32.0);
                setGraphic(logo);
            }

            @Override
            protected void update() {
                FeedsSource item = getItem();
                label.setText(getConverter().toString(item));

                if (item != null) {
                    String url = item.link();
                    String baseUrl = getBaseUrl(url);
                    if (baseUrl != null) {
                        String iconUrl = baseUrl + "/favicon.ico";
                        IM_CACHE.request(iconUrl)
                            .onStateChanged(r -> {
                                ImRequest.RequestState state = r.state();
                                switch (state) {
                                    case SUCCEEDED, CACHE_HIT -> {
                                        BufferedImage bImg = r.unwrapOut().asImage();
                                        WritableImage fxImage = SwingFXUtils.toFXImage(bImg, null);
                                        Platform.runLater(() -> logo.setImage(fxImage));
                                    }
                                    case FAILED -> Platform.runLater(() -> logo.setImage(FALLBACK_IMAGE));
                                    case null, default -> {}
                                }
                            })
                            .execute();
                    }
                }
            }

            @Override
            protected void initBehavior(CellBaseBehavior<FeedsSource> behavior) {
                super.initBehavior(behavior);
                events(
                    WhenEvent.intercept(FeedsSourceCell.this, MouseEvent.MOUSE_CLICKED)
                        .process(e -> {
                            if (e.getButton() == MouseButton.SECONDARY) return;
                            VFXContainer<FeedsSource> container = getContainer();
                            if (container instanceof WithSelectionModel<?> wsm) {
                                ISelectionModel<FeedsSource> sm = (ISelectionModel<FeedsSource>) wsm.getSelectionModel();
                                int index = getIndex();
                                boolean selected = sm.contains(index);
                                if (selected) return;
                                sm.selectIndex(index);
                            }
                        })
                );
            }

            @Override
            protected void layoutChildren(double x, double y, double w, double h) {
                surface.resizeRelocate(0, 0, getWidth(), getHeight());

                deleteBtn.autosize();
                positionInArea(deleteBtn, x, y, w, h, 0, HPos.RIGHT, VPos.CENTER);

                super.layoutChildren(x, y, w, h);
            }

            @Override
            public void dispose() {
                if (surface != null) {
                    surface.dispose();
                    surface = null;
                }
                super.dispose();
            }
        };
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public boolean isSelected() {
        return selected.get();
    }

    public ReadOnlyBooleanProperty selectedProperty() {
        return selected;
    }

    protected void setSelected(boolean selected) {
        this.selected.set(selected);
    }
}
