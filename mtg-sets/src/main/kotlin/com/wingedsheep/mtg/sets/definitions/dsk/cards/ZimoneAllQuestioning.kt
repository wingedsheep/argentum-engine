package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Zimone, All-Questioning
 * {1}{G}{U}
 * Legendary Creature — Human Wizard
 * 1/1
 *
 * At the beginning of your end step, if a land entered the battlefield under your control this turn
 * and you control a prime number of lands, create Primo, the Indivisible, a legendary 0/0 green and
 * blue Fractal creature token, then put that many +1/+1 counters on it.
 *
 * The "prime number of lands" gate is the engine's new unary numeric-predicate condition,
 * [Conditions.AmountIsPrime], over `AggregateBattlefield(You, Land)` — the counterpart to the
 * threshold-only `Compare` family. The "a land entered this turn" half reuses the existing
 * `LANDS_ENTERED_UNDER_CONTROL` turn tracker (≥ 1), and the two are ANDed as the intervening-if
 * (re-checked at resolution per CR 603.4 — if you no longer control a prime number of lands when
 * the ability resolves, it does nothing).
 *
 * The token half is a pure composition: a named legendary 0/0 GU Fractal is created and published
 * to the [CREATED_TOKENS] pipeline collection, then "that many" (= the prime land count) +1/+1
 * counters land on exactly that token via `PipelineTarget(CREATED_TOKENS, 0)` — the same shape as
 * Wild Hypothesis / Applied Geometry.
 */
val ZimoneAllQuestioning = card("Zimone, All-Questioning") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your end step, if a land entered the battlefield under your " +
        "control this turn and you control a prime number of lands, create Primo, the Indivisible, " +
        "a legendary 0/0 green and blue Fractal creature token, then put that many +1/+1 counters " +
        "on it. (2, 3, 5, 7, 11, 13, 17, 19, 23, 29, and 31 are prime numbers.)"

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.All(
            // "a land entered the battlefield under your control this turn"
            Conditions.CompareAmounts(
                DynamicAmounts.landsEnteredUnderControlThisTurn(Player.You),
                com.wingedsheep.sdk.scripting.conditions.ComparisonOperator.GTE,
                DynamicAmount.Fixed(1),
            ),
            // "and you control a prime number of lands"
            Conditions.AmountIsPrime(
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ),
        )
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(1),
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf("Fractal"),
            name = "Primo, the Indivisible",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/9/c990db6b-e1f2-4802-b8b1-80a8b768be0e.jpg?1775827823",
        ).then(
            Effects.AddDynamicCounters(
                Counters.PLUS_ONE_PLUS_ONE,
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/7722f4f7-fe38-4107-a715-7b27b6a4e341.jpg?1726597486"
    }
}
