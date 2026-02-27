package net.mehvahdjukaar.vista.common.broadcast;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class TriResult<T> {

    private boolean isValid;
    private @Nullable T value;

    public static <T> TriResult<T> valid(T value) {
        TriResult<T> r = new TriResult<>();
        r.isValid = true;
        r.value = value;
        return r;
    }

    public static <T> TriResult<T> invalid() {
        TriResult<T> r = new TriResult<>();
        r.isValid = false;
        r.value = null;
        return r;
    }

    public static <T> TriResult<T> empty() {
        TriResult<T> r = new TriResult<T>();
        r.isValid = true;
        r.value = null;
        return r;
    }

    public boolean isValid() {
        return isValid;
    }

    public @Nullable T getValue() {
        return value;
    }

    public boolean isEmpty() {
        return isValid && value == null;
    }

    public  void ifValid(Consumer<T> consumer) {
        if (isValid && value != null) {
            consumer.accept(value);
        }
    }

    public  void ifPresent(Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

}
