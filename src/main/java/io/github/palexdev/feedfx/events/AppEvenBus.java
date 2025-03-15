package io.github.palexdev.feedfx.events;

import io.github.palexdev.mfxcore.events.bus.SimpleEventBus;

/*
 * Making a singleton allows to use the bus in JavaFX controls too which are not reachable by the Inverno Framework.
 *
 * Not the ideal solution but faster to implement compared to custom JavaFX events.
 */
public class AppEvenBus extends SimpleEventBus {
    //================================================================================
    // Singleton
    //================================================================================
    private static final AppEvenBus instance = new AppEvenBus();

    public static AppEvenBus instance() {
        return instance;
    }
}
