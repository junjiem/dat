## User Guide of how to develop a DAT Project

Hi there, looks like you have already created a Project, now let's get you started with the development!

I believe you have the right project configurations for your Project while initialization it, if not, you can change it later by modifying the `dat_project.yaml` file.


### Project YAML

Now you can edit the `dat_project.yaml` file to describe your Project, here is the basic structure of it:

- version (integer, optional): Project's version
- name (string, required): Project's name
- description (string, optional): Project's description
- db (object, required): Data source configuration
  - provider (string, required): Database adapter factory identifier
  - configuration (object): Configuration options
- embedding (object, optional): Embedding model configuration
  - provider (string, required): Embedding model factory identifier
  - configuration (object): Configuration options
- embedding_store (object, optional): Embedding store configuration
  - provider (string, required): Embedding store factory identifier
  - configuration (object): Configuration options
- llms (list\[object\]): Large language models configurations
  - name (string, required): LLM's name
  - provider (string, required): Large language model factory identifier
  - configuration (object): Configuration options
- reranking (object, optional): Scoring (reranking) model configuration
  - provider (string, required): Scoring (reranking) model factory identifier
  - configuration (object): Configuration options
- content_store (object, optional): Content store configuration
  - provider (string, required): Content store factory identifier
  - configuration (object): Configuration options
- agents (list\[object\]): Ask data agents configurations
  - name (string, required): Agent's name
  - description (string, optional): Agent's description
  - provider (string, required): Askdata agent factory identifier
  - semantic_models (list\[string\], optional): You need to fill in correct semantic model names
  - configuration (object): Configuration options


> **Note:** For more project configurations, please refer to [`dat_project.yaml.template`](./dat_project.yaml.template).


### Models

[Semantic Model Configuration Guide](./MODEL_GUIDE.md)


### Examples

[Examples Configuration Guide](./EXAMPLE_GUIDE.md)


### Seeds

[Seed Configuration Guide](./SEED_GUIDE.md)


## Run Agent specified in the Project

After all, just run your Agent by running the following command:

```bash
dat run -p ./ROOT_DIRECTORY_OF_YOUR_PROJECT -a AGENT_NAME
```

Start the Ask data interactive command line.

> **Note:** Incremental build will be automatically performed before run.

