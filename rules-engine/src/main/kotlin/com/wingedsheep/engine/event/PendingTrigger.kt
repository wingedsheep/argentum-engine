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
    val controllerId: EntityId,
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
    val sagaChapterInfo: SagaChapterInfo? = null
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
