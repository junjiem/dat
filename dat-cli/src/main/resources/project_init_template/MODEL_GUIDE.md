# 语义模型配置指南

**本页指南包含如下内容**

* [语义模型概述](#语义模型概述)
* [语义模型组件](#语义模型组件)
  * [基本属性](#基本属性)
  * [实体配置](#实体配置)
  * [维度配置](#维度配置)
  * [度量配置](#度量配置)
* [配置示例](#配置示例)
* [文件结构和组织](#文件结构和组织)
* [最佳实践](#最佳实践)
* [常见问题](#常见问题)

## 语义模型概述

**语义模型是DAT系统的核心**，它定义了数据的业务含义和结构，使AI能够理解用户的自然语言查询并转换为精确的SQL语句。

* **语义模型由三个核心组件构成**：
  * **实体 (entities)**: 描述各种语义模型之间的**关系**（类似主键/外键）
  * **维度 (dimensions)**: 你想要**切片、分组和过滤**的列（时间戳、类别等）
  * **度量 (measures)**: 你想要**聚合的数量值**

* 每个语义模型对应一个数据模型，通过YAML配置文件进行定义
* 在`models`目录下的`.yaml`文件中配置语义模型
* 语义模型纯粹通过YAML定义，描述关系和表达式

## 语义模型组件

### 基本属性

每个语义模型的完整规范如下：

```yaml
version: 1
semantic_models:
  - name: 语义模型名称                    ## 必填
    description: 模型描述               ## 可选
    model: 详见model配置示例          ## 可选
    alias: 语义模型别名                 ## 可选
    tags: [标签列表]                   ## 可选
    defaults:                         ## 可选
      agg_time_dimension: 维度名称
    entities:                         ## 必填
      - 详见实体配置章节
    dimensions:                       ## 必填  
      - 详见维度配置章节
    measures:                         ## 可选
      - 详见度量配置章节
```

下表描述了语义模型的基本属性：

| 组件                        | 描述                                                   | 必填 | 类型     |
|---------------------------|------------------------------------------------------|------|--------|
| **name**                  | 语义模型的唯一名称。                                           | 必填 | String |
| **description**           | 包含重要详细信息的描述                                          | 可选 | String |
| **[model](#model配置示例)**   | 使用`ref`函数指定数据模型、设置`查询SQL语句`或配置`库表名`，不填默认将`语义模型名直接映射为数据模型名` | 可选 | String |
| **alias**                 | 语义模型的别名                                              | 可选 | String |
| **tags**                  | 语义模型的标签，用于分类和检索的标签数组                                 | 可选 | Array  |
| **defaults**              | 模型的默认配置，目前仅支持agg_time_dimension                      | 可选 | Object |
| **[entities](#实体配置)**     | 作为连接键的列，指示其类型为primary、foreign或unique                 | 必填 | List   |
| **[dimensions](#维度配置)** | 对度量进行分组或切片的不同方式，可以是时间或分类                             | 必填 | List   |
| **[measures](#度量配置)**     | 应用于数据模型中列的聚合。可以是最终度量或复杂度量的构建块                        | 可选 | List   |


#### model配置示例

#### 方式一：引用数据模型（推荐）

```yaml
semantic_models:
  - name: covid_cases
    model: ref('country_covid_cases')
```

> **注：** 数据模型（models目录下.sql后缀）文件中支持 [Jinja](https://jinja.palletsprojects.com) 模板语言，在执行`build`、`run`、`server`命令时可以指定需要传入的变量。
>
> 示例：models/**/country_covid_cases.sql
> ```sql
> select CAST(STR_TO_DATE(cases.date_rep, '%d/%m/%Y') AS DATE) as report_date, -- 报告日期
>        cases.cases                                           as cases, -- 病例数
>        cases.deaths                                          as deaths, -- 死亡数
>        country_codes.country                                 as country, -- 国家
>        cases.geo_id                                          as geo_id -- 地理标识
> from covid_cases as cases
>          join country_codes
>               on cases.geo_id = country_codes.alpha_2code
> {% if start_date -%}
> where CAST(STR_TO_DATE(cases.date_rep, '%d/%m/%Y') AS DATE) > '{{ start_date }}'
> {% endif %}
> ;
> ```
>
> ```shell
> dat run -var start_date="2020-01-01" -var key1=value1 -var k2=v2
> ```

#### 方式二：设置查询SQL语句

```yaml
semantic_models:
  - name: covid_cases
    model: select * from ...
```

> **注：** 模型SQL中支持 [Jinja](https://jinja.palletsprojects.com) 模板语言，在执行`build`、`run`、`server`命令时可以指定需要传入的变量。
> 
> 示例：
> 
> ```yaml
> semantic_models:
>  - name: covid_cases
>    model: |
>      select
>        CAST(STR_TO_DATE(date_rep, '%d/%m/%Y') AS DATE) as date_rep,
>        cases,
>        deaths,
>        geo_id
>      from covid_cases
>      {% if start_date -%}
>      where CAST(STR_TO_DATE(date_rep, '%d/%m/%Y') AS DATE) > '{{ start_date }}'
>      {% endif %}
> ```
> 
> ```shell
> dat run -var start_date="2020-01-01" -var key1=value1 -var k2=v2
> ```

#### 方式三：配置库表名

完整表名
```yaml
semantic_models:
  - name: covid_cases
    model: covid.covid_cases
```

简单表名
```yaml
semantic_models:
  - name: covid_cases
    model: covid_cases
```

#### 方式四：不配置model

```yaml
semantic_models:
  - name: covid_cases
```
等同于
```yaml
semantic_models:
  - name: covid_cases
    model: ref('covid_cases')
```

### 实体配置

**实体是数据中拥有维度和度量的对象和概念**。你可以将它们视为项目的**名词**、查询的**脊柱**或简单的**连接键**。

#### 实体类型

| 类型 | 描述 |
|------|------|
| **primary** | 主键对于表中的每一行只有一条记录，并包括数据平台中的每条记录。它必须包含唯一值，并且不能包含空值。使用主键可确保表中的每条记录都是不同且可识别的。 |
| **foreign** | 外键是一个表中的一个字段（或一组字段），用于唯一标识另一个表中的一行。外键在两个表中的数据之间建立链接。它可以包括同一记录的零个、一个或多个实例。它还可以包含空值。 |
| **unique** | 唯一键在表中每行仅包含一条记录，但在数据仓库中可能包含记录的子集。但是，与主键不同，唯一键允许空值。唯一键确保列的值是不同的，空值除外。 |

#### 实体配置参数

| 参数              | 描述                          | 必填 | 类型 |
|-----------------|-----------------------------|------|------|
| **name**        | 实体名称，语义模型内必须唯一              | 必填 | String |
| **type**        | 实体类型：primary、foreign或unique | 必填 | String |
| **description** | 实体的描述                       | 可选 | String |
| **alias**       | 实体的别名                       | 可选 | String |
| **expr**        | 引用现有列或使用SQL表达式创建新列          | 可选 | String |
| **data_type**   | 数据类型（这因数据库而异）          | 可选 | String |

#### 实体配置示例

```yaml
entities:
  - name: order_id
    type: primary
    description: "订单唯一标识符"
    alias: "订单ID"
    
  - name: customer
    type: foreign  
    expr: customer_id
    description: "客户标识符"
    alias: "客户"
    
  - name: location
    type: foreign
    expr: location_id  
    description: "店铺位置标识符"
    alias: "门店"
```

#### 使用键组合列示例

如果表没有任何键（如：主键），请使用代理组合形成一个键，该键将帮助您通过组合两列来识别记录。这适用于任何实体类型。 例如，你可以组合 `raw_brand_target_weekly` 表中的 `date_key` 和 `brand_code` 以形成代理键。

以下示例通过使用管道 (`|`) 作为分隔符连接 `date_key` 和 `brand_code` 来创建代理键。

```yaml
entities:
  - name: brand_target_key # 实体名称或标识
    type: foreign # 可以是任何实体类型键
    expr: date_key || '|' || brand_code # 定义链接字段以形成代理键的表达式
```

### 维度配置

**维度代表数据集中不可聚合的列**，它们是描述或分类数据的属性、特征或特性。在SQL中，维度通常包含在查询的`group by`子句中。

#### 维度类型

* **categorical（分类）**: 描述属性或特征，如地理位置或销售区域
* **time（时间）**: 基于时间的维度，如时间戳或日期

#### 维度配置参数

| 参数 | 描述 | 必填 | 类型     |
|------|------|------|--------|
| **name** | 维度名称，在同一语义模型内必须唯一 | 必填 | String |
| **type** | 维度类型：categorical或time | 必填 | String |
| **type_params** | 特定类型参数，如时间粒度 | 可选 | Object |
| **description** | 维度的清晰描述 | 可选 | String |
| **alias** | 维度的别名 | 可选 | String |
| **expr** | 定义底层列或SQL查询 | 可选 | String |
| **enum_values** | 分类维度的枚举值定义 | 可选 | Array  |
| **data_type** | 数据类型（这因数据库而异） | 可选 | String |

#### 分类维度示例

```yaml
dimensions:
  - name: order_status
    type: categorical
    description: "订单状态"
    alias: "订单状态"
    expr: status
    enum_values:
      - value: "pending"
        label: "待处理"
      - value: "processing" 
        label: "处理中"
      - value: "shipped"
        label: "已发货"
      - value: "delivered"
        label: "已送达"

  - name: product_category
    type: categorical
    description: "产品类别"
    alias: "商品分类"
    enum_values:
      - value: "electronics"
        label: "电子产品"
      - value: "clothing"
        label: "服装"
      - value: "books"
        label: "图书"
```

#### 时间维度示例

```yaml
dimensions:
  - name: order_date
    type: time
    description: "订单创建日期"  
    alias: "下单时间"
    expr: "created_at"
    type_params:
      time_granularity: day

  - name: registration_month
    type: time
    description: "客户注册月份"
    alias: "注册月份" 
    expr: "DATE_TRUNC('month', registration_date)"
    type_params:
      time_granularity: month
```

#### 时间粒度选项

时间维度支持以下粒度：

* `second` - 秒
* `minute` - 分钟  
* `hour` - 小时
* `day` - 天
* `week` - 周
* `month` - 月
* `quarter` - 季度
* `year` - 年

### 度量配置

**度量是对模型中列执行的聚合操作**。它们可以作为最终度量或作为更复杂度量的构建块。

#### 度量配置参数

| 参数 | 描述 | 必填 | 类型 |
|------|------|------|------|
| **name** | 度量名称，在所有语义模型中必须唯一 | 必填 | String |
| **description** | 描述计算的度量 | 可选 | String |
| **alias** | 度量的别名 | 可选 | String |
| **expr** | 引用现有列或使用SQL表达式创建新列 | 可选 | String |
| **data_type** | 数据类型（这因数据库而异） | 可选 | String |
| **agg** | 聚合类型 | 可选 | String |
| **agg_time_dimension** | 时间字段，默认为语义模型的默认聚合时间维度 | 可选 | String |
| **non_additive_dimension** | 为无法按某些维度聚合的度量指定非加性维度 | 可选 | Object |

#### 支持的聚合类型

| 聚合类型 | 描述 |
|----------|------|
| **sum** | 对值求和 |
| **min** | 最小值 |
| **max** | 最大值 |
| **avg** | 平均值 |
| **count** | 计数 |
| **count_distinct** | 去重计数 |
| **median** | 中位数计算 |
| **sum_boolean** | 布尔类型求和 |
| **none** | 无聚合（默认值） |

#### 度量配置示例

```yaml
measures:
  - name: total_amount
    description: "订单总金额"
    alias: "总金额"
    expr: amount
    agg: sum
    agg_time_dimension: "order_date"

  - name: order_count  
    description: "订单数量"
    alias: "订单数"
    expr: "1"
    agg: count

  - name: avg_order_value
    description: "平均订单价值"
    alias: "平均订单金额"
    expr: amount
    agg: avg

  - name: unique_customers
    description: "独特客户数量"
    alias: "客户数"
    expr: customer_id
    agg: count_distinct
```

#### 非加性维度

对于某些度量（如银行账户余额），不能简单地跨某些维度进行聚合，以避免产生不正确的结果：

```yaml
measures:
  - name: account_balance
    description: "账户余额"
    expr: balance
    agg: sum
    non_additive_dimension:
      name: "balance_date"
      window_choice: "max"
      window_groupings: ["account_id"]
```

## 配置示例

### 完整订单语义模型

```yaml
version: 1
semantic_models:
  - name: orders
    description: "订单事实表，包含所有订单交易信息"
    alias: "订单"
    model: "ref('stg_orders')"
    tags: ["sales", "transaction", "core"]
    defaults:
      agg_time_dimension: order_date
    
    entities:
      - name: order_id
        type: primary
        description: "订单唯一标识符"
        alias: "订单ID"
      
      - name: customer
        type: foreign
        expr: customer_id
        description: "客户标识符"
        alias: "客户"

      - name: location
        type: foreign
        expr: location_id
        description: "店铺位置标识符"
        alias: "门店"
    
    dimensions:
      - name: order_date
        type: time
        description: "订单创建日期"
        alias: "下单时间"
        expr: "created_at"
        type_params:
          time_granularity: day
      
      - name: order_status
        type: categorical
        description: "订单状态"
        alias: "订单状态"
        expr: status
        enum_values:
          - value: "pending"
            label: "待处理"
          - value: "processing"
            label: "处理中"
          - value: "shipped"
            label: "已发货"
          - value: "delivered"
            label: "已送达"
          - value: "cancelled"
            label: "已取消"
      
      - name: payment_method
        type: categorical
        description: "支付方式"
        alias: "支付方式"
        enum_values:
          - value: "credit_card"
            label: "信用卡"
          - value: "debit_card"
            label: "借记卡"
          - value: "paypal"
            label: "PayPal"
          - value: "cash"
            label: "现金"
    
    measures:
      - name: total_amount
        description: "订单总金额"
        alias: "总金额"
        expr: amount
        agg: sum
        agg_time_dimension: "order_date"
      
      - name: order_count
        description: "订单数量"
        alias: "订单数"
        expr: "1"
        agg: count
      
      - name: avg_order_value
        description: "平均订单价值"
        alias: "平均订单金额"
        expr: amount
        agg: avg
```

### 客户维度模型

```yaml
version: 1
semantic_models:
  - name: customers
    description: "客户维度表，包含客户的基本信息"
    alias: "客户"
    model: "ref('dim_customers')"
    tags: ["customer", "dimension"]
    defaults:
      agg_time_dimension: registration_date
    
    entities:
      - name: customer_id
        type: primary
        description: "客户唯一标识符"
        alias: "客户ID"
      
      - name: email
        type: unique
        description: "客户邮箱地址"
        alias: "邮箱"
    
    dimensions:
      - name: registration_date
        type: time
        description: "客户注册日期"
        alias: "注册时间"
        expr: "created_at"
        type_params:
          time_granularity: day
      
      - name: customer_segment
        type: categorical
        description: "客户细分"
        alias: "客户类型"
        enum_values:
          - value: "vip"
            label: "VIP客户"
          - value: "regular"
            label: "普通客户"
          - value: "new"
            label: "新客户"
      
      - name: age_group
        type: categorical
        description: "年龄段"
        alias: "年龄组"
        expr: "CASE 
                WHEN age < 25 THEN 'young'
                WHEN age BETWEEN 25 AND 45 THEN 'middle'
                ELSE 'senior'
               END"
        enum_values:
          - value: "young"
            label: "年轻客户(25岁以下)"
          - value: "middle"
            label: "中年客户(25-45岁)"
          - value: "senior"
            label: "中老年客户(45岁以上)"
    
    measures:
      - name: customer_count
        description: "客户总数"
        alias: "客户数量"
        expr: "1"
        agg: count_distinct
```

## 文件结构和组织

### 推荐的文件结构

DAT项目推荐以下文件组织方式：

```
models/
├── customers.sql       # 客户数据模型
├── customers.yaml       # 客户语义模型
└── marts/
    ├── orders.sql          # 订单数据模型
    ├── orders.yaml          # 订单语义模型
    ├── products.sql        # 产品数据模型
    └── products.yaml        # 产品语义模型
```

### 两种组织方式

🏡 **共置方式**
* 将语义模型配置与对应的数据模型放在同一个YAML文件中
* 减少文件切换，更适合小项目

🏘️ **分离方式**  
* 将语义模型配置与对应的数据模型分别放在YAML和SQL文件中
* 便于维护，更适合大项目

## 最佳实践

### 命名规范

✅ **使用有意义的名称**
```yaml
# 推荐
- name: customer_lifetime_value
  description: "客户生命周期价值"
  alias: "客户LTV"

# 不推荐  
- name: clv_calc
  description: "LTV calculation field"
```

✅ **保持一致性**
* 在整个项目中使用一致的命名约定
* 使用业务用户能理解的术语，避免技术术语

✅ **提供清晰的描述和别名**
```yaml
# 推荐
- name: monthly_recurring_revenue
  description: "月度经常性收入，指每月可预期的稳定收入"
  alias: "月度经常性收入"

# 不推荐
- name: mrr
  description: "MRR"
```

### 维度设计

✅ **合理选择维度类型**
* 根据数据特性选择合适的维度类型
* 为分类维度提供完整的枚举值列表
* 根据业务需求选择时间维度的粒度

✅ **时间维度最佳实践**
```yaml
# 推荐：根据业务需求选择合适的粒度
- name: order_month
  description: "订单月份"
  type: time
  expr: "DATE_TRUNC('month', order_date)"
  type_params:
    time_granularity: month
```

✅ **分类维度最佳实践**  
```yaml
# 推荐：提供完整的枚举值
- name: product_category
  description: "产品类别"
  type: categorical
  enum_values:
    - value: "electronics"
      label: "电子产品"
    - value: "clothing"
      label: "服装"
    - value: "books"
      label: "图书"
```

### 度量设计

✅ **选择合适的聚合函数**
```yaml
# 普通度量
- name: revenue
  description: "收入总额"
  expr: amount
  agg: sum

# 计数度量
- name: order_count
  description: "订单数量"
  expr: "1"
  agg: count
```

✅ **处理非加性度量**
```yaml
# 非加性度量（如余额）
- name: account_balance
  description: "账户余额"
  expr: balance
  agg: sum
  non_additive_dimension:
    name: "snapshot_date"
    window_choice: "max"
    window_groupings: ["account_id"]
```

### 配置验证

🔍 **语法验证**
* 确保YAML文件语法正确
* 使用支持JSON Schema验证的编辑器

🔍 **Schema验证**  
* 配置文件必须符合DAT定义的JSON Schema规范
* 推荐使用支持JSON Schema验证的编辑器

🔍 **业务逻辑测试**
* 验证实体关系是否正确
* 确认维度分类是否完整
* 测试度量聚合结果是否符合预期

## 常见问题

### Q: 如何处理复杂的SQL表达式？

**A:** 可以在expr字段中使用完整的SQL表达式，包括函数调用、条件判断等：

```yaml
- name: profit_margin
  description: "利润率"
  expr: "(revenue - cost) / revenue * 100"
  agg: avg
```

### Q: 时间维度的粒度如何选择？

**A:** 根据业务需求和数据查询频率选择：
* **实时分析**: 选择较细粒度（hour, minute）
* **报表分析**: 选择较粗粒度（day, month）  
* **趋势分析**: 选择适中粒度（week, month）

### Q: 何时使用非加性维度？

**A:** 当度量值不能简单相加时使用，典型场景包括：
* 账户余额
* 库存数量
* 温度平均值
* 比率和百分比

### Q: 如何组织多个语义模型？

**A:** 建议按业务域或数据表组织：
* 一个YAML文件对应一个主要的业务实体
* 相关的维度表可以合并在一个文件中
* 保持文件大小适中，便于维护

### Q: 实体类型如何选择？

**A:** 根据数据特征选择：
* **primary**: 表的主键，每行唯一
* **foreign**: 外键，可以有多个相同值
* **unique**: 唯一键，但可能有空值

---

> **警告**  
> 语义模型是DAT项目的核心，正确的配置能够显著提升AI理解用户查询的准确性。务必从业务角度思考模型设计，确保配置的实体、维度和度量能够准确反映业务现实。

---

