# Cosense MCP Server

Java / Spring AI で実装した、プライベートな [Cosense（旧Scrapbox）](https://scrapbox.io/) ページを  
AIツールから安全に参照するための[MCP（Model Context Protocol）サーバー](https://modelcontextprotocol.io/docs/learn/server-concepts)です。

## 概要

Claude Desktop などのMCP対応AIツールから、本MCPサーバーを介して、  
プライベートCosenseプロジェクトに蓄積されたナレッジを安全に活用できるようになります。

```
AIツール (Claude等)
    │  MCP (STDIO)
    ▼
Cosense MCP Server  (本プロジェクト)
    │  HTTPS
    ▼
Cosense API (scrapbox.io)
```

## 機能

| MCPツール名            | 説明                                                           |
| ---------------------- | -------------------------------------------------------------- |
| `cosense_search_pages` | キーワードでタイトル・本文を全文検索しページタイトル一覧を返す |
| `cosense_get_page`     | 指定タイトルのページ本文を全量取得して返す                     |

## 技術スタック

- Java 25 / Spring Boot 4.1.0 / Spring AI 2.0.0
- MCP STDIO プロトコル

## セットアップ

### 前提条件

- Java 25 以上
- Maven 3.9 以上（同梱の `mvnw` でも可）
- Cosenseの対象プロジェクトへのアクセス権（プライベートプロジェクトの場合は認証クッキーが必要）

### ビルド

```bash
./mvnw clean package
```

### `connect.sid` の取得

1. ブラウザで [Cosense](https://scrapbox.io) にログインする。
2. ブラウザの開発者ツール → Application タブ → Cookies → `https://scrapbox.io` を開く。
3. `connect.sid` の値をコピーする（`s%3A...` から始まる文字列）。

### MCPクライアントへの登録

#### Claude Desktop の場合

`claude_desktop_config.json`

```json
{
  "mcpServers": {
    "cosense": {
      "command": "java",
      "args": ["-jar", "/path/to/cosense-mcp-1.0.0-SNAPSHOT.jar"],
      "env": {
        "COSENSE_PROJECT_NAME": "your-project-name",
        "COSENSE_CONNECT_SID": "s%3A..."
      }
    }
  }
}
```

#### その他のMCPクライアント

```bash
COSENSE_PROJECT_NAME=your-project \
COSENSE_CONNECT_SID='s%3A...' \
java -jar cosense-mcp-1.0.0-SNAPSHOT.jar
```

## 環境変数

| 変数名                 | 必須 | 説明                            |
| ---------------------- | ---- | ------------------------------- |
| `COSENSE_PROJECT_NAME` | ✓    | 対象Cosenseプロジェクト名       |
| `COSENSE_CONNECT_SID`  | ✓    | 認証クッキー値（`connect.sid`） |

## ドキュメント

詳細は [`documents/`](documents/) フォルダを参照してください。

- [要件定義書](documents/requirements.md)
- [システム仕様書](documents/specification.md)
