package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Evaluates conditions against GameState.
 *
 * This provides ECS-compatible condition checking for the effect handler system.
 * It evaluates the existing Condition sealed hierarchy against the ECS game state.
 */
object ConditionEvaluator {

    /**
     * Evaluate a condition against the ECS game state.
     *
     * @param state The current ECS game state
     * @param condition The condition to evaluate
     * @param context The execution context (provides controller and source)
     * @return true if the condition is met
     */
    fun evaluate(
        state: GameState,
        condition: Condition,
        context: ExecutionContext
    ): Boolean {
        val controllerId = context.controllerId
        val sourceId = context.sourceId

        return when (condition) {
            // Life Total Conditions
            is LifeTotalAtMost -> {
                val life = state.getComponent<LifeComponent>(controllerId)?.life ?: 0
                life <= condition.threshold
            }

            is LifeTotalAtLeast -> {
                val life = state.getComponent<LifeComponent>(controllerId)?.life ?: 0
                life >= condition.threshold
            }

            is MoreLifeThanOpponent -> {
                val myLife = state.getComponent<LifeComponent>(controllerId)?.life ?: 0
                getOpponents(state, controllerId).any { opponentId ->
                    val opponentLife = state.getComponent<LifeComponent>(opponentId)?.life ?: 0
                    myLife > opponentLife
                }
            }

            is LessLifeThanOpponent -> {
                val myLife = state.getComponent<LifeComponent>(controllerId)?.life ?: 0
                getOpponents(state, controllerId).any { opponentId ->
                    val opponentLife = state.getComponent<LifeComponent>(opponentId)?.life ?: 0
                    myLife < opponentLife
                }
            }

            // Battlefield Conditions
            is ControlCreature -> {
                countCreaturesControlledBy(state, controllerId) > 0
            }

            is ControlCreaturesAtLeast -> {
                countCreaturesControlledBy(state, controllerId) >= condition.count
            }

            is ControlCreatureWithKeyword -> {
                getCreaturesControlledBy(state, controllerId).any { entityId ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    cardComponent?.definition?.keywords?.contains(condition.keyword) == true
                }
            }

            is ControlCreatureOfType -> {
                getCreaturesControlledBy(state, controllerId).any { entityId ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    cardComponent?.definition?.typeLine?.subtypes?.contains(condition.subtype) == true
                }
            }

            is ControlEnchantment -> {
                getBattlefieldEntitiesControlledBy(state, controllerId).any { entityId ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    cardComponent?.definition?.isEnchantment == true
                }
            }

            is ControlArtifact -> {
                getBattlefieldEntitiesControlledBy(state, controllerId).any { entityId ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    cardComponent?.definition?.isArtifact == true
                }
            }

            is OpponentControlsCreature -> {
                getOpponents(state, controllerId).any { opponentId ->
                    countCreaturesControlledBy(state, opponentId) > 0
                }
            }

            is OpponentControlsMoreCreatures -> {
                val myCreatures = countCreaturesControlledBy(state, controllerId)
                getOpponents(state, controllerId).any { opponentId ->
                    countCreaturesControlledBy(state, opponentId) > myCreatures
                }
            }

            is OpponentControlsMoreLands -> {
                val myLands = countLandsControlledBy(state, controllerId)
                getOpponents(state, controllerId).any { opponentId ->
                    countLandsControlledBy(state, opponentId) > myLands
                }
            }

            // Hand/Library Conditions
            is EmptyHand -> {
                val handZone = ZoneId(ZoneType.HAND, controllerId)
                state.getZone(handZone).isEmpty()
            }

            is CardsInHandAtLeast -> {
                val handZone = ZoneId(ZoneType.HAND, controllerId)
                state.getZone(handZone).size >= condition.count
            }

            is CardsInHandAtMost -> {
                val handZone = ZoneId(ZoneType.HAND, controllerId)
                state.getZone(handZone).size <= condition.count
            }

            // Graveyard Conditions
            is CreatureCardsInGraveyardAtLeast -> {
                val graveyardZone = ZoneId(ZoneType.GRAVEYARD, controllerId)
                val creatureCount = state.getZone(graveyardZone).count { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.definition?.isCreature == true
                }
                creatureCount >= condition.count
            }

            is CardsInGraveyardAtLeast -> {
                val graveyardZone = ZoneId(ZoneType.GRAVEYARD, controllerId)
                state.getZone(graveyardZone).size >= condition.count
            }

            is GraveyardContainsSubtype -> {
                val graveyardZone = ZoneId(ZoneType.GRAVEYARD, controllerId)
                state.getZone(graveyardZone).any { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.definition?.typeLine?.subtypes?.contains(condition.subtype) == true
                }
            }

            // Source Conditions
            is SourceIsAttacking -> {
                state.getEntity(sourceId)?.has<AttackingComponent>() == true
            }

            is SourceIsBlocking -> {
                state.getEntity(sourceId)?.has<BlockingComponent>() == true
            }

            is SourceIsTapped -> {
                state.getEntity(sourceId)?.has<TappedComponent>() == true
            }

            is SourceIsUntapped -> {
                state.getEntity(sourceId)?.has<TappedComponent>() != true
            }

            // Turn/Phase Conditions
            is IsYourTurn -> {
                state.activePlayerId == controllerId
            }

            is IsNotYourTurn -> {
                state.activePlayerId != controllerId
            }

            // Composite Conditions
            is AllConditions -> {
                condition.conditions.all { evaluate(state, it, context) }
            }

            is AnyCondition -> {
                condition.conditions.any { evaluate(state, it, context) }
            }

            is NotCondition -> {
                !evaluate(state, condition.condition, context)
            }
        }
    }

    // Helper functions

    private fun getOpponents(state: GameState, playerId: EntityId): List<EntityId> {
        return state.getPlayerIds().filter { it != playerId }
    }

    private fun getBattlefieldEntitiesControlledBy(state: GameState, controllerId: EntityId): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            val controller = state.getEntity(entityId)?.get<ControllerComponent>()
            controller?.controllerId == controllerId
        }
    }

    private fun getCreaturesControlledBy(state: GameState, controllerId: EntityId): List<EntityId> {
        return getBattlefieldEntitiesControlledBy(state, controllerId).filter { entityId ->
            val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
            cardComponent?.definition?.isCreature == true
        }
    }

    private fun countCreaturesControlledBy(state: GameState, controllerId: EntityId): Int {
        return getCreaturesControlledBy(state, controllerId).size
    }

    private fun countLandsControlledBy(state: GameState, controllerId: EntityId): Int {
        return getBattlefieldEntitiesControlledBy(state, controllerId).count { entityId ->
            val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
            cardComponent?.definition?.isLand == true
        }
    }
}
