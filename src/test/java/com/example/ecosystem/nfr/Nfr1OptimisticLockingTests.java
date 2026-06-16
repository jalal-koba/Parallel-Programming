package com.example.ecosystem.nfr;

import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.repository.ProductRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NFR #1 — {@code @Version} optimistic locking on {@link Product}.
 * Checkout concurrency is covered by {@link com.example.ecosystem.OrderServiceConcurrencyTests}.
 */
@SpringBootTest
@Tag("nfr1")
class Nfr1OptimisticLockingTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void productHasVersionColumnThatIncrementsOnUpdate() {
        Product product = transactionTemplate.execute(status -> productRepository.save(newProduct("version-test", 10)));

        assertThat(product.getVersion()).isNotNull();

        Integer versionBefore = product.getVersion();
        transactionTemplate.executeWithoutResult(status -> {
            Product loaded = productRepository.findById(product.getId()).orElseThrow();
            loaded.setStockQuantity(9);
            productRepository.saveAndFlush(loaded);
        });

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isGreaterThan(versionBefore);
        assertThat(reloaded.getStockQuantity()).isEqualTo(9);
    }

    @Test
    void staleVersionTriggersOptimisticLockingFailure() {
        Product product = transactionTemplate.execute(status -> productRepository.save(newProduct("stale-version", 10)));
        Long productId = product.getId();

        Product staleSnapshot = transactionTemplate.execute(
                status -> productRepository.findById(productId).orElseThrow());

        transactionTemplate.executeWithoutResult(status -> {
            Product current = productRepository.findById(productId).orElseThrow();
            current.setStockQuantity(9);
            productRepository.saveAndFlush(current);
        });

        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            staleSnapshot.setStockQuantity(8);
            productRepository.saveAndFlush(staleSnapshot);
            return null;
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void pessimisticLockRepositoryMethodIsAvailable() {
        Product product = transactionTemplate.execute(status -> productRepository.save(newProduct("pessimistic", 5)));

        transactionTemplate.executeWithoutResult(status -> {
            Product locked = productRepository.findByIdWithPessimisticLock(product.getId()).orElseThrow();
            assertThat(locked.getStockQuantity()).isEqualTo(5);
            locked.setStockQuantity(4);
            productRepository.saveAndFlush(locked);
        });

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(4);
    }

    private static Product newProduct(String name, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("NFR1 test product");
        product.setPrice(100F);
        product.setStockQuantity(stock);
        return product;
    }
}
