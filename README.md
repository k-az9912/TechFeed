# TechFeed

IT ニュースの取得・絞り込み・保存・翻訳に対応した Android アプリ。

## 機能

- 9フィードから RSS／Atom を取得
- 取得元・話題タグによる絞り込み
- 記事の保存とオフライン閲覧（タイトル・概要）
- 取得元・タグの表示設定と並べ替え
- 英語記事の日本語翻訳（ML Kit、端末内処理）

配信元：Android Developers、Publickey、Qiita、ITmedia NEWS、Impress Watch、Zenn、Ars Technica、IPA お知らせ、IPA セキュリティ。

## 起動

1. Android Studio で `app/` を開く。
2. Gradle JDK に Android Studio 同梱 JDK、Android SDK に API 36 を指定。
3. Gradle 同期後、API 26 以上の端末で実行。

## 検証

プロジェクト直下で実行。

```sh
./scripts/check-language.sh  # lint・JUnit・debug ビルド
./scripts/check-android.sh   # AndroidTest（端末接続が必要）
```

コミット前フック：pre-commit と Gitleaks の導入後、`pre-commit install`。GitHub Actions でも履歴を検査。

## 制約

- 更新は起動時・手動操作時のみ
- 全文保存・ログイン・端末間同期は未対応
- 翻訳モデルは初回ダウンロードが必要
- 話題タグはキーワード等による簡易判定

## ドキュメント

- [仕様](docs/requirements.md)
- [構成](docs/architecture.md)
- [依存性の注入](docs/hilt-di.md)
- [Android テスト](docs/android-tests.md)
