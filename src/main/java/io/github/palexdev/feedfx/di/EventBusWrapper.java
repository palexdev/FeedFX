package io.github.palexdev.feedfx.di;

import io.github.palexdev.feedfx.events.AppEvenBus;
import io.github.palexdev.mfxcore.events.bus.IEventBus;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Wrapper;
import java.util.function.Supplier;

@Bean
@Wrapper
public class EventBusWrapper implements Supplier<IEventBus> {
    private final IEventBus events = AppEvenBus.instance();

    @Override
    public IEventBus get() {
        return events;
    }
}
