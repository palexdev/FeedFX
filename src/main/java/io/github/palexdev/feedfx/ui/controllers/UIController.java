package io.github.palexdev.feedfx.ui.controllers;

import java.util.Objects;

import io.github.palexdev.architectfx.backend.model.Initializable;
import io.github.palexdev.feedfx.FeedFX;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.events.AppEvenBus;
import io.github.palexdev.feedfx.events.ModelEvent;
import io.github.palexdev.feedfx.model.AppModel;
import io.github.palexdev.feedfx.model.Feed;
import io.github.palexdev.feedfx.model.FeedsSource;
import io.github.palexdev.feedfx.model.Tag;
import io.github.palexdev.feedfx.theming.ThemeEngine;
import io.github.palexdev.feedfx.ui.components.FeedCard;
import io.github.palexdev.feedfx.ui.components.FeedsSourceCell;
import io.github.palexdev.feedfx.ui.components.SelectableList;
import io.github.palexdev.feedfx.ui.components.TagCell;
import io.github.palexdev.feedfx.ui.components.dialogs.AddEditTagDialog;
import io.github.palexdev.feedfx.ui.components.dialogs.AddFeedDialog;
import io.github.palexdev.feedfx.ui.components.dialogs.AddSourceDialog;
import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.feedfx.utils.UIUtils;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXIconButton;
import io.github.palexdev.mfxcomponents.controls.fab.MFXFab;
import io.github.palexdev.mfxcomponents.controls.progress.MFXProgressIndicator;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import javafx.application.HostServices;
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
    private HostServices  hostServices;
    private ThemeEngine themeEngine;
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

    private AddSourceDialog addSrcDialog;
    private MFXIconButton addSrcButton;
    private MFXIconButton syncButton;
    private MFXIconButton showReadButton;
    private SelectableList<FeedsSource, FeedsSourceCell> sourcesList;

    private AddEditTagDialog tagDialog;
    private MFXIconButton addTagButton;
    private SelectableList<Tag, TagCell> tagsList;

    // Content
    private StackPane scrim;
    private MFXProgressIndicator syncIndicator;
    private VFXGrid<Feed, FeedCard> feedsGrid;

    private AddFeedDialog addFeedDialog;
    private MFXFab addFeedBtn;

    // Actions
    private MFXIconButton updateBtn;
    private MFXIconButton themeBtn;
    private MFXIconButton settingsBtn;

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
            .map(n -> NumberUtils.clamp(n.doubleValue(), 300.0, 360.0))
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
        addSrcButton.setOnAction(e -> addSource());
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
        addTagButton.setOnAction(_ -> addEditTag(null));
        AppEvenBus.instance().subscribe(ModelEvent.EditTagEvent.class, e -> addEditTag(e.data()));

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

        addFeedBtn.setOnAction(e -> addFeed());

        // Actions
        updateBtn.disableProperty().bind(FeedFX.updateAvailableProperty().not());
        updateBtn.setOnAction(_ -> hostServices.showDocument(FeedFX.RELEASES_PAGE));
        UIUtils.installTooltip(updateBtn, "Update Available", Pos.BOTTOM_CENTER);

        themeBtn.setOnAction(_ -> themeEngine.nextMode());
        UIUtils.installTooltip(themeBtn, "Switch Theme Mode", Pos.BOTTOM_CENTER);

        settingsBtn.setDisable(true);
        /* TODO implement settings */
    }

    protected void addSource() {
        if (addSrcDialog == null) {
            addSrcDialog = new AddSourceDialog();
            addSrcDialog.setScrimOwner(true);
            addSrcDialog.setDraggable(true);
        }
        addSrcDialog.showAndWaitOpt(mainWindow, Pos.CENTER)
            .ifPresent(t -> appModel.addSource(t.a(), t.b()));
    }

    protected void addEditTag(Tag tag) {
        if (tagDialog == null) {
            tagDialog = new AddEditTagDialog();
            tagDialog.setScrimOwner(true);
            tagDialog.setDraggable(true);
        }
        tagDialog.setTagToEdit(tag);
        tagDialog.showAndWaitOpt(mainWindow, Pos.CENTER)
            .ifPresent(t -> {
                if (t.a() != null) {
                    appModel.editTag(t.a(), t.b(), t.c());
                } else {
                    appModel.addTag(t.b(), t.c());
                }
            });
    }

    protected void addFeed() {
        if (addFeedDialog == null) {
            addFeedDialog = new AddFeedDialog();
            addFeedDialog.setScrimOwner(true);
            addFeedDialog.setDraggable(true);
        }
        addFeedDialog.showAndWaitOpt(mainWindow, Pos.CENTER)
            .ifPresent(f -> {
                appModel.addFeeds(f);
                appModel.refresh(true);
            });
    }
}
