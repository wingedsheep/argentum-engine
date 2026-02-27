package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect

/**
 * Cephalid Pathmage
 * {2}{U}
 * Creature — Octopus Wizard
 * 1/2
 * This creature can't be blocked.
 * {T}, Sacrifice this creature: Target creature can't be blocked this turn.
 */
val CephalidPathmage = card("Cephalid Pathmage") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Octopus Wizard"
    power = 1
    toughness = 2
    oracleText = "Cephalid Pathmage can't be blocked.\n{T}, Sacrifice Cephalid Pathmage: Target creature can't be blocked this turn."

    flags(AbilityFlag.CANT_BE_BLOCKED)

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val t = target("target", Targets.Creature)
        effect = GrantKeywordUntilEndOfTurnEffect(AbilityFlag.CANT_BE_BLOCKED.name, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Alex Horley-Orlandelli"
        flavorText = "Pathmages can open doors that aren't even there."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88528929-4953-452a-b85e-dac15786e094.jpg?1562922565"
    }
}
