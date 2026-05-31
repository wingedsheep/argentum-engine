package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Tracks a pending "copy next spell" effect.
 *
 * When a player casts their next spell matching [spellFilter] this turn,
 * [copies] copies of that spell are created on the stack.
 * The entry is consumed (removed) after being applied.
 *
 * @property controllerId The player whose next spell will be copied
 * @property copies Number of copies to create
 * @property sourceId The entity that created this pending copy (e.g., Howl of the Horde)
 * @property sourceName Human-readable name of the source
 * @property spellFilter Which spell to copy (e.g., instant or sorcery, or creature)
 * @property persistent When true the entry is kept after copying (copies every matching
 *   spell for the rest of the turn) rather than consumed after one use.
 */
@Serializable
data class PendingSpellCopy(
    val controllerId: EntityId,
    val copies: Int,
    val sourceId: EntityId,
    val sourceName: String,
    val spellFilter: GameObjectFilter = GameObjectFilter.InstantOrSorcery,
    val persistent: Boolean = false
)
