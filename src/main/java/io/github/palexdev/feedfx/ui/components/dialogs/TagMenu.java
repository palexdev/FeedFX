package io.github.palexdev.feedfx.ui.components.dialogs;

import io.github.palexdev.architectfx.backend.loaders.UILoader;
import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.architectfx.backend.model.Initializable;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.model.AppModel;
import io.github.palexdev.feedfx.model.Feed;
import io.github.palexdev.feedfx.model.Tag;
import io.github.palexdev.feedfx.ui.AppUILoader;
import io.github.palexdev.feedfx.ui.components.CheckTagCell;
import io.github.palexdev.feedfx.ui.components.SelectableList;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXButton;
import io.github.palexdev.mfxcomponents.window.popups.MFXPopup;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

public class TagMenu extends MFXPopup {
    //================================================================================
    // Properties
    //================================================================================
    private Feed feed;

    //================================================================================
    // Constructors
    //================================================================================
    public TagMenu() {
        loadContent();
    }

    //================================================================================
    // Methods
    //================================================================================
    protected void loadContent() {
        try {
            JUIFXLoader loader = AppUILoader.instance();
            loader.config().setControllerFactory(Controller::new);
            UILoader.Loaded<Node> res = loader.load(Resources.loadURL("TagMenu.jui"));
            setContent(res.root());
        } catch (IOException ex) {
            Logger.error("Failed to load menu UI because:\n{}", ex);
        }
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    public class Controller implements Initializable {
        private AppModel appModel;
        private SelectableList<Tag, CheckTagCell> tagsList;
        private MFXButton confirmButton;
        private MFXButton cancelButton;

        @Override
        public void initialize() {
            tagsList.setCellFactory(CheckTagCell::new);
            tagsList.itemsProperty().bind(ObjectBindingBuilder.<ObservableList<Tag>>build()
                .setMapper(() -> {
                    Set<Tag> tags = new LinkedHashSet<>(appModel.getTags());
                    return FXCollections.observableArrayList(tags);
                })
                .addSources(appModel.getTags())
                .get()
            );

            confirmButton.setOnAction(_ -> {
                if (feed == null) throw new NullPointerException("Feed should not be null!");
                List<Tag> tags = tagsList.getSelectionModel().getSelectedItems();
                appModel.tagFeed(feed, tags.toArray(Tag[]::new));
                hide();
            });
            cancelButton.setOnAction(_ -> {
                hide();
                tagsList.getSelectionModel().clearSelection();
            });

            addEventFilter(WindowEvent.WINDOW_SHOWING, _ -> {
                if (feed == null) throw new NullPointerException("Feed should not be null!");
                List<Tag> tags = appModel.getTagsForFeed(feed);
                tagsList.getSelectionModel().replaceSelection(tags.toArray(Tag[]::new));
            });
        }
    }
}
