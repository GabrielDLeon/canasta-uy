package uy.eleven.canasta.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(Long clientId) {
        super("Client not found with id: " + clientId);
    }
    
    public ClientNotFoundException(String field, String value) {
        super("Client not found with " + field + ": " + value);
    }
}
