# 開発構成

PowerShell で `JDK21_HOME` を設定するか、JDK 21 をユーザーの `.jdks` または `.gradle/jdks` に配置して `.\scripts\use-java.ps1` を実行します。Java toolchain はコンパイル用 JDK を指定します。Gradle Wrapper の JVM は `JAVA_HOME` または `PATH` で決まります。

## バージョンと依存関係

| 設定 | 参照先 |
| --- | --- |
| Minecraft、開発用 NeoForge、配布時の NeoForge 下限、固定ファイル ID | [gradle.properties](../gradle.properties) |
| 開発時の依存と実行構成 | [build.gradle](../build.gradle) |
| 配布時の必須・任意 MOD とバージョン条件 | [neoforge.mods.toml](../src/main/templates/META-INF/neoforge.mods.toml) |

標準構成には Magitech、Modonomicon、Lodestone、GeckoLib、Curios、playerAnimator、TerraBlender が入ります。Better Combat 構成は標準構成に Better Combat と Cloth Config API を追加し、playerAnimator を共有します。配布 jar は依存 MOD の jar を同梱せず、利用者は使う構成の前提 MOD を導入します。

依存更新時は Minecraft 1.21.1 / NeoForge 向けの公式配布情報と、各 jar の MOD ID、前提 MOD、NeoForge 下限を照合してファイル ID を固定します。該当版の公開が beta のみの場合は、対応する最新の配布ファイルを確認します。NeoForge の下限は必須・任意構成で使う依存の最高要求に合わせます。現在は Curios の要求が基準です。

## Gradle タスク

Windows ではリポジトリルートで `./gradlew.bat <タスク>` を実行します。

| タスク | 用途 |
| --- | --- |
| `build` | ビルド、文字コード検査、配布 jar の生成 |
| `runGameTestServer` | 標準構成の検証。PR CI でも実行する |
| `runGameTestServerBetterCombat` | Better Combat ありのローカル検証 |
| `runClient` / `runClientBetterCombat` | 標準／Better Combat 構成の手動確認 |
| `runServer` / `runData` | 標準構成の専用サーバー起動／データ生成 |
| `neoForgeIdeSync` | IDE 実行構成の生成 |
| `checkTextEncodingHygiene` | UTF-8・BOM・明らかな文字化けの単独検査 |

GameTest は構成ごとの専用ディレクトリに置き、各タスクが自分の `world` を初期化します。`run/world` と `run/better_combat/world` は手動用です。ログは `run/gametest/logs/` または `run/gametest_better_combat/logs/` にあります。

build 設定を変えたら IntelliJ IDEA で Gradle プロジェクトを再読み込みします。`neoForgeIdeSync` は IDE 起動用ファイルを更新します。TOML の依存 MOD ID に IDE 警告が出る場合は、生成した TOML、実行構成、MOD のロード結果を照合してください。

Magitech と Better Combat の併用時に `setLastAttackedTicks` の Mixin 競合が出る場合は、双方の実装とロードログを確認します。依存由来の警告と機能上の影響は、その作業の検証結果に記録します。
