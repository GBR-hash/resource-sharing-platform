"""
可视化 OpsAgent LangGraph 工作流
"""
import os
import sys

# 确保项目根目录在 Python 路径中
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.agent.graph import agent_graph


def print_ascii_graph():
    """打印 ASCII 格式的工作流图"""
    print("=" * 80)
    print("OpsAgent LangGraph 工作流 - ASCII 视图")
    print("=" * 80)
    print()

    graph = agent_graph.get_graph()
    print(graph.draw_ascii())


def print_mermaid_graph():
    """生成 Mermaid 语法的工作流图"""
    print("=" * 80)
    print("OpsAgent LangGraph 工作流 - Mermaid 语法")
    print("=" * 80)
    print()
    print("将以下内容复制到支持 Mermaid 的工具中查看 (如 Typora, VS Code, GitHub):")
    print()

    graph = agent_graph.get_graph()
    mermaid_code = graph.draw_mermaid()
    print("```mermaid")
    print(mermaid_code)
    print("```")


def print_detailed_structure():
    """打印详细的工作流结构"""
    print("=" * 80)
    print("OpsAgent LangGraph 工作流 - 详细结构")
    print("=" * 80)
    print()

    graph = agent_graph.get_graph()

    print("【节点列表】")
    print("-" * 40)
    for node_id, node in graph.nodes.items():
        print(f"  - {node_id}: {node.__class__.__name__}")

    print()
    print("【边列表】")
    print("-" * 40)
    for edge in graph.edges:
        print(f"  {edge[0]} → {edge[1]}")

    print()
    print("【条件路由说明】")
    print("-" * 40)
    print("  router → {rag: 诊断/动作/查询意图, respond: 普通对话}")
    print("  diagnosis → {java_client: 需要更多数据, respond: 诊断完成, human_approval: 需要执行动作}")
    print("  verification → {respond: 修复成功, diagnosis: 修复失败需重新诊断}")


if __name__ == "__main__":
    print_detailed_structure()
    print("\n\n")
    print_ascii_graph()
    print("\n\n")
    print_mermaid_graph()
