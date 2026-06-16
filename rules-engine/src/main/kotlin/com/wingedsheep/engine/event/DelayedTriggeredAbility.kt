package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
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
     * For event-based delayed triggers: when true this is a one-shot — it is removed the
     * first time it fires ("when you next … this turn"). When false it persists, firing on
     * every matching event until [expiry] removes it.
     */
    val fireOnce: Boolean = false,
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
    val fireOnPlayerId: EntityId? = null
)
