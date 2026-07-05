# テスト仕様書：Cosense MCP Server

## 1. 目的

- フェーズ1の参照系機能である `cosense_search_pages` と
  `cosense_get_page` について、要件定義書とシステム仕様書に沿った
  テスト観点を明確化する。
- 実装は本仕様書に基づいて Red → Green → Refactor で進め、
  仕様追加や修正が発生した場合は本書とテストコードを同時に更新する。

## 2. 対象範囲

- **MCPツール**  
  `cosense_get_page`（現行実装） / `cosense_search_pages`（提供予定）
- **サービス層**  
  Cosense API 呼び出し、レスポンス変換、ウェイト制御、
  HTTPステータスに応じたエラーマッピング
- **設定**  
  `cosense.project-name` / `cosense.connect-sid` /
  `cosense.api-wait-ms`

## 3. テストレベル

- **単体テスト**  
  `CosenseApiService` を対象に、Cosense API 通信結果の変換、
  エラーマッピング、待機設定の適用を検証する。
- **コンテキストテスト**  
  `CosenseMcpApplication` の起動と、MCPツール公開に必要な Bean の
  構成が成立することを確認する。
- **実機確認**  
  JAR ビルド後に MCP Inspector からツール一覧と応答を確認する。

## 4. テストケース

### 4.1 `cosense_search_pages`

| ID | 観点 | 条件 | 期待結果 |
| --- | --- | --- | --- |
| SP-01 | 正常系 | 検索APIが複数件の `pages[].title` を返す | タイトル一覧が同順で返る |
| SP-02 | 正常系 | 検索APIが0件を返す | 空リストが返る |
| SP-03 | 異常系 | APIが401を返す | `認証エラー: connect.sidが無効か期限切れです` が通知される |
| SP-04 | 異常系 | APIが400など401以外の4xxを返す | `リクエストエラーが発生しました (HTTP {コード})` が通知される |
| SP-05 | 異常系 | APIが5xxを返す | `Cosenseサーバーエラーが発生しました (HTTP {コード})` が通知される |

### 4.2 `cosense_get_page`

| ID | 観点 | 条件 | 期待結果 |
| --- | --- | --- | --- |
| GP-01 | 正常系 | ページAPIが複数行の `lines[].text` を返す | 各行が `\n` で結合された全文が返る |
| GP-02 | 正常系 | `lines` が1件のみ | 余分な改行なしで1行本文が返る |
| GP-03 | 異常系 | APIが404を返す | `指定したページが見つかりません` が通知される |
| GP-04 | 異常系 | APIが401を返す | `認証エラー: connect.sidが無効か期限切れです` が通知される |
| GP-05 | 異常系 | APIが400など404/401以外の4xxを返す | `リクエストエラーが発生しました (HTTP {コード})` が通知される |
| GP-06 | 異常系 | APIが5xxを返す | `Cosenseサーバーエラーが発生しました (HTTP {コード})` が通知される |

### 4.3 ウェイト制御

| ID | 観点 | 条件 | 期待結果 |
| --- | --- | --- | --- |
| WT-01 | 設定反映 | `cosense.api-wait-ms=0` | API呼び出し前の待機をスキップできる |
| WT-02 | 設定反映 | `cosense.api-wait-ms=500` | API呼び出し前に約500msの待機が入る |

### 4.4 コンテキスト起動

| ID | 観点 | 条件 | 期待結果 |
| --- | --- | --- | --- |
| CT-01 | 起動確認 | 必須設定を与えてコンテキスト起動 | `CosenseMcpApplication` が正常起動する |
| CT-02 | ツール公開 | コンテキスト起動後に Controller を取得 | `cosense_get_page` の公開と `cosense_search_pages` 追加に必要な構成が成立する |

## 5. テスト実施順

1. `cosense_search_pages` の単体テストを追加し、Red を確認する。
2. `cosense_get_page` の単体テストを追加し、Red を確認する。
3. エラーマッピングの単体テストを追加し、Red を確認する。
4. ウェイト制御のテストを追加し、Red を確認する。
5. コンテキスト起動テストを追加または更新し、Red を確認する。
6. 実装後に `./mvnw clean test` を実行し、全件 Green を確認する。
7. `./mvnw clean package` 後に MCP Inspector で実機確認する。
