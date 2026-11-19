# Magi Tinkerer

## 概要

Magitech用の小さなアドオンMODです。


### ブランチとサポート方針

- 開発ブランチ `main` では Java 21 を使用します。
- `main` は Minecraft 1.21.1 / NeoForge の主要開発ブランチです。
- `build.gradle` では Java toolchain を 21 に固定していますが、`gradlew.bat` 自体が使う JVM は `JAVA_HOME` または `PATH` に依存します。

## 導入方法

- `mods`配下に`jar`を入れればOKです。
- 現時点でMagitech及びそれの前提MOD以外の必須前提MODはありません.

## 不具合報告

不具合は [GitHub Issues](https://github.com/hexqua/magi_tinkerer/issues) へ報告してください。
この README は主に開発者向けの情報を扱います。プレイヤー向けの機能紹介と任意 MOD 連携の説明は、公開時に CurseForge の Description に集約する予定です。

## 開発環境

- Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21
- MOD ID: `magitinkerer`
- バージョン・依存の正は `gradle.properties` と `build.gradle` です。

PowerShell では `JDK21_HOME` を設定するか、JDK 21 を `%USERPROFILE%\.jdks` または `%USERPROFILE%\.gradle\jdks` に配置します。

```powershell
.\scripts\use-java.ps1
./gradlew.bat build
./gradlew.bat runGameTestServer
```

データ生成は `./gradlew.bat runData`、人間による client 確認は `./gradlew.bat runClient`、IDE 実行構成の同期は `./gradlew.bat neoForgeIdeSync` を使います。client の表示・操作は GameTest では検証できません。

必須依存は (todo:依存全て記載予定)です。実行構成の MOD ロードと別構成からの混入は GameTest で確認し、機能・連携の検証は対象の挙動テストで行います。

`build` では UTF-8・BOM・明らかな文字化けの検査も行います。文書・翻訳の変更時は `./gradlew.bat checkTextEncodingHygiene` で単独実行できます。

## PR 運用

`main` への取り込みはブランチと PR を使い、バージョン更新を含め merge commit に統一します。rebase merge と squash merge は使用しません。

通常の実装・修正は自動検証とローカルレビューを終えた未コミット差分で人間へ引き渡し、人間確認後の明示指示を受けてコミットします。リモート操作の可否は開発環境の共通ルールと作業時の指示に従います。

CI は `PR CI / build` と `PR CI / gametest` を確認する方針です。GameTest の CI 対象は通常構成の `runGameTestServer` のみで、Irons / Magic / Create の追加構成はローカル検証用です。GitHub 側の設定・適用状況は [GitHub PR・Actions 設定](docs/github-pr-protection.md) を参照してください。

## ライセンスや使用について

### 許可(Permissions)

- modpackにはご自由にどうぞ
- 前提modにするのもご自由にどうぞ
- リソースパックもご自由にどうぞ
- スクリーンショット、プレイ動画、動画配信も収益化含めご自由にどうぞ

### 禁止(Restrictions)

- 必須MOD側の禁止事項を破るのはやめてください
- 素材のライセンスはCC BY-SA 4.0のため、継承無しで再利用するのはやめてください(コピーレフト)
- `THIRD_PARTY_NOTICES.md`にある素材のライセンスを破るのはやめてください

### License

- Code: MIT (see `LICENSE`).
- Original assets created for this project: CC BY-SA 4.0.(Creative Commons Attribution-ShareAlike 4.0 International)
- Third-party assets: see `THIRD_PARTY_NOTICES.md` for the applicable licenses.
