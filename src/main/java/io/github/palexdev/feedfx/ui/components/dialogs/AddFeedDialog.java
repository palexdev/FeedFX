package io.github.palexdev.feedfx.ui.components.dialogs;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import io.github.palexdev.architectfx.backend.loaders.UILoader;
import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.architectfx.backend.model.Initializable;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.model.AppModel;
import io.github.palexdev.feedfx.model.Feed;
import io.github.palexdev.feedfx.model.FeedsSource;
import io.github.palexdev.feedfx.ui.AppUILoader;
import io.github.palexdev.feedfx.ui.components.ComboBox;
import io.github.palexdev.feedfx.ui.components.FeedsSourceComboCell;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXButton;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

public class AddFeedDialog extends AddDialog<Feed> {

    //================================================================================
    // Constructors
    //================================================================================
    public AddFeedDialog() {
        loadContent();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected void loadContent() {
        try {
            JUIFXLoader loader = AppUILoader.instance();
            loader.config().setControllerFactory(Controller::new);
            UILoader.Loaded<Node> res = loader.load(Resources.loadURL("AddFeedDialog.jui"));
            setContent(res.root());
        } catch (IOException ex) {
            Logger.error("Failed to load dialog UI because:\n{}", ex);
        }
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    protected class Controller implements Initializable {
        private AppModel appModel;

        private ComboBox<FeedsSource> sourcesCombo;
        private TextField titleField;
        private TextField linkField;
        private TextField imgField;

        private MFXButton addButton;
        private MFXButton cancelButton;

        @Override
        public void initialize() {
            sourcesCombo.setCellFactory(FeedsSourceComboCell::new);

            addButton.disableProperty().bind(BooleanBindingBuilder.build()
                .setMapper(() -> !isValid())
                .addSources(titleField.textProperty(), linkField.textProperty())
                .addSources(sourcesCombo.getSelectionModel().selection())
                .get()
            );
            addButton.setOnAction(e -> close());
            cancelButton.setOnAction(e -> {
                reset();
                close();
            });

            addEventFilter(WindowEvent.WINDOW_SHOWING, _ -> {
                    reset();
                    sourcesCombo.getItems().setAll(appModel.getSourcesExclAll());
                }
            );
        }

        protected boolean isValid() {
            FeedsSource source = sourcesCombo.getSelectedItem();
            if (source == null) return false;

            String title = getTitle();
            if (title.isBlank()) return false;

            String url = getLink();
            if (url.isBlank()) return false;

            try {
                URI.create(url).toURL();
            } catch (Exception ex) {
                return false;
            }

            return true;
        }

        public void reset() {
            titleField.clear();
            linkField.clear();
            imgField.clear();
            result = null;
        }

        protected void close() {
            result = new Feed(
                getSourceId(),
                getTitle(),
                getLink(),
                getImage(),
                Instant.now().toEpochMilli()
            );
            hide();
        }

        public int getSourceId() {
            return Optional.ofNullable(sourcesCombo.getSelectedItem())
                .map(FeedsSource::id)
                .orElse(-2);
        }

        public String getTitle() {
            return titleField.getText();
        }

        public String getLink() {
            return linkField.getText();
        }

        public String getImage() {
            return imgField.getText();
        }
    }
}
