package io.github.palexdev.feedfx.model;

public record Tag(
    int id,
    String name,
    String color
) {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final String DEFAULT_COLOR = "#178BFF";
}
