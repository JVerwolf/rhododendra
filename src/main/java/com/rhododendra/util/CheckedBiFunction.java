package com.rhododendra.util;

@FunctionalInterface
public interface CheckedBiFunction<T, U, R, E extends Throwable> {
    R apply(T t, U u) throws E;
}
