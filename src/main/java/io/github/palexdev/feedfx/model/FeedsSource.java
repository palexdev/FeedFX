package io.github.palexdev.feedfx.model;

import java.util.Comparator;

public record FeedsSource(
    int id,
    String name,
    String link
) {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final FeedsSource ALL = new FeedsSource(-1, "All Sources", null);
    public static final Comparator<FeedsSource> DEFAULT_COMPARATOR = (s1, s2) -> {
        if (s1 == ALL) return -1;
        if (s2 == ALL) return 1;
        return s1.name().compareTo(s2.name());
    };
}
