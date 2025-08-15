
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/junjiem/dat)


![DAT](./images/dat.png)

DAT (Data Ask Tool): Dating with your data.


---


## Definition

Asking yours data in a natural language way through pre-modeling (data models and semantic models).


---


## Description

```
 .--------------.
/  dat language  \
|    .--------.   |
|   /          \  |
|  | dat engine | |
|   \          /  |
|    '--------'   |
\                /
 '--------------'
caption: the dat framework
```

The language and the engine create the dat framework.

dat的“语言”涵盖了在dat项目中编写的所有内容。也可以将其称为“创作层”————即你项目中的所有代码。

我们正在进入生成式人工智能的新时代，语言是界面，数据是燃料。  
人工智能要想在企业中兑现其承诺，就必须能够流畅、安全、准确地使用业务数据语言。
但要让企业真正采用，我们必须解决数据上下文的难题——确保人工智能不仅能自信地表达，而且能正确地表达。


---


## Conception

1. DAT CLI只是用于本地开发、单元测试、调试使用，它可以在本地通过IDE（vscode、idea或eclipse中）开发dat智能问数项目，将提示（上下文）工程转变成数据工程。
2. DAT 它不是一个platform，而是一个framework；二次开发者可以开发自己的Web UI，可以是web ide、拖拉拽的workflow、列表等交互方式。
3. 这种模式可以让数据工程师或数据分析师可以借鉴软件工程师开发应用一般来开发智能问数应用。


---

## Feature

- 1. 数据模型（表或视图）的配置；
- 2. 语义模型（与数据模型绑定）的配置，包括：实体、维度、度量等；
- 3. 基于LLM的生成语义SQL，将语义SQL转真实SQL，最后执行返回数据；
- 4. 智能问数支持 HITL (Human-in-the-Loop) 交互；
- 5. 基于LLM的数据探查辅助生成语义模型；（TODO）
- 6. 数据模型、语义模型、智能问数的单元测试；（TODO）
- 7. SQL问答对、文本内容等向量化入库与检索；（TODO）
- 8. 指标的配置（构建语义模型后可以更进一步添加指标）；（TODO）



---

## Star history

[![Star History Chart](https://api.star-history.com/svg?repos=junjiem/dat&type=Date)](https://star-history.com/#junjiem/dat&Date)


---

## License

This project uses the Apache 2.0 license. For details, please refer to the [LICENSE](https://github.com/junjiem/dat/blob/main/LICENSE) file.

