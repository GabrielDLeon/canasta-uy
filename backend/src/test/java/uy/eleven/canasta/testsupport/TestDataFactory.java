package uy.eleven.canasta.testsupport;

import uy.eleven.canasta.model.ApiKey;
import uy.eleven.canasta.model.Category;
import uy.eleven.canasta.model.Client;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.PriceId;
import uy.eleven.canasta.model.Product;
import uy.eleven.canasta.model.RefreshToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static Client client(Long id, String email) {
        return Client.builder()
                .clientId(id)
                .username(email)
                .email(email)
                .password("$2a$10$encoded")
                .isActive(true)
                .build();
    }

    public static Category category(Integer id, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        return category;
    }

    public static Product product(Integer id, String name, Category category) {
        Product product = new Product();
        product.setProductId(id);
        product.setName(name);
        product.setBrand("Brand");
        product.setSpecification("Spec");
        product.setCategory(category);
        return product;
    }

    public static Price price(
            Integer productId,
            LocalDate date,
            BigDecimal min,
            BigDecimal max,
            BigDecimal avg,
            BigDecimal median) {
        PriceId priceId = new PriceId();
        priceId.setProductId(productId);
        priceId.setDate(date);

        Price price = new Price();
        price.setId(priceId);
        price.setPriceMinimum(min);
        price.setPriceMaximum(max);
        price.setPriceAverage(avg);
        price.setPriceMedian(median);
        price.setStoreCount(1);
        price.setOfferCount(0);
        price.setOfferPercentage(BigDecimal.ZERO);
        return price;
    }

    public static ApiKey apiKey(Long id, Client client, String keyValue, boolean isActive) {
        return ApiKey.builder()
                .id(id)
                .client(client)
                .keyValue(keyValue)
                .name("default")
                .isActive(isActive)
                .build();
    }

    public static RefreshToken refreshToken(
            Long id, Client client, String value, LocalDateTime expiresAt, boolean revoked) {
        RefreshToken token = new RefreshToken();
        token.setId(id);
        token.setClient(client);
        token.setTokenValue(value);
        token.setExpiresAt(expiresAt);
        token.setRevoked(revoked);
        return token;
    }
}
