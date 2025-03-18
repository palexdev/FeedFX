package io.github.palexdev.feedfx.utils.update;

public class UpdateCheckException extends RuntimeException {

    //================================================================================
    // Constructors
    //================================================================================
    public UpdateCheckException(String message) {
        super(message);
    }

    public UpdateCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpdateCheckException(Throwable cause) {
        super(cause);
    }
}
