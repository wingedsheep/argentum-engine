package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Where a "gains '<ability>'" grant may land: a permanent on the battlefield, or a **permanent
 * spell on the stack**. CR 400.7a carries an effect that changes a permanent spell's
 * characteristics onto the permanent that spell becomes — Thief of Existence's cast trigger gives
 * the creature spell a leaves-the-battlefield ability that it still has once it resolves. The
 * stack object keeps its entity id as it becomes the permanent (`PermanentEntry`), so a grant keyed
 * to that id simply survives the resolution; a stack object that ends any other way drops its
 * grants in [GameState.removeFromStack].
 *
 * Shared by the triggered, state-triggered and activated grant executors.
 */
internal object ObjectGrantTarget {

    fun canReceive(state: GameState, entityId: EntityId): Boolean {
        if (state.getBattlefield().contains(entityId)) return true
        if (entityId !in state.stack) return false
        val container = state.getEntity(entityId) ?: return false
        return container.has<SpellOnStackComponent>() &&
            container.get<CardComponent>()?.typeLine?.isPermanent == true
    }

    const val NOT_A_PERMANENT_OR_PERMANENT_SPELL = "Target is not a permanent or a permanent spell"
}
