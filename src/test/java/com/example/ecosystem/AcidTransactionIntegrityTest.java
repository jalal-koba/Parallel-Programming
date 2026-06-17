package com.example.ecosystem;

import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.dto.ProductRequest;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import com.example.ecosystem.service.CartService;
import com.example.ecosystem.service.InsufficientStockException;
import com.example.ecosystem.service.OrderService;
import com.example.ecosystem.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ===================================================================
 * اختبارات التكامل: المتطلب الثامن - سلامة المعاملات (ACID)
 * ===================================================================
 *
 * هذه الاختبارات تثبت أن:
 * 1. Atomicity: لا يوجد حالة جزئية (مخزون مخصوم بدون طلب)
 * 2. Consistency: المخزون لا يصبح سالباً تحت أي ظرف
 * 3. Isolation: الوصول المتزامن لا يُسبب Race Condition
 * 4. Durability: البيانات تبقى صحيحة بعد COMMIT
 */
@SpringBootTest
class AcidTransactionIntegrityTest {

    private static final Logger log = LoggerFactory.getLogger(AcidTransactionIntegrityTest.class);

    @Autowired private ProductService productService;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;

    // ─────────────────────────────────────────────────────────────────
    // الاختبار 1: Atomicity - لا حالة جزئية
    // ─────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("ACID-1: Atomicity - مخزون مخصوم بدون طلب = مستحيل")
    void atomicity_noPartialState_stockDeductedOnlyWithSuccessfulOrder() throws Exception {

        log.info("=== اختبار Atomicity بدأ ===");

        // ترتيب: 50 مستخدم يحاولون شراء منتج فيه 10 وحدات فقط
        int STOCK = 10;
        int USERS  = 50;

        Product product = productService.createProduct(
                new ProductRequest("ACID-Test-Laptop", "Atomicity test", 999F, STOCK, null));

        List<Long> userIds = createUsersWithCart(USERS, product.getId(), 1);

        // تشغيل 50 checkout متزامن
        ConcurrentCheckoutResult result = runConcurrentCheckouts(userIds);

        // ─── التحقق من ACID ──────────────────────────────────────────
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        long totalOrders = orderRepository.findByStatus("COMPLETED").stream()
                .filter(o -> o.getItems().stream()
                        .anyMatch(i -> i.getProduct().getId().equals(product.getId())))
                .count();

        log.info("النتائج: نجح={} | فشل={} | مخزون متبقٍ={} | طلبات في DB={}",
                result.successes(), result.failures(), reloaded.getStockQuantity(), totalOrders);

        // Atomicity: عدد الطلبات الناجحة = ما تم حفظه في DB بالضبط
        assertThat(result.successes()).isEqualTo(STOCK)
                .as("يجب أن ينجح بالضبط " + STOCK + " checkout (= حجم المخزون)");

        // Consistency: المخزون = 0 وليس سالباً
        assertThat(reloaded.getStockQuantity()).isZero()
                .as("يجب أن يكون المخزون صفراً (لا سالباً)");

        // Atomicity: عدد الطلبات في DB = عدد النجاحات بالضبط
        assertThat(totalOrders).isEqualTo(STOCK)
                .as("لا يوجد طلب في DB بدون خصم مخزون مقابل");

        // الباقون رفضوا بـ InsufficientStockException
        assertThat(result.failures()).isEqualTo(USERS - STOCK);

        log.info("=== اختبار Atomicity نجح ✓ ===");
    }

    // ─────────────────────────────────────────────────────────────────
    // الاختبار 2: Consistency - المخزون لا يصبح سالباً أبداً
    // ─────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("ACID-2: Consistency - المخزون لا يصبح سالباً تحت أي ضغط")
    void consistency_stockNeverGoesNegative_underHighConcurrency() throws Exception {

        log.info("=== اختبار Consistency بدأ ===");

        int STOCK = 5;
        int USERS  = 100; // ضغط أعلى بكثير من المخزون

        Product product = productService.createProduct(
                new ProductRequest("ACID-Consistency-Item", "Consistency test", 50F, STOCK, null));

        List<Long> userIds = createUsersWithCart(USERS, product.getId(), 1);
        runConcurrentCheckouts(userIds);

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();

        log.info("المخزون النهائي: {}", reloaded.getStockQuantity());

        // Consistency: المخزون >= 0 دائماً
        assertThat(reloaded.getStockQuantity()).isGreaterThanOrEqualTo(0)
                .as("المخزون يجب أن يكون صفراً أو أكثر، لا يُمكن أن يكون سالباً");

        log.info("=== اختبار Consistency نجح ✓ ===");
    }

    // ─────────────────────────────────────────────────────────────────
    // الاختبار 3: Isolation - لا Race Condition تحت وصول متزامن
    // ─────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("ACID-3: Isolation - مستخدمان يشترون نفس المنتج في نفس اللحظة")
    void isolation_twoUsersCheckoutSameProduct_onlyOneSucceeds_whenStock1() throws Exception {

        log.info("=== اختبار Isolation بدأ (مخزون=1) ===");

        // أحرج حالة: مخزون وحدة واحدة ومستخدمان يحاولان في نفس اللحظة
        int STOCK = 1;
        int USERS  = 2;

        Product product = productService.createProduct(
                new ProductRequest("ACID-Last-Item", "Isolation test", 200F, STOCK, null));

        List<Long> userIds = createUsersWithCart(USERS, product.getId(), 1);
        ConcurrentCheckoutResult result = runConcurrentCheckouts(userIds);

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();

        log.info("نجح: {} | فشل: {} | مخزون: {}",
                result.successes(), result.failures(), reloaded.getStockQuantity());

        // Isolation: بالضبط واحد نجح والآخر رُفض
        assertThat(result.successes()).isEqualTo(1)
                .as("بالضبط مستخدم واحد يجب أن يحصل على آخر وحدة");
        assertThat(result.failures()).isEqualTo(1)
                .as("المستخدم الثاني يجب أن يُرفض بـ InsufficientStockException");
        assertThat(reloaded.getStockQuantity()).isZero();

        log.info("=== اختبار Isolation نجح ✓ ===");
    }

    // ─────────────────────────────────────────────────────────────────
    // الاختبار 4: Atomicity - Rollback عند فشل جزئي
    // ─────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("ACID-4: Atomicity - فشل أحد المنتجات يُلغي كل الطلب")
    void atomicity_rollback_whenOneProductOutOfStockInMultiItemCart() throws Exception {

        log.info("=== اختبار Rollback بدأ ===");

        // ترتيب: سلة فيها منتجان، الثاني ليس له مخزون
        Product productA = productService.createProduct(
                new ProductRequest("Rollback-ProductA", "Has stock", 100F, 10, null));
        Product productB = productService.createProduct(
                new ProductRequest("Rollback-ProductB", "NO STOCK", 200F, 0, null));

        User user = createUser("rollback-user-test");

        // أضف كلا المنتجين للسلة
        cartService.addProduct(user.getId(), productA.getId(), 2);
        cartService.addProduct(user.getId(), productB.getId(), 1); // هذا سيفشل

        Integer stockABefore = productRepository.findById(productA.getId())
                .orElseThrow().getStockQuantity();
        long ordersBefore = orderRepository.count();

        // محاولة Checkout
        boolean checkoutFailed = false;
        try {
            orderService.checkout(user.getId());
        } catch (InsufficientStockException e) {
            checkoutFailed = true;
            log.info("✓ تم رمي InsufficientStockException كما هو متوقع: {}", e.getMessage());
        }

        // ─── التحقق من Rollback ──────────────────────────────────────
        Integer stockAAfter = productRepository.findById(productA.getId())
                .orElseThrow().getStockQuantity();
        long ordersAfter = orderRepository.count();

        log.info("Rollback Result: checkoutFailed={} | stockA: {} → {} | orders: {} → {}",
                checkoutFailed, stockABefore, stockAAfter, ordersBefore, ordersAfter);

        assertThat(checkoutFailed).isTrue()
                .as("يجب أن يفشل الـ Checkout");

        // Atomicity: مخزون A لم يتغير رغم أنه تم معالجته قبل B
        assertThat(stockAAfter).isEqualTo(stockABefore)
                .as("Rollback: مخزون المنتج A يجب أن يعود لقيمته الأصلية");

        // Atomicity: لم يُنشَأ أي طلب
        assertThat(ordersAfter).isEqualTo(ordersBefore)
                .as("Rollback: لا يجب أن يُنشَأ طلب عند الفشل الجزئي");

        log.info("=== اختبار Rollback نجح ✓ ===");
    }

    // ─── Helper Methods ───────────────────────────────────────────────

    private List<Long> createUsersWithCart(int count, Long productId, int quantity) {
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = createUser("acid-user-" + System.nanoTime() + "-" + i);
            userIds.add(user.getId());
            cartService.addProduct(user.getId(), productId, quantity);
        }
        return userIds;
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("password");
        user.setRole(Role.customer);
        return userRepository.save(user);
    }

    /**
     * تشغيل عمليات checkout متزامنة باستخدام CountDownLatch
     * لضمان أن جميع الخيوط تبدأ في نفس اللحظة تماماً
     */
    private ConcurrentCheckoutResult runConcurrentCheckouts(List<Long> userIds) throws Exception {
        int count = userIds.size();
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count); // ينتظر حتى يكون كل خيط جاهزاً
        CountDownLatch start = new CountDownLatch(1);     // إشارة البدء للجميع في آنٍ واحد

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Long userId : userIds) {
            futures.add(executor.submit(() -> {
                ready.countDown();  // أعلن أنك جاهز
                start.await();      // انتظر إشارة البدء
                try {
                    orderService.checkout(userId);
                    successes.incrementAndGet();
                    return true;
                } catch (InsufficientStockException e) {
                    failures.incrementAndGet();
                    return false;
                }
            }));
        }

        ready.await();   // انتظر حتى يكون الجميع جاهزاً
        start.countDown(); // أطلق الجميع في نفس اللحظة

        for (Future<Boolean> f : futures) f.get();
        executor.shutdown();

        return new ConcurrentCheckoutResult(successes.get(), failures.get());
    }

    private record ConcurrentCheckoutResult(int successes, int failures) {}
}