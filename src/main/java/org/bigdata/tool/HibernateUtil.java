package org.bigdata.tool;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Hibernate 连接管理工具类
 * <p>
 * 提供 SessionFactory 单例管理、事务封装、通用 CRUD 操作、HQL 参数化查询等能力。
 * 基于 HikariCP 连接池，支持高性能数据库访问。
 * </p>
 *
 * @author xty
 * @version 1.0
 * @since 2026-06-16
 */
public class HibernateUtil {
    private static final String DEFAULT_CONFIG_RESOURCE = "hibernate.cfg.xml";
    private static final String CONFIG_RESOURCE_PROPERTY = "hibernate.cfg.file";
    private static final String CONFIG_RESOURCE_ENV = "HIBERNATE_CFG_FILE";
    private static final String JDBC_URL_PROPERTY = "db.jdbcUrl";
    private static final String JDBC_URL_ENV = "DB_JDBC_URL";
    private static final String DB_HOST_PROPERTY = "db.host";
    private static final String DB_PORT_PROPERTY = "db.port";
    private static final String DB_NAME_PROPERTY = "db.name";
    private static final String DB_USERNAME_PROPERTY = "db.username";
    private static final String DB_PASSWORD_PROPERTY = "db.password";

    private static volatile SessionFactory sessionFactory;
    private static final Object lock = new Object();

    private HibernateUtil() {}

    /**
     * 获取 SessionFactory 单例实例
     * <p>
     * 使用双重检查锁确保线程安全的单例初始化
     * </p>
     *
     * @return SessionFactory 实例
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            synchronized (lock) {
                if (sessionFactory == null || sessionFactory.isClosed()) {
                    sessionFactory = buildSessionFactory();
                }
            }
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        Configuration configuration = loadConfiguration();
        applyRuntimeOverrides(configuration);
        return configuration.buildSessionFactory();
    }

    private static Configuration loadConfiguration() {
        Configuration configuration = new Configuration();
        String configLocation = firstNonBlank(
            System.getProperty(CONFIG_RESOURCE_PROPERTY),
            System.getenv(CONFIG_RESOURCE_ENV)
        );

        if (configLocation == null) {
            return configuration.configure(DEFAULT_CONFIG_RESOURCE);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            URL resource = contextClassLoader.getResource(configLocation);
            if (resource != null) {
                return configuration.configure(resource);
            }
        }

        File configFile = new File(configLocation);
        if (configFile.isFile()) {
            return configuration.configure(configFile);
        }

        throw new IllegalStateException("Hibernate configuration file not found: " + configLocation);
    }

    private static void applyRuntimeOverrides(Configuration configuration) {
        String jdbcUrl = firstNonBlank(
            System.getProperty(JDBC_URL_PROPERTY),
            System.getenv(JDBC_URL_ENV)
        );

        if (jdbcUrl == null) {
            String host = firstNonBlank(System.getProperty(DB_HOST_PROPERTY), System.getenv("DB_HOST"));
            String port = firstNonBlank(System.getProperty(DB_PORT_PROPERTY), System.getenv("DB_PORT"));
            String databaseName = firstNonBlank(System.getProperty(DB_NAME_PROPERTY), System.getenv("DB_NAME"));

            if (host != null || port != null || databaseName != null) {
                if (host == null || port == null || databaseName == null) {
                    throw new IllegalStateException("DB_HOST, DB_PORT and DB_NAME must be provided together.");
                }
                jdbcUrl = buildMySqlJdbcUrl(host, port, databaseName);
            }
        }

        overrideProperty(configuration, "hibernate.connection.url", jdbcUrl);
        overrideProperty(
            configuration,
            "hibernate.connection.username",
            firstNonBlank(System.getProperty(DB_USERNAME_PROPERTY), System.getenv("DB_USERNAME"))
        );
        overrideProperty(
            configuration,
            "hibernate.connection.password",
            firstNonBlank(System.getProperty(DB_PASSWORD_PROPERTY), System.getenv("DB_PASSWORD"))
        );
    }

    private static String buildMySqlJdbcUrl(String host, String port, String databaseName) {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
            + "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    }

    private static void overrideProperty(Configuration configuration, String key, String value) {
        if (value != null) {
            configuration.setProperty(key, value);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    /**
     * 在事务中执行操作
     * <p>
     * 自动管理事务的提交和回滚。如果操作成功则提交，如果抛出异常则回滚。
     * </p>
     *
     * @param action 要执行的操作，接收 Session 参数
     * @throws RuntimeException 当事务执行失败时
     */
    public static void executeInTransaction(Consumer<Session> action) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                action.accept(session);
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Transaction failed", e);
            }
        }
    }

    /**
     * 执行只读查询
     * <p>
     * 封装查询操作，自动管理 Session 生命周期。
     * </p>
     *
     * @param <T>    返回类型
     * @param query  查询函数，接收 Session 参数并返回结果
     * @return 查询结果
     * @throws RuntimeException 当查询执行失败时
     */
    public static <T> T executeQuery(Function<Session, T> query) {
        try (Session session = getSessionFactory().openSession()) {
            return query.apply(session);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    /**
     * 保存实体
     *
     * @param entity 要保存的实体对象
     */
    public static void save(Object entity) {
        executeInTransaction(session -> session.persist(entity));
    }

    /**
     * 更新实体
     *
     * @param entity 要更新的实体对象
     */
    public static void update(Object entity) {
        executeInTransaction(session -> session.merge(entity));
    }

    /**
     * 删除实体
     *
     * @param entity 要删除的实体对象
     */
    public static void delete(Object entity) {
        executeInTransaction(session -> session.remove(entity));
    }

    /**
     * 根据主键查询实体
     *
     * @param <T>         实体类型
     * @param entityClass 实体类
     * @param id          主键值
     * @return 实体对象，如果不存在则返回 null
     */
    public static <T> T findById(Class<T> entityClass, Object id) {
        return executeQuery(session -> session.get(entityClass, id));
    }

    public static <T> List<T> findAll(Class<T> entityClass) {
        return executeQuery(session ->
            session.createQuery("FROM " + entityClass.getSimpleName(), entityClass).list()
        );
    }

    public static <T> List<T> executeHQL(String hql, Class<T> resultClass, Object... params) {
        return executeQuery(session -> {
            Query<T> query = session.createQuery(hql, resultClass);
            bindPositionalParameters(query, params);
            return query.list();
        });
    }

    public static <T> List<T> executeHQL(String hql, Class<T> resultClass, Map<String, ?> namedParams) {
        return executeQuery(session -> {
            Query<T> query = session.createQuery(hql, resultClass);
            bindNamedParameters(query, namedParams);
            return query.list();
        });
    }

    public static int executeUpdate(String hql, Object... params) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                Query<?> query = session.createQuery(hql);
                bindPositionalParameters(query, params);
                int result = query.executeUpdate();
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Update failed", e);
            }
        }
    }

    public static int executeUpdate(String hql, Map<String, ?> namedParams) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                Query<?> query = session.createQuery(hql);
                bindNamedParameters(query, namedParams);
                int result = query.executeUpdate();
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Update failed", e);
            }
        }
    }

    private static void bindPositionalParameters(Query<?> query, Object... params) {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
    }

    private static void bindNamedParameters(Query<?> query, Map<String, ?> namedParams) {
        if (namedParams == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : namedParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    public static void shutdown() {
        synchronized (lock) {
            if (sessionFactory != null) {
                if (!sessionFactory.isClosed()) {
                    sessionFactory.close();
                }
                sessionFactory = null;
            }
        }
        sessionFactory = null;
    }
}
