# Magi Tinkerer 開発規約

## 対象と環境

- Minecraft 1.21.1 / NeoForge / Java 21 を対象に、`main` を開発基準とする。MOD ID は `magitinkerer`、Java パッケージは `jp.aquafactory.magitinkerer`。
- Magitech は必須、Better Combat は任意。前提 MOD、固定ファイル ID、起動タスクは [開発構成](docs/development.md) を参照する。
- やり取り、文書、コメントは原則日本語。翻訳可能なプレイヤー向けテキストは lang を使い、Issue フォーム、設定コメント、ログ、GameTest の assertion などは英語にする。ファイルは UTF-8（BOM なし）を使う。日本語・翻訳・Markdown・Skill の編集前は [text-encoding-hygiene](.codex/skills/text-encoding-hygiene/SKILL.md) に従う。
- IntelliJ IDEA でシンボル・依存・変更ファイルの Inspection を確認する。Gradle の実行には共通の `minecraft-mod-local-development` Skill を適用する。

## 実装・検証

- Java の型は import と単純名で参照する。登録は適切な EventBus に接続し、client 専用参照と任意 MOD への参照は対応するロード境界に置く。
- リソース追加・更新は [リソース管理方針](docs/resource-generation-policy.md) を参照する。外部素材の取り込み・翻案では [ASSETS_LICENSE.md](ASSETS_LICENSE.md) に従い、出典、権利表示、変更内容を [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) へ記録する。
- コード・依存・リソース・ビルド設定の変更では `build` を実行する。依存・実行構成・サーバー登録・データ読込・GameTest の変更では標準 GameTest を実行し、共通依存や Better Combat 構成に関わる変更では Better Combat 構成も実行する。GameTest タスクは各実行の終了を待って直列に実行する。
- [テスト設計方針](docs/testing-policy.md) に従い、検証する挙動と必要な実行構成を選ぶ。ツールの終了待ちと GameTest 側の timeout を区別する。失敗した場合は `report-gametest-failure`、client 手動確認の引き継ぎには `report-client-verification` を使う。
- 自動確認は build、必要な GameTest・datagen、Inspection で行う。GUI 起動・操作はユーザーが明示した範囲で行う。

## レビューと引き渡し

- staged、unstaged、untracked を確認し、[レビュー方針](docs/review-policy.md) と `review-local-change` に従って差分を確認する。機能ブランチ全体には `review-feature-branch`、client 由来の入力を扱う変更には `review-client-server-authority` を使う。
- 自動検証とローカルレビューを済ませた差分は未コミットで人間へ引き渡す。対象差分の人間確認とコミットの明示指示を受けてから commit / amend を行う。リモート操作は共通ルールに従い読み取りまでとする。
- [GitHub PR・Actions 設定](docs/github-pr-protection.md) に従い、PR で `main` へ取り込む。
- 常設文書は開発手順と継続する判断基準を扱う。依存バージョンや機能の定義は設定・コードを参照する。作業ごとの検証結果、未確認事項、手動シナリオは引き渡し時に報告する。
