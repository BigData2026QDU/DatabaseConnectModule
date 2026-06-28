package org.bigdata.service;

import org.bigdata.tool.HibernateUtil;
import org.hibernate.query.NativeQuery;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据库元数据服务
 * 用于获取表名列表、预览表数据等
 */
public class DatabaseMetaService {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern SAFE_COLUMN_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    /**
     * 获取当前数据库的所有表名
     */
    public List<String> getAllTableNames() {
        return HibernateUtil.executeQuery(session -> session.doReturningWork(connection -> {
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                List<String> tableNames = new ArrayList<>();
                try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, "%", null)) {
                    while (resultSet.next()) {
                        String tableType = resultSet.getString("TABLE_TYPE");
                        if (tableType != null
                            && !"TABLE".equalsIgnoreCase(tableType)
                            && !"BASE TABLE".equalsIgnoreCase(tableType)) {
                            continue;
                        }
                        String tableName = resultSet.getString("TABLE_NAME");
                        if (tableName != null) {
                            tableNames.add(tableName);
                        }
                    }
                }
                tableNames.sort(String.CASE_INSENSITIVE_ORDER);
                return tableNames;
            } catch (SQLException e) {
                throw new IllegalStateException("读取数据库表列表失败", e);
            }
        }));
    }

    /**
     * 预览表数据（前N行）
     * @param tableName 表名
     * @param limit 行数限制
     * @return {columns: [], rows: []}
     */
    public Map<String, Object> previewTable(String tableName, int limit) {
        return previewTable(tableName, limit, null);
    }

    /**
     * 预览表数据（前N行），支持按列预览
     * @param tableName 表名
     * @param limit 行数限制
     * @param selectedColumns 需要预览的列（null/空表示所有列）
     */
    public Map<String, Object> previewTable(String tableName, int limit, List<String> selectedColumns) {
        requireSafeTableName(tableName);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        return HibernateUtil.executeQuery(session -> {
            List<String> columns = loadTableColumns(session, tableName);

            if (columns.isEmpty()) {
                throw new IllegalArgumentException("表不存在或无列：" + tableName);
            }

            List<String> selected = resolveSelectedColumns(selectedColumns, columns);
            if (!selected.isEmpty()) {
                columns = new ArrayList<>(selected);
            }

            // 2. 获取数据
            String sql;
            if (selected.isEmpty()) {
                sql = "SELECT * FROM " + tableName + " LIMIT " + safeLimit;
            } else {
                String selectCols = String.join(", ", columns);
                sql = "SELECT " + selectCols + " FROM " + tableName + " LIMIT " + safeLimit;
            }

            NativeQuery<?> dataQuery = session.createNativeQuery(sql);
            List<?> dataResults = dataQuery.getResultList();

            List<List<Object>> rows = new ArrayList<>();
            for (Object r : dataResults) {
                if (r instanceof Object[]) {
                    rows.add(Arrays.asList((Object[]) r));
                } else {
                    rows.add(Collections.singletonList(r));
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("tableName", tableName);
            result.put("rowCount", rows.size());
            return result;
        });
    }

    public List<String> getTableColumns(String tableName) {
        requireSafeTableName(tableName);
        return HibernateUtil.executeQuery(session -> loadTableColumns(session, tableName));
    }

    private static void requireSafeTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名：" + tableName);
        }
    }

    private static List<String> normalizeColumns(List<String> columns) {
        if (columns == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String c : columns) {
            if (c == null) continue;
            String col = c.trim();
            if (col.isEmpty()) continue;
            if (!SAFE_COLUMN_NAME.matcher(col).matches()) {
                throw new IllegalArgumentException("非法列名：" + col);
            }
            if (!result.contains(col)) {
                result.add(col);
            }
        }
        return result;
    }

    private static List<String> resolveSelectedColumns(List<String> selectedColumns, List<String> availableColumns) {
        List<String> normalized = normalizeColumns(selectedColumns);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> resolved = new ArrayList<>();
        for (String requested : normalized) {
            String matched = null;
            for (String available : availableColumns) {
                if (available.equalsIgnoreCase(requested)) {
                    matched = available;
                    break;
                }
            }
            if (matched == null) {
                throw new IllegalArgumentException("列不存在：" + requested);
            }
            if (!resolved.contains(matched)) {
                resolved.add(matched);
            }
        }
        return resolved;
    }

    private static List<String> loadTableColumns(org.hibernate.Session session, String tableName) {
        return session.doReturningWork(connection -> {
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                for (String candidate : buildNameCandidates(tableName)) {
                    List<String> columns = new ArrayList<>();
                    try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, candidate, "%")) {
                        while (resultSet.next()) {
                            String columnName = resultSet.getString("COLUMN_NAME");
                            if (columnName != null) {
                                columns.add(columnName);
                            }
                        }
                    }
                    if (!columns.isEmpty()) {
                        return columns;
                    }
                }
                return Collections.<String>emptyList();
            } catch (SQLException e) {
                throw new IllegalStateException("读取表字段失败: " + tableName, e);
            }
        });
    }

    private static List<String> buildNameCandidates(String name) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(name);
        candidates.add(name.toUpperCase());
        candidates.add(name.toLowerCase());
        return new ArrayList<>(candidates);
    }
}
