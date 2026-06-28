package org.bigdata.service;

import org.bigdata.tool.HibernateUtil;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseMetaService 元数据服务测试
 */
class DatabaseMetaServiceTest {

    private DatabaseMetaService metaService;

    @BeforeAll
    static void setUp() {
        System.setProperty("hibernate.cfg.file", "hibernate-test.cfg.xml");
        // 创建测试表
        HibernateUtil.executeInTransaction(session -> {
            session.createNativeQuery(
                "CREATE TABLE IF NOT EXISTS test_meta (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100), " +
                "metric_value INT)"
            ).executeUpdate();
        });
    }

    @AfterAll
    static void tearDown() {
        HibernateUtil.executeInTransaction(session -> {
            session.createNativeQuery("DROP TABLE IF EXISTS test_meta").executeUpdate();
        });
        HibernateUtil.shutdown();
    }

    @BeforeEach
    void init() {
        metaService = new DatabaseMetaService();
    }

    @Test
    void getAllTableNames_shouldReturnNonEmptyList() {
        List<String> tables = metaService.getAllTableNames();
        assertNotNull(tables);
        assertFalse(tables.isEmpty(), "应至少有一个表");
        assertTrue(tables.contains("TEST_META"), "应包含测试表");
    }

    @Test
    void getTableColumns_shouldReturnColumnNames() {
        List<String> columns = metaService.getTableColumns("test_meta");
        assertNotNull(columns);
        assertEquals(3, columns.size(), "应有 3 列");
        assertTrue(columns.contains("ID"));
        assertTrue(columns.contains("NAME"));
        assertTrue(columns.contains("METRIC_VALUE"));
    }

    @Test
    void previewTable_shouldReturnTableData() {
        // 插入测试数据
        HibernateUtil.executeInTransaction(session -> {
            session.createNativeQuery(
                "INSERT INTO test_meta (name, metric_value) VALUES ('test1', 100)"
            ).executeUpdate();
        });

        Map<String, Object> result = metaService.previewTable("test_meta", 10);
        assertNotNull(result);
        assertNotNull(result.get("columns"));
        assertNotNull(result.get("rows"));
        assertNotNull(result.get("tableName"));
        assertNotNull(result.get("rowCount"));

        assertEquals("test_meta", result.get("tableName"));
        assertTrue((int) result.get("rowCount") >= 1);
    }

    @Test
    void previewTable_withSelectedColumns_shouldFilterColumns() {
        Map<String, Object> result = metaService.previewTable("test_meta", 10, List.of("NAME"));
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) result.get("columns");
        assertEquals(1, columns.size());
        assertEquals("NAME", columns.get(0));
    }

    @Test
    void previewTable_shouldThrowOnInvalidTableName() {
        assertThrows(IllegalArgumentException.class, () -> {
            metaService.previewTable("'; DROP TABLE test_meta;--", 10);
        });
    }

    @Test
    void previewTable_shouldThrowOnEmptyTableName() {
        assertThrows(IllegalArgumentException.class, () -> {
            metaService.previewTable("", 10);
        });
    }

    @Test
    void previewTable_shouldThrowOnNullTableName() {
        assertThrows(IllegalArgumentException.class, () -> {
            metaService.previewTable(null, 10);
        });
    }

    @Test
    void previewTable_shouldThrowOnInvalidColumnName() {
        assertThrows(IllegalArgumentException.class, () -> {
            metaService.previewTable("test_meta", 10, List.of("'; DROP TABLE--"));
        });
    }

    @Test
    void previewTable_shouldThrowOnNonExistColumn() {
        assertThrows(IllegalArgumentException.class, () -> {
            metaService.previewTable("test_meta", 10, List.of("NON_EXIST_COLUMN"));
        });
    }

    @Test
    void previewTable_shouldLimitRows() {
        // 插入多条数据
        HibernateUtil.executeInTransaction(session -> {
            for (int i = 0; i < 20; i++) {
                session.createNativeQuery(
                    "INSERT INTO test_meta (name, metric_value) VALUES ('user" + i + "', " + i + ")"
                ).executeUpdate();
            }
        });

        Map<String, Object> result = metaService.previewTable("test_meta", 5);
        int rowCount = (int) result.get("rowCount");
        assertTrue(rowCount <= 5, "行数不应超过限制");
    }

    @Test
    void getTableColumns_shouldThrowOnInvalidTableName() {
        assertThrows(IllegalArgumentException.class, () -> {
            metaService.getTableColumns("'; DROP TABLE--");
        });
    }
}
