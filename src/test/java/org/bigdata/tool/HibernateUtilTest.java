package org.bigdata.tool;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HibernateUtil 工具类测试
 */
class HibernateUtilTest {

    @BeforeAll
    static void setUp() {
        // 添加测试实体映射
        System.setProperty("hibernate.cfg.file", "hibernate-test.cfg.xml");
    }

    @AfterAll
    static void tearDown() {
        HibernateUtil.shutdown();
    }

    @Test
    void getSessionFactory_shouldReturnNonEmptyFactory() {
        SessionFactory factory = HibernateUtil.getSessionFactory();
        assertNotNull(factory, "SessionFactory 不应为 null");
        assertFalse(factory.isClosed(), "SessionFactory 不应已关闭");
    }

    @Test
    void getSessionFactory_shouldReturnSameInstance() {
        SessionFactory factory1 = HibernateUtil.getSessionFactory();
        SessionFactory factory2 = HibernateUtil.getSessionFactory();
        assertSame(factory1, factory2, "多次调用应返回同一实例");
    }

    @Test
    void executeInTransaction_shouldCommitOnSuccess() {
        TestUser user = new TestUser("testuser", "test@example.com");

        HibernateUtil.executeInTransaction(session -> {
            session.persist(user);
        });

        assertNotNull(user.getId(), "保存后 ID 不应为 null");
    }

    @Test
    void executeInTransaction_shouldRollbackOnException() {
        TestUser user = new TestUser("rollback_user", "rollback@example.com");

        assertThrows(RuntimeException.class, () -> {
            HibernateUtil.executeInTransaction(session -> {
                session.persist(user);
                throw new RuntimeException("模拟异常");
            });
        });
    }

    @Test
    void save_shouldPersistEntity() {
        TestUser user = new TestUser("save_user", "save@example.com");
        HibernateUtil.save(user);
        assertNotNull(user.getId());
    }

    @Test
    void update_shouldMergeEntity() {
        TestUser user = new TestUser("update_user", "update@example.com");
        HibernateUtil.save(user);

        user.setEmail("updated@example.com");
        HibernateUtil.update(user);

        TestUser found = HibernateUtil.findById(TestUser.class, user.getId());
        assertNotNull(found);
        assertEquals("updated@example.com", found.getEmail());
    }

    @Test
    void delete_shouldRemoveEntity() {
        TestUser user = new TestUser("delete_user", "delete@example.com");
        HibernateUtil.save(user);
        Long id = user.getId();

        HibernateUtil.delete(user);

        TestUser found = HibernateUtil.findById(TestUser.class, id);
        assertNull(found, "删除后不应找到实体");
    }

    @Test
    void findById_shouldReturnEntity() {
        TestUser user = new TestUser("find_user", "find@example.com");
        HibernateUtil.save(user);

        TestUser found = HibernateUtil.findById(TestUser.class, user.getId());
        assertNotNull(found);
        assertEquals("find_user", found.getUsername());
    }

    @Test
    void findById_shouldReturnNullForNonExistId() {
        TestUser found = HibernateUtil.findById(TestUser.class, 999999L);
        assertNull(found, "不存在的 ID 应返回 null");
    }

    @Test
    void findAll_shouldReturnListOfEntities() {
        HibernateUtil.save(new TestUser("findall_1", "a@example.com"));
        HibernateUtil.save(new TestUser("findall_2", "b@example.com"));

        List<TestUser> users = HibernateUtil.findAll(TestUser.class);
        assertNotNull(users);
        assertTrue(users.size() >= 2, "应至少返回 2 条记录");
    }

    @Test
    void executeHQL_shouldReturnFilteredResults() {
        HibernateUtil.save(new TestUser("hql_user", "hql@example.com"));

        List<TestUser> result = HibernateUtil.executeHQL(
                "FROM TestUser WHERE username = :username",
                TestUser.class,
                Map.of("username", "hql_user")
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("hql_user", result.get(0).getUsername());
    }

    @Test
    void executeUpdate_shouldModifyEntities() {
        HibernateUtil.save(new TestUser("update_hql", "old@example.com"));

        int affected = HibernateUtil.executeUpdate(
                "UPDATE TestUser SET email = :email WHERE username = :username",
                Map.of(
                    "email", "new@example.com",
                    "username", "update_hql"
                )
        );

        assertEquals(1, affected);
    }

    @Test
    void executeQuery_shouldExecuteCustomQuery() {
        HibernateUtil.save(new TestUser("query_user", "query@example.com"));

        Long count = HibernateUtil.executeQuery(session -> {
            return session.createQuery("SELECT COUNT(*) FROM TestUser", Long.class)
                    .getSingleResult();
        });

        assertTrue(count > 0, "查询结果应大于 0");
    }
}
