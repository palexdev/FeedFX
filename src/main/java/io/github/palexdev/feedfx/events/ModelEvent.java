package io.github.palexdev.feedfx.events;

import io.github.palexdev.feedfx.model.Feed;
import io.github.palexdev.feedfx.model.FeedsSource;
import io.github.palexdev.feedfx.model.Tag;
import io.github.palexdev.mfxcore.events.Event;

public abstract class ModelEvent extends Event {

    //================================================================================
    // Constructors
    //================================================================================
    public ModelEvent() {
    }

    public ModelEvent(Object data) {
        super(data);
    }


    //================================================================================
    // Impl
    //================================================================================
    public static class DeleteSourceEvent extends ModelEvent {
        public DeleteSourceEvent(FeedsSource source) {
            super(source);
        }

        @Override
        public FeedsSource data() {
            return (FeedsSource) super.data();
        }
    }

    public static class DeleteTagEvent extends ModelEvent {
        public DeleteTagEvent(Tag tag) {
            super(tag);
        }

        @Override
        public Tag data() {
            return (Tag) super.data();
        }
    }

    public static class OpenFeedEvent extends ModelEvent {
        public OpenFeedEvent(Feed feed) {
            super(feed);
        }

        @Override
        public Feed data() {
            return (Feed) super.data();
        }
    }

    public static class MarkFeedEvent extends ModelEvent {
        public MarkFeedEvent(Feed feed) {
            super(feed);
        }

        @Override
        public Feed data() {
            return (Feed) super.data();
        }
    }
}
