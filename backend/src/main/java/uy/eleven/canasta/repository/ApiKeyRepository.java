package uy.eleven.canasta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uy.eleven.canasta.model.ApiKey;
import uy.eleven.canasta.model.Client;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyValue(String keyValue);

    List<ApiKey> findByClient(Client client);

    List<ApiKey> findByClientAndIsActiveTrue(Client client);
}
