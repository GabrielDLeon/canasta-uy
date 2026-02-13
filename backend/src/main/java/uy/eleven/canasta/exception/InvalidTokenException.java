package uy.eleven.canasta.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Invalid or malformed token");
    }
    
    public InvalidTokenException(String message) {
        super(message);
    }
}
