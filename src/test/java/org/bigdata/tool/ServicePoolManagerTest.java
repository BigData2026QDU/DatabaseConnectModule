package org.bigdata.tool;

import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServicePoolManager 服务对象池测试
 */
class ServicePoolManagerTest {

    private ServicePoolManager poolManager;

    @BeforeEach
    void setUp() {
        poolManager = ServicePoolManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        poolManager.shutdown();
    }

    @Test
    void getInstance_shouldReturnSameInstance() {
        ServicePoolManager instance1 = ServicePoolManager.getInstance();
        ServicePoolManager instance2 = ServicePoolManager.getInstance();
        assertSame(instance1, instance2, "单例应返回同一实例");
    }

    @Test
    void registerService_shouldRegisterSuccessfully() {
        poolManager.registerService(String.class, () -> "test_service", 5);
        assertTrue(poolManager.isRegistered(String.class), "注册后应返回 true");
    }

    @Test
    void registerService_shouldThrowOnDuplicate() {
        poolManager.registerService(String.class, () -> "service1", 5);

        assertThrows(IllegalStateException.class, () -> {
            poolManager.registerService(String.class, () -> "service2", 5);
        });
    }

    @Test
    void registerService_shouldThrowOnNullClass() {
        assertThrows(NullPointerException.class, () -> {
            poolManager.registerService(null, () -> "service", 5);
        });
    }

    @Test
    void registerService_shouldThrowOnNullCreator() {
        assertThrows(NullPointerException.class, () -> {
            poolManager.registerService(String.class, null, 5);
        });
    }

    @Test
    void registerService_shouldThrowOnInvalidPoolSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            poolManager.registerService(String.class, () -> "service", 0);
        });
    }

    @Test
    void borrowService_shouldReturnServiceInstance() {
        AtomicInteger counter = new AtomicInteger(0);
        poolManager.registerService(String.class, () -> "service_" + counter.incrementAndGet(), 5);

        String service = poolManager.borrowService(String.class);
        assertNotNull(service);
        assertTrue(service.startsWith("service_"));
    }

    @Test
    void borrowService_shouldReturnDifferentInstances() {
        AtomicInteger counter = new AtomicInteger(0);
        poolManager.registerService(String.class, () -> "service_" + counter.incrementAndGet(), 5);

        String service1 = poolManager.borrowService(String.class);
        String service2 = poolManager.borrowService(String.class);

        assertNotEquals(service1, service2, "借出的对象应不同");
    }

    @Test
    void returnService_shouldReturnToPool() {
        poolManager.registerService(String.class, () -> "service", 5);

        String service = poolManager.borrowService(String.class);
        poolManager.returnService(String.class, service);

        // 归还后应该能再次借出同一个对象
        String service2 = poolManager.borrowService(String.class);
        assertEquals(service, service2, "归还后应能借出同一对象");
    }

    @Test
    void returnService_shouldThrowOnNull() {
        poolManager.registerService(String.class, () -> "service", 5);

        assertThrows(NullPointerException.class, () -> {
            poolManager.returnService(String.class, null);
        });
    }

    @Test
    void borrowService_shouldThrowOnUnregisteredClass() {
        assertThrows(IllegalStateException.class, () -> {
            poolManager.borrowService(Integer.class);
        });
    }

    @Test
    void isRegistered_shouldReturnFalseForUnregistered() {
        assertFalse(poolManager.isRegistered(Integer.class));
    }

    @Test
    void borrowService_shouldSupportConcurrentAccess() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        poolManager.registerService(String.class, () -> "service_" + counter.incrementAndGet(), 10);

        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    String service = poolManager.borrowService(String.class);
                    assertNotNull(service);
                    poolManager.returnService(String.class, service);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertEquals(threadCount, successCount.get(), "所有线程应成功借出和归还");
    }

    @Test
    void destroyCallback_shouldBeCalledOnInvalidate() {
        AtomicInteger destroyCount = new AtomicInteger(0);
        poolManager.registerService(String.class, () -> "service", 5, s -> destroyCount.incrementAndGet());

        String service = poolManager.borrowService(String.class);
        poolManager.invalidateService(String.class, service);

        assertEquals(1, destroyCount.get(), "销毁回调应被调用一次");
    }

    @Test
    void shutdown_shouldClearAllPools() {
        poolManager.registerService(String.class, () -> "service", 5);
        poolManager.registerService(Integer.class, () -> 123, 5);

        poolManager.shutdown();

        assertFalse(poolManager.isRegistered(String.class));
        assertFalse(poolManager.isRegistered(Integer.class));
    }
}
