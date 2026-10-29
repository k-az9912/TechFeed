# 構成

単一の Android Gradle モジュール。Kotlin の起点は `app/app/src/main/java/com/example/techfeed/`。

| パッケージ | 責務 |
| --- | --- |
| 直下 | MainActivity、TechFeedApplication：Android／Hilt の起点 |
| app | 画面切り替え、ViewModel との接続 |
| feature/news/data | Repository、HTTPS 取得、RSS／Atom 解析 |
| feature/news/domain | 絞り込み、日時・トレンド順の計算 |
| feature/news/ui | 一覧・詳細・画面部品、ViewModel、表示状態 |
| feature/settings | DataStore、設定値・表示順、設定画面、DI |
| feature/translation | ML Kit、翻訳状態、モデル取得確認、帰属表示、DI |
| core | 共有モデル、配信元、タグ、Room、URL 検査、日時変換、外部リンク、表示文言、DB の DI |

## 依存関係

- `app` が各機能を接続。
- `core` は `feature`・`app` に依存しない。
- ニュース固有の解析・並べ替えは `feature/news` に配置。
- `NewsViewModel` がニュース・設定・翻訳を調整。
- `TechFeedScreens` が画面を切り替え、`SaveableStateHolder` で各画面の状態を保持。
- 更新結果の判定は `feature/news/domain/FeedRefreshResult.kt` に分離。全失敗時のみ前回一覧を使用。
- インスタンスの生成・共有範囲は [Hilt 構成](hilt-di.md) に集約。

## 永続データ

| 対象 | 名前 |
| --- | --- |
| Room DB | `news.db`、version 1 |
| テーブル | `saved_articles` |
| DataStore | `news_settings` |
| Room スキーマ | `app/app/schemas/com.example.techfeed.core.database.NewsDatabase/` |

DB スキーマ変更時は migration を追加。

## テスト配置

- `src/test/`：解析・並び替え・絞り込み・URL・タグ・設定値の JUnit テスト。
- `src/androidTest/`：Room・DataStore・Compose／Espresso。[対象と実行方法](android-tests.md)。

いずれも対象コードに対応するパッケージに配置。

## 表示と書式

- 表示文言：`res/values/strings.xml`。ViewModel のエラーは `UiText` で保持し、画面側で解決。
- 配信元名・話題タグは現行の識別値を維持。英語 UI は未提供。
- ランチャー：独自ベクターの adaptive icon。API 33 以上は単色レイヤー付き。
- Kotlin／Gradle Kotlin DSL：ktfmt 0.65 の Kotlinlang スタイル。
