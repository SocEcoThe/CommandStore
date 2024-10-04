# CommandShop 插件

CommandShop 是一个基于 Spigot API 开发的 Minecraft 服务器商店插件。它允许玩家通过命令界面买卖物品，并且支持多货币。

## 玩家使用教程

### 基本命令

- `/sd` 或 `/shop` 或 `/shangdian`: 打开商店主菜单
- `/sd help` 或 `/sd bangzhu`: 显示帮助信息

### 搜索物品

使用 `/sd sou <关键词> [页码]` 来搜索物品。例如：
- `/sd sou 钻石`
- `/sd sou 木头 2`

### 购买物品

1. 先使用搜索命令找到想要的物品
2. 使用 `/sd mai <物品序号> <数量>` 购买物品。例如：
   - `/sd mai 1 64` 购买搜索结果中第一个物品的 64 个

### 出售物品

手持要出售的物品，然后使用 `/sd chu <数量> <货币> <价格>` 命令。例如：
- `/sd chu 64 RMB 100` 以 100 RMB的价格出售手中物品的 64 个

### 管理自己的商品

- 使用 `/sd wode` 查看你正在出售的商品
- 使用 `/sd che <序号>` 撤回已上架的商品。例如：
  - `/sd che 1` 撤回列表中的第一个商品

### 小贴士

- 使用 Tab 键可以自动补全命令
- 如果物品很多，记得使用分页功能查看更多结果
- 经常检查自己的出售列表，及时调整价格或撤回商品

## 技术介绍

### 插件架构

CommandShop 插件采用模块化设计，主要包含以下组件：

1. `CommandShopPlugin`: 插件的主类，负责初始化各个管理器
2. `ShopManager`: 处理商店的核心逻辑，如搜索、购买和出售
3. `DatabaseManager`: 管理与数据库的交互，处理数据的存储和检索
4. `EconomyManager`: 处理经济相关的操作，与 MultiCurrency 插件集成
5. `ItemManager`: 处理物品相关的操作，如创建 ItemStack 和管理库存
6. `ShopCommand`: 处理所有的命令输入和执行相应的操作

### 主要特性

- 多货币支持：集成 MultiCurrency 插件，支持多货币交易
- 高效搜索：使用数据库索引优化搜索性能
- 物品序列化：使用自定义的 ItemSerializer 来存储和恢复复杂的物品数据
- 翻译系统：支持物品名称的本地化翻译
- 分页显示：大量搜索结果时支持分页显示
- 命令补全：实现 TabCompleter 接口，提供智能命令补全功能

### 数据库结构

插件使用 MySQL 数据库，主要表结构如下：

```sql
CREATE TABLE IF NOT EXISTS `shop_items` (
    `id` INT AUTO_INCREMENT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `price` INT NOT NULL,
    `currency` VARCHAR(20) NOT NULL,
    `stock` INT NOT NULL,
    `hash` VARCHAR(255) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `S_name` VARCHAR(16) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_shop_items_name` (`name`),
    INDEX `idx_shop_items_price` (`price`),
    INDEX `idx_shop_items_type` (`type`),
    INDEX `idx_shop_items_seller` (`S_name`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='命令商店表';
```