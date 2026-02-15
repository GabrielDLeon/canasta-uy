package uy.eleven.canasta.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token has expired");
    }
    
    public TokenExpiredException(String message) {
        super(message);
    }
}
