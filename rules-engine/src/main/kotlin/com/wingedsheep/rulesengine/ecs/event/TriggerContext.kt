package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import kotlinx.serialization.Serializable

/**
 * Context information about what caused a trigger to fire.
 * This information may be needed when the triggered ability resolves.
 *
 * ECS version using EntityId instead of CardId/PlayerId.
 */
@Serializable
sealed interface TriggerContext {

    /**
     * No additional context needed.
     */
    @Serializable
    data object None : TriggerContext

    /**
     * Context for zone change triggers (ETB, LTB, death).
     */
    @Serializable
    data class ZoneChange(
        val entityId: EntityId,
        val cardName: String,
        val fromZone: ZoneId?,
        val toZone: ZoneId
    ) : TriggerContext

    /**
     * Context for damage triggers.
     */
    @Serializable
    data class DamageDealt(
        val sourceId: EntityId?,
        val targetId: EntityId,
        val amount: Int,
        val isPlayer: Boolean,
        val isCombatDamage: Boolean
    ) : TriggerContext

    /**
     * Context for phase/step triggers.
     */
    @Serializable
    data class PhaseStep(
        val phase: String,
        val step: String,
        val activePlayerId: EntityId
    ) : TriggerContext

    /**
     * Context for spell cast triggers.
     */
    @Serializable
    data class SpellCast(
        val spellId: EntityId,
        val spellName: String,
        val casterId: EntityId
    ) : TriggerContext

    /**
     * Context for card draw triggers.
     */
    @Serializable
    data class CardDrawn(
        val playerId: EntityId,
        val cardId: EntityId
    ) : TriggerContext

    /**
     * Context for attack/block triggers.
     */
    @Serializable
    data class Combat(
        val attackerId: EntityId?,
        val blockerId: EntityId?,
        val defendingPlayerId: EntityId?
    ) : TriggerContext

    companion object {
        /**
         * Create context from a game event.
         */
        fun fromEvent(event: GameEvent): TriggerContext {
            return when (event) {
                is GameEvent.EnteredBattlefield -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = event.fromZone,
                    toZone = ZoneId.BATTLEFIELD
                )

                is GameEvent.LeftBattlefield -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = ZoneId.BATTLEFIELD,
                    toZone = event.toZone
                )

                is GameEvent.CreatureDied -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = ZoneId.BATTLEFIELD,
                    toZone = ZoneId.graveyard(event.ownerId)
                )

                is GameEvent.CardExiled -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = event.fromZone,
                    toZone = ZoneId.EXILE
                )

                is GameEvent.ReturnedToHand -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = null,
                    toZone = ZoneId.hand(EntityId.of("unknown")) // Owner not available
                )

                is GameEvent.CardDrawn -> CardDrawn(
                    playerId = event.playerId,
                    cardId = event.cardId
                )

                is GameEvent.CardDiscarded -> ZoneChange(
                    entityId = event.cardId,
                    cardName = event.cardName,
                    fromZone = ZoneId.hand(event.playerId),
                    toZone = ZoneId.graveyard(event.playerId)
                )

                is GameEvent.DamageDealtToPlayer -> DamageDealt(
                    sourceId = event.sourceId,
                    targetId = event.targetPlayerId,
                    amount = event.amount,
                    isPlayer = true,
                    isCombatDamage = event.isCombatDamage
                )

                is GameEvent.DamageDealtToCreature -> DamageDealt(
                    sourceId = event.sourceId,
                    targetId = event.targetCreatureId,
                    amount = event.amount,
                    isPlayer = false,
                    isCombatDamage = event.isCombatDamage
                )

                is GameEvent.SpellCast -> SpellCast(
                    spellId = event.spellId,
                    spellName = event.spellName,
                    casterId = event.casterId
                )

                is GameEvent.AttackerDeclared -> Combat(
                    attackerId = event.creatureId,
                    blockerId = null,
                    defendingPlayerId = event.defendingPlayerId
                )

                is GameEvent.BlockerDeclared -> Combat(
                    attackerId = event.attackerId,
                    blockerId = event.blockerId,
                    defendingPlayerId = null
                )

                is GameEvent.CombatBegan -> PhaseStep(
                    phase = "Combat",
                    step = "BeginCombat",
                    activePlayerId = event.attackingPlayerId
                )

                is GameEvent.UpkeepBegan -> PhaseStep(
                    phase = "Beginning",
                    step = "Upkeep",
                    activePlayerId = event.activePlayerId
                )

                is GameEvent.EndStepBegan -> PhaseStep(
                    phase = "Ending",
                    step = "End",
                    activePlayerId = event.activePlayerId
                )

                // Events without specific trigger context
                is GameEvent.CombatEnded,
                is GameEvent.LifeGained,
                is GameEvent.LifeLost,
                is GameEvent.PermanentTapped,
                is GameEvent.PermanentUntapped,
                is GameEvent.CountersAdded,
                is GameEvent.CountersRemoved,
                is GameEvent.PlayerLost,
                is GameEvent.GameEnded -> None
            }
        }
    }
}
