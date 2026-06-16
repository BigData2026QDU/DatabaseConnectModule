# DatabaseConnect — Java 数据库连接与操作模块

基于 Hibernate + HikariCP 的数据库连接管理与操作模块，提供连接池、CRUD、HQL 查询、数据库元数据查询等能力。

## 主要功能

- Hibernate SessionFactory 单例管理（双重检查锁）
- 事务封装（自动提交/回滚）
- 通用 CRUD 操作（save/update/delete/findById/findAll）
- HQL 查询与更新
- HikariCP 连接池配置
- 服务对象池管理（Apache Commons Pool2）
- 数据库元数据查询（表名/列名/数据预览）

## 环境要求

- Java 17 (JDK 17)
- Hibernate ORM 6.4+
- HikariCP 5.x
- MySQL 8.x

## 安装

### Maven

```xml
<dependency>
    <groupId>org.bigdata</groupId>
    <artifactId>database-connect</artifactId>
    <version>1.0.0</version>
</dependency>
```

配置 GitHub Packages 仓库：

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/BigData2026QDU/DatabaseConnectModule</url>
    </repository>
</repositories>
```

### 直接下载 JAR

从 [Releases](https://github.com/BigData2026QDU/DatabaseConnectModule/releases) 页面下载最新的 JAR 文件。

## 快速开始

1. 在 `src/main/resources/` 下创建 `hibernate.cfg.xml`，配置数据库连接信息：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-configuration PUBLIC
        "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
        "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">
<hibernate-configuration>
    <session-factory>
        <property name="hibernate.connection.url">jdbc:mysql://localhost:3306/mydb</property>
        <property name="hibernate.connection.username">root</property>
        <property name="hibernate.connection.password">password</property>
        <property name="hibernate.dialect">org.hibernate.dialect.MySQLDialect</property>
    </session-factory>
</hibernate-configuration>
```

2. 使用 `HibernateUtil` 进行数据库操作：

```java
import org.bigdata.tool.HibernateUtil;
import org.bigdata.service.DatabaseMetaService;

// 保存实体
HibernateUtil.save(myEntity);

// 查询
User user = HibernateUtil.findById(User.class, 1L);
List<User> users = HibernateUtil.findAll(User.class);

// HQL 查询
List<User> admins = HibernateUtil.executeHQL(
    "FROM User WHERE isAdmin = :admin", User.class, true
);

// 事务操作
HibernateUtil.executeInTransaction(session -> {
    session.merge(entity);
});

// 数据库元数据查询
DatabaseMetaService metaService = new DatabaseMetaService();
List<String> tables = metaService.getAllTableNames();
```

## 项目结构

```
DatabaseConnect/
├── src/main/java/org/bigdata/
│   ├── tool/
│   │   ├── HibernateUtil.java            # 连接管理与 CRUD
│   │   ├── ServicePoolManager.java       # 服务对象池
│   │   └── ServicePooledObjectFactory.java # 池工厂
│   └── service/
│       └── DatabaseMetaService.java      # 数据库元数据查询
├── .github/workflows/
│   ├── build.yml                         # 构建工作流
│   └── release.yml                       # 发布工作流
├── pom.xml                               # Maven 配置
├── hibernate.cfg.xml                     # 数据库配置模板
├── Architecture.md                       # 架构说明
├── README.md                             # 项目简介
├── File_Index.md                         # 文件索引
└── .gitignore
```

## 构建

```bash
mvn clean package
```

生成的 JAR 文件位于 `target/` 目录。

## 如何贡献

1. Fork 本仓库
2. 创建功能分支
3. 提交代码并更新文档
4. 发起 Pull Request
