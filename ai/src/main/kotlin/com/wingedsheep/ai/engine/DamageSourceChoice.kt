package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeflectDamageSourceChoiceContinuation
import com.wingedsheep.engine.core.PreventDamageFromChosenSourceContinuation
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * "A source of your choice" for a damage shield — the question a chosen-source
 * [com.wingedsheep.sdk.scripting.effects.PreventDamageEffect] asks as it resolves
 * ([PreventDamageFromChosenSourceContinuation]: Mourner's Shield, Burrenton Forge-Tender, Samite
 * Ministration, the Circles of Protection) and the reflecting form asks too
 * ([DeflectDamageSourceChoiceContinuation]: Deflecting Palm, Eye for an Eye).
 *
 * Each of them is bought against a **threat**, so the source to name is the one most likely to deal
 * damage before the shield wears off — not the card worth keeping most, which is what the generic
 * selection ranked, and that put the shield on one of our own creatures. In order:
 *
 *  1. **An opposing spell or ability on the stack**, the topmost first: it is about to resolve, and
 *     it is usually what the shield was raised in response to.
 *  2. **An opposing creature in this combat** — attacking or blocking — the hardest hitter first.
 *  3. **Any other opposing creature**, by power.
 *  4. **Anything else of theirs**, then ours last — the choice is mandatory, so with nothing of
 *     theirs eligible the least dangerous of our own sources is named.
 */
internal object DamageSourceChoice {

    fun isSourceChoice(answer: Any?): Boolean =
        answer is PreventDamageFromChosenSourceContinuation || answer is DeflectDamageSourceChoiceContinuation

    fun choose(state: GameState, decision: SelectCardsDecision, playerId: EntityId): List<EntityId>? {
        if (decision.options.isEmpty() || decision.maxSelections < 1) return null
        return listOf(decision.options.maxBy { threat(state, playerId, it) })
    }

    private fun threat(state: GameState, playerId: EntityId, source: EntityId): Int {
        val projected = state.projectedState
        val onStack = state.stack.indexOf(source)
        val controller = if (onStack >= 0) {
            state.getEntity(source)?.get<ControllerComponent>()?.playerId
        } else {
            projected.getController(source)
        }
        val power = if (projected.isCreature(source)) (projected.getPower(source) ?: 0).coerceAtLeast(0) else 0
        if (controller == playerId) return -1 - power
        if (onStack >= 0) return STACK + onStack
        val entity = state.getEntity(source)
        val inCombat = entity?.has<AttackingComponent>() == true || entity?.has<BlockingComponent>() == true
        return if (inCombat) COMBAT + power else power
    }

    private const val STACK = 100_000
    private const val COMBAT = 10_000
}
