package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.OpponentsMustAttackYou
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * The player-level attack requirement of [OpponentsMustAttackYou] (Trove of Temptation): "each
 * opponent must attack you or a planeswalker you control with at least one creature each combat
 * if able" (CR 508.1d).
 *
 * Shared by the declare-attackers validator and the AI, which has to build a declaration that
 * obeys it: the requirement names no creature, so it can't travel as a `mandatoryAttackers` entry.
 */
object OpponentsMustAttackYouRequirement {

    /**
     * The players [attackingPlayer] is required to attack this combat: every player they may
     * legally attack (CR 802 / 803 attack modes included) who controls a permanent with an active
     * [OpponentsMustAttackYou]. A face-down permanent or one that lost all abilities imposes
     * nothing (CR 708.2 / 613.1f).
     */
    fun requiringPlayers(
        state: GameState,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        attackingPlayer: EntityId,
    ): List<EntityId> {
        val projected = state.projectedState
        return CombatDefenders.legalDefendingPlayers(state, attackingPlayer).filter { player ->
            projected.getBattlefieldControlledBy(player).any { permId ->
                val container = state.getEntity(permId) ?: return@any false
                if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(permId)) return@any false
                val cardDef = container.get<CardComponent>()
                    ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return@any false
                RoomFaceStatics.activeStaticAbilities(container, cardDef).any {
                    imposes(state, predicateEvaluator, it, permId, player)
                }
            }
        }
    }

    /**
     * Whether an attack aimed at [defenderId] satisfies [player]'s requirement: it attacks that
     * player or a planeswalker they control. A battle never does — the ability doesn't name one.
     */
    fun isAttackOn(state: GameState, projected: ProjectedState, defenderId: EntityId, player: EntityId): Boolean {
        if (state.getEntity(defenderId)?.has<LifeTotalComponent>() == true) return defenderId == player
        return projected.isPlaneswalker(defenderId) && projected.getController(defenderId) == player
    }

    /** [player] and every planeswalker they control — the defenders that satisfy their requirement. */
    fun defendersOf(state: GameState, projected: ProjectedState, player: EntityId): List<EntityId> =
        listOf(player) + projected.getBattlefieldControlledBy(player).filter { projected.isPlaneswalker(it) }
            .filter { it in state.getBattlefield() }

    private fun imposes(
        state: GameState,
        predicateEvaluator: PredicateEvaluator,
        ability: StaticAbility,
        sourceId: EntityId,
        controllerId: EntityId,
    ): Boolean = when (ability) {
        is OpponentsMustAttackYou -> true
        is ConditionalStaticAbility ->
            predicateEvaluator.conditions.evaluate(
                state, ability.condition, EffectContext(sourceId = sourceId, controllerId = controllerId)
            ) && imposes(state, predicateEvaluator, ability.ability, sourceId, controllerId)
        is CompositeStaticAbility ->
            ability.abilities.any { imposes(state, predicateEvaluator, it, sourceId, controllerId) }
        else -> false
    }
}
