package io.github.palexdev.feedfx.ui.components.dialogs;

import io.github.palexdev.architectfx.backend.utils.Tuple2;

public abstract class AddDialog extends MFXDialog<Tuple2<String, String>> {
    //================================================================================
    // Properties
    //================================================================================
    protected Tuple2<String, String> result;

    //================================================================================
    // Abstract Methods
    //================================================================================
    protected abstract void loadContent();

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public Tuple2<String, String> getResult() {
        return result;
    }
}
