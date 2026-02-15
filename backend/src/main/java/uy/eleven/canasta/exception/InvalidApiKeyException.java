package uy.eleven.canasta.exception;

public class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException() {
        super("Invalid or revoked API key");
    }

    public InvalidApiKeyException(String message) {
        super(message);
    }
}
