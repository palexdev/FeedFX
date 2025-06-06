package io.github.palexdev.feedfx.utils;

public record Tuple3<A, B, C>(
    A a,
    B b,
    C c
) {
    public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
        return new Tuple3<>(a, b, c);
    }
}
