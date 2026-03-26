# Alibaba Bailian and DashScope Overview

## Platform Positioning
Alibaba Bailian is a large model service platform provided by Alibaba Cloud, and DashScope is its model API invocation entry point. Developers typically invoke model capabilities through an API Key.

## Common Chat Models
- `qwen-plus`: Suitable for most general Q&A and application integration scenarios.
- `qwen-turbo`: Lower cost, suitable for lightweight Q&A.
- `qwen-max`: More capable, suitable for complex tasks.

## Embedding Models
- `text-embedding-v3`: Suitable for semantic search and RAG.
- `text-embedding-v3` supports multiple dimensions, common optional dimensions include 1024, 768, 512, 256, 128, 64.

## Authentication Methods
- Recommended to inject API Key through environment variable `AI_DASHSCOPE_API_KEY`.
- Common configuration item in Spring AI Alibaba is `spring.ai.dashscope.api-key`.

## Role in RAG
In a minimal RAG application:
1. First use the embedding model to embed knowledge base documents after splitting.
2. Save vectors to the vector store.
3. When a user asks a question, first perform similarity search.
4. Then append retrieved fragments to the prompt for the chat model to answer.