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

    public static class MinimizeEvent extends UIEvent {}

    public static class NotifyEvent extends UIEvent {
        public NotifyEvent(String message) {
            super(message);
        }

        @Override
        public String data() {
            return (String) super.data();
        }
    }

    public static class ThemeSwitchEvent extends UIEvent {}
}