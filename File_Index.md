# File_Index.md — 文件索引

## 根目录

| 文件 | 作用 |
|------|------|
| `Architecture.md` | 项目架构说明文档 |
| `README.md` | 项目简介及使用说明 |
| `File_Index.md` | 文件索引（本文件） |
| `.gitignore` | Git 忽略规则 |

## DatabaseConnect/Tool/

| 文件 | 作用 |
|------|------|
| `HibernateUtil.java` | 核心连接管理工具类。提供 SessionFactory 单例、事务封装（executeInTransaction）、只读查询（executeQuery）、通用 CRUD（save/update/delete/findById/findAll）、HQL 参数化查询与更新、连接池关闭（shutdown） |
| `ServicePoolManager.java` | 服务对象池管理器。基于 Apache Commons Pool2，单例模式，支持注册/借出/归还/销毁服务实例，线程安全，可配置池大小 |
| `ServicePooledObjectFactory.java` | 池工厂实现。继承 BasePooledObjectFactory，支持自定义创建和销毁回调，AutoCloseable 自动关闭 |

## DatabaseConnect/Service/

| 文件 | 作用 |
|------|------|
| `DatabaseMetaService.java` | 数据库元数据查询服务。提供获取所有表名、获取表列名、预览表数据（支持列选择和行数限制），内置 SQL 注入防护（表名/列名校验） |

## DatabaseConnect/

| 文件 | 作用 |
|------|------|
| `hibernate.cfg.xml` | Hibernate 配置模板。定义数据库驱动、连接 URL、连接池参数（HikariCP）、方言、DDL 策略，使用前需修改为实际数据库凭据 |

## AGENTS/

| 文件 | 作用 |
|------|------|
| `AGENTS.md` | 学期项目仓库规范文档 |
| `CLAUDE.md` | Claude Code 项目配置 |
| `.gitignore` | Git 忽略规则 |
