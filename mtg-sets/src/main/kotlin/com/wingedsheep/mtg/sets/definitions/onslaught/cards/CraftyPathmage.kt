package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect

/**
 * Crafty Pathmage
 * {2}{U}
 * Creature — Human Wizard
 * 1/1
 * {T}: Target creature with power 2 or less can't be blocked this turn.
 */
val CraftyPathmage = card("Crafty Pathmage") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature with power 2 or less can't be blocked this turn."

    activatedAbility {
        cost = Costs.Tap
        target = Targets.CreatureWithPowerAtMost(2)
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.CANT_BE_BLOCKED, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Wayne England"
        flavorText = "\"If you want to get from here to there without being seen, I'm the one to call.\""
        imageUri = "https://cards.scryfall.io/large/front/c/5/c5d91378-f831-40ef-a79b-b044af1470e0.jpg?1562941736"
    }
}
