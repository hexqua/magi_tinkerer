# GitHub PR・Actions 設定

この文書は `main` の運用目標を記載する。実際の適用状態は GitHub 側で確認する。

- `main` への取り込みは PR 経由の merge commit を使う。バージョン更新も同じ方式とする。
- 必須チェックは GitHub Actions の `build` と `gametest`。画面上では `PR CI / build` と `PR CI / gametest` と表示される。Better Combat 構成はローカルで検証する。
- ruleset では直接更新と force push を制限し、PR と必須チェックを要求する。初回は対象ブランチに workflow があり、両チェックが正常に報告されることを確認してから必須化する。
- [PR CI](../.github/workflows/pr-ci.yml) は `pull_request` で実行し、読み取り権限と SHA 固定の Actions を使う。Gradle Wrapper の検証後にビルドとテストを起動する。
- Codex Cloud レビューは利用できる場合の補助とする。CI 結果と人間のレビューで取り込みを判断する。
