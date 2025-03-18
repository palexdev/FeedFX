package io.github.palexdev.feedfx.ui.components.dialogs;

import java.io.IOException;

import io.github.palexdev.architectfx.backend.loaders.UILoader;
import io.github.palexdev.architectfx.backend.loaders.jui.JUIFXLoader;
import io.github.palexdev.architectfx.backend.model.Initializable;
import io.github.palexdev.architectfx.backend.utils.Tuple2;
import io.github.palexdev.feedfx.Resources;
import io.github.palexdev.feedfx.ui.AppUILoader;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXButton;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import org.tinylog.Logger;

/* TODO allow tag edit */
public class AddTagDialog extends AddDialog<Tuple2<String, String>> {

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
        private MFXFontIcon colorIcon;
        private TextField colorField;

        private MFXButton addButton;
        private MFXButton cancelButton;

        @Override
        public void initialize() {
            colorIcon.setOnMouseClicked(_ -> colorField.setText(randomColor()));
            colorIcon.colorProperty().bind(ObjectBindingBuilder.<Color>build()
                .setMapper(() -> {
                    String colorString = getColor();
                    try {
                        return Color.web(colorString);
                    } catch (Exception ex) {
                        return Color.GRAY;
                    }
                })
                .addSources(colorField.textProperty())
                .get()
            );

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

            addEventFilter(WindowEvent.WINDOW_SHOWING, _ -> {
                makeDraggable(header);
                reset();
            });
        }

        public void reset() {
            nameField.clear();
            colorField.setText(randomColor());
            result = null;
        }

        public String getName() {
            return nameField.getText();
        }

        public String getColor() {
            String color = colorField.getText();
            if (color == null || color.isBlank()) color = randomColor();
            return color;
        }

        protected String randomColor() {
            return ColorUtils.toWeb(
                ColorUtils.getRandomColor()
            );
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
