package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerStepScope
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * A delayed triggered ability that fires at a specific step.
 *
 * Used for effects like Astral Slide that exile a creature and return it
 * "at the beginning of the next end step."
 *
 * @property id Unique identifier for this delayed trigger
 * @property effect The effect to execute when the trigger fires
 * @property fireAtStep The step at which this trigger should fire
 * @property sourceId The entity that created this delayed trigger
 * @property sourceName Human-readable name of the source
 * @property controllerId The player who controls this delayed trigger
 */
@Serializable
data class DelayedTriggeredAbility(
    val id: String,
    val effect: Effect,
    /** Step at which this step-based delayed trigger fires. Null for event-based triggers. */
    val fireAtStep: Step? = null,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    /**
     * For event-based delayed triggers: the TriggerSpec describing what event fires this.
     * When non-null, this ability is event-based and [fireAtStep] is unused.
     */
    val trigger: TriggerSpec? = null,
    /**
     * For event-based delayed triggers: the entity that scopes the trigger.
     * The trigger only fires for events whose relevant entity (e.g. damage source)
     * matches this id.
     */
    val watchedEntityId: EntityId? = null,
    /**
     * The identity of [watchedEntityId]'s object when this event-based trigger was created. "When
     * *that* creature dies" follows that object only (CR 400.7): once it changes zones it is a new
     * object, so a later departure of the same entity id does not match, and the cleanup step
     * drops the trigger once the object is no longer current. Null for player-scoped watchers and
     * for objects created without identity bookkeeping.
     */
    val watchedObject: com.wingedsheep.engine.state.ObjectRef? = null,
    /**
     * For event-based delayed triggers: scopes the trigger to events whose *recipient*
     * (the damaged/targeted entity) matches this id. Whereas [watchedEntityId] narrows by the
     * event's source, this narrows by the event's recipient — e.g. "whenever a creature you
     * control deals combat damage to *that player* this turn" (Great Train Heist). Baked from
     * [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.watchedRecipient].
     */
    val watchedRecipientId: EntityId? = null,
    /** For event-based delayed triggers: the expiry rule. */
    val expiry: DelayedTriggerExpiry? = null,
    /**
     * For [DelayedTriggerExpiry.EndOfPlayersNextTurn]: the player whose next turn ends the trigger,
     * resolved at creation, and the first turn number that may be that turn (`turnNumber + 1` at
     * creation, so the current turn never counts). The trigger is removed at the cleanup of the
     * first turn at or after [expiresAfterTurn] that [expiryPlayerId] takes — the same floor plus
     * active-player guard `Duration.EndOfYourNextTurn` uses. Null for every other expiry.
     */
    val expiryPlayerId: EntityId? = null,
    val expiresAfterTurn: Int? = null,
    /**
     * For event-based delayed triggers: when true this is a one-shot — it is removed the
     * first time it fires ("when you next … this turn"). When false it persists, firing on
     * every matching event until [expiry] removes it.
     */
    val fireOnce: Boolean = false,
    /** Whether a step-based delayed trigger remains resident after each matching step. */
    val repeatAtEachMatchingStep: Boolean = false,
    /**
     * Which step beginnings match [fireAtStep]: that step exactly, or any main phase ("your next
     * main phase"). Baked from [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.stepScope].
     */
    val stepScope: DelayedTriggerStepScope = DelayedTriggerStepScope.STEP,
    /** If set, this trigger won't fire before this turn number. Used for "your next end step" effects. */
    val notBeforeTurn: Int? = null,
    /**
     * Target requirement chosen each time this delayed trigger fires. Threaded into the
     * synthesised [com.wingedsheep.sdk.scripting.TriggeredAbility] so the player picks a
     * target per firing (e.g. Rediscover the Way chapter III). Null for non-targeting
     * delayed triggers.
     */
    val targetRequirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement? = null,
    /**
     * Further target requirements beyond [targetRequirement], for a delayed trigger that targets
     * more than once — Feral Encounter's "target creature you control deals damage equal to its
     * power to up to one target creature you don't control". Mirrors
     * [com.wingedsheep.sdk.scripting.TriggeredAbility.additionalTargetRequirements], which is where
     * these end up on the synthesised ability; the whole list is chosen fresh each firing.
     */
    val additionalTargetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement> =
        emptyList(),
    /**
     * For step-based delayed triggers: the single "whose turn" gate. If non-null, only fires
     * when this player is the active player; null means it fires on the next matching step of
     * any turn. Setting it to the source's [controllerId] expresses the common "only on the
     * controller's turn" case (e.g. "at the beginning of *your* next end step"); setting it to
     * the triggering player expresses "at the beginning of *their* next [step]" (Nafs Asp).
     *
     * Also exposed as `triggeringPlayerId` / `triggeringEntityId` on the synthesised
     * [com.wingedsheep.sdk.scripting.TriggeredAbility]'s trigger context, so
     * `Player.TriggeringPlayer` inside [effect] resolves back to this same player. Set by
     * [com.wingedsheep.engine.handlers.effects.composite.CreateDelayedTriggerExecutor] from
     * [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.fireOnPlayer].
     */
    val fireOnPlayerId: EntityId? = null,
    /**
     * The creating pipeline's collections named by
     * [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.carryCollections], frozen
     * at creation and seeded into the pipeline the delayed ability's effect resolves in (CR 603.7c:
     * "those cards" still means the objects the creating effect referred to). Each entry keeps the
     * object's identity, so one that has since changed zones — a new object, or a token that ceased
     * to exist — is dropped when the trigger fires ([carriedPipelineFor]).
     */
    val carriedCollections: Map<String, List<com.wingedsheep.engine.handlers.CapturedObjectBinding>> = emptyMap(),
    /**
     * Copiable values of departed objects the delayed effect copies, frozen when it was scheduled
     * ("when this dies, … create a token that's a copy of it at the beginning of the next end step"
     * — Ochre Jelly): a token that died has ceased to exist by the time this fires (CR 704.5d).
     * Handed to the firing trigger as [TriggerContext.capturedEntitySnapshots].
     */
    val capturedEntitySnapshots: List<com.wingedsheep.engine.state.components.stack.EntitySnapshot> = emptyList(),
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment()
)

/**
 * The pipeline a firing delayed trigger resolves in: its [DelayedTriggeredAbility.carriedCollections]
 * reduced to the objects that are still the same objects (CR 603.7c — an object that left the zone it
 * was expected in, or ceased to exist, is no longer affected). Null when nothing was carried.
 */
fun DelayedTriggeredAbility.carriedPipelineFor(state: com.wingedsheep.engine.state.GameState): com.wingedsheep.engine.handlers.PipelineState? {
    if (carriedCollections.isEmpty()) return null
    return com.wingedsheep.engine.handlers.PipelineState(
        storedCollections = carriedCollections.mapValues { (_, bindings) ->
            bindings.filter { b -> b.objectRef?.let { state.isCurrentObject(it) } == true }.map { it.entityId }
        }
    )
}
