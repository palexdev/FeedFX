package io.github.palexdev.feedfx.ui.components.dialogs;

import io.github.palexdev.architectfx.backend.loaders.UILoader;
import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.architectfx.backend.model.Initializable;
import io.github.palexdev.architectfx.backend.utils.Tuple2;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.model.Tag;
import io.github.palexdev.feedfx.ui.AppUILoader;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXButton;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import java.io.IOException;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

/* TODO allow tag edit */
public class AddTagDialog extends AddDialog {

    //================================================================================
    // Constructors
    //================================================================================
    public AddTagDialog() {
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
            UILoader.Loaded<Node> res = loader.load(Resources.loadURL("AddTagDialog.jui"));
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
        private TextField colorField;

        private MFXButton addButton;
        private MFXButton cancelButton;

        @Override
        public void initialize() {
            addButton.disableProperty().bind(BooleanBindingBuilder.build()
                .setMapper(() -> !isValid())
                .addSources(nameField.textProperty(), colorField.textProperty())
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
            colorField.clear();
            result = null;
        }

        public String getName() {
            return nameField.getText();
        }

        public String getColor() {
            String color = colorField.getText();
            if (color == null || color.isBlank()) color = Tag.DEFAULT_COLOR;
            return color;
        }

        protected boolean isValid() {
            String name = getName();
            if (name.isBlank()) return false;

            try {
                Color.web(getColor());
            } catch (Exception ex) {
                return false;
            }
            return true;
        }

        protected void close() {
            result = Tuple2.of(getName(), getColor());
            hide();
        }
    }
}
