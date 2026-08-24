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
    /** Projected subtypes / card types the triggering permanent had as it left the battlefield
     *  (CR 603.10), preserved across target selection for the intervening-"if"'s second check. */
    val triggerLastKnownSubtypes: Set<String>? = null,
    val triggerLastKnownCardTypes: Set<String>? = null,
    val triggerLastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /** Creatures blocking/blocked by the trigger's source on leave-battlefield (CR 509 LKI, Abu Ja'far). */
    val triggerLastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    val lastKnownPower: Int? = null,
    val lastKnownToughness: Int? = null,
    /** Total last-known power of a creatures-died batch (CR 603.2c). Null for non-batch triggers. */
    val diedBatchTotalPower: Int? = null,
    val triggerModesChosenCount: Int? = null,
    /** Power of the aura/equipment's attached creature, captured at trigger time (CR 608.2h LKI). */
    val enchantedCreatureLastKnownPower: Int? = null,
    /** Cards looked at by the scry that fired this trigger (CR 701.22). Null for non-scry triggers. */
    val triggerScryCount: Int? = null,
    /** Cards discarded in the batch that fired this trigger (CR 603.2c). Read via
     *  `ContextPropertyKey.TRIGGER_DISCARD_COUNT` (Magmakin Artillerist). Null for non-discard triggers. */
    val triggerDiscardCount: Int? = null,
    /** Discover value N of the discover that fired this trigger (CR 701.57). Null for non-discover triggers. */
    val triggerDiscoverValue: Int? = null,
    /** Damage past lethal dealt to the trigger's creature recipient (CR 120.4a). Null for non-damage triggers. */
    val triggerExcessDamageAmount: Int? = null,
    /** Recipient creature's toughness when the triggering damage was dealt (CR 603.10 LKI). Read via
     *  `ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS` (Taii Wakeen). Null for non-creature recipients. */
    val triggerRecipientToughness: Int? = null,
    /** Total mana spent to cast the spell that fired this trigger (Aberrant Manawurm, Expressive
     *  Firedancer). Read via `ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL`. Null for non-cast triggers. */
    val triggerManaSpentOnTriggeringSpell: Int? = null,
    /** Distinct colors of mana spent to cast the spell that fired this trigger (Magmablood Archaic).
     *  Read via `ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL`. Null for non-cast triggers. */
    val triggerColorsSpentOnTriggeringSpell: Int? = null,
    /** Mana value of the spell that fired this trigger (Kellan, the Kid). Read via
     *  `ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE`. Null for non-cast triggers. */
    val triggerManaValueOfTriggeringSpell: Int? = null,
    /** Value chosen for {X} on the spell that fired this trigger (Geometer's Arthropod). Read via
     *  `ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL`. Null for non-cast / no-{X} triggers. */
    val triggerXValueOfTriggeringSpell: Int? = null,
    /** The trigger's own X — the value announced for an `{X}` cost on the *action that fired it*
     *  (an `{X}` cycling cost, a megamorph turn-up), as opposed to
     *  [triggerXValueOfTriggeringSpell], which is a *cast spell's* X. Read as
     *  `DynamicAmount.XValue` and by X-relative target filters (`manaValueEqualsX()`), so it must
     *  survive target selection or the ability fizzles its own legal target on resolution. */
    val xValue: Int? = null,
    /** Pipeline state carried from a `ReflexiveTriggerEffect`'s action half, preserved across target
     *  selection so the stack object built on resume carries it (CR 603.12). Null otherwise. */
    val carriedPipeline: com.wingedsheep.engine.handlers.PipelineState? = null,
    /** The objects a batch trigger captured as "the ones that caused it" (CR 603.2c), preserved
     *  across target selection so the stack object built on resume still exposes them to the
     *  payoff under `PipelineState.TRIGGER_CAPTURED_COLLECTION`. Without this a batch trigger that
     *  *also* targets — "…are put into exile, you may choose a creature card from among them.
     *  Until end of turn, **target** token you control becomes a copy of it" (Kaya, Spirits'
     *  Justice) — resolves with an empty "them". Empty for non-batch triggers. */
    val capturedEntityIds: List<EntityId> = emptyList(),
    /** The ability's intervening-"if" (CR 603.4), preserved across target selection so the stack
     *  object built on resume can re-check it as it resolves. See
     *  [com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent.interveningIf]. */
    val interveningIf: com.wingedsheep.sdk.scripting.conditions.Condition? = null
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
    /** Projected subtypes / card types the triggering permanent had as it left the battlefield
     *  (CR 603.10), preserved across target selection for the intervening-"if"'s second check. */
    val triggerLastKnownSubtypes: Set<String>? = null,
    val triggerLastKnownCardTypes: Set<String>? = null,
    val triggerLastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /** Creatures blocking/blocked by the trigger's source on leave-battlefield (CR 509 LKI, Abu Ja'far). */
    val triggerLastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    val selectedTargets: List<ChosenTarget>,
    val targetRequirements: List<TargetRequirement>,
    val totalDamage: Int,
    val lastKnownPower: Int? = null,
    val lastKnownToughness: Int? = null,
    /** The objects a batch trigger captured (CR 603.2c), carried on through this second pause so
     *  they reach the stack object alongside the distribution. Empty for non-batch triggers. */
    val capturedEntityIds: List<EntityId> = emptyList(),
    /** The ability's intervening-"if" (CR 603.4), preserved across the distribution decision so the
     *  stack object built on resume can re-check it as it resolves. */
    val interveningIf: com.wingedsheep.sdk.scripting.conditions.Condition? = null
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
 * Resume after the controller answers a [com.wingedsheep.engine.core.BatchYesNoDecision] raised on
 * behalf of a run of structurally identical optional ("you may … target …") triggers.
 *
 * The run shares one may-question; on resume the answer is fanned back out:
 *  - "yes to all" unwraps the may-gate on every trigger in [triggers] and processes them as ordinary
 *    targeted triggers (each then chooses its own target via the existing per-trigger machinery);
 *  - "no to all" drops the whole run;
 *  - a peel-off answer ("yes/no to this one") resolves [triggers].first() that way and re-runs the
 *    rest (which re-batch if still ≥ 2), so the player can change their mind partway.
 *
 * Triggers after the run are queued separately as a [PendingTriggersContinuation] beneath this frame
 * when the batch is raised, so they resume in APNAP order regardless of the answer.
 */
@Serializable
data class BatchMayTriggerContinuation(
    override val decisionId: String,
    val triggers: List<PendingTrigger>,
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
 * @property bodyCollections Pipeline collections produced by *this pass's* body, captured when the
 *   body paused for a decision. The AFTER_BODY resumer feeds these to the repeat condition as
 *   bodyOutputs — mirroring the synchronous path, where the body's own outputs (e.g. `putting`,
 *   the land put this pass) are what a WhileCondition evaluates. Kept separate from [effectContext]
 *   on purpose: [effectContext] must stay the pristine pre-loop context so the *next* iteration
 *   re-gathers fresh (a stale collection leaking forward would mask the next pass and the loop
 *   would never terminate — see RepeatWhileExecutor.executeIteration).
 */
@Serializable
data class RepeatWhileContinuation(
    override val decisionId: String,
    val body: Effect,
    val repeatCondition: com.wingedsheep.sdk.scripting.effects.RepeatCondition,
    val resolvedDeciderId: EntityId? = null,
    val sourceName: String?,
    val phase: RepeatWhilePhase,
    val effectContext: EffectContext,
    val bodyCollections: Map<String, List<EntityId>> = emptyMap()
) : ContinuationFrame

/**
 * Resume after the flipper answers "flip another coin?" during a
 * [com.wingedsheep.sdk.scripting.effects.FlipCoinsUntilLossEffect] (Fiery Gambit).
 *
 * [winsSoFar] is the whole reason this frame exists. The tally cannot ride the pipeline between
 * flips: pipeline `storedNumbers` only reach a consumer on the result that publishes them, so a
 * per-flip tally would be dropped at each pause and the card would pay out differently depending on
 * whether a prompt was raised — the "pipeline numbers lost across pause" shape. Carrying it in the
 * frame, and publishing once when the run ends, is the same idiom as
 * [PayManaCostRepeatedlyContinuation]'s count.
 *
 * @property flipperId The player flipping — resolved once, so the run stays with them.
 * @property storeWinsAs Pipeline variable the final tally is published under.
 * @property winsSoFar Flips won *before* the next flip; the answer "stop" publishes exactly this.
 * @property sourceId The spell or ability doing the flipping, for the coin-flip events' source.
 */
@Serializable
data class FlipCoinsUntilLossContinuation(
    override val decisionId: String,
    val flipperId: EntityId,
    val storeWinsAs: String,
    val winsSoFar: Int,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume a coin flip after the flipper says which of the coins to keep — the pause a
 * [com.wingedsheep.sdk.scripting.FlipAdditionalCoins] replacement (Krark's Thumb) introduces into
 * every coin-flip executor.
 *
 * One frame serves all four flip effects because the *only* thing the pause interrupts is producing
 * the results; what each effect does with them afterwards is decided from [effect] on resume. That
 * is why [effect] and [effectContext] are carried whole rather than the four executors each getting
 * a frame of their own: the sub-effect a [com.wingedsheep.sdk.scripting.effects.FlipCoinEffect]
 * runs on a win needs the original context's targets, and re-deriving them field by field is how
 * continuations lose them.
 *
 * A batch can owe several answers (one per coin whose replacement came up mixed), so resuming may
 * push this same frame again — [pending] carries how far the batch got.
 *
 * @property effect The flip effect that was executing; decides what happens once the coins settle.
 * @property effectContext The context that effect was running under, restored verbatim on resume.
 * @property pending The batch part-way through being resolved (see
 *   [com.wingedsheep.engine.handlers.effects.CoinFlipService.PendingCoinFlipChoice]).
 * @property winsSoFar Only meaningful for
 *   [com.wingedsheep.sdk.scripting.effects.FlipCoinsUntilLossEffect]: flips won before this one, so
 *   the run's tally survives the extra pause exactly as it survives the "flip again?" one.
 */
@Serializable
data class CoinFlipChoiceContinuation(
    override val decisionId: String,
    val effect: Effect,
    val effectContext: EffectContext,
    val pending: com.wingedsheep.engine.handlers.effects.CoinFlipService.PendingCoinFlipChoice,
    val winsSoFar: Int = 0
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
 * Pre-pushed before executing a `ReflexiveTriggerEffect`'s action half. Auto-resumed after the
 * action completes to emit a [com.wingedsheep.engine.core.ReflexiveAbilityTriggeredEvent] — CR
 * 603.12's "when you do" reflexive triggered ability is a genuinely separate stack object, not
 * something resolved inline, so this continuation's only job is to notice the action finished and
 * fire the event that [com.wingedsheep.engine.event.TriggerDetector] turns into a real
 * [com.wingedsheep.engine.event.PendingTrigger].
 *
 * Flow: executor pre-pushes this → executes action → on success, pops and emits inline; on pause,
 * the auto-resumer emits after the action's own decision resolves.
 *
 * @property reflexiveEffect The effect the reflexive triggered ability will run once it resolves
 * @property reflexiveTargetRequirements Target requirements for the reflexive triggered ability
 * @property effectContext The execution context from the parent ability (source/controller/pipeline)
 */
@Serializable
data class ReflexiveTriggerTargetContinuation(
    override val decisionId: String,
    val reflexiveEffect: Effect,
    val reflexiveTargetRequirements: List<TargetRequirement>,
    val effectContext: EffectContext,
    /** Optional human-readable description override, carried through to the emitted
     *  [com.wingedsheep.engine.core.ReflexiveAbilityTriggeredEvent]. */
    val descriptionOverride: String? = null
) : ContinuationFrame
