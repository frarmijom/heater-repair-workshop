package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.InventoryItemCreationRequest;
import com.heaterworkshop.domain.inventory.InventoryItemCreationRequestRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.sql.SQLException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaInventoryItemCreationRequestRepositoryAdapter implements InventoryItemCreationRequestRepository {
    private final SpringDataInventoryItemCreationRequestRepository repository;
    private final boolean h2;

    public JpaInventoryItemCreationRequestRepositoryAdapter(SpringDataInventoryItemCreationRequestRepository repository,
                                                            DataSource dataSource) {
        this.repository = repository;
        try (var connection = dataSource.getConnection()) {
            h2 = connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not detect inventory database", exception);
        }
    }

    @Override
    public boolean claim(String requestId, UUID itemId, String payloadHash) {
        int inserted = h2
            ? repository.claimH2(requestId, itemId, payloadHash, Instant.now())
            : repository.claim(requestId, itemId, payloadHash, Instant.now());
        return inserted == 1;
    }

    @Override @Transactional(readOnly = true)
    public Optional<InventoryItemCreationRequest> findByRequestId(String requestId) {
        return repository.findByRequestId(requestId).map(JpaInventoryItemCreationRequestEntity::toDomain);
    }
}