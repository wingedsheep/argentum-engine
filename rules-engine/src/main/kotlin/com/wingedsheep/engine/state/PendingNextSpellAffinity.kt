package com.wingedsheep.engine.state

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "the next [spellFilter] spell you cast this turn has affinity for [forType]"
 * rider (Don & Raph, Hard Science). One-shot counterpart of [PendingNextSpellRider]: when
 * [controllerId] next casts a spell matching [spellFilter] this turn, that spell's cost is reduced
 * by the number of [forType] permanents the controller has *at cast time* (the cost calculator
 * reads this rider) and the entry is consumed.
 *
 * @property controllerId The player whose next matching spell gains affinity.
 * @property spellFilter Which spell the rider waits for (Don & Raph: noncreature spells).
 * @property forType The card type whose permanents reduce the matched spell's cost (artifacts).
 * @property sourceId The entity that created this rider.
 * @property sourceName Human-readable name of the source.
 */
@Serializable
data class PendingNextSpellAffinity(
    val controllerId: EntityId,
    val spellFilter: GameObjectFilter,
    val forType: CardType,
    val sourceId: EntityId,
    val sourceName: String
)
