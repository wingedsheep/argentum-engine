package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId

/**
 * The characteristics a spell is announced with when they differ from the printed card — bestow's
 * Aura (CR 702.103) and prototype's cost and size (CR 718.3). One entry point for the cast rails.
 */
object CastCharacteristics {
    fun announce(state: GameState, action: CastSpell, registry: CardRegistry): GameState =
        PrototypeCasts.announce(BestowCasts.announce(state, action, registry), action, registry)

    fun definitionForCast(definition: CardDefinition?, action: CastSpell): CardDefinition? =
        PrototypeCasts.definitionForCast(BestowCasts.definitionForCast(definition, action), action)

    /** Abandons an announcement whose cast never reached the stack. */
    fun end(state: GameState, id: EntityId): GameState =
        PrototypeCasts.end(BestowCasts.end(state, id), id)
}
