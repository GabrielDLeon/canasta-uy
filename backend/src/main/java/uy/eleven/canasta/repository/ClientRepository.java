package uy.eleven.canasta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uy.eleven.canasta.model.Client;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUsername(String username);

    Optional<Client> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
