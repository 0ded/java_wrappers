import java.io.Closeable;

/**
 * Java 7 compatible Rust-like Result<T, E>.
 *
 * Usage (example):
 *
 * try (Result<String, String> r = Result.ok("hello")) {
 *     switch (r.getTag()) {
 *         case OK:
 *             // safe to call getValue()
 *             String v = r.getValue();
 *             System.out.println("value: " + v);
 *             break;
 *         case ERR:
 *             // handle error (must call getError() or handle(...) to mark handled)
 *             String err = r.getError();
 *             System.out.println("error: " + err);
 *             break;
 *     }
 * } // if an Err was produced and not handled, close() will throw UnhandledResultException
 */
public abstract class Result<T, E> implements AutoCloseable {
    public static enum Tag { OK, ERR }

    private boolean handled = false;

    protected void markHandled() {
        this.handled = true;
    }

    /**
     * Use this in a try-with-resources block so close() can enforce that errors were handled.
     */
    @Override
    public void close() {
        if (!handled && this.getTag() == Tag.ERR) {
            throw new UnhandledResultException("Result was Err and not handled");
        }
    }

    public abstract Tag getTag();

    /**
     * If this is Ok, returns the value; otherwise throws IllegalStateException.
     * Calling this marks the Result as handled.
     */
    public abstract T getValue();

    /**
     * If this is Err, returns the error; otherwise throws IllegalStateException.
     * Calling this marks the Result as handled.
     */
    public abstract E getError();

    /**
     * Convenient match/handle: provide two handlers. Calling this marks handled.
     */
    public abstract void handle(OnOk<T> onOk, OnErr<E> onErr);

    /**
     * Factory: Ok
     */
    public static <T, E> Result<T, E> ok(final T value) {
        return new Ok<T, E>(value);
    }

    /**
     * Factory: Err
     */
    public static <T, E> Result<T, E> err(final E error) {
        return new Err<T, E>(error);
    }

    /**
     * Concrete Ok implementation
     */
    private static final class Ok<T, E> extends Result<T, E> {
        private final T value;
        public Ok(T value) { this.value = value; }

        @Override
        public Tag getTag() { return Tag.OK; }

        @Override
        public T getValue() {
            markHandled();
            return value;
        }

        @Override
        public E getError() {
            throw new IllegalStateException("Called getError() on Ok");
        }

        @Override
        public void handle(OnOk<T> onOk, OnErr<E> onErr) {
            markHandled();
            if (onOk != null) {
                try { onOk.handle(value); } catch (RuntimeException re) { throw re; }
                  // checked exceptions not thrown from lambdas here (Java 7)
            }
        }
    }

    /**
     * Concrete Err implementation
     */
    private static final class Err<T, E> extends Result<T, E> {
        private final E error;
        public Err(E error) { this.error = error; }

        @Override
        public Tag getTag() { return Tag.ERR; }

        @Override
        public T getValue() {
            throw new IllegalStateException("Called getValue() on Err");
        }

        @Override
        public E getError() {
            markHandled();
            return error;
        }

        @Override
        public void handle(OnOk<T> onOk, OnErr<E> onErr) {
            markHandled();
            if (onErr != null) {
                try { onErr.handle(error); } catch (RuntimeException re) { throw re; }
            }
        }
    }

    /**
     * Handler interfaces (lightweight - for Java 7).
     * They do not declare checked exceptions to keep usage straightforward.
     */
    public static interface OnOk<T> { void handle(T v); }
    public static interface OnErr<E> { void handle(E e); }

    /**
     * Exception thrown when an Err result was not handled before close().
     */
    public static class UnhandledResultException extends RuntimeException {
        public UnhandledResultException(String msg) { super(msg); }
    }
}
