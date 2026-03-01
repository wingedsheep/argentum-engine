package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scion of Glaciers
 * {2}{U}{U}
 * Creature — Elemental
 * 2/5
 * {U}: Scion of Glaciers gets +1/-1 until end of turn.
 */
val ScionOfGlaciers = card("Scion of Glaciers") {
    manaCost = "{2}{U}{U}"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 5
    oracleText = "{U}: This creature gets +1/-1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Titus Lunter"
        flavorText = "\"There is nothing so free as the spring river born of winter's ice.\" —Nitula, the Hunt Caller"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e370939-831b-4c42-8291-8ffb0b6c1af7.jpg?1562787263"
    }
}
