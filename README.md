# TechFeed

IT ニュースを集める Android 学習用アプリです。Compose / Material 3 の画面に、RSS／Atom の取得、ViewModel と StateFlow による状態管理、Room による保存記事の永続化を接続しています。

起動時と「更新」で Android Developers、Publickey、ITmedia NEWS、Impress Watch、Qiita、Zenn、Ars Technica、IPA お知らせ、IPA セキュリティのフィードを取得します。取得元と話題タグは別々の段で表示し、組み合わせて絞り込めます。取得元は並列に取得します。Qiita と Zenn はそれぞれのトレンド配信順で先頭最大30件を表示します。記事の概要を確認し、元記事をブラウザーで開けます。保存したタイトル・概要・URL は再起動後も端末内に残り、オフラインでも確認できます。全文の保存、ログイン、端末間同期、定期更新はありません。取得失敗時に未保存のニュースをディスクから復元する機能はありません。

## 始め方

1. Android Studio で `app/` をプロジェクトとして開きます。
2. Gradle JDK に Android Studio の同梱 JDK 17 以上を設定し、Android SDK 36 を用意します。
3. Gradle 同期後、Android 8.0（API 26）以上の実機またはエミュレーターで実行します。初回の依存関係取得にはネットワークが必要です。

`./scripts/check-language.sh` で lint、単体テスト、debug ビルドを実行できます。フィード解析・URL 検査・日付処理の単体テストを含みます。設計は docs/requirements.md を参照してください。

## Git と秘密情報の検査

Git は初期化済みで、pre-commit からローカルの Gitleaks を呼び出すコミット前フックを導入しています。コミット時には pre-commit と Gitleaks が必要です。Git のフックは複製時には引き継がれないため、別環境では両ツールを導入後 `pre-commit install` を実行してください。GitHub Actions の秘密情報検査は設定済みです。`local.properties`、秘密情報、署名鍵はコミットしないでください。

手動でステージ済みの変更を検査する場合は `pre-commit run gitleaks` を実行します。この検査は未ステージの変更を含みません。

ニュース／保存済みの切り替えと更新はヘッダーに集約し、下部ナビゲーションは設けない。トレンドは各最大30件、通常のフィードは各最大100件まで扱う。配信件数が少ない場合はフィードに含まれる分だけ表示する。

## 設定・タグ・翻訳

ヘッダーの設定アイコンで取得サイトと表示タグを選べます。各項目の上下ボタンで表示順を変更でき、順序も保存します。DataStore で自動保存し、取得元の変更時に再取得します。全サイトを無効にすると通信を停止します。保存済み記事は取得元の選択に関係なく残ります。タグを隠しても記事そのものは非表示になりません。

セキュリティ・AI タグは記事のタイトル／概要と配信元から簡易判定します。誤判定や見逃しがあり、重要な判断では原文を確認してください。

英語配信元（Android Developers／Ars Technica）の記事詳細には「Google で翻訳」を表示します。Google Translate の技術による ML Kit の端末内翻訳を使用します。モデルが未取得のときだけダウンロード確認ダイアログを表示し、了承後に取得します。モバイル通信を使用する場合があります。取得済みなら確認なしで翻訳します。設定からモデルを先に取得することもできます。翻訳対象はタイトルとRSSの概要です。訳文は ViewModel のメモリ内に保持し、プロセス終了後は再翻訳します。翻訳モデル自体は端末に保持されます。

翻訳ボタンは翻訳後に「原文を表示／日本語訳を表示」の一つの切り替えボタンになります。訳文表示中は公式ルールにより帰属バッジを表示し、設定画面に免責表示を掲載します。翻訳の案内: https://cloud.google.com/translate
帰属バッジは Google の公式配布物（https://docs.cloud.google.com/static/translate/images/google-translate-attribution.zip）から改変せず使用しています。

表示する話題タグは「セキュリティ」「AI」「Android」「開発」「ITニュース」「お知らせ」です。Qiita／Zenn などのサイト名は取得元の段にのみ表示します。上段の取得元と下段の話題タグを組み合わせて絞り込めます。

ニュース一覧上部の見出し・説明・件数は表示せず、「取得元」「タグ」の2段だけにする。下方向に読み進めると絞り込み欄を折りたたみ、上方向のスクロールで再表示する。ヘッダーの操作アイコンは常に表示する。取得元・タグ・保存済みの切り替え時は先頭へ戻し、絞り込み欄を再表示する。

## Hilt と DI の学習

ViewModel は Repository・設定 Repository・翻訳サービスをコンストラクターで受け取ります。Hilt が依存関係を組み立て、Room の生成方法は core/di/、ML Kit は feature/translation/di/ の Module にまとめています。導入前後の比較と読む順序は [Hilt の学習メモ](docs/hilt-di.md) を参照してください。


## コードの配置

Kotlin の起点は app/app/src/main/java/com/example/techfeed/ です。

| フォルダ | 内容 |
|---|---|
| app/ | 画面をつなぐ TechFeedApp |
| feature/news/ | ニュース取得・RSS解析・一覧／詳細画面・ViewModel |
| feature/settings/ | 設定画面・DataStore・表示順 |
| feature/translation/ | ML Kit 翻訳・モデル取得・確認ダイアログ・DI |
| core/ | 共有モデル・Room DB・配信元・タグ・URL／リンク処理・DBのDI |

MainActivity と TechFeedApplication は起点として直下に残しています。テストも src/test/ 内で対象と同じパッケージに配置しています。詳しい責務と依存方向は [フォルダ構成](docs/architecture.md) を参照してください。

## Android テスト

Roomの保存・削除・再オープン、DataStoreの保存・再生成、Composeの絞り込み等を src/androidTest/ に用意しています。端末・エミュレーターを接続して ./scripts/check-android.sh で実行します。テスト専用のDB・設定ファイルを使用します。対象と実行方法は [Androidテスト](docs/android-tests.md) を参照してください。
