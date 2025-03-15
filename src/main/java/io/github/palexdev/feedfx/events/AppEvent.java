package io.github.palexdev.feedfx.events;

import io.github.palexdev.mfxcore.events.Event;

public abstract class AppEvent extends Event {

    //================================================================================
    // Impl
    //================================================================================
    public static class AppCloseEvent extends AppEvent {}

    public static class AppReadyEvent extends AppEvent {}
}
