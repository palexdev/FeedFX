package io.github.palexdev.feedfx.ui.components;

import io.github.palexdev.feedfx.ui.components.selection.ISelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.SelectionModel;
import io.github.palexdev.feedfx.ui.components.selection.WithSelectionModel;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import java.util.function.Function;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

public class SelectableList<T, C extends VFXCell<T>> extends VFXList<T, C> implements WithSelectionModel<T> {
    //================================================================================
    // Properties
    //================================================================================
    private final ISelectionModel<T> selectionModel = new SelectionModel<>(itemsProperty());

    //================================================================================
    // Constructors
    //================================================================================
    public SelectableList() {}

    public SelectableList(ObservableList<T> items, Function<T, C> cellFactory) {
        super(items, cellFactory);
    }

    public SelectableList(ObservableList<T> items, Function<T, C> cellFactory, Orientation orientation) {
        super(items, cellFactory, orientation);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public ISelectionModel<T> getSelectionModel() {
        return selectionModel;
    }
}
