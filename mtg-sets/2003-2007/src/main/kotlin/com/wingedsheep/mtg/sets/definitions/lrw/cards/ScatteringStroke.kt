package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerStepScope
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scattering Stroke
 * {2}{U}{U}
 * Instant
 *
 * Counter target spell. Clash with an opponent. If you win, at the beginning of your next main
 * phase, you may add an amount of {C} equal to that spell's mana value.
 *
 * Three time-shifts stack up here, and each one is a place the amount can silently read zero:
 *
 *  1. **"That spell" is already gone.** The counter moves it to its owner's graveyard before the
 *     clash even begins, and [com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget] is
 *     `LIVE_ONLY` by design (CR 608.2b) with no target LKI behind it. So the mana value is frozen
 *     up front with the pipeline's `storeNumber` and read back through its handle's
 *     `amount` — the same shape Weed Strangle uses for a destroyed
 *     creature's toughness. Mana Sculpt solves the same problem the other way, by creating its
 *     delayed trigger *before* the counter; that isn't available here because the clash sits
 *     between the two and decides whether the trigger exists at all.
 *  2. **The clash pauses twice** for the two top-or-bottom decisions, so the win rider runs on the
 *     far side of a gated pause and the stored number has to survive it.
 *  3. **The payoff fires a phase later**, when the resolution pipeline that holds the stored number
 *     is gone. `CreateDelayedTriggerExecutor` snapshots an
 *     [com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect]'s amount into a literal at
 *     creation time for exactly this reason, and it recurses through the [Effects.May] consent gate
 *     to reach it — so the number is baked in while the pipeline can still answer.
 *
 * "You may" is a resolution-time consent gate on the delayed trigger, not a choice made now: the
 * 2007-10-01 ruling says you decide as the delayed ability resolves, and it is all-or-nothing.
 *
 * "Your next main phase" is whichever main phase of the controller's comes next
 * ([DelayedTriggerStepScope.ANY_MAIN_PHASE] gated to [Player.You]) — cast in your precombat main
 * phase or your combat phase, that is this turn's postcombat main phase (CR 505.1).
 * [DelayedTriggerTiming.CURRENT_TURN_OR_LATER] lets the current turn qualify, which is what an
 * instant cast in your own upkeep wants.
 */
val ScatteringStroke = card("Scattering Stroke") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. Clash with an opponent. If you win, at the beginning of " +
        "your next main phase, you may add an amount of {C} equal to that spell's mana value. " +
        "(Each clashing player reveals the top card of their library, then puts that card on their " +
        "choice of the top or bottom. A player wins if their card had a greater mana value.)"

    spell {
        val spellTarget = target(TargetFilter.SpellOnStack)
        effect = Effects.Pipeline {
            val manaValue = storeNumber(DynamicAmounts.manaValueOf(spellTarget))
            run(Effects.CounterSpell())
            run(
                Patterns.Mechanic.clash(
                    Effects.CreateDelayedTrigger(
                        step = Step.PRECOMBAT_MAIN,
                        fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                        timing = DelayedTriggerTiming.CURRENT_TURN_OR_LATER,
                        stepScope = DelayedTriggerStepScope.ANY_MAIN_PHASE,
                        effect = Effects.May(Effects.AddColorlessMana(manaValue.amount))
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c536c1ce-a012-4d77-ab29-8574be164731.jpg?1783942899"
        ruling(
            "2007-10-01",
            "You decide whether or not to add that much {C} as the delayed triggered ability " +
                "resolves at the beginning of your next main phase. You don't decide before then."
        )
        ruling("2007-10-01", "You can't add just some of the mana. You either add all the mana or none of it.")
    }
}
