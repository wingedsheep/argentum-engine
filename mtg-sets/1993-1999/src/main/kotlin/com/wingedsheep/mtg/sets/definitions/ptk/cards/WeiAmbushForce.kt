package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wei Ambush Force
 * {1}{B}
 * Creature — Human Soldier
 * 1/1
 * Whenever this creature attacks, it gets +2/+0 until end of turn.
 */
val WeiAmbushForce = card("Wei Ambush Force") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature attacks, it gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Ku Xueming"
        flavorText = "The battle of Puyang marked the beginning of the end for Lu Bu. He lost the city—and later his life—to Cao Cao."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b241ba4-cff5-48ce-83bd-d70fb5e20ff4.jpg"
    }
}
