package io.github.palexdev.feedfx.ui.controllers;

import io.github.palexdev.architectfx.backend.model.Initializable;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.model.AppModel;
import io.github.palexdev.feedfx.model.Feed;
import io.github.palexdev.feedfx.model.FeedsSource;
import io.github.palexdev.feedfx.model.Tag;
import io.github.palexdev.feedfx.ui.components.FeedCard;
import io.github.palexdev.feedfx.ui.components.FeedsSourceCell;
import io.github.palexdev.feedfx.ui.components.SelectableList;
import io.github.palexdev.feedfx.ui.components.TagCell;
import io.github.palexdev.feedfx.ui.components.dialogs.AddFeedDialog;
import io.github.palexdev.feedfx.ui.components.dialogs.AddTagDialog;
import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXIconButton;
import io.github.palexdev.mfxcomponents.controls.progress.MFXProgressIndicator;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import java.util.Objects;
import javafx.beans.InvalidationListener;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UIController implements Initializable {
    private AppModel appModel;
    private Stage mainWindow;
    private StackPane root;

    // Header
    private ImageView iw;
    private Region separator;
    private MFXFontIcon aotIcon;
    private MFXFontIcon minIcon;
    private MFXFontIcon maxIcon;
    private MFXFontIcon clsIcon;

    // Sidebar
    private VBox sidebar;

    private AddFeedDialog addFeedDialog;
    private MFXIconButton addSrcButton;
    private MFXIconButton syncButton;
    private MFXIconButton showReadButton;
    private SelectableList<FeedsSource, FeedsSourceCell> sourcesList;

    private AddTagDialog addTagDialog;
    private MFXIconButton addTagButton;
    private SelectableList<Tag, TagCell> tagsList;

    // Content
    private StackPane scrim;
    private MFXProgressIndicator syncIndicator;
    private VFXGrid<Feed, FeedCard> feedsGrid;

    @Override
    public void initialize() {
        /* Init window controls */
        HeaderController hc = new HeaderController(mainWindow);
        hc.handleWindow(root, separator);
        hc.handleAot(aotIcon);
        hc.handleMinimize(minIcon);
        hc.handleMaximize(maxIcon);
        hc.handleClose(clsIcon);

        /* JavaFX sucks as always... */
        Image logo = new Image(
            Resources.load("assets/logo.png"),
            48,
            48,
            false,
            true
        );
        iw.setImage(logo);

        /* Sidebar */
        sidebar.minWidthProperty().bind(root.widthProperty()
            .multiply(0.3)
            .map(n -> Math.min(n.doubleValue(), 270.0))
        );

        ISelectionModel<FeedsSource> ssModel = sourcesList.getSelectionModel();
        sourcesList.setCellFactory(FeedsSourceCell::new);
        sourcesList.setItems(appModel.getSources().getView());
        ssModel.setAllowsMultipleSelection(false);
        ssModel.selectItem(FeedsSource.ALL);
        ssModel.selection().addListener((InvalidationListener) _ -> {
            if (ssModel.isEmpty()) {
                ssModel.selectIndex(0);
                return;
            }
            appModel.selectSource(ssModel.getSelectedItem());
        });
        addSrcButton.setOnAction(e -> addFeed());
        syncButton.setOnAction(e -> appModel.refresh(true));
        showReadButton.setOnAction(e -> appModel.setShowRead(showReadButton.isSelected()));

        ISelectionModel<Tag> tsModel = tagsList.getSelectionModel();
        tagsList.setCellFactory(TagCell::new);
        tagsList.setItems(appModel.getTags());
        tsModel.setAllowsMultipleSelection(false);
        tsModel.selection().addListener((InvalidationListener) _ -> {
            Tag sTag = tsModel.getSelectedItem();
            PseudoClasses.setOn(feedsGrid, "tagged", sTag != null);
            appModel.selectTag(sTag);
        });
        addTagButton.setOnAction(e -> addTag());

        // Content
        scrim.visibleProperty().bind(appModel.updatingProperty());
        syncIndicator.visibleProperty().bind(appModel.updatingProperty());

        feedsGrid.setCellFactory(FeedCard::new);
        feedsGrid.setItems(appModel.getFeeds().getView());
        When.onInvalidated(feedsGrid.widthProperty())
            .then(_ -> feedsGrid.autoArrange(1))
            .invalidating(feedsGrid.cellSizeProperty())
            .invalidating(feedsGrid.hSpacingProperty())
            .listen();

        /* FIXME improve on VirtualizedFX side */
        When.onInvalidated(feedsGrid.clipProperty())
            .condition(Objects::nonNull)
            .then(_ -> feedsGrid.setClip(null))
            .oneShot()
            .listen();
    }

    protected void addFeed() {
        if (addFeedDialog == null) {
            addFeedDialog = new AddFeedDialog();
            addFeedDialog.setScrimOwner(true);
            addFeedDialog.setDraggable(true);
        }
        addFeedDialog.showAndWaitOpt(mainWindow, Pos.CENTER)
            .ifPresent(t -> appModel.addSource(t.a(), t.b()));
    }

    protected void addTag() {
        if (addTagDialog == null) {
            addTagDialog = new AddTagDialog();
            addTagDialog.setScrimOwner(true);
            addTagDialog.setDraggable(true);
        }
        addTagDialog.showAndWaitOpt(mainWindow, Pos.CENTER)
            .ifPresent(t -> appModel.addTag(t.a(), t.b()));
    }
}
