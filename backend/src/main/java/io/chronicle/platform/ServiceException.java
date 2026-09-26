package io.chronicle.platform;

public class ServiceException extends RuntimeException {
    private final int status;

    public ServiceException(String message) {
        this(message, 400);
    }

    public ServiceException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
