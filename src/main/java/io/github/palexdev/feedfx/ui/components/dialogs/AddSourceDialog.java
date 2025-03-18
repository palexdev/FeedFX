package io.github.palexdev.feedfx.ui.components.dialogs;

import io.github.palexdev.architectfx.backend.loaders.UILoader;
import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.architectfx.backend.model.Initializable;
import io.github.palexdev.architectfx.backend.utils.Tuple2;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.ui.AppUILoader;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXButton;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import java.io.IOException;
import java.net.URI;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

public class AddSourceDialog extends AddDialog<Tuple2<String, String>> {

    //================================================================================
    // Constructors
    //================================================================================
    public AddSourceDialog() {
        loadContent();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    protected void loadContent() {
        try {
            JUIFXLoader loader = AppUILoader.instance();
            loader.config().setControllerFactory(Controller::new);
            UILoader.Loaded<Node> res = loader.load(Resources.loadURL("AddSourceDialog.jui"));
            setContent(res.root());
        } catch (IOException ex) {
            Logger.error("Failed to load dialog UI because:\n{}", ex);
        }
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    protected class Controller implements Initializable {
        private StackPane header;

        private TextField nameField;
        private TextField urlField;

        private MFXButton addButton;
        private MFXButton cancelButton;

        @Override
        public void initialize() {
            addButton.disableProperty().bind(BooleanBindingBuilder.build()
                .setMapper(() -> !isValid())
                .addSources(nameField.textProperty(), urlField.textProperty())
                .get()
            );
            addButton.setOnAction(e -> close());

            cancelButton.setOnAction(e -> {
                reset();
                close();
            });

            addEventFilter(WindowEvent.WINDOW_SHOWING, e -> {
                makeDraggable(header);
                reset();
            });
        }

        public void reset() {
            nameField.clear();
            urlField.clear();
            result = null;
        }

        public String getName() {
            return nameField.getText();
        }

        public String getURL() {
            return urlField.getText();
        }

        protected boolean isValid() {
            String name = getName();
            if (name.isBlank()) return false;

            String url = getURL();
            if (url.isBlank()) return false;

            try {
                URI.create(url).toURL();
            } catch (Exception ex) {
                return false;
            }

            return true;
        }

        protected void close() {
            result = Tuple2.of(getName(), getURL());
            hide();
        }
    }
}
