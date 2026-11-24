
package com.markethub.orders.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.markethub.customer.infrastructure.jpa.AddressEntity;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.orders.infrastructure.jpa.OrderEntity;
import com.markethub.payments.infrastructure.jpa.PaymentEntity;
import com.markethub.payments.infrastructure.jpa.RefundEntity;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/markethub_test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "markethub.mail.from=test@markethub.local"
})
class AdminOrderConcurrencyIntegrationTest {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminOrderService adminOrderService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldConnectToIsolatedTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();

            assertTrue(
                    url.contains("/markethub_test"),
                    "Integration test must use markethub_test, not the development database");
        }
    }

    @Test
    void concurrentRefundFailedWebhooksShouldCreateOnlyOneHistoryEntry() throws Exception {

        TestData data = createTestData();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        CountDownLatch firstTransactionUpdated = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        CountDownLatch secondCallStarted = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> firstCall = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {

                        adminOrderService.refundWebhook(
                                data.providerRefundId(), "refund.failed");

                        // First transaction has updated the refund,
                        // but deliberately has not committed yet.
                        firstTransactionUpdated.countDown();

                        await(allowFirstCommit);
                    }));

            assertTrue(
                    firstTransactionUpdated.await(10, TimeUnit.SECONDS),
                    "First transaction did not complete its refund update");

            Future<?> secondCall = executor.submit(() -> {
                secondCallStarted.countDown();

                adminOrderService.refundWebhook(
                        data.providerRefundId(), "refund.failed");
            });

            assertTrue(
                    secondCallStarted.await(10, TimeUnit.SECONDS),
                    "Second webhook call did not start");

            // The second call should be blocked by the first
            // transaction's pessimistic database lock.
            Thread.sleep(300);

            assertFalse(
                    secondCall.isDone(),
                    "Second webhook should wait for the first transaction to commit");

            // Allow the first transaction to commit.
            allowFirstCommit.countDown();

            firstCall.get(10, TimeUnit.SECONDS);
            secondCall.get(10, TimeUnit.SECONDS);

            assertEquals(
                    "FAILED",
                    jdbcTemplate.queryForObject(
                            "SELECT status FROM refunds WHERE provider_refund_id = ?",
                            String.class,
                            data.providerRefundId()));

            assertEquals(
                    "CAPTURED",
                    jdbcTemplate.queryForObject(
                            "SELECT status FROM payments WHERE order_id = ?",
                            String.class,
                            data.orderId()));

            assertEquals(
                    "CONFIRMED",
                    jdbcTemplate.queryForObject(
                            "SELECT status FROM orders WHERE id = ?",
                            String.class,
                            data.orderId()));

            Integer historyCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM order_status_history
                    WHERE order_id = ?
                      AND reason = 'Provider refund failed'
                    """,
                    Integer.class,
                    data.orderId());

            assertEquals(
                    1,
                    historyCount,
                    "Only one refund failure history entry should be created");

        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private TestData createTestData() {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {

            String uniqueId = UUID.randomUUID().toString().replace("-", "");

            UserEntity customer = new UserEntity(
                    "customer-" + uniqueId + "@test.local",
                    "test-password-hash",
                    "Test Customer");

            UserEntity admin = new UserEntity(
                    "admin-" + uniqueId + "@test.local",
                    "test-password-hash",
                    "Test Admin");

            entityManager.persist(customer);
            entityManager.persist(admin);

            AddressEntity address = new AddressEntity(
                    customer,
                    "Test Customer",
                    "9876543210",
                    "Test Street",
                    null,
                    "Bengaluru",
                    "Karnataka",
                    "560001",
                    "IN",
                    "Home",
                    true);

            entityManager.persist(address);

            OrderEntity order = new OrderEntity(
                    customer,
                    "MH-TEST-" + uniqueId.substring(0, 12),
                    "CONFIRMED",
                    "RAZORPAY",
                    new BigDecimal("100.00"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    new BigDecimal("100.00"),
                    "INR",
                    address);

            entityManager.persist(order);

            PaymentEntity payment = new PaymentEntity(
                    order,
                    "RAZORPAY",
                    "order_" + uniqueId);

            payment.capture("pay_" + uniqueId);
            payment.refundPending();

            entityManager.persist(payment);

            RefundEntity refund = new RefundEntity(
                    payment,
                    admin,
                    "Concurrency integration test",
                    "CONFIRMED");

            refund.submitted("rfnd_" + uniqueId, "processing");

            entityManager.persist(refund);

            entityManager.flush();

            return new TestData(
                    order.getId(),
                    refund.getProviderRefundId());
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for transaction coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private record TestData(Long orderId, String providerRefundId) {
    }
}
