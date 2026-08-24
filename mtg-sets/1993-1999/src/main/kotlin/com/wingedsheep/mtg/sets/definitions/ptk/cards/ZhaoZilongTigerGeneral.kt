package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zhao Zilong, Tiger General
 * {3}{W}{W}
 * Legendary Creature — Human Soldier Warrior
 * 3 / 3
 *
 * Horsemanship (This creature can't be blocked except by creatures with horsemanship.)
 * Whenever Zhao Zilong blocks, it gets +1/+1 until end of turn.
 */
val ZhaoZilongTigerGeneral = card("Zhao Zilong, Tiger General") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier Warrior"
    power = 3
    toughness = 3
    oracleText =
        "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "Whenever Zhao Zilong blocks, it gets +1/+1 until end of turn."

    keywords(Keyword.HORSEMANSHIP)

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Quan Xuejun"
        flavorText = "Zhao Zilong was a brave and noble warrior. Twice he rescued Liu Bei's son, Liu Shan."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d16cf1d-a7c3-4038-a648-299c1bedae99.jpg"
    }
}
