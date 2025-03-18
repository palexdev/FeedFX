package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.events.AppEvenBus;
import io.github.palexdev.feedfx.events.ModelEvent;
import io.github.palexdev.feedfx.model.Tag;
import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.WithSelectionModel;
import io.github.palexdev.mfxcomponents.controls.MaterialSurface;
import io.github.palexdev.mfxcomponents.controls.buttons.MFXIconButton;
import io.github.palexdev.mfxcomponents.theming.enums.PseudoClasses;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.cells.VFXLabeledCellSkin;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class TagCell extends VFXSimpleCell<Tag> {
    //================================================================================
    // Properties
    //================================================================================
    private final BooleanProperty selected = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            PseudoClasses.SELECTED.setOn(TagCell.this, get());
        }
    };

    //================================================================================
    // Constructors
    //================================================================================
    public TagCell(Tag item) {
        super(item, FunctionalStringConverter.to(Tag::name));
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public void onCreated(VFXContainer<Tag> container) {
        super.onCreated(container);
        if (container instanceof WithSelectionModel<?> wsm) {
            ISelectionModel<Tag> sm = (ISelectionModel<Tag>) wsm.getSelectionModel();
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
            MaterialSurface surface = new MaterialSurface(TagCell.this);
            final MFXFontIcon icon = new MFXFontIcon();
            final MFXIconButton editBtn = new MFXIconButton();
            final MFXIconButton deleteBtn = new MFXIconButton();

            {
                surface.getStates().add(new MaterialSurface.State(
                    0,
                    _ -> TagCell.this.isSelected(),
                    _ -> 0.16
                ));
                surface.setManaged(false);
                getChildren().addFirst(surface);

                deleteBtn.getStyleClass().add("delete");
                deleteBtn.visibleProperty().bind(hoverProperty());
                deleteBtn.setManaged(false);
                deleteBtn.setOnAction(_ ->
                    AppEvenBus.instance()
                        .publish(
                            new ModelEvent.DeleteTagEvent(getItem())
                        )
                );

                editBtn.getStyleClass().add("edit");
                editBtn.visibleProperty().bind(hoverProperty());
                editBtn.setManaged(false);
                editBtn.setOnAction(_ ->
                    AppEvenBus.instance()
                        .publish(
                            new ModelEvent.EditTagEvent(getItem())
                        ));

                getChildren().addAll(editBtn, deleteBtn);
                setGraphic(icon);
                update();
            }

            @Override
            protected void update() {
                Tag item = getItem();
                if (icon != null) icon.setColor(Color.web(item.color()));
                label.setText(getConverter().toString(item));
            }

            @Override
            protected void initBehavior(CellBaseBehavior<Tag> behavior) {
                super.initBehavior(behavior);
                events(
                    WhenEvent.intercept(TagCell.this, MouseEvent.MOUSE_CLICKED)
                        .process(e -> {
                            if (e.getButton() == MouseButton.SECONDARY) return;
                            VFXContainer<Tag> container = getContainer();
                            if (container instanceof WithSelectionModel<?> wsm) {
                                ISelectionModel<Tag> sm = (ISelectionModel<Tag>) wsm.getSelectionModel();
                                int index = getIndex();
                                boolean selected = sm.contains(index);
                                if (selected) {
                                    sm.deselectIndex(index);
                                } else {
                                    sm.selectIndex(index);
                                }
                            }
                        })
                );
            }

            @Override
            protected void layoutChildren(double x, double y, double w, double h) {
                surface.resizeRelocate(0, 0, getWidth(), getHeight());

                editBtn.autosize();
                deleteBtn.autosize();

                positionInArea(editBtn, x - deleteBtn.getWidth() - 6.0, y, w, h, 0, HPos.RIGHT, VPos.CENTER);
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
