package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Breaking of the Fellowship
 * {1}{R}
 * Sorcery
 *
 * Target creature an opponent controls deals damage equal to its power to another target
 * creature that player controls. The Ring tempts you.
 *
 * Modeled as a single two-target requirement constrained to creatures the same opponent
 * controls (`sameController = true`, count = 2 — which also enforces "another", since the
 * two targets must be distinct). The first chosen creature deals damage equal to its power
 * to the second (`DealDamage(targetPower(0), ContextTarget(1), damageSource = ContextTarget(0))`).
 */
val BreakingOfTheFellowship = card("Breaking of the Fellowship") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature an opponent controls deals damage equal to its power to " +
        "another target creature that player controls. The Ring tempts you."

    spell {
        target(
            "creatures an opponent controls",
            TargetCreature(
                count = 2,
                filter = TargetFilter.CreatureOpponentControls,
                sameController = true
            )
        )
        effect = DealDamageEffect(
            amount = DynamicAmounts.targetPower(0),
            target = EffectTarget.ContextTarget(1),
            damageSource = EffectTarget.ContextTarget(0)
        ).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        flavorText = "\"It might have been mine. It should be mine. Give it to me!\""
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/130f60d0-4fac-4e4e-a938-58f3b96e5335.jpg?1686968821"
    }
}
