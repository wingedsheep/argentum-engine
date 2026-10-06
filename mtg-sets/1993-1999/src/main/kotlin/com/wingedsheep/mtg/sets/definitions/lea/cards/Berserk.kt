package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Berserk
 * {G}
 * Instant
 * Cast this spell only before the combat damage step.
 * Target creature gains trample and gets +X/+0 until end of turn, where X is its power. At the
 * beginning of the next end step, destroy that creature if it attacked this turn.
 *
 * "Before the combat damage step" is every step with priority up to and including declare
 * blockers. The first-strike combat damage step is itself a combat damage step, so it's excluded.
 *
 * X is read once, on resolution, off the target's current power ([ModifyStatsExecutor] evaluates
 * the amount when it creates the floating effect), so later pumps don't re-double it.
 *
 * The delayed trigger watches the targeted creature itself — its identity is captured at
 * resolution — and checks "attacked this turn" only when it resolves. Per the 2026-03-20 ruling the
 * permanent is destroyed even if it has stopped being a creature, so the check is on
 * `GameObjectFilter.Any`, not a creature filter. Same composition as Nettling Imp.
 */
val Berserk = card("Berserk") {
    manaCost = "{G}"
    typeLine = "Instant"
    oracleText = "Cast this spell only before the combat damage step.\nTarget creature gains trample and gets +X/+0 until end of turn, where X is its power. At the beginning of the next end step, destroy that creature if it attacked this turn."

    spell {
        castOnlyIf(
            Conditions.IsInStep(
                Step.UPKEEP,
                Step.DRAW,
                Step.PRECOMBAT_MAIN,
                Step.BEGIN_COMBAT,
                Step.DECLARE_ATTACKERS,
                Step.DECLARE_BLOCKERS,
                yoursOnly = false,
            )
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.ModifyStats(DynamicAmounts.powerOf(creature), DynamicAmounts.fixed(0), creature) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                watchedTarget = creature,
                effect = Effects.If(
                    Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Any.attackedThisTurn()),
                    Effects.Destroy(EffectTarget.TriggeringEntity)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e173c8ce-2352-405e-ad00-e3bb94ced1ad.jpg?1783948679"
        ruling("2026-03-20", "If the target permanent stops being a creature before the next end step, it is still destroyed when the delayed trigger resolves.")
    }
}
