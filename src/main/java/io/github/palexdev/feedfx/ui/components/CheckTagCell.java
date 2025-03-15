package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.model.Tag;
import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.WithSelectionModel;
import io.github.palexdev.mfxcomponents.controls.MaterialSurface;
import io.github.palexdev.mfxcomponents.controls.checkbox.MFXCheckbox;
import io.github.palexdev.mfxcomponents.controls.checkbox.TriState;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.cells.VFXLabeledCellSkin;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleCell;
import java.util.Optional;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ContentDisplay;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class CheckTagCell extends VFXSimpleCell<Tag> {
    //================================================================================
    // Properties
    //================================================================================
    private final BooleanProperty selected = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            PseudoClasses.SELECTED.setOn(CheckTagCell.this, get());
        }
    };

    //================================================================================
    // Constructors
    //================================================================================
    public CheckTagCell(Tag item) {
        super(item, FunctionalStringConverter.to(Tag::name));
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public void onCreated(VFXContainer<Tag> container) {
        super.onCreated(container);
        getSelectionModel().ifPresent(sm -> {
            selected.bind(BooleanBindingBuilder.build()
                .setMapper(() -> sm.contains(getIndex()))
                .addSources(sm.selection(), indexProperty())
                .get()
            );
        });
    }

    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new VFXLabeledCellSkin<>(this) {
            MaterialSurface surface = new MaterialSurface(CheckTagCell.this);
            final MFXFontIcon icon = new MFXFontIcon();
            final MFXCheckbox checkBox = new MFXCheckbox();

            {
                surface.getStates().add(new MaterialSurface.State(
                    0,
                    _ -> CheckTagCell.this.isSelected(),
                    _ -> 0.16
                ));
                surface.setManaged(false);
                getChildren().addFirst(surface);

                checkBox.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                getSelectionModel().ifPresent(sm ->
                    checkBox.stateProperty().bind(ObjectBindingBuilder.<TriState>build()
                        .setMapper(() -> isSelected() ? TriState.SELECTED : TriState.UNSELECTED)
                        .addSources(selectedProperty())
                        .get()
                    )
                );

                icon.colorProperty().bind(itemProperty().map(t -> Color.web(t.color())));
                setGraphic(new HBox(checkBox, icon));
            }

            @Override
            protected void update() {
                Tag item = getItem();
                label.setText(getConverter().toString(item));
            }

            @Override
            protected void initBehavior(CellBaseBehavior<Tag> behavior) {
                super.initBehavior(behavior);
                events(
                    WhenEvent.intercept(checkBox, MouseEvent.MOUSE_CLICKED)
                        .process(e -> {
                            if (e.getButton() == MouseButton.PRIMARY) {
                                getSelectionModel().ifPresent(sm -> {
                                    int index = getIndex();
                                    boolean selected = sm.contains(index);
                                    if (selected) {
                                        sm.deselectIndex(index);
                                    } else {
                                        sm.selectIndex(index);
                                    }
                                });
                            }
                            e.consume();
                        })
                        .asFilter()
                );
            }

            @Override
            protected void layoutChildren(double x, double y, double w, double h) {
                surface.resizeRelocate(0, 0, getWidth(), getHeight());
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

    @SuppressWarnings("unchecked")
    protected Optional<ISelectionModel<Tag>> getSelectionModel() {
        return Optional.ofNullable(getContainer())
            .filter(c -> c instanceof WithSelectionModel<?>)
            .map(c -> ((WithSelectionModel<Tag>) c).getSelectionModel());
    }
}
