package io.github.palexdev.feedfx.events;

import io.github.palexdev.mfxcore.events.Event;

public abstract class UIEvent extends Event {

    //================================================================================
    // Constructors
    //================================================================================
    public UIEvent() {}

    public UIEvent(Object data) {
        super(data);
    }

    //================================================================================
    // Impl
    //================================================================================

    public static class ThemeSwitchEvent extends UIEvent {}
}