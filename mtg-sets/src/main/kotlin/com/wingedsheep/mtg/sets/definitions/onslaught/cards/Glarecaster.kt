package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.RedirectNextDamageEffect
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Glarecaster
 * {4}{W}{W}
 * Creature — Bird Cleric
 * 3/3
 * Flying
 * {5}{W}: The next time damage would be dealt to Glarecaster and/or you this turn,
 * that damage is dealt to any target instead.
 */
val Glarecaster = card("Glarecaster") {
    manaCost = "{4}{W}{W}"
    typeLine = "Creature — Bird Cleric"
    power = 3
    toughness = 3
    oracleText = "Flying\n{5}{W}: The next time damage would be dealt to Glarecaster and/or you this turn, that damage is dealt to any target instead."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{5}{W}")
        target = AnyTarget()
        effect = RedirectNextDamageEffect(
            protectedTargets = listOf(EffectTarget.Self, EffectTarget.Controller),
            redirectTo = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Greg Staples"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e505e8e-51aa-4415-81e6-cf022279edb0.jpg?1562924771"
    }
}
