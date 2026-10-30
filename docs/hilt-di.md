# Hilt 構成

ViewModel は Repository・設定・翻訳サービスをコンストラクターで受け取り、Hilt が依存関係を生成。

## 接続

| 箇所 | 設定・役割 |
| --- | --- |
| TechFeedApplication | `@HiltAndroidApp`。Manifest に登録 |
| MainActivity | `@AndroidEntryPoint` |
| TechFeedApp | `hiltViewModel()` で取得 |
| NewsViewModel | `@HiltViewModel`、`@Inject constructor` |
| StorageModule | Room DB・DAO の提供 |
| SettingsModule | 本番 DataStore の提供 |
| NewsModule | NewsRepository → DefaultNewsRepository の接続 |
| TranslationModule | TranslationService → ArticleTranslator、ML Kit Translator の提供 |

NewsRepository → DefaultNewsRepository → ArticleDao・FeedClient → FeedParser。
SettingsRepository → DataStore。
TranslationService → ArticleTranslator → ML Kit Translator。

## スコープ

| 指定 | 対象 | 共有範囲 |
| --- | --- | --- |
| `@Singleton` | NewsDatabase、DefaultNewsRepository、SettingsRepository | SingletonComponent |
| `@ViewModelScoped` | ArticleTranslator、ML Kit Translator | ViewModelComponent |
| スコープなし | FeedClient、FeedParser、DAO 提供関数 | 同一インスタンスの保証なし |

ML Kit クライアントは `NewsViewModel.onCleared()` で明示的に閉じる。スコープ指定による自動解放はない。

## ビルド

- Hilt：2.60.1
- AndroidX Hilt Compose：1.3.0
- KSP：2.3.9
- 生成コード：`app/app/build/generated/`（直接編集不可）

## 参考

- [Hilt Gradle 設定](https://dagger.dev/hilt/gradle-setup)
- [KSP サポート](https://dagger.dev/dev-guide/ksp.html)
- [Android／Compose 連携](https://developer.android.com/training/dependency-injection/hilt-jetpack)
