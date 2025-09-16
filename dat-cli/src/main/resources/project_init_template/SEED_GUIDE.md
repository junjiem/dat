# 种子数据配置指南

**本页指南包含如下内容**

* [种子数据概述](#种子数据概述)
* [种子数据组件](#种子数据组件)
  * [基本属性](#基本属性)
  * [配置选项](#配置选项)
  * [列信息配置](#列信息配置)
* [配置示例](#配置示例)
* [文件结构和组织](#文件结构和组织)
* [最佳实践](#最佳实践)
* [常见问题](#常见问题)

## 种子数据概述

**种子数据是DAT系统中的静态数据集**，通常用于存储较少变化的引用数据、配置数据或测试数据。种子数据以CSV文件的形式存储，并通过YAML配置文件定义其结构和元数据。

* **种子数据的主要用途**：
  * **引用数据**: 国家代码、货币代码、产品分类等相对稳定的数据
  * **配置数据**: 业务规则配置、费率表、阈值设置等
  * **测试数据**: 用于开发和测试环境的静态数据集
  * **维度数据**: 小型维度表，如状态枚举、类别映射等

* 种子数据配置通过YAML文件定义，描述CSV文件的结构和属性
* 在`seeds`目录下组织CSV数据文件和对应的YAML配置文件
* 种子数据会被加载到数据库中，可以像普通表一样被查询和引用

## 种子数据组件

### 基本属性

每个种子数据的完整规范如下：

```yaml
version: 1
seeds:
  - name: 种子数据名称                    ## 必填
    description: 种子数据描述             ## 必填
    config:                            ## 可选
      delimiter: 分隔符                 ## 可选，默认为逗号
    columns:                           ## 可选
      - 详见列信息配置章节
```

下表描述了种子数据的基本属性：

| 组件                        | 描述                                                   | 必填 | 类型     |
|---------------------------|------------------------------------------------------|------|--------|
| **name**                  | 种子数据的唯一名称，对应CSV文件名                                   | 必填 | String |
| **description**           | 种子数据的详细描述，说明数据的用途和内容                                | 必填 | String |
| **[config](#配置选项)**     | 种子数据的解析配置，如分隔符等                                      | 可选 | Object |
| **[columns](#列信息配置)**   | 种子数据的列定义，描述每列的名称、描述和数据类型                           | 可选 | List   |

### 配置选项

种子数据支持以下配置选项：

| 参数              | 描述                          | 必填 | 类型     | 默认值 |
|-----------------|-----------------------------|------|--------|-------|
| **delimiter**   | CSV文件的字段分隔符                | 可选 | String | ","   |

#### 配置示例

```yaml
seeds:
  - name: country_codes
    description: "国际标准国家代码表"
    config:
      delimiter: ","  # 使用逗号分隔
    
  - name: product_categories  
    description: "产品分类层次结构"
    config:
      delimiter: \|  # 使用管道符分隔
```

### 列信息配置

列信息描述种子数据中每个字段的结构和属性：

| 参数              | 描述                          | 必填 | 类型     |
|-----------------|-----------------------------|------|--------|
| **name**        | 列名，必须与CSV文件中的列名匹配        | 必填 | String |
| **description** | 列的详细描述，说明字段含义             | 必填 | String |
| **data_type**   | 数据类型，根据目标数据库而定           | 可选 | String |

#### 列配置示例

```yaml
columns:
  - name: country_code
    description: "ISO 3166-1 alpha-2国家代码"
    data_type: "VARCHAR(2)"
    
  - name: country_name
    description: "国家名称（中文）"
    data_type: "VARCHAR(100)"
    
  - name: continent
    description: "所属大洲"
    data_type: "VARCHAR(50)"
    
  - name: population
    description: "人口数量"
    data_type: "BIGINT"
```

## 配置示例

### 国家代码种子数据

**配置文件: seeds/country_codes.yaml**

```yaml
version: 1
seeds:
  - name: country_codes
    description: "ISO标准国家代码及相关信息，用于地理维度分析"
    config:
      delimiter: ","
    columns:
      - name: country_code
        description: "ISO 3166-1 alpha-2 国家代码"
        data_type: "VARCHAR(2)"
      - name: country_name
        description: "国家名称（中文）"
        data_type: "VARCHAR(100)"
      - name: country_name_en
        description: "国家名称（英文）"
        data_type: "VARCHAR(100)"
      - name: continent
        description: "所属大洲"
        data_type: "VARCHAR(50)"
      - name: region
        description: "地理区域"
        data_type: "VARCHAR(100)"
      - name: population
        description: "人口数量（最新统计）"
        data_type: "BIGINT"
      - name: currency_code
        description: "货币代码"
        data_type: "VARCHAR(3)"
```

**数据文件: seeds/country_codes.csv**

```csv
country_code,country_name,country_name_en,continent,region,population,currency_code
CN,中国,China,亚洲,东亚,1411780000,CNY
US,美国,United States,北美洲,北美,331900000,USD
JP,日本,Japan,亚洲,东亚,125800000,JPY
DE,德国,Germany,欧洲,西欧,83200000,EUR
GB,英国,United Kingdom,欧洲,西欧,67500000,GBP
```

### 产品分类种子数据

**配置文件: seeds/product_categories.yaml**

```yaml
version: 1
seeds:
  - name: product_categories
    description: "产品分类层次结构，支持多级分类查询和分析"
    config:
      delimiter: ","
    columns:
      - name: category_id
        description: "分类唯一标识符"
        data_type: "VARCHAR(20)"
      - name: category_name
        description: "分类名称"
        data_type: "VARCHAR(100)"
      - name: parent_category_id
        description: "父分类ID，顶级分类为空"
        data_type: "VARCHAR(20)"
      - name: category_level
        description: "分类层级，从1开始"
        data_type: "INT"
      - name: sort_order
        description: "排序序号"
        data_type: "INT"
      - name: is_active
        description: "是否激活状态"
        data_type: "BOOLEAN"
```

**数据文件: seeds/product_categories.csv**

```csv
category_id,category_name,parent_category_id,category_level,sort_order,is_active
CAT001,电子产品,,1,1,true
CAT002,服装,,1,2,true
CAT003,图书,,1,3,true
CAT101,手机,CAT001,2,1,true
CAT102,电脑,CAT001,2,2,true
CAT103,家电,CAT001,2,3,true
CAT201,男装,CAT002,2,1,true
CAT202,女装,CAT002,2,2,true
CAT203,童装,CAT002,2,3,true
```

### 汇率配置种子数据

**配置文件: seeds/exchange_rates.yaml**

```yaml
version: 1
seeds:
  - name: exchange_rates
    description: "货币汇率配置表，用于多币种金额转换计算"
    config:
      delimiter: ","
    columns:
      - name: base_currency
        description: "基准货币代码"
        data_type: "VARCHAR(3)"
      - name: target_currency  
        description: "目标货币代码"
        data_type: "VARCHAR(3)"
      - name: exchange_rate
        description: "汇率（1基准货币=X目标货币）"
        data_type: "DECIMAL(10,6)"
      - name: effective_date
        description: "生效日期"
        data_type: "DATE"
      - name: source
        description: "汇率来源"
        data_type: "VARCHAR(50)"
```

### 状态枚举种子数据

**配置文件: seeds/order_status_mapping.yaml**

```yaml
version: 1
seeds:
  - name: order_status_mapping
    description: "订单状态映射表，标准化订单状态显示"
    columns:
      - name: status_code
        description: "状态代码"
        data_type: "VARCHAR(20)"
      - name: status_name
        description: "状态名称"
        data_type: "VARCHAR(50)"
      - name: status_category
        description: "状态分类"
        data_type: "VARCHAR(30)"
      - name: display_order
        description: "显示顺序"
        data_type: "INT"
      - name: is_final_status
        description: "是否为最终状态"
        data_type: "BOOLEAN"
```

## 文件结构和组织

### 推荐的文件结构

DAT项目推荐以下种子数据组织方式：

```
seeds/
├── country_codes.csv          # 国家代码数据文件
├── country_codes.yaml         # 国家代码配置文件
├── product_categories.csv     # 产品分类数据文件
├── product_categories.yaml    # 产品分类配置文件
├── exchange_rates.csv         # 汇率数据文件
├── exchange_rates.yaml        # 汇率配置文件
└── reference/
    ├── currencies.csv         # 货币信息
    ├── currencies.yaml        # 货币配置
    ├── time_zones.csv         # 时区信息
    └── time_zones.yaml        # 时区配置
```

### 组织原则

🗂️ **按数据类型分组**
* 将相关的种子数据放在同一目录下
* 使用子目录组织不同类型的引用数据

📝 **配置与数据配对**
* 每个CSV文件对应一个同名的YAML配置文件
* 保持文件名的一致性和可读性

🔄 **版本控制友好**
* 将种子数据文件纳入版本控制
* 便于跟踪数据变化和协作开发

## 最佳实践

### 文件命名规范

✅ **使用描述性名称**
```
# 推荐
country_codes.csv
product_categories.csv
exchange_rates.csv

# 不推荐
data1.csv
ref_table.csv
lookup.csv
```

✅ **保持命名一致性**
* 使用下划线分隔单词
* 避免特殊字符和空格
* 使用小写字母

### 数据质量管理

✅ **数据完整性**
```csv
# 推荐：完整的数据记录
country_code,country_name,continent,population
CN,中国,亚洲,1411780000
US,美国,北美洲,331900000

# 不推荐：缺失关键信息
country_code,country_name,continent,population
CN,中国,,
US,美国,北美洲,
```

✅ **数据一致性**
* 统一的日期格式（YYYY-MM-DD）
* 统一的货币格式和精度
* 统一的文本编码（UTF-8）

✅ **数据验证**
```yaml
# 为数值字段指定合适的数据类型
columns:
  - name: population
    description: "人口数量"
    data_type: "BIGINT"  # 明确指定数据类型
    
  - name: exchange_rate
    description: "汇率"
    data_type: "DECIMAL(10,6)"  # 指定精度
```

### 配置文件最佳实践

✅ **提供详细描述**
```yaml
# 推荐
seeds:
  - name: country_codes
    description: "ISO 3166-1标准国家代码表，包含国家名称、大洲、人口等基础信息，用于地理维度分析和报表展示"
    
# 不推荐
seeds:
  - name: country_codes
    description: "国家数据"
```

✅ **合理使用数据类型**
```yaml
# 根据数据特征选择合适的类型
columns:
  - name: country_code
    data_type: "VARCHAR(2)"      # 固定长度的代码
  - name: population
    data_type: "BIGINT"          # 大数值
  - name: is_active
    data_type: "BOOLEAN"         # 布尔值
  - name: created_date
    data_type: "DATE"            # 日期
```

### 性能优化

✅ **合理的文件大小**
* 单个种子文件建议不超过10MB
* 对于大型引用数据，考虑分割为多个文件
* 定期清理过时的数据记录

✅ **选择合适的分隔符**
```yaml
# 当数据包含逗号时，使用其他分隔符
config:
  delimiter: \|  # 避免数据中的逗号干扰解析
```

### 维护和更新

✅ **版本控制策略**
* 为重要的数据变更添加注释
* 使用分支管理大规模的数据更新
* 保留数据变更的历史记录

✅ **更新流程**
1. 在开发环境中测试数据变更
2. 验证数据完整性和一致性
3. 更新对应的YAML配置文件
4. 在测试环境中验证影响
5. 部署到生产环境

## 常见问题

### Q: 种子数据与语义模型有什么关系？

**A:** 种子数据可以作为语义模型的数据源（表），也可以与其他数据源（表）进行关联：

```yaml
# 在语义模型中引用种子数据
semantic_models:
  - name: orders_with_country
    model: |
      SELECT o.*, c.country_name, c.continent
      FROM orders o
      LEFT JOIN country_codes c 
        ON o.country_code = c.country_code
```

### Q: 如何处理包含特殊字符的CSV数据？

**A:** 使用适当的分隔符和引号处理：

```csv
# 当数据包含逗号时，使用引号包围
product_id,product_name,description
PROD001,"苹果 iPhone 14, 128GB","高性能智能手机，支持5G网络"
PROD002,三星 Galaxy S23,"旗舰级Android手机"
```

```yaml
# 或者使用其他分隔符
config:
  delimiter: \|
```

### Q: 种子数据文件过大时如何处理？

**A:** 有几种处理策略：

1. **数据分割**：按逻辑拆分为多个文件
```
seeds/
├── countries_asia.csv
├── countries_europe.csv
├── countries_americas.csv
└── countries_africa.csv
```

2. **数据压缩**：移除不必要的列和记录

### Q: 如何验证种子数据的正确性？

**A:** 建议的验证步骤：

1. **格式验证**：确保CSV格式正确
2. **Schema验证**：YAML配置符合JSON Schema
3. **数据验证**：检查数据类型和约束
4. **业务验证**：验证数据的业务逻辑正确性

### Q: 种子数据可以包含空值吗？

**A:** 可以，但需要注意：

```csv
# 空值的处理
country_code,country_name,population
CN,中国,1411780000
XX,未知国家,  # 空值
```

```yaml
# 在配置中说明空值含义
columns:
  - name: population
    description: "人口数量，空值表示数据未知"
    data_type: "BIGINT"
```

### Q: 如何在种子数据中处理日期格式？

**A:** 推荐使用ISO 8601标准格式：

```csv
# 推荐的日期格式
effective_date,exchange_rate
2024-01-01,6.8945
2024-01-02,6.8956
```

```yaml
columns:
  - name: effective_date
    description: "生效日期，格式：YYYY-MM-DD"
    data_type: "DATE"
```

---

> **提示**  
> 种子数据是DAT项目中管理静态引用数据的重要机制。正确配置种子数据可以提高查询效率，简化复杂的数据关联，并确保数据的一致性。建议定期维护和更新种子数据，保持数据的准确性和时效性。

---