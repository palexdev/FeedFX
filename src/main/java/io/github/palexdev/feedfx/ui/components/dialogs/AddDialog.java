package io.github.palexdev.feedfx.ui.components.dialogs;

public abstract class AddDialog<R> extends MFXDialog<R> {
    //================================================================================
    // Properties
    //================================================================================
    protected R result;

    //================================================================================
    // Abstract Methods
    //================================================================================
    protected abstract void loadContent();

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public R getResult() {
        return result;
    }
}
