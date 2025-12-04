import java.io.Closeable;


public abstract class Option<T> implements AutoCloseable {

    public static enum Tag { SOME, NONE }

    private boolean handled = false;

    protected void markHandled() { this.handled = true; }

    @Override
    public void close() {
        if (!handled && getTag() == Tag.NONE) {
            throw new UnhandledOptionException("Option was None and not handled");
        }
    }

    public abstract Tag getTag();

    /**
     * Returns value if Some; otherwise throws IllegalStateException.
     * Calling this marks the Option as handled.
     */
    public abstract T getValue();

    /**
     * Mark explicitly handled even without extracting a value.
     * This is helpful for the NONE case.
     */
    public void handle(OnSome<T> onSome, OnNone onNone) {
        markHandled();
        switch (getTag()) {
            case SOME:
                if (onSome != null) onSome.handle(getValue());
                break;
            case NONE:
                if (onNone != null) onNone.handle();
                break;
        }
    }

    // Factories
    public static <T> Option<T> some(final T value) {
        return new Some<T>(value);
    }

    public static <T> Option<T> none() {
        return new None<T>();
    }

    // --- Implementations ---

    private static final class Some<T> extends Option<T> {
        private final T value;

        public Some(T value) { this.value = value; }

        @Override
        public Tag getTag() { return Tag.SOME; }

        @Override
        public T getValue() {
            markHandled();
            return value;
        }
    }

    private static final class None<T> extends Option<T> {

        @Override
        public Tag getTag() { return Tag.NONE; }

        @Override
        public T getValue() {
            throw new IllegalStateException("Called getValue() on None");
        }
    }

    // Handler interfaces (Java 7 friendly)
    public static interface OnSome<T> { void handle(T v); }
    public static interface OnNone { void handle(); }

    // Exception thrown when None is not handled
    public static class UnhandledOptionException extends RuntimeException {
        public UnhandledOptionException(String msg) { super(msg); }
    }
}
