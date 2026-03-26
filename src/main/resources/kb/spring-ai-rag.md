# Spring AI RAG Key Concepts

## ChatClient
`ChatClient` is a unified chat invocation entry point provided by Spring AI, suitable for initiating prompt calls directly in business code.

## VectorStore
`VectorStore` is used to store document vectors and perform similarity search.
In demo scenarios, you can prioritize using `SimpleVectorStore` because it doesn't depend on external databases and is suitable for quick local prototyping.

## QuestionAnswerAdvisor
`QuestionAnswerAdvisor` is one of the easiest RAG integration approaches in Spring AI.
When a user asks a question, it will:
- First retrieve relevant document fragments from `VectorStore`;
- Then append these fragments to the prompt;
- Finally pass it to the chat model to generate an answer.

## Document Import Process
A minimal knowledge base import process typically involves:
1. Reading documents using `MarkdownDocumentReader` or PDF Reader;
2. Splitting documents using `TokenTextSplitter`;
3. Building the index by calling `vectorStore.add()`.