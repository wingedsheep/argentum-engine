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
sealed interface EcsTriggerContext {

    /**
     * No additional context needed.
     */
    @Serializable
    data object None : EcsTriggerContext

    /**
     * Context for zone change triggers (ETB, LTB, death).
     */
    @Serializable
    data class ZoneChange(
        val entityId: EntityId,
        val cardName: String,
        val fromZone: ZoneId?,
        val toZone: ZoneId
    ) : EcsTriggerContext

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
    ) : EcsTriggerContext

    /**
     * Context for phase/step triggers.
     */
    @Serializable
    data class PhaseStep(
        val phase: String,
        val step: String,
        val activePlayerId: EntityId
    ) : EcsTriggerContext

    /**
     * Context for spell cast triggers.
     */
    @Serializable
    data class SpellCast(
        val spellId: EntityId,
        val spellName: String,
        val casterId: EntityId
    ) : EcsTriggerContext

    /**
     * Context for card draw triggers.
     */
    @Serializable
    data class CardDrawn(
        val playerId: EntityId,
        val cardId: EntityId
    ) : EcsTriggerContext

    /**
     * Context for attack/block triggers.
     */
    @Serializable
    data class Combat(
        val attackerId: EntityId?,
        val blockerId: EntityId?,
        val defendingPlayerId: EntityId?
    ) : EcsTriggerContext

    companion object {
        /**
         * Create context from a game event.
         */
        fun fromEvent(event: EcsGameEvent): EcsTriggerContext {
            return when (event) {
                is EcsGameEvent.EnteredBattlefield -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = event.fromZone,
                    toZone = ZoneId.BATTLEFIELD
                )

                is EcsGameEvent.LeftBattlefield -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = ZoneId.BATTLEFIELD,
                    toZone = event.toZone
                )

                is EcsGameEvent.CreatureDied -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = ZoneId.BATTLEFIELD,
                    toZone = ZoneId.graveyard(event.ownerId)
                )

                is EcsGameEvent.CardExiled -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = event.fromZone,
                    toZone = ZoneId.EXILE
                )

                is EcsGameEvent.ReturnedToHand -> ZoneChange(
                    entityId = event.entityId,
                    cardName = event.cardName,
                    fromZone = null,
                    toZone = ZoneId.hand(EntityId.of("unknown")) // Owner not available
                )

                is EcsGameEvent.CardDrawn -> CardDrawn(
                    playerId = event.playerId,
                    cardId = event.cardId
                )

                is EcsGameEvent.CardDiscarded -> ZoneChange(
                    entityId = event.cardId,
                    cardName = event.cardName,
                    fromZone = ZoneId.hand(event.playerId),
                    toZone = ZoneId.graveyard(event.playerId)
                )

                is EcsGameEvent.DamageDealtToPlayer -> DamageDealt(
                    sourceId = event.sourceId,
                    targetId = event.targetPlayerId,
                    amount = event.amount,
                    isPlayer = true,
                    isCombatDamage = event.isCombatDamage
                )

                is EcsGameEvent.DamageDealtToCreature -> DamageDealt(
                    sourceId = event.sourceId,
                    targetId = event.targetCreatureId,
                    amount = event.amount,
                    isPlayer = false,
                    isCombatDamage = event.isCombatDamage
                )

                is EcsGameEvent.SpellCast -> SpellCast(
                    spellId = event.spellId,
                    spellName = event.spellName,
                    casterId = event.casterId
                )

                is EcsGameEvent.AttackerDeclared -> Combat(
                    attackerId = event.creatureId,
                    blockerId = null,
                    defendingPlayerId = event.defendingPlayerId
                )

                is EcsGameEvent.BlockerDeclared -> Combat(
                    attackerId = event.attackerId,
                    blockerId = event.blockerId,
                    defendingPlayerId = null
                )

                is EcsGameEvent.CombatBegan -> PhaseStep(
                    phase = "Combat",
                    step = "BeginCombat",
                    activePlayerId = event.attackingPlayerId
                )

                is EcsGameEvent.UpkeepBegan -> PhaseStep(
                    phase = "Beginning",
                    step = "Upkeep",
                    activePlayerId = event.activePlayerId
                )

                is EcsGameEvent.EndStepBegan -> PhaseStep(
                    phase = "Ending",
                    step = "End",
                    activePlayerId = event.activePlayerId
                )

                // Events without specific trigger context
                is EcsGameEvent.CombatEnded,
                is EcsGameEvent.LifeGained,
                is EcsGameEvent.LifeLost,
                is EcsGameEvent.PermanentTapped,
                is EcsGameEvent.PermanentUntapped,
                is EcsGameEvent.CountersAdded,
                is EcsGameEvent.CountersRemoved,
                is EcsGameEvent.PlayerLost,
                is EcsGameEvent.GameEnded -> None
            }
        }
    }
}
