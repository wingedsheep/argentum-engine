package com.wingedsheep.engine.support

import com.wingedsheep.engine.state.components.player.CreatureDeathsThisTurnComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId

/**
 * A last-known creature death as `ZoneTransitionService` records it in
 * [CreatureDeathsThisTurnComponent] — for tests that seed the turn's death history directly
 * instead of killing a creature.
 */
fun creatureDeath(name: String = "Test Creature", subtypes: Set<String> = emptySet()): EntitySnapshot =
    EntitySnapshot(
        entityId = EntityId.generate(),
        subtypes = subtypes,
        typeLine = TypeLine.creature(subtypes.mapTo(mutableSetOf()) { Subtype(it) }),
        name = name,
    )

/** Seed [player]'s death history for this turn with [deaths], replacing any recorded so far. */
fun GameTestDriver.recordCreatureDeaths(player: EntityId, vararg deaths: EntitySnapshot) =
    addComponent(player, CreatureDeathsThisTurnComponent(deaths.toList()))
