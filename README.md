# Magi Tinkerer

## 概要

Magitech用の小さなアドオンMODです。

### ブランチとサポート方針

- 開発ブランチ `main` では Java 21 を使用します。
- `main` は Minecraft 1.21.1 / NeoForge の主要開発ブランチです。
- `build.gradle` では Java toolchain を 21 に固定していますが、`gradlew.bat` 自体が使う JVM は `JAVA_HOME` または `PATH` に依存します。

## 導入方法

- `mods`配下に`jar`を入れればOKです。
- 現時点で [Magitech](https://modrinth.com/mod/magitech_mod) とその前提 MOD が必須です。
- 必要に応じて任意依存のMODを入れると連動する要素もあります。

## 不具合報告

不具合は [GitHub Issues](https://github.com/hexqua/magi_tinkerer/issues) へ報告してください。
この README は主に開発者向けの情報を扱います。プレイヤー向けの機能紹介と任意 MOD 連携の説明は、公開時に CurseForge と Modrinth の Description に掲載する予定です。

## 開発環境

- Minecraft 1.21.1 / NeoForge / Java 21
- MOD ID: `magitinkerer`
- バージョン・依存の正は `gradle.properties` と `build.gradle` です。

PowerShell では `JDK21_HOME` を設定するか、JDK 21 を `%USERPROFILE%\.jdks` または `%USERPROFILE%\.gradle\jdks` に配置します。

```powershell
.\scripts\use-java.ps1
./gradlew.bat build
./gradlew.bat runGameTestServer
```

データ生成は `./gradlew.bat runData`、人間による client 確認は `./gradlew.bat runClient`、IDE 実行構成の同期は `./gradlew.bat neoForgeIdeSync` を使います。Better Combat ありの構成では `runClientBetterCombat` と `runGameTestServerBetterCombat` を使います。

実行構成の MOD ロードと別構成からの混入は GameTest で確認し、機能・連携の検証は対象の挙動テストで行います。client の表示・操作は手動で確認します。

`build` では UTF-8・BOM・明らかな文字化けの検査も行います。文書・翻訳の変更時は `./gradlew.bat checkTextEncodingHygiene` で単独実行できます。

| 文書 | 内容 |
| --- | --- |
| [AGENTS.md](AGENTS.md) | 作業規約、検証、変更の引き渡し |
| [開発構成](docs/development.md) | JDK、依存関係、実行構成 |
| [テスト設計方針](docs/testing-policy.md) | GameTest の設計・配置・安定性 |
| [レビュー方針](docs/review-policy.md) | 互換性とレビューの判断基準 |
| [リソース管理方針](docs/resource-generation-policy.md) | 手管理、datagen、素材の出典 |
| [GitHub PR・Actions 設定](docs/github-pr-protection.md) | CI と保護設定の目標 |

## PR 運用

`main` への取り込みはブランチと PR を使い、バージョン更新を含め merge commit に統一します。

通常の実装・修正は自動検証とローカルレビューを終えた未コミット差分で人間へ引き渡し、人間確認後の明示指示を受けてコミットします。リモート操作の可否は開発環境の共通ルールと作業時の指示に従います。

CI は `PR CI / build` と `PR CI / gametest` を確認する方針です。GameTest の CI 対象は通常構成の `runGameTestServer` です。Better Combat の追加構成はローカルで検証します。

## ライセンスや使用について

### 許可(Permissions)

- modpackにはご自由にどうぞ
- 前提modにするのもご自由にどうぞ
- リソースパックもご自由にどうぞ
- スクリーンショット、プレイ動画、動画配信も収益化含めご自由にどうぞ

### 禁止(Restrictions)

- 必須 MOD の利用条件に従ってください。Magitech の動画・画像に関する特則は [Magitech のライセンス](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/LICENSE.md) を参照してください。
- 素材を再配布する場合は著作者・ライセンスを表示し、変更した場合はその旨を示してください。翻案を配布する場合は継承条件に従ってください。
- 第三者素材には [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) に記載のライセンスが適用されます。

### License

- Code is licensed under [MIT](LICENSE).
- Original assets and adaptations of Magitech assets are licensed under [CC BY-SA 4.0](ASSETS_LICENSE.md).
- Third-party asset licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
