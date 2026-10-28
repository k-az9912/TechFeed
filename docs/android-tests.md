# Android テスト

配置：`app/app/src/androidTest/`。実行環境：AndroidJUnitRunner。

## 対象

| テストクラス | 対象 | 件数 |
| --- | --- | ---: |
| ArticleDaoTest | 保存・削除の観測、DB 再オープン、保存切り替え | 3 |
| SettingsRepositoryTest | 取得元・タグ・表示順・全無効設定の永続化 | 3 |
| NewsScreenTest | AND 絞り込み、保存一覧、戻る操作、スクロール | 4 |
| ArticleDetailDialogTest | モデル取得確認・キャンセル、原文切り替え、日本語記事 | 3 |
| NewsSettingsScreenTest | スイッチ、並べ替え、モデル取得確認 | 3 |

UI 操作は Compose Test API。戻る操作は `Espresso.pressBack()`。

## 実行

端末接続またはエミュレーター起動後、プロジェクト直下で実行。

```sh
./scripts/check-android.sh
```

Android Studio では `androidTest` のクラス横から実行。
レポート：`app/app/build/reports/androidTests/connected/debug/`。

## 分離と範囲

- Room：UUID 付きテスト DB を作成・削除。本番 DB は対象外。
- DataStore：専用ファイルを使用。スコープ終了後に再生成し、保持を確認。
- 画面：実際の Composable に固定データとコールバックを渡す。通信・DB・翻訳モデルに非依存。
- 保存ボタンの表示確認と、Room の永続化確認は別テスト。
- 対象外：実モデル取得・翻訳品質、Hilt 経由の全画面統合、プロセス強制終了からの復元。

## 最終実行結果

2026-10-28、Android Studio 経由。

| 検証 | 結果 |
| --- | --- |
| AndroidTest（Medium Phone API 37） | 16件成功、失敗・エラー・スキップ0件 |
| JUnit | 20件成功、失敗・エラー・スキップ0件 |
| assembleDebug | 成功 |
| lintDebug | エラー0件、警告17件 |
