# Architecture.md — 数据库连接与操作模块架构说明

## 项目概述

DatabaseConnect 是一个基于 Hibernate + HikariCP 的数据库连接管理模块，提供连接池、事务管理、CRUD 操作、HQL 查询和数据库元数据查询能力。

**包命名规范：** `org.bigdata.tool` 和 `org.bigdata.service`

## 架构图

```
┌──────────────────────────────────────────────────┐
│                   应用层                          │
│  ┌──────────────────┐  ┌───────────────────┐     │
│  │ DatabaseMetaService│  │ 其他业务 Service  │     │
│  │ (元数据查询)       │  │ (使用 HibernateUtil)│    │
│  └────────┬─────────┘  └────────┬──────────┘     │
│           │                     │                 │
│  ┌────────▼─────────────────────▼──────────┐     │
│  │            HibernateUtil                │     │
│  │  (连接管理 / 事务 / CRUD / HQL 查询)     │     │
│  └──────────────────┬──────────────────────┘     │
│                     │                             │
│  ┌──────────────────▼──────────────────────┐     │
│  │     ServicePoolManager                  │     │
│  │  (服务对象池 — Apache Commons Pool2)     │     │
│  └──────────────────┬──────────────────────┘     │
│                     │                             │
│  ┌──────────────────▼──────────────────────┐     │
│  │     HikariCP Connection Pool            │     │
│  │  (数据库连接池)                           │     │
│  └──────────────────┬──────────────────────┘     │
│                     │                             │
│  ┌──────────────────▼──────────────────────┐     │
│  │     MySQL Database                      │     │
│  └─────────────────────────────────────────┘     │
└──────────────────────────────────────────────────┘
```

## 项目结构

```
DatabaseConnect/
├── src/main/java/org/bigdata/
│   ├── tool/                          # 工具类
│   │   ├── HibernateUtil.java         # 核心连接管理
│   │   ├── ServicePoolManager.java    # 服务对象池
│   │   └── ServicePooledObjectFactory.java
│   └── service/                       # 业务服务
│       └── DatabaseMetaService.java   # 数据库元数据查询
├── pom.xml                            # Maven 配置
├── .github/workflows/
│   ├── build.yml                      # 构建工作流
│   └── release.yml                    # 发布到 GitHub Packages
└── hibernate.cfg.xml                  # 配置模板
```

## 模块职责

### HibernateUtil（核心连接管理）
`org.bigdata.tool.HibernateUtil`

- SessionFactory 单例（双重检查锁）
- `executeInTransaction()` — 事务封装，自动提交/回滚
- `executeQuery()` — 只读查询封装
- `save()` / `update()` / `delete()` — 通用 CRUD
- `findById()` / `findAll()` — 按主键查询
- `executeHQL()` — HQL 参数化查询
- `executeUpdate()` — HQL 更新/删除
- `shutdown()` — 关闭 SessionFactory

### ServicePoolManager（服务对象池）
`org.bigdata.tool.ServicePoolManager`

- 基于 Apache Commons Pool2 的通用对象池
- 支持注册/借出/归还/销毁服务实例
- 线程安全（ConcurrentHashMap）
- 可配置池大小

### DatabaseMetaService（元数据查询）
`org.bigdata.service.DatabaseMetaService`

- `getAllTableNames()` — 获取所有表名
- `getTableColumns()` — 获取表列名
- `previewTable()` — 预览表数据（支持列选择）
- SQL 注入防护（表名/列名校验）

## 数据流

```
业务代码调用
  │
  ├── HibernateUtil.save(entity)
  │     │
  │     ▼
  │   executeInTransaction()
  │     │
  │     ├── openSession() ──► HikariCP 获取连接
  │     ├── beginTransaction()
  │     ├── session.persist(merge/remove)
  │     ├── commit()  ──► 成功
  │     └── rollback() ──► 异常时回滚
  │
  └── DatabaseMetaService.previewTable("user", 10)
        │
        ▼
      executeQuery()
        │
        ├── NativeQuery: INFORMATION_SCHEMA 查询列名
        └── NativeQuery: SELECT * FROM 表名 LIMIT N
```

## 关键技术点

1. **双重检查锁单例**：SessionFactory 线程安全初始化
2. **HikariCP 连接池**：高性能数据库连接池，自动管理连接生命周期
3. **Apache Commons Pool2**：服务对象池化，复用 Service 实例
4. **参数化查询**：HQL 和 NativeQuery 均使用参数绑定，防止 SQL 注入
5. **表名/列名校验**：正则白名单 `^[A-Za-z0-9_]+$`，防止注入攻击
6. **事务自动回滚**：`executeInTransaction` 模式，异常时自动回滚

## 构建与发布

### 本地构建

```bash
mvn clean package
```

生成文件：
- `target/database-connect-1.0.0.jar` — 主 JAR
- `target/database-connect-1.0.0-sources.jar` — 源码 JAR
- `target/database-connect-1.0.0-javadoc.jar` — Javadoc JAR

### CI/CD

- **build.yml**：每次 push/PR 自动构建并上传 JAR 到 Artifacts
- **release.yml**：打 tag 时自动发布到 GitHub Packages 和 Releases
