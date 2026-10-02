package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.BlocksCreatedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.combat.BlockingRelationships
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.BecomeBlockingEffect
import kotlin.reflect.KClass

class BecomeBlockingExecutor : EffectExecutor<BecomeBlockingEffect> {
    override val effectType: KClass<BecomeBlockingEffect> = BecomeBlockingEffect::class

    override fun execute(state: GameState, effect: BecomeBlockingEffect, context: EffectContext): EffectResult {
        val blocker = context.resolveTarget(effect.blocker, state) ?: return EffectResult.success(state)
        val attacker = context.resolveTarget(effect.attacker, state) ?: return EffectResult.success(state)
        val projected = state.projectedState
        if (blocker !in state.getBattlefield() || attacker !in state.getBattlefield() ||
            !projected.isCreature(blocker) || !projected.isCreature(attacker) || projected.isBattle(blocker) ||
            state.getEntity(blocker)?.has<AttackingComponent>() == true
        ) return EffectResult.success(state)
        val attack = state.getEntity(attacker)?.get<AttackingComponent>() ?: return EffectResult.success(state)
        val controller = projected.getController(blocker) ?: return EffectResult.success(state)
        val defendingPlayer = CombatDefenders.defendingPlayerOf(state, attack, projected)
            ?: return EffectResult.success(state)
        if (controller !in state.sharedTurnTeam(defendingPlayer)) {
            return EffectResult.success(state)
        }
        val band = attack.bandId
        val attackers = if (band == null) listOf(attacker) else state.getBattlefield().filter {
            state.getEntity(it)?.get<AttackingComponent>()?.bandId == band
        }
        val previous = state.getEntity(blocker)?.get<BlockingComponent>()
        val added = attackers.filter { it !in previous?.blockedAttackerIds.orEmpty() }
        if (added.isEmpty()) return EffectResult.success(state)
        val newlyBlocked = added.filter { state.getEntity(it)?.has<BlockedComponent>() != true }.toSet()
        val pairs = mapOf(blocker to added)
        val event = BlocksCreatedEvent(
            blockers = pairs,
            newBlockers = if (previous == null) setOf(blocker) else emptySet(),
            newlyBlockedAttackers = newlyBlocked,
            previousBlockedCounts = mapOf(blocker to previous?.blockedAttackerIds.orEmpty().size),
            blockedCounts = mapOf(blocker to previous?.blockedAttackerIds.orEmpty().size + added.size),
            blockerNames = mapOf(blocker to nameVisibleToAll(state, blocker, state.getEntity(blocker)?.get<CardComponent>()?.name ?: "Creature")),
            attackerNames = added.associateWith { nameVisibleToAll(state, it, state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature") },
        )
        return EffectResult.success(BlockingRelationships.establish(state, pairs), listOf(event))
    }
}
