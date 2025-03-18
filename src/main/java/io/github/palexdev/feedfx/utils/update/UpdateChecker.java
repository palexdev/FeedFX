package io.github.palexdev.feedfx.utils.update;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.tinylog.Logger;

public interface UpdateChecker {
    String getCurrentVersion();

    String getLatestVersion() throws UpdateCheckException;

    /// Similar to [Comparator#compare(Object,Object)] but inverted:
    ///
    ///  - negative number: current version is newer than latest
    ///  - positive number: current version is older than latest
    ///  - zero: same version
    int compare(String current, String latest);

    default boolean isBeta(String version) {
        return false;
    }

    default boolean allowBeta() {
        return false;
    }

    default boolean isUpdateAvailable() {
        try {
            String curr = getCurrentVersion();
            String latest = getLatestVersion();
            return compare(curr, latest) > 0
                   && (!isBeta(latest) || allowBeta());
        } catch (UpdateCheckException ex) {
            Logger.error(ex, "Failed to check for updates");
            return false;
        }
    }

    default void isUpdateAvailableAsync(Consumer<Boolean> callback) {
        CompletableFuture.supplyAsync(this::isUpdateAvailable, Helper.executor)
            .thenAccept(callback);
    }

    //================================================================================
    // Inner Classes
    //================================================================================
    class Helper {
        protected static final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
    }
}