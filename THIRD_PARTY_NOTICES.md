# Third-Party Notices

## Magitech -Arcane Engineering-

下表の出典に記載した Magitech 素材は **Copyright (c) Stln**、CC BY-SA 4.0 です。Magi Tinkerer で作成した下表の翻案にも CC BY-SA 4.0 を適用します。上流の権利表示と無保証・責任制限はライセンス原文を参照してください。[Magitech のライセンス](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/LICENSE.md) · [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) · [無保証・責任制限](https://creativecommons.org/licenses/by-sa/4.0/legalcode.ja#s5)

| 配布先ファイル | 出典 | 変更内容 |
| --- | --- | --- |
| `assets/magitinkerer/textures/palettes/tool_base.png` | [Magitech 2.4.6](https://cdn.modrinth.com/data/mJ7yWnrA/versions/lavat0qn/magitech-2.4.6.jar) の `assets/magitech/textures/item/part/sample_*.png` と `assets/magitech/textures/item/tool/*/sample_*.png` | サンプルパーツの描画色6色を抽出し、未使用色を1色追加して7×1の置換元パレットを作成。サンプル画像本体は依存 MOD から実行時に参照。 |
| `assets/magitinkerer/lang/en_us.json`、`assets/magitinkerer/lang/ja_jp.json` のパーツ名書式12件 | [Magitech 2.4.6](https://cdn.modrinth.com/data/mJ7yWnrA/versions/lavat0qn/magitech-2.4.6.jar) の `assets/magitech/lang/en_us.json`、`assets/magitech/lang/ja_jp.json` | `item.magitech.<パーツID>` の書式を `item.magitinkerer.<パーツID>` として使用。翻訳文は原文どおり。 |
| `assets/magitinkerer/textures/block/maneuvering_coated_glass.png`、`restraint_coated_glass.png`、および両方の `_connected.png` | [Magitech の Mana Insulating Glass](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/src/main/resources/assets/magitech/textures/block/mana_insulating_glass.png)（[Magitech 2.4.6](https://cdn.modrinth.com/data/mJ7yWnrA/versions/lavat0qn/magitech-2.4.6.jar) に収録） | 原版のガラステクスチャを改変して2種の被覆ガラス用テクスチャを作成。枠と角の有無を切り替える接続用タイルシートに展開。 |
| `assets/magitinkerer/textures/block/mana_durum_mender.png` | [Magitech のブロックテクスチャの一部](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/tree/main/src/main/resources/assets/magitech/textures/block)（元ファイルは下記） | 切り出し・縮小などの加工を加えて修繕装置用テクスチャを作成。 |

<details>
<summary>mana_durum_mender.png の元ファイル</summary>

- [`enhanced_mana_relay`](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/src/main/resources/assets/magitech/textures/block/machine/fluxium/enhanced_mana_relay.png)
- [`fluxium_enclosure`](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/src/main/resources/assets/magitech/textures/block/machine/fluxium/fluxium_enclosure.png)
- [`fluxium_mana_port`](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/src/main/resources/assets/magitech/textures/block/machine/fluxium/fluxium_mana_port.png)
- [`pedestal_pylon_top`](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/src/main/resources/assets/magitech/textures/block/machine/mystwood/pedestal_pylon_top.png)
- [`mana_insulating_glass`](https://github.com/STLN-S1NcLair/Magitech-Neoforge-1.21.1/blob/main/src/main/resources/assets/magitech/textures/block/mana_insulating_glass.png)

</details>

## Create

接続タイルの選択処理は Create の [`AllCTTypes.OMNIDIRECTIONAL`](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/foundation/block/connected/AllCTTypes.java) を翻案しています。Copyright (c) The Create Team / The Creators of Create。[MIT ライセンス全文](src/main/resources/META-INF/licenses/create-connected-textures.txt) は配布 JAR の `META-INF/licenses/create-connected-textures.txt` に同梱します。

## NeoForge MDK

NeoForge MDK 由来のテンプレートには [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt) の条件が適用されます。
