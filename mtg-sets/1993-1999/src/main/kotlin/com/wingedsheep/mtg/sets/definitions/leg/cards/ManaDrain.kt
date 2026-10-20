package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerStepScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mana Drain
 * {U}{U}
 * Instant
 * Counter target spell. At the beginning of your next main phase, add an amount of {C} equal to
 * that spell's mana value.
 *
 * The delayed trigger is created *before* the counter, while the target is still on the stack:
 * `CreateDelayedTriggerExecutor` snapshots the [Effects.AddColorlessMana] amount into a literal at
 * creation, so the mana value is read off the live spell (Mana Sculpt's shape). It is scheduled
 * whether or not the counter succeeds — a legal target that can't be countered still pays out, per
 * the 2020-11-10 ruling; an illegal target fizzles the whole spell, so nothing is scheduled.
 *
 * "Your next main phase" is [DelayedTriggerStepScope.ANY_MAIN_PHASE] gated to [Player.You]: cast in
 * your precombat main phase or your combat phase, it fires at that turn's postcombat main phase.
 */
val ManaDrain = card("Mana Drain") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. At the beginning of your next main phase, add an amount of {C} " +
        "equal to that spell's mana value."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CreateDelayedTrigger(
            step = Step.PRECOMBAT_MAIN,
            stepScope = DelayedTriggerStepScope.ANY_MAIN_PHASE,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            effect = Effects.AddColorlessMana(DynamicAmounts.manaValueOf(spell)),
        ) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e691adef-3027-4e6a-889f-9f4e2df36a7c.jpg?1783948074"
        ruling(
            "2020-11-10",
            "If the target spell is an illegal target by the time Mana Drain tries to resolve, Mana Drain " +
                "doesn't resolve. You don't add mana at the beginning of your next main phase. If the target " +
                "is legal but not countered (most likely because an effect says that the spell can't be " +
                "countered), you do add mana."
        )
        ruling(
            "2020-11-10",
            "Mana Drain's delayed triggered ability will usually trigger at the beginning of your precombat " +
                "main phase. However, if you cast Mana Drain during your precombat main phase or during your " +
                "combat phase, its delayed triggered ability will trigger at the beginning of that turn's " +
                "postcombat main phase."
        )
    }
}
