# 示例配置指南

## 1. 目的与总体说明

`examples` 配置用于给 DAT Agent 提供额外的语义线索，帮助模型理解自然语言问题、识别别名与背景知识。
必须在`models`目录下的`.yaml`文件中配置 `examples` ， `examples` 是一个对象，可包含如下三个可选模块中的任意一个：

- `sql_pairs`
- `synonyms_pairs`
- `knowledge`

> **注意**：至少需要配置其中一个模块（`sql_pairs`、`synonyms_pairs` 或 `knowledge`），否则无法通过 Schema 校验。


## 2. 配置总体结构

```yaml
examples:
  sql_pairs:
    - question: <字符串，必填>
      sql: |
        <ANSI SQL 语句，必填>
  synonyms_pairs:
    - word: <字符串，必填>
      synonyms:
        - <字符串，同义词，至少 1 个>
  knowledge:
    - |
      <字符串，自由文本，至少 1 条>
```

- `sql_pairs`、`synonyms_pairs`、`knowledge` 三个数组均要求元素唯一（`uniqueItems: true`）。
- 可按需同时配置多个模块，也可只配置其中一个。

项目模板中已提供示例文件供参考：

- `models/examples/sql_examples.yaml`
- `models/examples/synonyms_examples.yaml`
- `models/examples/knowledge_examples1.yaml`
- `models/examples/knowledge_examples2.yaml`

## 3. `sql_pairs` 问答 SQL 对

`sql_pairs` 用于存储自然语言问题与对应语义 SQL 的映射，提升 Agent 生成 SQL 的准确性。

| 字段 | 必填 | 说明                          |
| --- | --- |-----------------------------|
| `question` | 是 | 自然语言提问，建议贴合业务语境，避免技术术语。     |
| `sql` | 是 | 与问题匹配的 ANSI SQL （语义SQL） 查询。 |

编写建议：

- 使用 ANSI SQL，避免数据库特定方言。
- 同一问题可提供多条公式化查询，但请避免重复完全相同的记录。
- 可以复制 `.dat/question_sql_pair_history` 中的真实问答对作为种子。

示例（节选自 `models/examples/sql_examples.yaml`）：

```yaml
examples:
  sql_pairs:
    - question: 各个国家的平均病例数
      sql: |
        SELECT country_covid_cases.country,
               AVG(country_covid_cases.cases_total) AS average_cases
        FROM country_covid_cases
        GROUP BY country_covid_cases.country
```

## 4. `synonyms_pairs` 同义词对

`synonyms_pairs` 用于增强词汇识别能力，让 Agent 可以理解同义词、缩写、英文/中文混用等情况。

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `word` | 是 | 业务关键词或字段名。 |
| `synonyms` | 是 | 该关键词的同义词列表，至少 1 个条目，所有条目需唯一。 |

编写建议：

- 覆盖常见别名、中文/英文写法、大小写变体与缩写。
- 对于实体名、度量名，可添加业务同义词以提升问答体验。

示例（节选自 `models/examples/synonyms_examples.yaml`）：

```yaml
examples:
  synonyms_pairs:
    - word: COVID-19
      synonyms:
        - 新型冠状病毒肺炎
        - 新冠
        - 2019冠状病毒病
```

## 5. `knowledge` 业务知识库

`knowledge` 是纯文本数组，用于补充领域背景知识、术语解释、政策规则等，帮助 Agent 在回答时引用正确的业务信息。

- 每个条目是一个字符串，可使用 `|` 保留多行格式。
- 建议以段落形式描述完整语义，不必过度简化。

示例（节选自 `models/examples/knowledge_examples1.yaml`）：

```yaml
examples:
  knowledge:
    - |
      COVID-19（新型冠状病毒肺炎）是由 SARS-CoV-2 病毒引起的呼吸道传染病。
      主要传播途径为飞沫和密切接触，在相对封闭的环境中可经气溶胶传播。
      典型症状包括发热、干咳、乏力等。
```

## 6. 最佳实践与校验

- **保持更新**：定期将新的问答对、同义词与业务知识纳入配置，确保 Agent 与业务同步。
- **去重检查**：添加条目前先检查是否已存在相同内容，避免违反唯一性约束。
- **语义一致**：问答 SQL 对必须与语义模型保持一致，引用的表字段需在模型中定义。
- **格式化工具**：推荐使用支持 YAML + JSON Schema 校验的编辑器，确保格式正确。
- **版本控制**：对 `examples` 的更新应与数据模型变更同步提交，便于回溯。

按照以上指南构建 `examples` 配置，可显著提升 DAT 项目的问答准确率与业务适配能力。

