package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Llanowar Vanguard
 * {2}{G}
 * Creature — Dryad
 * 1/1
 * {T}: This creature gets +0/+4 until end of turn.
 */
val LlanowarVanguard = card("Llanowar Vanguard") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 1
    toughness = 1
    oracleText = "{T}: This creature gets +0/+4 until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = ModifyStatsEffect(0, 4, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72e6ed79-bdfd-49f9-bfa4-be4196880487.jpg?1562917999"
    }
}
