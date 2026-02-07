package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect

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

    activatedAbility {
        cost = Costs.Tap
        target = Targets.CreatureWithPowerAtMost(2)
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.CANT_BE_BLOCKED, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Scott M. Fischer"
        flavorText = "\"If you want to get from here to there without being seen, I'm the one to call.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e8ee20d-b0e8-46e5-89c0-11945e4b5085.jpg?1562903889"
    }
}
