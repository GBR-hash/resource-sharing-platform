from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import MarkdownHeaderTextSplitter
from langchain_core.documents import Document
import os

# 项目根目录
_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class KnowledgeRetriever:
    def __init__(self, file_path: str = None):
        if file_path is None:
            file_path = os.path.join(_PROJECT_ROOT, "knowledge-base.md")
        self.file_path = file_path
        self.documents = []
        self.load_and_split()

    def load_and_split(self):
        if not os.path.exists(self.file_path):
            print(f"Warning: {self.file_path} not found.")
            return

        loader = TextLoader(self.file_path, encoding="utf-8")
        raw_text = loader.load()[0].page_content

        # 按 Markdown 标题层级切分
        headers_to_split_on = [
            ("#", "Header 1"),
            ("##", "Header 2"),
            ("###", "Header 3"),
        ]
        splitter = MarkdownHeaderTextSplitter(headers_to_split_on=headers_to_split_on)
        self.documents = splitter.split_text(raw_text)

    def search(self, query: str, top_k: int = 3) -> str:
        """简单的关键词匹配检索 (生产环境建议替换为 VectorStore)"""
        if not self.documents:
            return "No knowledge base loaded."

        results = []
        query_lower = query.lower()

        for doc in self.documents:
            if query_lower in doc.page_content.lower():
                results.append(doc.page_content)
                if len(results) >= top_k:
                    break

        if not results:
            return "No relevant information found in the knowledge base."

        return "\n\n---\n\n".join(results)