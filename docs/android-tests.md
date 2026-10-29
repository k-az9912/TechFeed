# Android テスト

配置：`app/app/src/androidTest/`。実行環境：AndroidJUnitRunner。

## 対象

| テストクラス | 対象 | 件数 |
| --- | --- | ---: |
| ArticlePresentationTest | 日付表示・不明日時・エラーのリソース引数 | 3 |
| ArticleDaoTest | 保存・削除の観測、DB 再オープン、保存切り替え | 3 |
| SettingsRepositoryTest | 取得元・タグ・表示順・全無効設定の永続化 | 3 |
| TechFeedScreensTest | 設定画面往復後の選択・スクロール保持、Android の戻る操作 | 2 |
| NewsScreenTest | AND 絞り込み、保存一覧、戻る操作、スクロール | 4 |
| ArticleDetailDialogTest | モデル取得確認・キャンセル、原文切り替え、日本語記事、翻訳・取得失敗後の再試行 | 5 |
| AdaptiveUiTest | 320／411dp × 文字100／200% × 明暗。長い見出し・保存・絞り込み | 8 |
| LauncherIconTest | Adaptive Icon とモノクロアイコンの解決・描画 | 1 |
| NewsSettingsScreenTest | スイッチ、並べ替え、モデル取得確認 | 3 |

UI 操作は Compose Test API。戻る操作は Espresso。ダイアログでは対象ウィンドウを明示。

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
- 翻訳の失敗・再試行は状態とコールバックによる画面テスト。SDK 自体の障害再現ではない。
- 表示テストは文字の欠けと操作を検証し、確認用画像を保存。画像の差分比較は未導入。
- 自動テストの対象外：実モデル取得・翻訳品質、Hilt 経由の全画面統合、プロセス強制終了からの復元。実アプリでの確認は後述。

## 最終実行結果

2026-10-30。AndroidTest は ADB instrumentation、JUnit は JUnitCore で実行。
実行環境のソケット制限により Gradle のテストワーカーは起動不可。APK 作成・Lint は Gradle で実行。

| 検証 | 結果 |
| --- | --- |
| AndroidTest（Medium Phone API 37） | 32件成功、失敗・エラー・スキップ0件 |
| JUnit | 41件成功、失敗・エラー・スキップ0件 |
| assembleDebug | 成功 |
| lintDebug | エラー0件、警告16件 |

## 実アプリでの確認

2026-10-29、Medium Phone API 37。通常の MainActivity → Hilt → ViewModel → Room／DataStore／ML Kit を使用。固定データ・代替コールバックは未使用。

| 操作 | 結果 |
| --- | --- |
| 実記事を保存 → プロセス強制終了 → 再起動 | 保存済み一覧に復元 |
| 保存を解除 → プロセス強制終了 → 再起動 | 削除状態を保持 |
| 取得元を無効化・並べ替え → 再起動 | 無効状態・表示順を保持 |
| タグを非表示・並べ替え → 再起動 | 非表示状態・表示順を保持 |
| 設定画面のモデル取得確認をキャンセル | 未取得のまま |
| 英語記事で初回翻訳 → 確認後ダウンロード | 実モデル取得・日本語訳表示に成功 |
| 原文／日本語訳の切り替え | ボタン1個で切り替え。訳文表示中のみ帰属バッジ |
| 保存した英語記事を、再起動後・機内モードで開いて翻訳 | 確認ダイアログなしで翻訳。既定のネットワーク接続なしも確認 |
| 通信断でニュース取得 → 通信復帰 → 更新 | 取得失敗を表示後、記事を再取得してエラー解消 |

再起動は `adb shell am force-stop com.example.techfeed` と Activity 起動によるプロセス再生成。端末自体の再起動は未確認。

確認用の保存記事は削除し、取得元・タグ設定と通信状態は確認前に復元。取得した翻訳モデルは保持。

制約：専門用語や語句に不自然な訳を確認。訳文の正確性を保証する検証ではない。ML Kit 自体の取得・推論エラーを意図的に起こした再試行、設定画面からの実ダウンロード完了は未確認。

## 表示・再試行の確認

2026-10-30、API 37。

- 長い見出しと保存ボタン：320／411dp、文字100／200%、明暗の8条件で文字領域・操作を確認。幅の測定は1pxまでの丸めを許容。
- 通常アプリ：文字200%のニュース一覧、文字200%・ダーク表示の設定画面を目視確認。ラベルと操作欄を分離。
- アイコン：Adaptive Icon の描画を目視確認。モノクロリソースを検証。各ランチャーのマスク・テーマ表示は未確認。
- 翻訳：処理中の無効化、失敗時の原文保持、再試行後の訳文表示とエラー解消を確認。モデル取得失敗後の再確認・再試行も対象。
- 確認画像：端末のアプリ外部ファイル領域 `ui-check/`。ローカル取得先は `app/app/build/outputs/ui-check/`。


## ViewModel の JUnit テスト

`src/test/.../feature/news/ui/NewsViewModelTest.kt`、11件。`kotlinx-coroutines-test` で処理を停止・再開し、待ち時間や実通信に依存せず検証。

- 翻訳・モデル取得：失敗後の再試行、エラー解消、重複実行防止、モデル・言語の条件。
- モデル取得成功後の翻訳、モデル状態確認の失敗からの復帰。
- 取得元変更時のキャンセル、遅れて返る旧応答の排除、連続更新、全取得元の無効化。
- 更新失敗時の一覧保持と再取得、ViewModel 破棄時の翻訳キャンセル・クライアント解放。

ViewModel と SettingsRepository は本番実装。NewsRepository・TranslationService と DataStore は制御可能なテスト実装。ML Kit・HTTP・永続化自体の検証は含まない。
