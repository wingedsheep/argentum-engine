package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import kotlinx.serialization.Serializable

/**
 * Resume a composite effect with remaining effects to execute.
 *
 * When a sub-effect of a CompositeEffect pauses for a decision, we push
 * this frame to remember which effects still need to run after the
 * decision is resolved.
 *
 * @property remainingEffects Effects that still need to execute (serialized)
 * @property context The execution context for these effects
 */
@Serializable
data class EffectContinuation(
    override val decisionId: String,
    val remainingEffects: List<Effect>,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resume placing a triggered ability on the stack after targets have been selected.
 *
 * When a triggered ability requires targets (like Fire Imp's "deal 1 damage to any target"),
 * we cannot put it directly on the stack. Instead, we pause to ask the player for targets,
 * storing this continuation to remember which ability we're processing.
 *
 * @property sourceId The permanent that has the triggered ability
 * @property sourceName Name of the source card for display
 * @property controllerId The player who controls the triggered ability
 * @property effect The effect to execute when the ability resolves
 * @property description Human-readable description of the ability
 */
@Serializable
data class TriggeredAbilityContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    val description: String,
    /** Definition-scoped identity of the triggered ability (see
     *  [com.wingedsheep.sdk.scripting.AbilityIdentity]); preserved across target selection so the
     *  stack object built on resume carries it. Null for sources with no card definition. */
    val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null,
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val elseEffect: Effect? = null,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val triggerCounterCount: Int? = null,
    val triggerTotalCounterCount: Int? = null,
    val triggerLastKnownCounters: Map<String, Int>? = null,
    val triggerLastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /** Creatures blocking/blocked by the trigger's source on leave-battlefield (CR 509 LKI, Abu Ja'far). */
    val triggerLastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    val lastKnownPower: Int? = null,
    val lastKnownToughness: Int? = null,
    val triggerModesChosenCount: Int? = null,
    /** Power of the aura/equipment's attached creature, captured at trigger time (CR 608.2h LKI). */
    val enchantedCreatureLastKnownPower: Int? = null,
    /** Cards looked at by the scry that fired this trigger (CR 701.18). Null for non-scry triggers. */
    val triggerScryCount: Int? = null,
    /** Damage past lethal dealt to the trigger's creature recipient (CR 120.4a). Null for non-damage triggers. */
    val triggerExcessDamageAmount: Int? = null,
    /** Total mana spent to cast the spell that fired this trigger (Aberrant Manawurm, Expressive
     *  Firedancer). Read via `ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL`. Null for non-cast triggers. */
    val triggerManaSpentOnTriggeringSpell: Int? = null
) : ContinuationFrame

/**
 * Resume placing a triggered ability on the stack after the player distributes damage.
 *
 * When a triggered ability uses DividedDamageEffect and has multiple targets,
 * we first ask for targets (via TriggeredAbilityContinuation), then pause again
 * to ask how to divide the damage among those targets. Once the distribution is
 * chosen, the ability goes on the stack with the distribution locked in.
 *
 * @property sourceId The permanent that has the triggered ability
 * @property sourceName Name of the source card for display
 * @property controllerId The player who controls the triggered ability
 * @property effect The effect to execute when the ability resolves
 * @property description Human-readable description of the ability
 * @property selectedTargets The targets already chosen in the previous step
 * @property targetRequirements The target requirements for the ability
 * @property totalDamage The total damage to distribute
 */
@Serializable
data class TriggerDamageDistributionContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    val description: String,
    /** Definition-scoped identity of the triggered ability (see
     *  [com.wingedsheep.sdk.scripting.AbilityIdentity]); preserved across damage distribution so
     *  the stack object built on resume carries it. Null for sources with no card definition. */
    val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null,
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val triggerCounterCount: Int? = null,
    val triggerTotalCounterCount: Int? = null,
    val triggerLastKnownCounters: Map<String, Int>? = null,
    val triggerLastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /** Creatures blocking/blocked by the trigger's source on leave-battlefield (CR 509 LKI, Abu Ja'far). */
    val triggerLastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    val selectedTargets: List<ChosenTarget>,
    val targetRequirements: List<TargetRequirement>,
    val totalDamage: Int,
    val lastKnownPower: Int? = null,
    val lastKnownToughness: Int? = null
) : ContinuationFrame

/**
 * Stores remaining pending triggers that still need to be processed.
 *
 * When multiple triggered abilities fire from the same event and the first
 * requires target selection (pausing execution), the remaining triggers are
 * stored in this continuation frame. After the first trigger's targets are
 * selected, the remaining triggers are processed.
 */
@Serializable
data class PendingTriggersContinuation(
    override val decisionId: String,
    val remainingTriggers: List<PendingTrigger>
) : ContinuationFrame

/**
 * Resume spell resolution after target or mode selection.
 *
 * @property spellId The spell entity on the stack
 * @property casterId The player who cast the spell
 */
@Serializable
data class ResolveSpellContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val casterId: EntityId
) : ContinuationFrame

/**
 * Pre-pushed by [com.wingedsheep.engine.handlers.effects.composite.GatedEffectExecutor] for a
 * [com.wingedsheep.sdk.scripting.effects.Gate.DoAction] gate, before executing the gated action.
 * Auto-resumes once the action's own continuation stack has fully resolved; evaluates
 * [successCriterion] against the snapshot to decide whether to dispatch [then] or [otherwise].
 *
 * This is the *action-drain* counterpart to [GatedEffectContinuation] (which resumes on a yes/no
 * decision): a [Gate.DoAction] has no decision to answer, so the auto-resumer picks it up when the
 * action's own continuations have drained.
 *
 * @property then Effect to run iff the action accomplished its work (the gate's `then`).
 * @property otherwise Effect to run iff the action did nothing (the gate's `otherwise`).
 * @property snapshot Pre-execution data the criterion needs to compute the delta
 *           (e.g., destination zone size before the action ran).
 */
@Serializable
data class GatedActionContinuation(
    override val decisionId: String,
    val then: Effect,
    val otherwise: Effect?,
    val successCriterion: SuccessCriterion,
    val snapshot: GatedActionSnapshot,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Probe data captured before [GatedActionContinuation]'s action ran.
 *
 * For pipeline-shaped actions ending in a `MoveCollectionEffect`, [destinationZoneOwner]
 * + [destinationZoneType] identify a zone whose pre-action size is stored in
 * [destinationZonePreSize]; the criterion evaluates "did the zone grow."
 *
 * Atomic action probes (life paid, sacrifice count, damage dealt) will add their own
 * fields here as they're implemented — this snapshot is intentionally extensible
 * rather than a sealed union of probe modes, since one action may need multiple
 * probes simultaneously.
 */
@Serializable
data class GatedActionSnapshot(
    val destinationZoneOwner: EntityId? = null,
    val destinationZoneType: Zone? = null,
    val destinationZonePreSize: Int = 0
)

/**
 * Resume after player makes a yes/no choice (may abilities).
 *
 * @property playerId The player who made the choice
 * @property sourceId The spell/ability with the may clause
 * @property sourceName Name of the source
 * @property effectIfYes The effect to execute if player chose yes
 * @property effectIfNo The effect to execute if player chose no (usually null/no-op)
 */
@Serializable
data class MayAbilityContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceName: String?,
    val effectIfYes: Effect?,
    val effectIfNo: Effect?,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resume a [com.wingedsheep.sdk.scripting.effects.GatedEffect] after its gate has been
 * resolved by a yes/no decision.
 *
 * The unified frame for the decision-driven gate kinds ([Gate.MayDecide], [Gate.MayPay]):
 * the executor pauses with a [YesNoDecision], and this continuation carries everything the
 * resumer needs to dispatch the right branch in the canonical order — run [then] on success
 * (for [Gate.MayPay], paying [Gate.MayPay.cost] first), or [otherwise] on a decline.
 *
 * [effectContext] carries the locked `targets` so a targeted [then] (e.g. "you may pay {2};
 * if you do, destroy target creature") resolves against the trigger-time target rather than
 * re-choosing one — see the engine load-bearing rule on propagating targets.
 *
 * @property gate The gate that was offered (determines how a "yes" is consumed).
 * @property then Effect to run iff the gate succeeds.
 * @property otherwise Effect to run iff the gate fails / is declined.
 */
@Serializable
data class GatedEffectContinuation(
    override val decisionId: String,
    val gate: Gate,
    val then: Effect,
    val otherwise: Effect?,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resume after the player picks a card (or declines) for [MayRevealCardFromHandEffect].
 *
 * @property revealerId The player who was asked to reveal
 * @property sourceId The source of the reveal effect (e.g. the entering shadowland)
 * @property sourceName Name of the source for prompts/events
 * @property otherwise Effect to run when the player declines or submits an empty selection
 * @property effectContext Effect context propagated to [otherwise] so `EffectTarget.Self`,
 *                          chosen targets, controller, etc. resolve correctly
 */
@Serializable
data class MayRevealCardFromHandContinuation(
    override val decisionId: String,
    val revealerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val otherwise: Effect?,
    val effectContext: EffectContext,
) : ContinuationFrame

/**
 * Resume after the player chooses to behold (or declines) for
 * [com.wingedsheep.sdk.scripting.effects.BeholdEffect].
 *
 * @property beholderId The player who was asked to behold
 * @property sourceName Name of the source for prompts/events
 * @property handOptionIds The subset of the decision options that live in the beholder's hand
 *                          (revealed when chosen); battlefield options are merely chosen.
 * @property ifBeheld Effect to run when the player successfully beholds
 * @property effectContext Effect context propagated to [ifBeheld]
 */
@Serializable
data class BeholdContinuation(
    override val decisionId: String,
    val beholderId: EntityId,
    val sourceName: String?,
    val handOptionIds: Set<EntityId>,
    val ifBeheld: Effect?,
    val effectContext: EffectContext,
) : ContinuationFrame

/**
 * Resume placing a triggered ability on the stack after the player answers a "may" question.
 *
 * When a triggered ability has both a bare "may" gate (a [Gate.MayDecide] with no `otherwise` —
 * the lowered `MayEffect`, recognized via `Effect.asMayDecide`) and targets (like Invigorating
 * Boon's "you may put a +1/+1 counter on target creature"), the may question is asked FIRST.
 * If the player says yes, we then proceed to target selection.
 * If the player says no, the trigger is skipped entirely.
 *
 * @property trigger The full pending trigger to process if the player says yes
 * @property targetRequirement The target requirement for the ability
 */
@Serializable
data class MayTriggerContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement
) : ContinuationFrame

/**
 * One snapshotted iteration item of a [com.wingedsheep.sdk.scripting.effects.ForEachEffect].
 * The variant corresponds to (but is deliberately decoupled from) the effect's
 * [com.wingedsheep.sdk.scripting.effects.IterationSpace]: targets iterate [OfTarget],
 * players iterate [OfPlayer], collections/groups iterate [OfEntity], colors iterate
 * [OfColor]. Serializable so a [ForEachContinuation] can carry the remaining items
 * across a mid-iteration pause.
 */
@Serializable
sealed interface ForEachItem {
    @Serializable
    data class OfTarget(val target: ChosenTarget) : ForEachItem

    @Serializable
    data class OfPlayer(val playerId: EntityId) : ForEachItem

    @Serializable
    data class OfEntity(val entityId: EntityId) : ForEachItem

    @Serializable
    data class OfColor(val color: com.wingedsheep.sdk.core.Color) : ForEachItem
}

/**
 * Continuation for [com.wingedsheep.sdk.scripting.effects.ForEachEffect] — one frame for
 * every iteration space (targets, players, collection, group, colors).
 *
 * Pre-pushed before each iteration's body executes; when the body pauses for a decision,
 * this frame remains beneath the body's own frames so the remaining iterations resume
 * after the decision resolves. The full effect is carried so the resumer can re-bind the
 * per-iteration context for the effect's space.
 *
 * @property remainingItems The snapshotted items still to process
 * @property effect The ForEach effect being iterated (space + body)
 * @property effectContext The outer execution context (re-bound per iteration)
 */
@Serializable
data class ForEachContinuation(
    override val decisionId: String,
    val remainingItems: List<ForEachItem>,
    val effect: com.wingedsheep.sdk.scripting.effects.ForEachEffect,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Continuation for RepeatWhileEffect.
 *
 * Stores the full body effect and repeat condition so the loop can re-execute.
 * For PlayerChooses conditions, the decider is resolved once and stored as
 * resolvedDeciderId to avoid re-resolving EffectTarget on subsequent iterations.
 *
 * Two phases:
 * - AFTER_BODY: Pre-pushed before body executes. Found in checkForMoreContinuations
 *   after the body (or its sub-effects) complete. Transitions to asking the condition.
 * - AFTER_DECISION: Waiting for the player's yes/no answer (PlayerChooses only).
 *
 * @property body The effect to execute each iteration
 * @property repeatCondition The serialized repeat condition
 * @property resolvedDeciderId For PlayerChooses — the resolved player entity ID
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the effect
 * @property xValue The X value (if applicable)
 * @property targets The chosen targets (for effect context)
 * @property phase Current phase of the repeat loop
 */
@Serializable
data class RepeatWhileContinuation(
    override val decisionId: String,
    val body: Effect,
    val repeatCondition: com.wingedsheep.sdk.scripting.effects.RepeatCondition,
    val resolvedDeciderId: EntityId? = null,
    val sourceName: String?,
    val phase: RepeatWhilePhase,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Phase discriminator for RepeatWhileContinuation.
 */
@Serializable
enum class RepeatWhilePhase {
    /** Pre-pushed before body executes; found in checkForMoreContinuations after body completes */
    AFTER_BODY,
    /** Waiting for the player's yes/no decision (PlayerChooses only) */
    AFTER_DECISION
}

/**
 * Pre-pushed before executing a reflexive trigger's action. Auto-resumed after
 * the action completes to present target selection for the reflexive effect.
 *
 * Flow: executor pre-pushes this → executes action → on success, pops and targets inline;
 * on pause, auto-resumer handles targeting after the action's decision resolves.
 *
 * @property reflexiveEffect The effect to execute after targets are chosen
 * @property reflexiveTargetRequirements Target requirements for the reflexive effect
 * @property effectContext The execution context from the parent ability
 */
@Serializable
data class ReflexiveTriggerTargetContinuation(
    override val decisionId: String,
    val reflexiveEffect: Effect,
    val reflexiveTargetRequirements: List<TargetRequirement>,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resumed after the player selects targets for a reflexive trigger's effect.
 *
 * @property reflexiveEffect The effect to execute with the chosen targets
 * @property reflexiveTargetRequirements The original target requirements (for validation)
 * @property effectContext The execution context (targets will be merged in from the response)
 */
@Serializable
data class ReflexiveTriggerResolveContinuation(
    override val decisionId: String,
    val reflexiveEffect: Effect,
    val reflexiveTargetRequirements: List<TargetRequirement>,
    val effectContext: EffectContext
) : ContinuationFrame
