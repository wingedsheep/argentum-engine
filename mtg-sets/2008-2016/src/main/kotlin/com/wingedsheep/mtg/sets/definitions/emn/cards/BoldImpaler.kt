package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bold Impaler (Eldritch Moon #120)
 * {R}
 * Creature — Vampire Knight
 * 1/2
 *
 * {2}{R}: This creature gets +2/+0 until end of turn.
 */
val BoldImpaler = card("Bold Impaler") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Knight"
    power = 1
    toughness = 2
    oracleText = "{2}{R}: This creature gets +2/+0 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Karl Kopinski"
        flavorText = "Burning with hatred for Nahiri, the Voldaren and Markov forces assaulted the warped " +
            "Markov Manor with a collective fury not seen in centuries."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45bb2e6d-2ead-4ce3-8e5e-fc6900435583.jpg"
    }
}
