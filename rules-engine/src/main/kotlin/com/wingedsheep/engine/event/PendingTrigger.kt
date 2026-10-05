package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * A triggered ability that is waiting to go on the stack.
 */
@kotlinx.serialization.Serializable
data class PendingTrigger(
    val ability: TriggeredAbility,
    val sourceId: EntityId,
    val sourceName: String,
    /** Source battlefield visit captured at detection, before target or ordering decisions. */
    val sourceBattlefieldTimestamp: Long? = null,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment =
        com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
    val controllerId: EntityId,
    /**
     * The permanent whose `GrantTriggeredAbility` static granted this triggered ability, when it is
     * a granted ability (e.g. an Equipment granting an attack trigger to the equipped creature).
     * Carried onto the stack so the resolving effect can reference the granter (CR 201.5a) via
     * [com.wingedsheep.engine.handlers.EffectContext.granterId] — e.g. Dire Blunderbuss's "sacrifice
     * an artifact other than Dire Blunderbuss". Null for the source's own printed abilities.
     */
    val granterId: EntityId? = null,
    val triggerContext: TriggerContext,
    /**
     * When set, this pending trigger came from a one-shot event-based delayed triggered
     * ability ([DelayedTriggeredAbility.fireOnce]); the delayed trigger with this id is
     * removed from game state the moment this trigger fires (goes on the stack), so a later
     * matching event the same turn won't fire it again.
     */
    val consumesDelayedTriggerId: String? = null,
    /**
     * Set on Saga chapter abilities so that, when this ability resolves, the engine can emit a
     * [com.wingedsheep.engine.core.SagaChapterResolvedEvent] (the cue for "whenever the final
     * chapter ability of a Saga you control resolves" — Tom Bombadil).
     */
    val sagaChapterInfo: SagaChapterInfo? = null,
    /**
     * Pipeline state carried from a `ReflexiveTriggerEffect`'s action half into this synthetic
     * reflexive ability, threaded onto [com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent]
     * when this pending trigger is placed on the stack. Null for ordinary triggered abilities.
     */
    val carriedPipeline: com.wingedsheep.engine.handlers.PipelineState? = null,
    /**
     * Which opponent answers this trigger's target decision, when a requirement carries
     * [com.wingedsheep.sdk.scripting.targets.TargetChooser.Opponent] ("target creature card of an
     * opponent's choice" — Mausoleum Turnkey).
     *
     * Pinned by `TriggerProcessor` once the deciding opponent is known — immediately in a
     * two-player game, and after the controller picks one in multiplayer — and read back by
     * `resolveTargetChooser`. It has to live on the trigger rather than be recomputed, because the
     * multiplayer pick is a decision of its own and the resumer re-enters target selection with
     * nothing else to carry the answer.
     *
     * Null on every other trigger, which is every trigger without an opponent chooser.
     */
    val opponentTargetChooserId: EntityId? = null,
    /** Frozen before stack-placement rewrites such as unwrapping a consent gate. */
    val rulesText: TriggeredAbility = ability
)

/**
 * Identifies a Saga chapter ability and which chapter it is, carried from trigger detection
 * through stack resolution so a [com.wingedsheep.engine.core.SagaChapterResolvedEvent] can be
 * emitted on resolution.
 */
@kotlinx.serialization.Serializable
data class SagaChapterInfo(
    val chapterNumber: Int,
    val finalChapterNumber: Int
) {
    val isFinalChapter: Boolean get() = chapterNumber >= finalChapterNumber
}
