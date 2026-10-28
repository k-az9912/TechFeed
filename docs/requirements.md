# TechFeed の要件と設計

## 目的
IT ニュースを複数の配信元から取得し、絞り込み、記事の保存、英語記事の日本語翻訳を行う Android アプリを提供する。

## 初期実装
- Android Developers: https://developer.android.com/static/blog/atom.xml
- Qiita トレンド: https://qiita.com/popular-items/feed.atom （配信順の先頭最大30件）
- ITmedia NEWS: https://rss.itmedia.co.jp/rss/2.0/news_bursts.xml
- Impress Watch: https://www.watch.impress.co.jp/data/rss/1.0/ipw/feed.rdf
- Zenn トレンド: https://zenn.dev/feed （配信順の先頭最大30件）
- Publickey: https://www.publickey1.jp/atom.xml
- アプリ起動時と手動更新時のみ通信する。API キーや独立サーバーは不要。
- タイトル、フィードの概要、公開日時、元記事 URL を表示する。本文スクレイピングや全文転載は行わない。
- カテゴリは配信元単位（Android／開発／ITニュース／Qiitaトレンド／Zennトレンド）。AI・セキュリティの自動分類は未実装。
- 通常のニュースを日時順に並べ、その後に Qiita と Zenn をそれぞれ配信順でまとめる。「Qiitaトレンド」カテゴリで上位最大30件を確認できる。累計いいね順ではなく公式トレンドの順序を使う。解析できない日時は末尾に並べる。
- 読み込み、空一覧、一部／全部取得失敗を表示し、更新ボタンで再試行できる。
- Room に保存記事を保持する。通信失敗時も保存一覧を使用できる。
- 未保存記事のディスクキャッシュ、定期更新、ログイン、端末間同期、全文オフライン閲覧は対象外。

## 構成
- NewsRepository: HTTPS 通信と Room へのアクセス。
- FeedParser: RSS 1.0／RSS 2.0／Atom の解析。記事 URL の重複を除去する。
- NewsViewModel: StateFlow で取得状態を公開する。画面は lifecycle に合わせて購読する。
- ArticleDao: トランザクションで保存状態を切り替える。
- Compose Preview は通信や DB を使用せず、明示したサンプルを表示する。

## 制限と判断
- 配信元は並列取得し、一つの配信元の失敗で他の取得を中断しない。
- 通信は IO dispatcher、接続／読込タイムアウトは各15秒、フィード上限は2MB。
- リダイレクトは自動追従しない。配信 URL が移転した場合は設定を更新する。
- 外部エンティティを解決せず、DOCTYPE を含むフィードは拒否する。
- 概要は簡易的に HTML タグを除去し500文字まで表示する。複雑な HTML の完全な再現は対象外。
- 記事リンクは http／https の絶対 URL のみ許可し、ユーザー操作でブラウザーを開く。
- 配信元の障害時は成功した配信元の記事を表示する。全失敗時はメモリ上の前回一覧を維持する。
- Room スキーマは app/app/schemas に出力する。今後の DB 変更では migration を用意する。
- 本番公開前には各配信元の利用条件と表示方法を改めて確認する。

ニュース／保存済みの切り替えと更新はヘッダーに集約し、下部ナビゲーションは設けない。トレンドは各最大30件、通常のフィードは各最大100件まで扱う。配信件数が少ない場合はフィードに含まれる分だけ表示する。

## 設定・翻訳・追加フィード

- Ars Technica: https://feeds.arstechnica.com/arstechnica/index （英語）
- IPA お知らせ: https://www.ipa.go.jp/about/newsonly-rss.rdf
- IPA 重要なセキュリティ情報: https://www.ipa.go.jp/security/alert-rss.rdf
- BleepingComputer は取得検証が403となったため、今回の配信元には含めない。
- SettingsRepository が Preferences DataStore に無効化サイト名・非表示タグを保持する。未設定時は全て有効。明示的な全無効と未設定を区別する。
- 取得元を変更すると進行中の取得をキャンセルして再取得する。無効な配信元の記事をニュース一覧から除く。保存済み記事は削除しない。
- 表示タグはチップとカード上の表示を制御し、記事の取得可否とは独立する。選択中のタグが非表示になると「すべて」に戻す。
- Article のDBスキーマは変更せず、配信元カテゴリとタイトル／概要のキーワードから複数タグを導出する。RSS固有タグの取り込みと手動タグ編集は未実装。
- 翻訳ボタンは英語配信元の詳細のみ表示する。英語→日本語、タイトルと最大500文字の概要を対象とする。
- モデルの初回ダウンロードをボタンの近くに明示する。結果表示は原文と切り替えでき、失敗時には原文を維持し再試行できる。
- 翻訳結果のキャッシュはViewModel存続中のみ。記事本文の取得やクラウド翻訳APIは使用しない。
- 翻訳クライアントはViewModel破棄時に閉じる。帰属表示・免責表示はGoogleのガイドラインに従う。

## 検証記録（2026-10-28）
- Android Studio 経由で assembleDebug、testDebugUnitTest、lintDebug を実行し成功。
- 単体テスト15件成功（フィード解析9件、タグ・英語配信元判定等6件）。
- lint はエラー0件、警告16件。主にSDK・ライブラリ更新案内とKTX推奨。警告抑制は追加していない。
- IPA 2フィードと Ars Technica のHTTP取得・XML構造を確認。
- 翻訳モデルの実ダウンロード、訳文表示、設定変更後の再起動保持の端末上での操作確認は未完了。

## 表示・翻訳操作の改善（2026-10-28）
- 取得元と話題タグのフィルターを別々の段に分離する。両方を選んだ場合はANDで絞り込む。
- 話題タグはセキュリティ／AI／Android／開発／ITニュース／お知らせ。Qiita・Zennの記事は開発、海外ITカテゴリはITニュース、IPA一般お知らせはお知らせに対応する。
- サイト名と話題タグそれぞれの表示順を設定画面の上下ボタンで変更できる。DataStoreに順序を保存し、未知・重複の項目を除去して新規項目を末尾に補完する。
- ヘッダーの保存／ニュース切り替え、更新、設定をアクセシビリティラベル付きアイコンにする。
- モデルの存在をML Kit RemoteModelManagerで確認し、未取得の場合のみ確認ダイアログを出す。取得済みの場合は説明文や確認を繰り返さない。
- 設定画面から翻訳モデルの取得、取得状態・失敗の確認ができる。ダウンロード処理と翻訳処理を分離し、翻訳メソッドからの無確認ダウンロードを行わない。
- 翻訳操作ボタンは未翻訳時「Google で翻訳」、翻訳後「原文を表示／日本語訳を表示」の一つのみ。
- 帰属バッジはGoogleの公式要件に従って訳文に隣接表示する。設定画面だけへの移動は行わない。
  https://developers.google.com/ml-kit/language/translation/translation-terms
  https://docs.cloud.google.com/translate/attribution

### 今回の確認範囲
- ローカルのKotlinコンパイラーでNewsData／NewsSettingsと関連テストをコンパイルし、JUnitで11件成功。
- Android Studioに画面操作ツールが接続できず、今回のCompose画面全体のビルドと端末操作確認は未完了。前節の15件・lint成功は改善前の記録。

ニュース一覧上部の見出し・説明・件数は表示せず、「取得元」「タグ」の2段だけにする。下方向に読み進めると絞り込み欄を折りたたみ、上方向のスクロールで再表示する。ヘッダーの操作アイコンは常に表示する。取得元・タグ・保存済みの切り替え時は先頭へ戻し、絞り込み欄を再表示する。

今回の見出し整理後は Android Studio で assembleDebug 成功、エミュレーターへのインストールと「取得元」「タグ」の2段表示を確認した。スクロール操作は画面操作ツールが noWindowsAvailable を返したため、折りたたみ・再表示の端末操作確認は未完了。

## Hilt による依存性の注入
- TechFeedApplication を @HiltAndroidApp、MainActivity を @AndroidEntryPoint として登録する。
- Compose は hiltViewModel() で @HiltViewModel を取得する。ViewModel は Application を保持せず、必要な依存をコンストラクターで受け取る。
- DB・ニュースRepository・設定Repositoryは SingletonComponent 内で共有する。DB名・スキーマ・DataStoreの設定名を維持する。
- 翻訳サービスとML Kitクライアントは ViewModelScoped とし、クライアントはViewModelの onCleared で閉じる。
- 生成方法と寿命の解説は docs/hilt-di.md を参照。

## 機能ごとのパッケージ分割
- app、feature/news、feature/settings、feature/translation、core に責務を分割する。
- 画面部品、通信、解析、保存、設定値、タグ・順序計算をファイル単位で分離する。
- 保存データの名前とスキーマを維持する。共通層から各機能への依存を作らない。
- 詳細なファイル配置とテストの対応は docs/architecture.md を参照。

## Android テスト
- Room の保存・削除・再オープンと DataStore の設定保持をテスト専用データで検証する。
- SettingsRepository は DataStore を受け取り、SettingsModule が同じ本番保存先 news_settings を提供する。
- Compose の絞り込み・保存表示・設定・翻訳確認と、Espresso の戻る操作を検証する。
- 詳細と実行結果は docs/android-tests.md を参照。
