package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.ui.components.ComboBox.ComboBoxBehavior;
import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.SelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.WithSelectionModel;
import io.github.palexdev.mfxcomponents.controls.base.MFXSkinBase;
import io.github.palexdev.mfxcomponents.controls.base.MFXStyleable;
import io.github.palexdev.mfxcomponents.window.popups.MFXPopup;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class ComboBox<T> extends Control<ComboBoxBehavior<T>> implements WithSelectionModel<T>, MFXStyleable {
    //================================================================================
    // Properties
    //================================================================================
    private final ObservableList<T> items;
    private final ISelectionModel<T> selectionModel;
    private final FunctionProperty<T, VFXCell<T>> cellFactory = new FunctionProperty<>();

    //================================================================================
    // Constructors
    //================================================================================
    public ComboBox() {
        this(FXCollections.observableArrayList());
    }

    public ComboBox(ObservableList<T> items) {
        this.items = items;
        this.selectionModel = new SelectionModel<>(items);
        selectionModel.setAllowsMultipleSelection(false);
        getStyleClass().setAll(defaultStyleClasses());
        setDefaultBehaviorProvider();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new ComboBoxSkin<>(this);
    }

    @Override
    public Supplier<ComboBoxBehavior<T>> defaultBehaviorProvider() {
        return () -> new ComboBoxBehavior<>(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return List.of("combo-box");
    }

    @Override
    public ISelectionModel<T> getSelectionModel() {
        return selectionModel;
    }

    //================================================================================
    // Getters/Setters
    //================================================================================
    public ObservableList<T> getItems() {
        return items;
    }

    public T getSelectedItem() {
        return selectionModel.getSelectedItem();
    }

    public Function<T, VFXCell<T>> getCellFactory() {
        return cellFactory.get();
    }

    public FunctionProperty<T, VFXCell<T>> cellFactoryProperty() {
        return cellFactory;
    }

    public void setCellFactory(Function<T, VFXCell<T>> cellFactory) {
        this.cellFactory.set(cellFactory);
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    protected static class ComboBoxSkin<T> extends MFXSkinBase<ComboBox<T>, ComboBoxBehavior<T>> {
        private VFXCell<T> comboCell;
        private final MFXFontIcon arrowIcon;

        private SelectableList<T, VFXCell<T>> list;
        private MFXPopup popup;

        protected ComboBoxSkin(ComboBox<T> combo) {
            super(combo);

            createPopup();

            arrowIcon = new MFXFontIcon("fas-caret-down");
            arrowIcon.setViewOrder(-1);
            getChildren().add(arrowIcon);

            listeners(
                When.onInvalidated(combo.getSelectionModel().selection())
                    .then(_ -> updateComboCell())
                    .executeNow(),
                When.onInvalidated(list.cellSizeProperty())
                    .then(_ -> combo.requestLayout())
            );
        }

        protected void updateComboCell() {
            ComboBox<T> combo = getSkinnable();
            CellFactory<T, VFXCell<T>> factory = list.getCellFactory();
            if (!factory.canCreate()) return;
            if (comboCell == null) {
                comboCell = factory.create(null);
                getChildren().add(comboCell.toNode());
            }
            comboCell.updateItem(combo.getSelectedItem());
            popup.hide();
        }

        protected void createPopup() {
            ComboBox<T> combo = getSkinnable();
            list = new SelectableList<>(combo.getItems(), null) {
                @Override
                public ISelectionModel<T> getSelectionModel() {
                    return combo.getSelectionModel();
                }
            };
            list.getCellFactory().bind(combo.cellFactoryProperty());
            popup = new MFXPopup() {
                {
                    When.onInvalidated(showingProperty())
                        .condition(s -> s)
                        .then(_ -> reposition())
                        .invalidating(contentBoundsProperty())
                        .listen();
                }

                @Override
                protected void show() {
                    arrowIcon.setRotate(180.0);
                    super.show();
                }

                @Override
                public void hide() {
                    arrowIcon.setRotate(0.0);
                    super.hide();
                }
            };
            popup.setConsumeAutoHidingEvents(true);
            popup.prefHeightProperty().bind(list.cellSizeProperty().multiply(5));
            popup.setContent(list.makeScrollable());
        }

        @Override
        protected void initBehavior(ComboBoxBehavior<T> behavior) {
            super.initBehavior(behavior);
            ComboBox<T> combo = getSkinnable();
            events(
                WhenEvent.intercept(combo, MouseEvent.MOUSE_PRESSED)
                    .process(_ -> {
                        popup.hide();
                        combo.requestFocus();
                    }),
                WhenEvent.intercept(arrowIcon, MouseEvent.MOUSE_CLICKED)
                    .condition(e -> e.getButton() == MouseButton.PRIMARY)
                    .process(_ -> {
                        if (popup.isShowing()) {
                            popup.hide();
                        } else if (!combo.getItems().isEmpty()) {
                            popup.show(combo, Pos.BOTTOM_CENTER);
                        }
                    })
            );
        }

        @Override
        public double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
            return leftInset + 200.0 + rightInset;
        }

        @Override
        public double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
            return topInset + list.getCellSize() + bottomInset;
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            if (comboCell != null) {
                Node cellNode = comboCell.toNode();
                layoutInArea(cellNode, x, y, w, h, 0, HPos.LEFT, VPos.CENTER);
            }
            layoutInArea(arrowIcon, x, y, w, h, 0, HPos.RIGHT, VPos.CENTER);
        }
    }

    protected static class ComboBoxBehavior<T> extends BehaviorBase<ComboBox<T>> {
        public ComboBoxBehavior(ComboBox<T> combo) {
            super(combo);
        }
    }
}
