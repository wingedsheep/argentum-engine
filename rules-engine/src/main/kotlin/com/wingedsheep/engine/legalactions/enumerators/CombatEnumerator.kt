package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

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
                // battle iff its *protector* is (CR 310.9b), which is why a Siege the attacking
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
                // CR 508.1b–c — each attacker picks its own defender, and per-defender
                // restrictions (Goblin Rock Sled's "unless defending player controls a Mountain",
                // Blazing Archon's "creatures can't attack you") rule out single defenders. Run the
                // same rules the declaration enforces, per attacker, and publish only the narrowed
                // ones. A creature left with no legal defender at all can't attack, even when no
                // single rule restricts it from every defender (two restrictions, one per seat).
                // A creature a directed requirement binds (Public Enemy) may only attack the
                // players it names, so its list narrows to them.
                val narrowedTargets = mutableMapOf<EntityId, List<EntityId>>()
                val boundDefenders = context.turnManager.boundAttackDefenders(state, playerId)
                val attackers = validAttackers.filter { attackerId ->
                    val legal = context.turnManager.legalDefendersFor(state, attackerId, playerId, validAttackTargets)
                        .let { all -> boundDefenders[attackerId]?.let { bound -> all.filter { it in bound } } ?: all }
                    if (legal.size < validAttackTargets.size) narrowedTargets[attackerId] = legal
                    legal.isNotEmpty() || validAttackTargets.isEmpty()
                }
                narrowedTargets.keys.retainAll(attackers.toSet())
                val mandatoryAttackers = context.turnManager.getMandatoryAttackers(state, playerId)
                return listOf(LegalAction(
                    actionType = "DeclareAttackers",
                    description = "Declare attackers",
                    action = DeclareAttackers(playerId, emptyMap()),
                    validAttackers = attackers,
                    mandatoryAttackers = mandatoryAttackers.ifEmpty { null },
                    validAttackTargets = validAttackTargets.ifEmpty { null },
                    attackTargetsByAttacker = narrowedTargets.ifEmpty { null },
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
                if (com.wingedsheep.engine.mechanics.combat.RandomizedBlockerPiles.isActive(state)) {
                    return listOf(LegalAction(
                        actionType = "DeclareBlockers",
                        description = "Choose blocker piles",
                        action = DeclareBlockers(playerId, emptyMap()),
                        validBlockers = emptyList(),
                    ))
                }
                val validBlockers = context.turnManager.getValidBlockers(state, playerId)
                val blockRules = com.wingedsheep.engine.mechanics.combat.BlockStaticRules(
                    state, context.cardRegistry, context.predicateEvaluator)
                val blockerMaxBlockCounts = validBlockers.mapNotNull { blocker ->
                    blockRules.maxBlocks(blocker).takeIf { it > 1 }?.let { blocker to it }
                }.toMap()
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
