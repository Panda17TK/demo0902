package io.github.panda17tk.arpg.item

/**
 * Every item that exists in this world: thrusters, armours, the classic guns as RANGED_WEAPON
 * items (their 種類 = [ItemDef.weaponType]), melee arms, accessories — and since v2.34 the
 * consumables (used from the ITEMS tab) and the LORE readables (読み物) that carry the universe's
 * stories: the beast kings, the relics, the peoples of the six biomes. Pure data — the single
 * source of item names/stats/texts.
 */
object ItemCatalog {
    val ALL: List<ItemDef> = listOf(
        // --- 推進器 (thrusters) ---
        ItemDef("thruster_std", "標準スラスター", ItemKind.THRUSTER, desc = "癖のない推進器"),
        ItemDef("thruster_light", "軽量スラスター", ItemKind.THRUSTER, desc = "鋭い立ち上がり　巡航は控えめ", accelMul = 1.3f, cruiseMul = 0.9f),
        ItemDef("thruster_heavy", "大推力スラスター", ItemKind.THRUSTER, desc = "重いが速い", accelMul = 0.8f, cruiseMul = 1.2f),
        ItemDef("thruster_oc", "OCスラスター", ItemKind.THRUSTER, desc = "フルスロットル可能　全開時 推力3倍/巡航2倍/スタミナ消費2倍", thrusterClass = ThrusterClass.OC),
        ItemDef("thruster_beast", "生体腱スラスター", ItemKind.THRUSTER, desc = "大型獣の腱を使った駆動索　立ち上がりも巡航も一段上", accelMul = 1.15f, cruiseMul = 1.1f),
        ItemDef("thruster_cruise", "巡航スラスター", ItemKind.THRUSTER, desc = "長距離向け　巡航+30% 立ち上がりは鈍い", accelMul = 0.85f, cruiseMul = 1.3f),
        ItemDef("thruster_vernier", "バーニアスラスター", ItemKind.THRUSTER, desc = "取り回し特化　立ち上がり+50% 巡航-20%", accelMul = 1.5f, cruiseMul = 0.8f),
        // --- 防具 (armor) ---
        ItemDef("armor_cloth", "パイロットスーツ", ItemKind.ARMOR, desc = "身軽な標準装備", damageMul = 0.95f),
        ItemDef("armor_light", "軽装甲", ItemKind.ARMOR, desc = "被ダメージ -15%", damageMul = 0.85f),
        ItemDef("armor_combat", "戦闘装甲", ItemKind.ARMOR, desc = "被ダメージ -30%　少し重い", damageMul = 0.7f, moveMul = 0.95f),
        ItemDef("armor_heavy", "重装甲", ItemKind.ARMOR, desc = "被ダメージ -45%　重い", damageMul = 0.55f, moveMul = 0.85f),
        ItemDef("armor_relic", "遺物合金の胸当て", ItemKind.ARMOR, desc = "死せる星の合金　被ダメ -35% 銃 +5%", damageMul = 0.65f, gunMul = 1.05f),
        ItemDef("armor_thermal", "耐熱スーツ", ItemKind.ARMOR, desc = "マグマの熱を完全に遮断　被ダメ -10%", damageMul = 0.9f, traits = setOf(ItemTrait.HEAT_PROOF)),
        ItemDef("armor_insulated", "防寒スーツ", ItemKind.ARMOR, desc = "雪の減速と氷の滑りを無効　被ダメ -10%", damageMul = 0.9f, traits = setOf(ItemTrait.COLD_PROOF)),
        ItemDef("armor_scout", "偵察スーツ", ItemKind.ARMOR, desc = "軽くて速い　移動 +8% 被ダメ -8%", damageMul = 0.92f, moveMul = 1.08f),
        ItemDef("armor_shock", "対衝撃フレーム", ItemKind.ARMOR, desc = "激突ダメージ無効　被ダメ -20%", damageMul = 0.8f, traits = setOf(ItemTrait.CRASH_PROOF)),
        // --- 遠距離武器 (the classic guns as items; 種類 = weaponType, v2.37 でグレード制) ---
        ItemDef("gun_pistol", "ピストル", ItemKind.RANGED_WEAPON, desc = "種類:ピストル　弾薬無限", weaponType = "pistol"),
        ItemDef("gun_shotgun", "ショットガン", ItemKind.RANGED_WEAPON, desc = "種類:ショットガン", weaponType = "shotgun"),
        ItemDef("gun_mg", "マシンガン", ItemKind.RANGED_WEAPON, desc = "種類:マシンガン　弾薬無限", weaponType = "mg"),
        ItemDef("gun_beam", "ビーム", ItemKind.RANGED_WEAPON, desc = "種類:ビーム　貫通+壁爆破　弾薬無限", weaponType = "beam"),
        ItemDef("gun_grenade", "グレネード", ItemKind.RANGED_WEAPON, desc = "種類:グレネード", weaponType = "grenade"),
        // 上位グレード (v2.37): 同じ種類のまま、威力・連射・装填・爆風・ブロック破壊が変わる
        ItemDef("gun_pistol_2", "ピストルM2", ItemKind.RANGED_WEAPON, desc = "種類:ピストル　威力+25% 装填-30%", weaponType = "pistol", gunMul = 1.25f, reloadMul = 0.7f),
        ItemDef("gun_pistol_3", "ピストルM3", ItemKind.RANGED_WEAPON, desc = "種類:ピストル　威力+50% 連射+18%", weaponType = "pistol", gunMul = 1.5f, fireRateMul = 0.85f),
        ItemDef("gun_shotgun_2", "ショットガン改", ItemKind.RANGED_WEAPON, desc = "種類:ショットガン　威力+30% 装填-20%", weaponType = "shotgun", gunMul = 1.3f, reloadMul = 0.8f),
        ItemDef("gun_shotgun_3", "解体ショットガン", ItemKind.RANGED_WEAPON, desc = "種類:ショットガン　ブロック破壊2.5倍 威力+15%", weaponType = "shotgun", gunMul = 1.15f, wallDmgMul = 2.5f),
        ItemDef("gun_mg_2", "マシンガン改", ItemKind.RANGED_WEAPON, desc = "種類:マシンガン　連射+43% 威力+10%", weaponType = "mg", fireRateMul = 0.7f, gunMul = 1.1f),
        ItemDef("gun_mg_3", "重機関銃", ItemKind.RANGED_WEAPON, desc = "種類:マシンガン　威力+40% ブロック破壊1.5倍 連射は重め", weaponType = "mg", gunMul = 1.4f, fireRateMul = 1.15f, wallDmgMul = 1.5f),
        ItemDef("gun_beam_2", "高出力ビーム", ItemKind.RANGED_WEAPON, desc = "種類:ビーム　威力+30% 着弾爆風1.5倍", weaponType = "beam", gunMul = 1.3f, blastMul = 1.5f),
        ItemDef("gun_grenade_2", "拡散グレネード", ItemKind.RANGED_WEAPON, desc = "種類:グレネード　爆風1.6倍 装填-15%", weaponType = "grenade", blastMul = 1.6f, reloadMul = 0.85f),
        // 新しい武器種 (v2.38): 装備することでのみ使える種類 — 数字キーの5種の外側
        ItemDef("gun_smg", "サブマシンガン", ItemKind.RANGED_WEAPON, desc = "種類:SMG　超速連射・低威力　弾薬無限", weaponType = "smg"),
        ItemDef("gun_smg_2", "サブマシンガン改", ItemKind.RANGED_WEAPON, desc = "種類:SMG　威力+20% 装填-25%", weaponType = "smg", gunMul = 1.2f, reloadMul = 0.75f),
        ItemDef("gun_rifle", "ライフル", ItemKind.RANGED_WEAPON, desc = "種類:ライフル　単発高威力・高精度", weaponType = "rifle"),
        ItemDef("gun_rifle_2", "マークスマンライフル", ItemKind.RANGED_WEAPON, desc = "種類:ライフル　威力+35% 装填-20%", weaponType = "rifle", gunMul = 1.35f, reloadMul = 0.8f),
        // 弾道バリアント (v2.40): 同じ種類でも連射・拡散・弾速・追尾・威力が別物 — 敵と壁の奥から拾い集める
        ItemDef("gun_pistol_rapid", "速射ピストル", ItemKind.RANGED_WEAPON, desc = "種類:ピストル　連射+67% 威力-15%", weaponType = "pistol", fireRateMul = 0.6f, gunMul = 0.85f),
        ItemDef("gun_pistol_heavy", "大口径ピストル", ItemKind.RANGED_WEAPON, desc = "種類:ピストル　威力+80% 弾速+20% 連射は重い", weaponType = "pistol", gunMul = 1.8f, fireRateMul = 1.4f, bulletSpeedMul = 1.2f),
        ItemDef("gun_pistol_seeker", "誘導ピストル", ItemKind.RANGED_WEAPON, desc = "種類:ピストル　弾が敵を追尾する", weaponType = "pistol", homing = 3f, gunMul = 0.9f),
        ItemDef("gun_mg_storm", "ストームマシンガン", ItemKind.RANGED_WEAPON, desc = "種類:マシンガン　連射+82% 拡散1.8倍 弾幕向き", weaponType = "mg", fireRateMul = 0.55f, spreadMul = 1.8f, gunMul = 0.9f),
        ItemDef("gun_mg_precise", "精密マシンガン", ItemKind.RANGED_WEAPON, desc = "種類:マシンガン　拡散1/3 弾速+30% 威力+15%", weaponType = "mg", spreadMul = 0.3f, bulletSpeedMul = 1.3f, gunMul = 1.15f),
        ItemDef("gun_shotgun_wide", "ワイドショットガン", ItemKind.RANGED_WEAPON, desc = "種類:ショットガン　拡散1.7倍の面制圧", weaponType = "shotgun", spreadMul = 1.7f, gunMul = 0.9f),
        ItemDef("gun_shotgun_slug", "スラッグショットガン", ItemKind.RANGED_WEAPON, desc = "種類:ショットガン　集弾スラッグ 威力+50% 弾速+40%", weaponType = "shotgun", spreadMul = 0.15f, gunMul = 1.5f, bulletSpeedMul = 1.4f),
        ItemDef("gun_smg_seeker", "誘導SMG", ItemKind.RANGED_WEAPON, desc = "種類:SMG　弾が敵を追尾する 威力-10%", weaponType = "smg", homing = 2.5f, gunMul = 0.9f),
        ItemDef("gun_rifle_rail", "レールライフル", ItemKind.RANGED_WEAPON, desc = "種類:ライフル　弾速2倍 威力+60% ブロック破壊1.5倍", weaponType = "rifle", bulletSpeedMul = 2f, gunMul = 1.6f, reloadMul = 1.2f, wallDmgMul = 1.5f),
        // --- 近距離武器 (melee) ---
        ItemDef("melee_knife", "コンバットナイフ", ItemKind.MELEE_WEAPON, desc = "素早い標準ナイフ"),
        ItemDef("melee_blade", "プラズマブレード", ItemKind.MELEE_WEAPON, desc = "近接ダメージ +30%", meleeDmgMul = 1.3f),
        ItemDef("melee_lance", "作業用ランス", ItemKind.MELEE_WEAPON, desc = "鉱区の長柄工具を転用　リーチ +40%", meleeReachMul = 1.4f, meleeDmgMul = 1.1f),
        ItemDef("melee_fang", "大型獣の牙", ItemKind.MELEE_WEAPON, desc = "頂点捕食者の牙を研いだ刃　近接 +50% リーチ +15%", meleeDmgMul = 1.5f, meleeReachMul = 1.15f),
        // 特殊効果つき近接 (v2.39): 振りの広さ・飛ぶ斬撃
        ItemDef("melee_cleaver", "大鉈", ItemKind.MELEE_WEAPON, desc = "振りが大きい　攻撃範囲1.5倍 近接 +20%", meleeDmgMul = 1.2f, meleeArcMul = 1.5f),
        ItemDef("melee_halberd", "ハルバード", ItemKind.MELEE_WEAPON, desc = "広く長い　範囲1.3倍 リーチ +30% 近接 +15%", meleeDmgMul = 1.15f, meleeReachMul = 1.3f, meleeArcMul = 1.3f),
        ItemDef("melee_resonant", "振動ブレード", ItemKind.MELEE_WEAPON, desc = "斬撃が波になって飛ぶ　近接 +10%", meleeDmgMul = 1.1f, meleeWave = true),
        // --- 装飾品 (accessories ×3 slots) ---
        ItemDef("acc_boots", "ランナーブーツ", ItemKind.ACCESSORY, desc = "移動速度 +10%", moveMul = 1.1f),
        ItemDef("acc_charm", "代謝促進バンド", ItemKind.ACCESSORY, desc = "スタミナ回復 +30%", staRegenMul = 1.3f),
        ItemDef("acc_scope", "精密スコープ", ItemKind.ACCESSORY, desc = "銃ダメージ +10%", gunMul = 1.1f),
        ItemDef("acc_plating", "追加プレート", ItemKind.ACCESSORY, desc = "被ダメージ -10%", damageMul = 0.9f),
        ItemDef("acc_feather", "軽量化フィン", ItemKind.ACCESSORY, desc = "移動 +5% 回復 +10%", moveMul = 1.05f, staRegenMul = 1.1f),
        ItemDef("acc_core", "牙のペンダント", ItemKind.ACCESSORY, desc = "猟獣の牙を加工した　近接 +15% 銃 +5%", meleeDmgMul = 1.15f, gunMul = 1.05f),
        ItemDef("acc_compass", "旧式の羅針盤", ItemKind.ACCESSORY, desc = "帰る星を指し続けるという　移動 +6% 回復 +5%", moveMul = 1.06f, staRegenMul = 1.05f),
        ItemDef("acc_idol", "氷の民の小さな偶像", ItemKind.ACCESSORY, desc = "手の中で少し冷たい　被ダメ -5% 回復 +5%", damageMul = 0.95f, staRegenMul = 1.05f),
        ItemDef("acc_ember", "加熱コア", ItemKind.ACCESSORY, desc = "溶岩の民の炉から取り出した熱源　銃 +8% 近接 +5%", gunMul = 1.08f, meleeDmgMul = 1.05f),
        ItemDef("acc_heatfoil", "遮熱フォイル", ItemKind.ACCESSORY, desc = "マグマダメージ無効", traits = setOf(ItemTrait.HEAT_PROOF)),
        ItemDef("acc_gripsole", "グリップソール", ItemKind.ACCESSORY, desc = "雪で減速せず氷で滑らない", traits = setOf(ItemTrait.COLD_PROOF)),
        ItemDef("acc_magnet", "回収磁石", ItemKind.ACCESSORY, desc = "ドロップ品を遠くから引き寄せる", traits = setOf(ItemTrait.MAGNET)),
        ItemDef("acc_repair", "自己修復パッチ", ItemKind.ACCESSORY, desc = "HPを毎秒 1.5 回復", hpRegen = 1.5f),
        ItemDef("acc_gyro", "ジャイロスタビライザー", ItemKind.ACCESSORY, desc = "激突ダメージ無効", traits = setOf(ItemTrait.CRASH_PROOF)),
        ItemDef("acc_grip", "反動制御グリップ", ItemKind.ACCESSORY, desc = "銃 +6% 近接 +3%", gunMul = 1.06f, meleeDmgMul = 1.03f),
        // --- 消費アイテム (consumables — used from the ITEMS tab, v2.34) ---
        ItemDef("med_spray", "応急スプレー", ItemKind.CONSUMABLE, desc = "HP 25 回復", consume = ConsumeKind.HEAL, power = 25f),
        ItemDef("med_kit", "野戦メディキット", ItemKind.CONSUMABLE, desc = "HP 60 回復", consume = ConsumeKind.HEAL, power = 60f),
        ItemDef("med_nano", "ナノ再生ゲル", ItemKind.CONSUMABLE, desc = "HP 120 回復　死せる星の技術", consume = ConsumeKind.HEAL, power = 120f),
        ItemDef("ration", "保存食『星の粥』", ItemKind.CONSUMABLE, desc = "HP 15 回復　六つの星のどこでも同じ味", consume = ConsumeKind.HEAL, power = 15f),
        ItemDef("sta_drink", "高麗藻ドリンク", ItemKind.CONSUMABLE, desc = "スタミナ全回復", consume = ConsumeKind.STAMINA, power = 999f),
        ItemDef("stim_feather", "軽量化スティム", ItemKind.CONSUMABLE, desc = "6秒間スタミナ消費なし", consume = ConsumeKind.STAMINA_INF, power = 6f),
        ItemDef("stim_dash", "反応加速スティム", ItemKind.CONSUMABLE, desc = "8秒間ダッシュ強化", consume = ConsumeKind.DASH_UP, power = 8f),
        ItemDef("smoke_bomb", "煙幕弾", ItemKind.CONSUMABLE, desc = "足元に煙幕を張る", consume = ConsumeKind.SMOKE, power = 5f),
        ItemDef("repair_pack", "携行資材パック", ItemKind.CONSUMABLE, desc = "壁材 +3", consume = ConsumeKind.BLOCKS, power = 3f),
        ItemDef("ammo_cache", "弾薬コンテナ", ItemKind.CONSUMABLE, desc = "全弾薬 +20", consume = ConsumeKind.AMMO_ALL, power = 20f),
        ItemDef("coat_heat", "耐熱コーティング", ItemKind.CONSUMABLE, desc = "60秒間マグマダメージ無効", consume = ConsumeKind.HEAT_PROOF, power = 60f),
        ItemDef("coat_cold", "防寒コーティング", ItemKind.CONSUMABLE, desc = "60秒間 雪の減速と氷の滑りを無効", consume = ConsumeKind.COLD_PROOF, power = 60f),
        ItemDef("field_magnet", "携行磁石", ItemKind.CONSUMABLE, desc = "45秒間ドロップ品を引き寄せる", consume = ConsumeKind.MAGNET, power = 45f),
        ItemDef("patch_regen", "応急再生パック", ItemKind.CONSUMABLE, desc = "20秒間HPを毎秒 2.5 回復", consume = ConsumeKind.REGEN, power = 20f),
        // --- 読み物 (lore items — the universe's stories, v2.34) ---
        ItemDef(
            "lore_letter", "出発の手紙", ItemKind.LORE, desc = "整備士の走り書き",
            lore = "相棒へ。\n" +
                "船は直しておいた。スラスターの癖は前のままだ——\n" +
                "初速はゼロ、噴かした分しか進まない。宇宙では止まる方が難しい。\n" +
                "惑星に近づけば船が勝手にスキャンする。カードが出たら、それが着陸の合図だ。\n" +
                "星々は客を覚える性質がある。行く先々で、恥ずかしくない振る舞いを。\n" +
                "——整備士 ハロ",
        ),
        ItemDef(
            "lore_log_hunter", "狩人の航海日誌・三日目", ItemKind.LORE, desc = "血の染みがある",
            lore = "三日目。緑の星の森で「主」を見た。\n" +
                "図体は岩ほどもあるのに、目は静かだった。群れは主を親のように囲む。\n" +
                "撃てば一財産。だが撃った瞬間、この星のすべてが敵になる気がした。\n" +
                "俺は引き金から指を離した。臆病と呼ばれてもいい。\n" +
                "……四日目の頁は白紙のままだ。",
        ),
        ItemDef(
            "lore_mural", "獣王の壁画の拓本", ItemKind.LORE, desc = "森の岩壁から写し取られた",
            lore = "壁画にはこうある。\n" +
                "「森は主とともに眠り、主とともに目覚める。\n" +
                "子らを守る者は森に守られ、子らを奪う者は森に忘れられぬ。」\n" +
                "拓本の端に、誰かが小さく書き足している——\n" +
                "『忘れられぬ、は比喩ではない。』",
        ),
        ItemDef(
            "lore_lullaby", "氷の民の子守唄", ItemKind.LORE, desc = "凍った石板に刻まれている",
            lore = "ねむれ ねむれ 氷の下\n" +
                "王のかがり火 消えぬうち\n" +
                "白い大蟲(おおむし) 遠まわり\n" +
                "ねむれ 朝には 星がくる\n" +
                "\n" +
                "——氷の惑星では、白い大蟲(フロストワーム)は畏れと祈りの対象である。",
        ),
        ItemDef(
            "lore_relic_shard", "遺物の断片の銘", ItemKind.LORE, desc = "読める部分だけを書き写した",
            lore = "断片の銘、判読できた部分:\n" +
                "「これを持ち出す者は、この星の記憶を持ち出す。\n" +
                "重さは腕にではなく、名にかかる。」\n" +
                "\n" +
                "遺物を聖とする星で、これを拾った異邦人の名がどう呼ばれるかは——\n" +
                "その星に戻れば分かる。",
        ),
        ItemDef(
            "lore_gas_hymn", "ガスの僧院の聖句", ItemKind.LORE, desc = "嵐の中で歌われる",
            lore = "「風は言葉を持たぬゆえ、我らが代わりに黙す。\n" +
                "嵐は形を持たぬゆえ、我らが代わりに立つ。」\n" +
                "\n" +
                "ガス惑星の僧たちは生涯に三語しか話さないという。\n" +
                "一語目は生まれた日に。二語目は継ぐ日に。\n" +
                "三語目は——異邦人に警告する日に。",
        ),
        ItemDef(
            "lore_dead_epitaph", "死せる星の碑文", ItemKind.LORE, desc = "風化した石碑の写し",
            lore = "碑文:\n" +
                "「ここに都があった。ここに歌があった。\n" +
                "我らは静寂を選んだのではない。静寂だけが残ったのだ。\n" +
                "廃墟を踏む者よ、せめて足音を小さく。」",
        ),
        ItemDef(
            "lore_lonely_scrap", "孤独な星の走り書き", ItemKind.LORE, desc = "誰かの手記の切れ端",
            lore = "「今日も誰も来なかった。\n" +
                "  明日も誰も来ないだろう。\n" +
                "  それでも着陸パッドの灯りは点けておく。\n" +
                "  ……もし来たら、怖がらせないように、そっと。」\n" +
                "\n" +
                "筆跡は震えているが、灯りの手入れの記録は一日も欠けていない。",
        ),
        ItemDef(
            "lore_oc_manual", "OCスラスター整備手帳", ItemKind.LORE, desc = "赤字の注意書きだらけ",
            lore = "OC-7型 整備手帳 抜粋:\n" +
                "・全開(フルスロットル)は推力3倍、巡航2倍。体はついてこない(スタミナ消費2倍)。\n" +
                "・全開のまま星に突っ込んだ馬鹿が過去に11人いる。12人目になるな。\n" +
                "・異音がしたら止めろ。歌い出したら逃げろ。",
        ),
        ItemDef(
            "lore_star_poem", "星渡りの詩", ItemKind.LORE, desc = "旅人の間で口伝えされる",
            lore = "ひとつ星巡り ふたつ星忘れ\n" +
                "みっつ星の子と 火を分けて\n" +
                "よっつ星の王に 頭(こうべ)を垂れて\n" +
                "いつつ星の獣と 目を合わせず\n" +
                "むっつ星でようやく 帰り道を知る",
        ),
        ItemDef(
            "lore_wanted", "手配書「星喰い」", ItemKind.LORE, desc = "どの港にも貼られている",
            lore = "手配: 通称「星喰い」\n" +
                "罪状: 惑星の子らの殺害、遺物の強奪、頂点捕食者の乱獲。\n" +
                "特徴: 不明。ただし——立ち寄った星が、その名を覚えている。\n" +
                "賞金: 星系通貨で山ひとつ。\n" +
                "\n" +
                "余白の落書き:『星は忘れないぞ。おまえが誰でも。』",
        ),
        ItemDef(
            "lore_recipe", "『星の粥』のレシピ", ItemKind.LORE, desc = "油染みたメモ",
            lore = "材料: その星で採れる穀物なんでも、水、塩、根気。\n" +
                "1. 穀物を割る。2. 煮る。3. 焦がさない。4. 待つ。\n" +
                "\n" +
                "「うまくはない。だがどの星の粥も、なぜか同じ味がする。\n" +
                "だから旅人はこれを食うと、帰ってきた気になるのだ。」",
        ),
    )

    private val byId = ALL.associateBy { it.id }
    fun byId(id: String): ItemDef? = byId[id]

    /** The RANGED_WEAPON item that behaves as the given WeaponDef id (pistol/shotgun/…). */
    fun byWeaponType(weaponType: String): ItemDef? = ALL.firstOrNull { it.weaponType == weaponType }

    // Drop pools (v2.34): consumables are the common spoils, equipment the good ones, lore the rare voices.
    private val consumables = ALL.filter { it.kind == ItemKind.CONSUMABLE }
    private val loreItems = ALL.filter { it.kind == ItemKind.LORE }
    private val equipment = ALL.filter { it.kind != ItemKind.CONSUMABLE && it.kind != ItemKind.LORE }

    /** What a fresh run wears: pistol + knife on a standard thruster in a pilot suit. */
    fun starterLoadout(): Loadout = Loadout(
        thruster = byId("thruster_std"),
        armor = byId("armor_cloth"),
        ranged = byId("gun_pistol"),
        melee = byId("melee_knife"),
    )

    /** What a fresh run carries: the rest of the classic guns, an OC thruster to try full throttle,
     *  a couple of consumables — and the mechanic's letter that teaches how to land (v2.34). */
    fun starterBackpack(): MutableList<ItemDef> = mutableListOf(
        byId("gun_shotgun")!!, byId("gun_mg")!!, byId("gun_beam")!!, byId("gun_grenade")!!,
        byId("thruster_oc")!!,
        byId("med_spray")!!, byId("smoke_bomb")!!,
        byId("lore_letter")!!,
    )

    /** A deterministic drop pick: ~55% consumable / ~30% equipment / ~15% lore (kill loot rolls an index). */
    fun dropFor(roll: Int): ItemDef {
        val r = ((roll % 1000) + 1000) % 1000
        val pool = when {
            r < 550 -> consumables
            r < 850 -> equipment
            else -> loreItems
        }
        return pool[r % pool.size]
    }

    private val guns = ALL.filter { it.kind == ItemKind.RANGED_WEAPON }

    /** v2.40: a deterministic GUN pick — the deep-wall caches yield weapons, nothing else. */
    fun gunFor(roll: Int): ItemDef = guns[((roll % guns.size) + guns.size) % guns.size]

    /** The backpack grouped for display: one row per distinct item (first-encounter order) with its count. */
    fun grouped(backpack: List<ItemDef>): List<Pair<ItemDef, Int>> =
        backpack.groupBy { it.id }.map { (_, items) -> items.first() to items.size }
}
