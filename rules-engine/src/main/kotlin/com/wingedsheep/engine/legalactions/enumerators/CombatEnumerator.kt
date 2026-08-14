package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber

/**
 * Enumerates DeclareAttackers and DeclareBlockers actions.
 *
 * These are turn-based actions that happen before priority (CR 507/508).
 * When active, they are the ONLY legal actions available — no spells, abilities, or PassPriority.
 * The LegalActionEnumerator checks this via [isCombatDeclarationStep].
 */
class CombatEnumerator : ActionEnumerator {

    /**
     * Check if we're in a combat declaration step where combat is the only legal action.
     * When true, the coordinator should ONLY include combat actions.
     */
    fun isCombatDeclarationStep(context: EnumerationContext): Boolean {
        val state = context.state
        val playerId = context.playerId
        if (state.step == Step.DECLARE_ATTACKERS && state.isActiveTurnFor(playerId)) {
            val attackersAlreadyDeclared = state.getEntity(playerId)
                ?.get<AttackersDeclaredThisCombatComponent>() != null
            if (!attackersAlreadyDeclared) return true
        }
        if (state.step == Step.DECLARE_BLOCKERS && !state.isActiveTurnFor(playerId)) {
            val blockersAlreadyDeclared = state.getEntity(playerId)
                ?.get<BlockersDeclaredThisCombatComponent>() != null
            if (!blockersAlreadyDeclared &&
                com.wingedsheep.engine.mechanics.combat.CombatDefenders.isDefendingPlayer(state, playerId)
            ) {
                return true
            }
        }
        return false
    }

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId

        // Declare attackers
        if (state.step == Step.DECLARE_ATTACKERS && state.isActiveTurnFor(playerId)) {
            val attackersAlreadyDeclared = state.getEntity(playerId)
                ?.get<AttackersDeclaredThisCombatComponent>() != null
            if (!attackersAlreadyDeclared) {
                val validAttackers = context.turnManager.getValidAttackers(state, playerId)
                val projected = context.projected
                // Opponents this player may attack under the game's AttackMode (CR 802 / 803).
                // A planeswalker is attackable iff its controller is one of those opponents; a
                // battle iff its *protector* is (CR 310.8b), which is why a Siege the attacking
                // player controls themselves shows up here once an opponent protects it.
                val attackableOpponents = com.wingedsheep.engine.mechanics.combat.CombatDefenders
                    .legalDefendingPlayers(state, playerId)
                val attackablePermanents = state.getBattlefield().filter { entityId ->
                    when {
                        projected.isBattle(entityId) -> com.wingedsheep.engine.mechanics.battle.Battles
                            .canBeAttackedBy(state, entityId, playerId, attackableOpponents)
                        projected.isPlaneswalker(entityId) ->
                            projected.getController(entityId) in attackableOpponents
                        else -> false
                    }
                }
                val validAttackTargets = attackableOpponents.toList() + attackablePermanents
                val mandatoryAttackers = context.turnManager.getMandatoryAttackers(state, playerId)
                return listOf(LegalAction(
                    actionType = "DeclareAttackers",
                    description = "Declare attackers",
                    action = DeclareAttackers(playerId, emptyMap()),
                    validAttackers = validAttackers,
                    mandatoryAttackers = mandatoryAttackers.ifEmpty { null },
                    validAttackTargets = validAttackTargets.ifEmpty { null }
                ))
            }
        }

        // Declare blockers — offered only to a defending player (one being attacked).
        if (state.step == Step.DECLARE_BLOCKERS && !state.isActiveTurnFor(playerId) &&
            com.wingedsheep.engine.mechanics.combat.CombatDefenders.isDefendingPlayer(state, playerId)
        ) {
            val blockersAlreadyDeclared = state.getEntity(playerId)
                ?.get<BlockersDeclaredThisCombatComponent>() != null
            if (!blockersAlreadyDeclared) {
                val validBlockers = context.turnManager.getValidBlockers(state, playerId)
                val projected = context.projected
                val blockerMaxBlockCounts = mutableMapOf<com.wingedsheep.sdk.model.EntityId, Int>()
                for (blockerId in validBlockers) {
                    val container = state.getEntity(blockerId) ?: continue
                    val card = container.get<CardComponent>() ?: continue
                    val isFaceDown = container.has<FaceDownComponent>()
                    val canBlockAny = if (!isFaceDown) {
                        // By definition id, not name — `BlockPhaseManager.validateBlocker`, the
                        // authoritative check, resolves by id. A renamed copy (CR 707.9) of a
                        // "can block any number of creatures" permanent would otherwise be capped
                        // at one blocker in the UI while the engine allowed more.
                        val cardDef = context.cardRegistry.getCard(card.cardDefinitionId)
                        cardDef?.staticAbilities?.any { it is CanBlockAnyNumber } == true
                    } else false
                    if (canBlockAny) {
                        blockerMaxBlockCounts[blockerId] = Int.MAX_VALUE
                    } else {
                        val additionalBlocks = projected.getAdditionalBlockCount(blockerId)
                        if (additionalBlocks > 0) {
                            blockerMaxBlockCounts[blockerId] = 1 + additionalBlocks
                        }
                    }
                }
                val mandatoryAssignments = context.turnManager.getMandatoryBlockerAssignments(state, playerId)
                return listOf(LegalAction(
                    actionType = "DeclareBlockers",
                    description = "Declare blockers",
                    action = DeclareBlockers(playerId, emptyMap()),
                    validBlockers = validBlockers,
                    blockerMaxBlockCounts = blockerMaxBlockCounts.ifEmpty { null },
                    mandatoryBlockerAssignments = mandatoryAssignments.ifEmpty { null }
                ))
            }
        }

        return emptyList()
    }
}
