# File_Index.md — 文件索引

## 根目录

| 文件 | 作用 |
|------|------|
| `pom.xml` | Maven 配置。定义依赖（Hibernate、HikariCP、Commons Pool2、MySQL）、测试插件、覆盖率插件、源码和 Javadoc 打包 |
| `Architecture.md` | 项目架构说明文档 |
| `README.md` | 项目简介及使用说明 |
| `File_Index.md` | 文件索引（本文件） |
| `.gitignore` | Git 忽略规则 |
| `.gitmodules` | Git Submodule 配置 |

## src/main/java/org/bigdata/tool/

| 文件 | 作用 |
|------|------|
| `HibernateUtil.java` | 核心连接管理工具类。提供 SessionFactory 单例、`hibernate.cfg.file` 配置文件选择、运行时数据库参数覆盖、事务封装（executeInTransaction）、只读查询（executeQuery）、通用 CRUD（save/update/delete/findById/findAll）、HQL 参数化查询与更新、连接池关闭（shutdown） |
| `ServicePoolManager.java` | 服务对象池管理器。基于 Apache Commons Pool2，单例模式，支持注册/借出/归还/销毁服务实例，线程安全，可配置池大小 |
| `ServicePooledObjectFactory.java` | 池工厂实现。继承 BasePooledObjectFactory，支持自定义创建和销毁回调，AutoCloseable 自动关闭 |

## src/main/java/org/bigdata/service/

| 文件 | 作用 |
|------|------|
| `DatabaseMetaService.java` | 数据库元数据查询服务。提供获取所有表名、获取表列名、预览表数据（支持列选择和行数限制），内置 SQL 注入防护（表名/列名校验） |

## 根目录配置文件

| 文件 | 作用 |
|------|------|
| `hibernate.cfg.xml` | Hibernate 配置模板。默认使用 `test_db` 示例库名，不再包含真实凭据；实体映射由业务项目自己的 classpath 配置补充 |

## .github/workflows/

| 文件 | 作用 |
|------|------|
| `build.yml` | 构建工作流。每次 push/PR 自动编译、测试、生成覆盖率并上传 JAR/覆盖率到 Artifacts |
| `release.yml` | 发布工作流。打 tag 时自动发布到 GitHub Packages 和 Releases |

## AGENTS/ (submodule)

| 文件 | 作用 |
|------|------|
| `AGENTS.md` | 学期项目仓库规范文档 |
| `CLAUDE.md` | Claude Code 项目配置 |
| `PROJECT/BACKEND.md` | 后端开发规范（包命名为 org.bigdata） |
| `PROJECT/FRONTEND.md` | 前端开发规范 |
| `PROJECT/PROJECT.md` | 项目通用规范 |
