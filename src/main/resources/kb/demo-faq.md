# RAG Demo FAQ

## What tech stack does this demo use?
This demo uses Spring Boot, Spring AI, Spring AI Alibaba, DashScope, SimpleVectorStore, and Markdown documents.

## Why choose SimpleVectorStore?
Because it is best suited for quick local demos without requiring additional installation of PostgreSQL, Milvus, or Elasticsearch.

## How to replace the knowledge base?
Replace the markdown files in the `src/main/resources/kb/` directory with your business documents, then call `/rag/rebuild` to rebuild the index.

## What questions is it suitable for?
Suitable for answering questions that are "clearly written in the knowledge base", such as:
- What chat model does this demo use?
- What embedding model does this demo use?
- How to rebuild the index?

## When will it answer "I don't know"?
When the question is not present in the knowledge base documents, or when relevant fragments are not retrieved, the system should answer "I don't know".