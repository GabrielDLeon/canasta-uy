package uy.eleven.canasta.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Integer productId) {
        super("Product not found with id: " + productId);
    }
}
