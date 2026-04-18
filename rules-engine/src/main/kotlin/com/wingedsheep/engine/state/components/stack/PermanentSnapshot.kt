package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Frozen projected characteristics of a permanent at a specific moment — typically
 * captured just before the permanent leaves the battlefield (Rule 112.7a / 608.2h
 * "as it last existed on the battlefield").
 *
 * Used so spells/abilities that reference a sacrificed permanent's characteristics
 * (e.g. Heart-Piercer Manticore power, "for each Goblin sacrificed") can read the
 * correct values after the permanent has already changed zones.
 */
@Serializable
data class PermanentSnapshot(
    val entityId: EntityId,
    val power: Int? = null,
    val toughness: Int? = null,
    val subtypes: Set<String> = emptySet(),
)

/**
 * Capture projected snapshots for a list of permanents in order. Caller must invoke
 * this BEFORE any zone change so projected values still resolve.
 */
fun capturePermanentSnapshots(
    ids: List<EntityId>,
    projected: ProjectedState,
): List<PermanentSnapshot> = ids.map { id ->
    PermanentSnapshot(
        entityId = id,
        power = projected.getPower(id),
        toughness = projected.getToughness(id),
        subtypes = projected.getSubtypes(id),
    )
}

fun List<PermanentSnapshot>.snapshotFor(id: EntityId): PermanentSnapshot? =
    firstOrNull { it.entityId == id }

val List<PermanentSnapshot>.entityIds: List<EntityId>
    get() = map { it.entityId }
