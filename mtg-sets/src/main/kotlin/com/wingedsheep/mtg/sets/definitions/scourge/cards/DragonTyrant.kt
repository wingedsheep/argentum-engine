package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Tyrant
 * {8}{R}{R}
 * Creature — Dragon
 * 6/6
 * Flying, trample
 * Double strike
 * At the beginning of your upkeep, sacrifice Dragon Tyrant unless you pay {R}{R}{R}{R}.
 * {R}: Dragon Tyrant gets +1/+0 until end of turn.
 */
val DragonTyrant = card("Dragon Tyrant") {
    manaCost = "{8}{R}{R}"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying, trample\nDouble strike (This creature deals both first-strike and regular combat damage.)\nAt the beginning of your upkeep, sacrifice Dragon Tyrant unless you pay {R}{R}{R}{R}.\n{R}: Dragon Tyrant gets +1/+0 until end of turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE, Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = PayCost.Mana(ManaCost.parse("{R}{R}{R}{R}")),
            suffer = SacrificeSelfEffect
        )
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = ModifyStatsEffect(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04d1a29b-af80-4f9a-881b-ef7374ecbce1.jpg?1758104231"
    }
}
