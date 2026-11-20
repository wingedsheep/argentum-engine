package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Viashino Firstblade
 * {1}{R}{W}
 * Creature — Lizard Soldier
 * 2/2
 * Haste
 * When this creature enters, it gets +2/+2 until end of turn.
 */
val ViashinoFirstblade = card("Viashino Firstblade") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Lizard Soldier"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhen this creature enters, it gets +2/+2 until end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Matt Stewart"
        flavorText = "\"All that steel doesn't make them slower. It makes them bolder.\"\n—Tajic, Blade of the Legion"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cb0c21c-bdf1-478a-9ad8-6c6bda6ffb0f.jpg?1783940019"
    }
}
