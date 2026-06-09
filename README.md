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

## 快速开始

1. 将 `Tool/` 和 `Service/` 目录拷贝到你的项目中
2. 将 `hibernate.cfg.xml` 放到 `src/main/resources/` 下，修改数据库连接信息
3. 使用 `HibernateUtil` 进行数据库操作：

```java
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
```

## 项目结构

```
DatabaseConnect/
├── Tool/
│   ├── HibernateUtil.java            # 连接管理与 CRUD
│   ├── ServicePoolManager.java       # 服务对象池
│   └── ServicePooledObjectFactory.java # 池工厂
├── Service/
│   └── DatabaseMetaService.java      # 数据库元数据查询
├── hibernate.cfg.xml                 # 数据库配置模板
├── Architecture.md                   # 架构说明
├── README.md                         # 项目简介
├── File_Index.md                     # 文件索引
└── .gitignore
```

## 如何贡献

1. Fork 本仓库
2. 创建功能分支
3. 提交代码并更新文档
4. 发起 Pull Request
