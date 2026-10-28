# Android テスト

端末・エミュレーター上で AndroidJUnitRunner により実行します。テストは app/app/src/androidTest/ に配置しています。

## 対象

| 領域 | テストクラス | 内容 |
|---|---|---|
| Room | core/database/ArticleDaoTest | 保存・削除の観測、DB再オープン後の保持、保存切り替え |
| DataStore | feature/settings/data/SettingsRepositoryTest | 取得元・タグ、表示順、全取得元無効の保存とストア再生成後の復元 |
| Compose一覧 | feature/news/ui/NewsScreenTest | 取得元＋タグの絞り込み、保存済み表示、Espressoの戻る操作、スクロール時の絞り込み欄 |
| Compose詳細 | feature/news/ui/ArticleDetailDialogTest | 初回取得確認・キャンセル、取得済み時の翻訳、原文切り替え、日本語記事の表示 |
| Compose設定 | feature/settings/ui/NewsSettingsScreenTest | スイッチ、表示順、モデル取得確認 |

Compose要素は Compose Test API で操作し、Androidの戻る操作には Espresso.pressBack() を使います。View用の onView(withText(...)) で Compose のテキストを探す構成ではありません。

## テストデータの分離

- Room は UUID を含むテスト専用DBを作り、終了時にそのDBだけを削除します。通常の news.db は操作しません。
- DataStore はキャッシュ領域のテスト専用ファイルを使います。スコープの終了を待ってから同じファイルを開き直し、メモリ上の値だけでなく永続化を確認します。
- SettingsRepository は DataStore をコンストラクターで受け取ります。本番の生成は SettingsModule が担い、保存先 news_settings とキーを維持しています。
- 画面テストは実際のComposableへ固定データとコールバックを渡します。実通信、ニュース配信元、Room、DataStore、翻訳モデル取得には依存しません。
- 保存ボタンの画面テストと、Roomの永続化テストは別です。前者だけでDB保存を確認したとは扱いません。
- 実際の翻訳モデルのダウンロード・翻訳品質、Hiltを通した全画面の統合、プロセス強制終了からの復元は対象外です。

## 実行方法

実機を接続するかエミュレーターを起動してから、プロジェクト直下で次を実行します。

```sh
./scripts/check-android.sh
```

Gradleを直接使う場合は app/ で実行します。

```sh
./gradlew connectedDebugAndroidTest
```

通常のJVMテスト・lint・debugビルドは、従来どおり ./scripts/check-language.sh です。

Android Studio では androidTest 内のテストクラス横の実行ボタンから、接続先を選択して実行できます。結果のHTMLレポートは通常 app/app/build/reports/androidTests/connected/debug/ に出力されます。

## 実行結果（2026-10-28）

Medium Phone API 37 エミュレーターで connectedDebugAndroidTest を実行し、16件成功、失敗・エラー・スキップ0件。内訳は Room 3件、DataStore 3件、Compose一覧4件、詳細3件、設定3件です。

同時に実行した既存のJUnitローカルテスト20件も成功しました。assembleDebug と lintDebug も成功し、lint はエラー0件・警告17件でした。翻訳テストは画面とコールバックを対象とし、ML Kit の実翻訳を実行した結果ではありません。
