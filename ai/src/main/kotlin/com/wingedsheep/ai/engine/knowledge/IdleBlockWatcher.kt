package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.CombatPartnersThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerStepScope
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.predicates.BlockDirection
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * "Who is that Wall going to block?" — a spell whose whole payoff is an end-of-combat sweep over
 * the creatures its target blocked this turn, cast where that creature has blocked nothing and
 * will block nothing before the sweep fires.
 *
 * Glyph of Doom is the card (2026-10-25 AI playtest, four-player game 1, turn 27): the active
 * player cast Wall of Caltrops and then Glyph of Doom on it in their own precombat main. A creature
 * never blocks on its controller's turn, so "destroy all creatures that were blocked by that
 * creature this turn" had nothing it could ever destroy. The leaf saw an instant leave hand for a
 * delayed trigger that scores nothing either way, and that was no worse than passing.
 *
 * The claim is the [TimingVerdict.NoWindow] kind, so the shape is narrow:
 *
 *  - an **instant or sorcery** with no faces, whose spell effect is a single step-based
 *    [CreateDelayedTriggerEffect] at [Step.END_COMBAT] — one-shot, no player gate, no targets of
 *    its own — watching the spell's **one** chosen creature;
 *  - whose delayed effect only gathers battlefield creatures that **were blocked by** the watched
 *    creature this turn ([StatePredicate.BlockPairedWithEntityThisTurn] with
 *    [BlockDirection.BLOCKED_BY], a top-level conjunct of the gather filter) and moves what it
 *    gathered — the lowered `DestroyAll` pipeline;
 *  - and the watched creature has **blocked nothing this turn** and **cannot block before the
 *    trigger fires**: either its controller is the active player and the trigger fires this turn
 *    only ([DelayedTriggerTiming.THIS_TURN_ONLY]; the active player is never a defending player),
 *    or blockers are already declared in this combat, it is not among them, and the trigger fires
 *    at this combat's own end.
 *
 * "Blocked or was blocked by" (Gaze of the Gorgon) is not this shape: that creature attacking on
 * its own turn is a payoff. Carried on [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs].
 */
internal object IdleBlockWatcher {

    private val BLOCKS_DECLARED = setOf(Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE)

    fun holds(
        state: GameState,
        definition: CardDefinition,
        targets: List<ChosenTarget>,
    ): Boolean {
        val type = definition.typeLine
        if (!(type.isInstant || type.isSorcery)) return false
        if (definition.cardFaces.isNotEmpty() || definition.backFace != null || definition.flipSide != null) return false
        val delayed = definition.script.spellEffect as? CreateDelayedTriggerEffect ?: return false
        if (delayed.trigger != null || delayed.step != Step.END_COMBAT || delayed.repeatAtEachMatchingStep ||
            delayed.fireOnPlayer != null || delayed.targetRequirement != null ||
            delayed.additionalTargetRequirements.isNotEmpty() || delayed.stepScope != DelayedTriggerStepScope.STEP
        ) {
            return false
        }
        if (delayed.watchedTarget == null || !sweepsOnlyWhatItBlocked(delayed)) return false

        val watched = (targets.singleOrNull() as? ChosenTarget.Permanent)?.entityId ?: return false
        val entity = state.getEntity(watched) ?: return false
        if (entity.get<CombatPartnersThisTurnComponent>()?.blockedIds?.isNotEmpty() == true) return false
        if (entity.has<BlockingComponent>()) return false
        val controller = state.projectedState.getController(watched) ?: return false

        val onItsControllersTurn = controller == state.activePlayerId &&
            delayed.timing == DelayedTriggerTiming.THIS_TURN_ONLY
        val blocksAreIn = state.step in BLOCKS_DECLARED &&
            state.getEntity(controller)?.has<BlockersDeclaredThisCombatComponent>() == true
        return onItsControllersTurn || blocksAreIn
    }

    /** Every leaf gathers creatures the watched one blocked, or moves what such a gather found. */
    private fun sweepsOnlyWhatItBlocked(delayed: CreateDelayedTriggerEffect): Boolean {
        val leaves = EffectWalker.leaves(delayed.effect)
        val gathered = leaves.filterIsInstance<GatherCardsEffect>()
            .filter { gather -> (gather.source as? CardSource.BattlefieldMatching)?.filter?.let(::keyedOnItsBlocks) == true }
            .map { it.storeAs }
            .toSet()
        return gathered.isNotEmpty() && leaves.all { leaf ->
            (leaf is GatherCardsEffect && leaf.storeAs in gathered) ||
                (leaf is MoveCollectionEffect && leaf.from in gathered)
        }
    }

    private fun keyedOnItsBlocks(filter: GameObjectFilter): Boolean =
        filter.statePredicates.any {
            it is StatePredicate.BlockPairedWithEntityThisTurn &&
                it.reference == EffectTarget.TriggeringEntity &&
                it.direction == BlockDirection.BLOCKED_BY
        }
}
