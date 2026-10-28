# TechFeed

IT ニュースを集める Android アプリの画面モックです。Jetpack Compose と Material 3 で、ニュース一覧、カテゴリ切り替え、保存済み一覧、記事詳細のダイアログを確認できます。表示内容は固定のサンプルで、通信は行いません。保存状態は画面上の一時的なもので、アプリを終了すると失われます。

## 始め方

1. Android Studio で `app/` をプロジェクトとして開きます。
2. Gradle JDK に Android Studio の同梱 JDK 17 以上を設定し、Android SDK 36 を用意します。
3. Gradle 同期後、Android 8.0（API 26）以上の実機またはエミュレーターで実行します。初回の依存関係取得にはネットワークが必要です。

`./scripts/check-language.sh` で lint、単体テスト、debug ビルドを実行できます。今後 RSS、Room による保存、複数ソースを追加します。

## Git と秘密情報の検査

Git は初期化済みで、pre-commit からローカルの Gitleaks を呼び出すコミット前フックを導入しています。コミット時には pre-commit と Gitleaks が必要です。Git のフックは複製時には引き継がれないため、別環境では両ツールを導入後 `pre-commit install` を実行してください。GitHub Actions の秘密情報検査は設定済みです。`local.properties`、秘密情報、署名鍵はコミットしないでください。

手動でステージ済みの変更を検査する場合は `pre-commit run gitleaks` を実行します。この検査は未ステージの変更を含みません。
