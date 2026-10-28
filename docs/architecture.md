# フォルダ構成

単一の Android Gradle モジュール内で、機能ごとに Kotlin パッケージを分けています。単なる移動だけでなく、複数の責務を持っていた NewsData.kt・NewsSettings.kt・MainActivity.kt を分割しました。

起点: app/app/src/main/java/com/example/techfeed/

| パッケージ | 主なファイル・責務 |
|---|---|
| 直下 | MainActivity、TechFeedApplication：Android と Hilt の開始点 |
| app | TechFeedApp：画面切り替えと ViewModel の接続 |
| feature/news/ui | NewsScreen、NewsState、NewsViewModel、ArticleDetailDialog |
| feature/news/ui/components | ArticleCard、ArticleFilterRow：ニュース画面の部品 |
| feature/news/data | NewsRepository：通信と保存記事を画面側へ提供 |
| feature/news/data/remote | FeedClient：HTTPS取得、FeedParser：RSS／Atom解析 |
| feature/news/domain | ArticleFilters、ArticleOrdering：絞り込みと日時順／トレンド順 |
| feature/settings/ui | NewsSettingsScreen：設定画面 |
| feature/settings/data | SettingsRepository：注入されたDataStoreによる設定保存 |
| feature/settings/di | SettingsModule：本番のDataStore生成 |
| feature/settings/model | NewsSettings、DisplayOrder：設定値と表示順の操作 |
| feature/translation/data | ArticleTranslator：ML Kit の呼び出し |
| feature/translation/model | TranslationState、TranslationAttribution：状態と帰属表示の定義 |
| feature/translation/ui | ModelDownloadDialog：モデル取得の確認 |
| feature/translation/di | TranslationModule：翻訳クライアントの生成 |
| core/model | Article：複数機能で扱う記事モデル |
| core/feed | FeedSources：配信元定義、英語記事判定 |
| core/tags | ArticleTags：共通の話題タグ定義・判定 |
| core/database | NewsDatabase、ArticleDao：保存記事DB |
| core/network | ArticleUrls：記事URLの検査 |
| core/ui | ExternalLinks：ブラウザーを開く共通処理 |
| core/di | StorageModule：DBとDAOの生成 |

## 共通処理の分け方

- 複数機能から使う配信元・タグ・記事モデルは core に置きます。
- 詳細画面と設定画面で重複していた外部リンク処理を core/ui にまとめました。
- ニュース専用の RSS 解析や並び替えは、汎用の utils にせず feature/news に置きます。
- 設定の並べ替え操作は feature/settings/model に置きます。
- core は feature や app を参照しません。app は各機能をつなぎます。
- 現在の NewsViewModel は、一覧から設定・翻訳も操作する調整役です。その処理を分散させず、今回の変更では動作を維持しています。

## 永続データ

DB名 news.db、テーブル saved_articles、DBバージョン1、DataStore名 news_settings と各キーを維持します。Roomスキーマの保存フォルダはDBクラスの新パッケージに合わせて com.example.techfeed.core.database.NewsDatabase に移しています。アプリの applicationId と namespace は変えていません。

Android の res/ は標準構成のままです。機能別の Gradle モジュール化は今回行っていません。
