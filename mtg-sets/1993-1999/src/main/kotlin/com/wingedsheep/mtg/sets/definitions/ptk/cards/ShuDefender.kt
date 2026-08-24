package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shu Defender
 * {2}{W}
 * Creature — Human Soldier
 * 2/2
 * Whenever this creature blocks, it gets +0/+2 until end of turn.
 */
val ShuDefender = card("Shu Defender") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature blocks, it gets +0/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.ModifyStats(0, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Sun Nan"
        flavorText = "Confronting Cao Cao's army at Steepslope Bridge, Zhang Fei bellowed, \"I am Zhang Fei of Yan! Who dares fight me to the death?\" Cao Cao's army cowered and fled."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee57a9ab-c385-4a51-aff7-6a654f5d7611.jpg"
    }
}
