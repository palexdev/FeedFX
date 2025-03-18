package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.model.FeedsSource;
import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.WithSelectionModel;
import io.github.palexdev.mfxcomponents.controls.MaterialSurface;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.VFXLabeledCellSkin;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleCell;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class FeedsSourceComboCell extends VFXSimpleCell<FeedsSource> {
    //================================================================================
    // Properties
    //================================================================================
    private final BooleanProperty selected = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            PseudoClasses.SELECTED.setOn(FeedsSourceComboCell.this, get());
        }
    };

    //================================================================================
    // Constructors
    //================================================================================
    public FeedsSourceComboCell(FeedsSource item) {
        super(item, FunctionalStringConverter.to(s ->
            s != null ? s.name() : ""
        ));
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public void onCreated(VFXContainer<FeedsSource> container) {
        super.onCreated(container);
        if (container instanceof WithSelectionModel<?>) {
            ISelectionModel<FeedsSource> sm = ((WithSelectionModel<FeedsSource>) container).getSelectionModel();
            selected.bind(BooleanBindingBuilder.build()
                .setMapper(() -> sm.contains(getIndex()))
                .addSources(sm.selection(), indexProperty())
                .get());
            WhenEvent.intercept(FeedsSourceComboCell.this, MouseEvent.MOUSE_CLICKED)
                .condition(e -> e.getButton() == MouseButton.PRIMARY)
                .process(_ -> {
                    int index = getIndex();
                    if (index < 0) return;
                    sm.selectIndex(index);
                })
                .register();
        }
    }

    @Override
    public List<String> defaultStyleClasses() {
        return List.of("src-combo-cell");
    }

    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new VFXLabeledCellSkin<>(this) {
            MaterialSurface surface = new MaterialSurface(FeedsSourceComboCell.this);

            {
                surface.getStates().add(new MaterialSurface.State(
                    0,
                    _ -> FeedsSourceComboCell.this.isSelected(),
                    _ -> 0.16
                ));
                surface.setManaged(false);
                getChildren().addFirst(surface);
            }

            @Override
            protected void update() {
                FeedsSource item = getItem();
                label.setText(getConverter().toString(item));
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
}
