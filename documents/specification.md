# システム仕様書：Cosense MCP Server

## 1. 技術スタック

- **言語:** Java 25
- **フレームワーク:** Spring Boot 4.1.0 / Spring AI (spring-ai-starter-mcp-server)
- **ビルドツール:** Maven (`pom.xml`)
- **HTTPクライアント:** `RestClient` (同期通信)
- **JSONパース:** Jackson (Recordへの自動マッピング)
- **通信プロトコル:** MCP STDIO (標準入出力経由の通信)

## 2. 環境変数 (Environment Variables)

起動時に以下の環境変数を必須とする。

- `COSENSE_PROJECT_NAME`: 対象とするCosenseのプロジェクト名（固定）
- `COSENSE_CONNECT_SID`: プライベートプロジェクト認証用のクッキー値 (`connect.sid`)

## 3. MCP ツール定義 (Tools)

### ① `cosense_search_pages`

- **概要**  
  指定されたキーワードでCosenseプロジェクト内を「全文検索（タイトルおよび本文）」し、  
  マッチしたページの候補（タイトル一覧）を返す。

- **引数 (JSON Schema)**
  - `query` (string, required): 検索キーワード（単語やエラーメッセージなど）

- **APIエンドポイント**  
  `GET https://scrapbox.io/api/pages/{project}/search/query?q={query}`

- **レスポンス構造（抜粋）**

  ```json
  {
    "pages": [
      { "title": "ページタイトル1" },
      { "title": "ページタイトル2" },
      { "title": "ページタイトル3" }
    ]
  }
  ```

- **戻り値**  
  `pages[].title` を抽出したタイトルの一覧。0件の場合は空リスト。

### ② `cosense_get_page`

- **概要**  
  指定されたタイトルのページ内容（本文テキスト全量）を取得する。

- **引数 (JSON Schema)**
  - `title` (string, required): ページタイトル

- **APIエンドポイント**  
  `GET https://scrapbox.io/api/pages/{project}/{title}`

- **レスポンス構造（抜粋）**

  ```json
  {
    "title": "ページタイトル",
    "lines": [
      { "text": "1行目のテキスト" },
      { "text": "2行目のテキスト" },
      { "text": "3行目のテキスト" }
    ]
  }
  ```

- **戻り値**  
  `lines[].text` を `\n` で結合した全文テキスト。

## 4. Cosense API 通信仕様

- **ウェイト処理（負荷軽減）**  
  Cosenseサーバーへの過度な負荷を防ぐため、APIを呼び出す処理（`RestClient`による通信）の直前に、  
  最低でも `500ms` のスリープ（ウェイト時間）を挟む防壁ロジックを実装する。  
  ウェイト時間は設定プロパティ `cosense.api-wait-ms`（デフォルト: `500`）で制御可能とし、  
  テスト時は `0` を設定することで待機をスキップできる。

- **エラーハンドリング**  
  APIから4xxや5xxなどのエラーレスポンスを受け取った場合、通信を異常終了させず、  
  原因（認証失敗、ページ不在等）を人間やAIに分かりやすいエラーメッセージに変換して通知する。  
  具体的なマッピングは以下の通り。

  | HTTPステータス | メッセージ例                                        |
  | -------------- | --------------------------------------------------- |
  | 401            | 認証エラー: connect.sidが無効か期限切れです         |
  | 404            | 指定したページが見つかりません                      |
  | その他4xx      | リクエストエラーが発生しました (HTTP {コード})      |
  | 5xx            | Cosenseサーバーエラーが発生しました (HTTP {コード}) |

## 5. プロジェクト構成

```text
cosense-mcp/
├ pom.xml
└ src/
    ├ main/
    │   ├ java/
    │   │   └ com/
    │   │       └ github/
    │   │           └ cucl2_similis/
    │   │               └ cosensemcp/
    │   │                   ├ McpCosenseApplication.java      # エントリポイント
    │   │                   ├ config/
    │   │                   │   └ McpConfig.java             # RestClient.Builder Beanの登録
    │   │                   ├ controller/
    │   │                   │   └ CosenseMcpController.java  # MCPツールの登録（@McpTool）
    │   │                   ├ dto/
    │   │                   │   ├ SearchResponse.java        # 検索APIレスポンスのルートRecord
    │   │                   │   ├ SearchPage.java            # 検索結果の各ページRecord
    │   │                   │   ├ PageResponse.java          # ページ取得APIレスポンスのルートRecord
    │   │                   │   └ PageLine.java              # ページ各行のRecord
    │   │                   └ service/
    │   │                        ├ CosenseApiService.java     # RestClientを使ったAPI通信
    │   │                        └ CosenseApiException.java   # API通信エラー用例外
    │   └ resources/
    │       └ application.yml
    └ test/
        └ java/
            └ com/
                └ github/
                    └ cucl2_similis/
                        └ cosensemcp/
                            ├ McpCosenseApplicationTests.java    # コンテキスト起動テスト
                            └ service/
                                ├ CosenseApiServiceTest.java     # CosenseApiServiceの単体テスト
                                └ CosenseApiServiceWaitTest.java # ウェイト動作の検証テスト
```

## 6. 設定プロパティ一覧

| プロパティキー         | 型     | デフォルト                         | 説明                          |
| ---------------------- | ------ | ---------------------------------- | ----------------------------- |
| `cosense.project-name` | String | 必須 (env: `COSENSE_PROJECT_NAME`) | 対象Cosenseプロジェクト名     |
| `cosense.connect-sid`  | String | 必須 (env: `COSENSE_CONNECT_SID`)  | 認証クッキー値                |
| `cosense.api-wait-ms`  | long   | `500`                              | APIコール間のウェイト時間(ms) |

## 7. テスト方針

- **単体テスト（`CosenseApiServiceTest`）**  
  `MockRestServiceServer.bindTo(RestClient.Builder)` を使ったピュア単体テスト。  
  Springコンテキストを起動せず高速に動作する。  
  `cosense.api-wait-ms=0` 相当の設定でウェイトをスキップする。

- **ウェイト検証テスト（`CosenseApiServiceWaitTest`）**  
  デフォルト500msのウェイトが実際に挿入されることを計測ベースで検証する。

- **コンテキストテスト（`McpCosenseApplicationTests`）**  
  Springコンテキスト全体が正常に起動できることを確認する統合テスト。

## 8. セットアップ・起動方法

### 前提条件

- Java 25 以上
- Maven 3.9 以上（またはプロジェクト同梱の `mvnw` を使用）

### ビルド

```bash
./mvnw clean package
```

`target/cosense-mcp-1.0.0-SNAPSHOT.jar` が生成される。

### `connect.sid` の取得方法

1. ブラウザでCosense（旧Scrapbox）にログインする。
2. ブラウザの開発者ツール（DevTools）を開き、Application タブ → Cookies → `https://scrapbox.io` を選択する。
3. `connect.sid` の値をコピーする（`s%3A...` から始まる文字列）。

### MCPクライアントへの登録

#### Claude Desktop の場合

`claude_desktop_config.json` に以下を追記する。

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

STDIO モードで起動するため、コマンドは以下の通り。

```bash
COSENSE_PROJECT_NAME=your-project \
COSENSE_CONNECT_SID='s%3A...' \
java -jar cosense-mcp-1.0.0-SNAPSHOT.jar
```
