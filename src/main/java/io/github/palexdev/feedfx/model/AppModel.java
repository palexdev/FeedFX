package io.github.palexdev.feedfx.model;

import io.github.palexdev.architectfx.backend.utils.Async;
import io.github.palexdev.feedfx.events.ModelEvent;
import io.github.palexdev.feedfx.utils.RefineList;
import io.github.palexdev.mfxcore.events.bus.IEventBus;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.BeanSocket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Bean
public class AppModel {
    //================================================================================
    // Properties
    //================================================================================
    private final DBManager dbManager;
    private final FeedHandler feedHandler;
    private final HostServices hostServices;

    private FeedsSource selectedSource = FeedsSource.ALL;
    private final RefineList<FeedsSource> sources = new RefineList<>(FXCollections.observableArrayList());

    private Tag selectedTag;
    private final ObservableList<Tag> tags = FXCollections.observableArrayList();

    private final RefineList<Feed> feeds = new RefineList<>(FXCollections.observableArrayList());
    private boolean showRead = false;

    private final BooleanProperty updating = new SimpleBooleanProperty(false);

    //================================================================================
    // Constructors
    //================================================================================
    public AppModel(IEventBus bus, DBManager dbManager, FeedHandler feedHandler, HostServices hostServices) {
        this.dbManager = dbManager;
        this.feedHandler = feedHandler;
        this.hostServices = hostServices;
        init();

        bus.subscribe(ModelEvent.DeleteSourceEvent.class, e -> deleteSource(e.data()));
        bus.subscribe(ModelEvent.DeleteTagEvent.class, e -> deleteTag(e.data()));

        bus.subscribe(ModelEvent.OpenFeedEvent.class, e -> open(e.data()));
        bus.subscribe(ModelEvent.MarkFeedEvent.class, e -> markFeedAs(e.data(), !e.data().read()));

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual()
                .name("Feeds Fetch Thread")
                .factory()
        );
        scheduler.scheduleWithFixedDelay(
            () -> refresh(false),
            1L,
            1L,
            TimeUnit.HOURS
        );
    }

    //================================================================================
    // Methods
    //================================================================================
    private void init() {
        sources.setComparator(FeedsSource.DEFAULT_COMPARATOR);
        sources.add(FeedsSource.ALL);
        sources.addAll(dbManager.getSources());

        tags.addAll(dbManager.getTags());

        feeds.setComparator(Feed.DEFAULT_COMPARATOR);
        refresh(true);
    }

    protected CompletableFuture<Boolean> fetch() {
        setUpdating(true);
        return Async.call(() -> {
            boolean updated = false;
            for (FeedsSource source : sources.subList(1, sources.size())) { // Exclude ALL
                List<Feed> feeds = feedHandler.fetch(source);
                updated |= dbManager.addFeeds(feeds.toArray(Feed[]::new));
            }
            return updated;
        });
    }

    protected void update() {
        Async.call(() -> dbManager.getFeeds(selectedSource, selectedTag, isShowRead()))
            .thenAccept(l -> Platform.runLater(() -> {
                feeds.setAll(l);
                feeds.setPredicate(f -> isShowRead() || !f.read()); // Triggers list update
                setUpdating(false);
            }));
    }

    public void refresh(boolean force) {
        fetch().thenAccept(u -> {
            if (u || force) {
                update();
            }
            setUpdating(false);
        });
    }

    public void open(Feed feed) {
        /* TODO Add in-app browser popup */
        hostServices.showDocument(feed.link());
        markFeedAs(feed, true);
    }

    public void markFeedAs(Feed feed, boolean read) {
        boolean res = dbManager.markFeedAs(feed, read);
        // Update only if operation on database is successful
        if (res != feed.read()) {
            feed.setRead(read);
            update();
        }
    }

    public void addSource(String name, String url) {
        if (!name.isBlank() && !url.isBlank()) {
            dbManager.addSource(name, url)
                .ifPresent(s -> {
                    sources.add(s);
                    refresh(selectedSource == FeedsSource.ALL);
                });
        }
    }

    public void selectSource(FeedsSource source) {
        if (this.selectedSource != source) {
            this.selectedSource = source;
            update();
        }
    }

    public void deleteSource(FeedsSource source) {
        Optional.ofNullable(source)
            .filter(dbManager::deleteSource)
            .ifPresent(s -> {
                if (selectedSource == s) {
                    // Prevents trigger of selectSource() when deleting selected source
                    selectedSource = FeedsSource.ALL;
                    sources.remove(s);
                    update();
                } else if (selectedSource == FeedsSource.ALL) {
                    sources.remove(s);
                    update();
                } else {
                    sources.remove(s);
                }
            });
    }

    public void addTag(String name, String color) {
        if (!name.isBlank() && !color.isBlank()) {
            dbManager.addTag(name, color)
                .ifPresent(tags::add);
        }
    }

    public void selectTag(Tag tag) {
        if (this.selectedTag != tag) {
            this.selectedTag = tag;
            update();
        }
    }

    public void deleteTag(Tag tag) {
        Optional.ofNullable(tag)
            .filter(dbManager::deleteTag)
            .ifPresent(tags::remove);
    }

    public void tagFeed(Feed feed, Tag... tags) {
        Async.run(() -> dbManager.tagFeed(feed, tags))
            .thenRunAsync(this::update);
    }

    public List<Tag> getTagsForFeed(Feed feed) {
        return dbManager.getTagsForFeed(feed);
    }

    //================================================================================
    // Getters
    //================================================================================
    public FeedsSource getSelectedSource() {
        return selectedSource;
    }

    public Tag getSelectedTag() {
        return selectedTag;
    }

    public RefineList<FeedsSource> getSources() {
        return sources;
    }

    public ObservableList<Tag> getTags() {
        return tags;
    }

    public RefineList<Feed> getFeeds() {
        return feeds;
    }

    public boolean isShowRead() {
        return showRead || selectedTag != null;
    }

    @BeanSocket(enabled = false)
    public void setShowRead(boolean showRead) {
        this.showRead = showRead;
        update();
    }

    public boolean isUpdating() {
        return updating.get();
    }

    public ReadOnlyBooleanProperty updatingProperty() {
        return updating;
    }

    protected void setUpdating(boolean updating) {
        this.updating.set(updating);
    }
}
