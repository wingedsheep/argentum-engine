package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Triton Tactics — Theros #71
 * {U} · Instant
 *
 * Up to two target creatures each get +0/+3 until end of turn. Untap those creatures. At this
 * turn's next end of combat, tap each creature that was blocked by one of those creatures this turn
 * and it doesn't untap during its controller's next untap step.
 *
 * One "up to two" requirement applied per target with [Effects.ForEachTarget] (Abandon Reason), so
 * an illegal target is skipped and is not one of "those creatures". The end-of-combat half is
 * Glyph of Doom's shape, set up per target: a step-based delayed trigger at [Step.END_COMBAT]
 * ([DelayedTriggerTiming.THIS_TURN_ONLY] — cast after the turn's last combat it never fires) whose
 * `watchedTarget` bakes in that creature, sweeping every creature whose turn-scoped block record
 * says it was blocked by it ([GameObjectFilter.wasBlockedByThisTurn] over
 * [EffectTarget.TriggeringEntity]). That record is written at block declaration and kept until the
 * turn ends, so blocks made before Triton Tactics resolved count and the blocker needn't still be on
 * the battlefield. Each swept creature is tapped and given [AbilityFlag.DOESNT_UNTAP] keyed to its
 * own controller's next untap step. Tapping and the untap lock are idempotent, so a creature blocked
 * by both targets ends up exactly as the single printed sweep leaves it.
 */
val TritonTactics = card("Triton Tactics") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Up to two target creatures each get +0/+3 until end of turn. Untap those creatures. " +
        "At this turn's next end of combat, tap each creature that was blocked by one of those creatures " +
        "this turn and it doesn't untap during its controller's next untap step."

    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(0, 3, target = EffectTarget.ContextTarget(0)),
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.CreateDelayedTrigger(
                step = Step.END_COMBAT,
                timing = DelayedTriggerTiming.THIS_TURN_ONLY,
                watchedTarget = EffectTarget.ContextTarget(0),
                effect = Effects.ForEachInGroup(
                    filter = GroupFilter(
                        GameObjectFilter.Creature.wasBlockedByThisTurn(EffectTarget.TriggeringEntity)
                    ),
                    effect = Effects.Tap(EffectTarget.IterationEntity) then
                        Effects.GrantKeyword(
                            AbilityFlag.DOESNT_UNTAP,
                            EffectTarget.IterationEntity,
                            Duration.UntilAfterAffectedControllersNextUntap(),
                        ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef2609e3-ec28-4a41-884e-3d10be5e31b4.jpg?1783939787"
    }
}
