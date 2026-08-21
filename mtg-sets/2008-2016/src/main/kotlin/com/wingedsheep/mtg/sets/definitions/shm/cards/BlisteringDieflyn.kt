package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blistering Dieflyn
 * {3}{R}
 * Creature — Imp
 * 0 / 1
 *
 * Flying
 * {B/R}: This creature gets +1/+0 until end of turn.
 *
 * - The hybrid activation cost goes into [Costs.Mana] verbatim; `{B/R}` is payable with either
 *   {B} or {R} and the parser handles the hybrid symbol, so nothing is overridden here.
 * - "This creature" is the source, so the pump targets [EffectTarget.Self] rather than declaring a
 *   target — the ability doesn't target and can be activated any number of times.
 */
val BlisteringDieflyn = card("Blistering Dieflyn") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Imp"
    power = 0
    toughness = 1
    oracleText = "Flying\n" +
        "{B/R}: This creature gets +1/+0 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{B/R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Scott Altmann"
        flavorText = "Any kithkin smith would love to catch a dieflyn for her kiln, relieving her of ever having to gather fuel again."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5720a5b2-60ca-49f9-83e8-b801471c92ea.jpg?1783942751"
    }
}
