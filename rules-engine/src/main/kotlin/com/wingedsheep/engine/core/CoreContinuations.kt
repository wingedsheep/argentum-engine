package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
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
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val elseEffect: Effect? = null,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val triggerCounterCount: Int? = null,
    val triggerTotalCounterCount: Int? = null,
    val lastKnownPower: Int? = null,
    val lastKnownToughness: Int? = null
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
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val triggerCounterCount: Int? = null,
    val triggerTotalCounterCount: Int? = null,
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
 * Pre-pushed by [com.wingedsheep.engine.handlers.effects.composite.IfYouDoEffectExecutor]
 * before executing the gated action. Auto-resumes once the action's own continuation
 * stack has fully resolved; evaluates [successCriterion] against the snapshot to decide
 * whether to dispatch [ifYouDo] or [ifYouDont].
 *
 * @property snapshot Pre-execution data the criterion needs to compute the delta
 *           (e.g., destination zone size before the action ran).
 */
@Serializable
data class IfYouDoContinuation(
    override val decisionId: String,
    val ifYouDo: Effect,
    val ifYouDont: Effect?,
    val successCriterion: SuccessCriterion,
    val snapshot: IfYouDoSnapshot,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Probe data captured before [IfYouDoContinuation]'s action ran.
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
data class IfYouDoSnapshot(
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
 * Resume placing a triggered ability on the stack after the player answers a "may" question.
 *
 * When a triggered ability has both a MayEffect wrapper and targets (like Invigorating Boon's
 * "you may put a +1/+1 counter on target creature"), the may question is asked FIRST.
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
 * Continuation for ForEachTargetEffect.
 *
 * When a sub-effect pipeline pauses for a decision during one iteration,
 * this continuation stores the remaining targets so execution continues
 * with the next target after the current pipeline completes.
 *
 * @property remainingTargets The targets still to process
 * @property effects The sub-effects to execute for each remaining target
 * @property sourceId The spell that caused this effect
 * @property controllerId The controller of the effect
 * @property opponentId The opponent (if applicable)
 * @property xValue The X value (if applicable)
 */
@Serializable
data class ForEachTargetContinuation(
    override val decisionId: String,
    val remainingTargets: List<ChosenTarget>,
    val effects: List<Effect>,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Continuation for ForEachPlayerEffect.
 *
 * When a sub-effect pipeline pauses for a decision during one iteration,
 * this continuation stores the remaining players so execution continues
 * with the next player after the current pipeline completes.
 *
 * @property remainingPlayers The players still to process
 * @property effects The sub-effects to execute for each remaining player
 * @property sourceId The spell that caused this effect
 * @property controllerId The original controller of the effect
 * @property opponentId The opponent (if applicable)
 * @property xValue The X value (if applicable)
 */
@Serializable
data class ForEachPlayerContinuation(
    override val decisionId: String,
    val remainingPlayers: List<EntityId>,
    val effects: List<Effect>,
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
 * @property opponentId The opponent (for effect context)
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
