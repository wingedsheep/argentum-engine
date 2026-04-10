package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.player.WasDealtCombatDamageThisTurnComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked
import com.wingedsheep.sdk.scripting.DivideCombatDamageFreely
import java.util.UUID

/**
 * Handles combat damage using a three-phase pipeline:
 *
 * 1. **Propose** — Generate [CombatDamageAssignment]s from attackers/blockers
 * 2. **Modify** — Transform assignments via [CombatDamageModifier] plugins
 *    (prevention, protection, redirection)
 * 3. **Apply** — Deal damage, handling amplification, shields, lifelink
 *
 * Also handles pre-damage decisions:
 * - DivideCombatDamageFreely (Butcher Orgg) damage distribution
 * - Manual damage assignment (trample, multiple blockers)
 * - Damage prevention choice (CR 615.7)
 */
internal class CombatDamageManager(
    private val cardRegistry: CardRegistry,
    private val damageCalculator: DamageCalculator,
) {

    private val damageModifiers: List<CombatDamageModifier> = listOf(
        PreventAllCombatDamageModifier(),
        PreventAllDamageFromSourceModifier(),
        PreventCombatDamageToAndByModifier(),
        PreventCombatDamageFromGroupModifier(),
        PreventDamageFromAttackingCreaturesModifier(),
        ProtectionModifier(),
        RedirectToControllerModifier()
    )

    /**
     * Calculate and apply combat damage.
     *
     * @param firstStrike If true, only creatures with first strike/double strike deal damage
     */
    fun applyCombatDamage(state: GameState, firstStrike: Boolean = false): ExecutionResult {
        if (isAllCombatDamagePrevented(state)) {
            return ExecutionResult.success(state)
        }

        val projected = state.projectedState
        val attackers = state.findEntitiesWith<AttackingComponent>()

        // Pre-check: if any blocked attacker has AssignCombatDamageAsUnblocked, ask the
        // controller whether to assign damage to the defending player instead of blockers.
        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in state.getBattlefield()) continue
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue

            // Only relevant when blocked
            val blockedBy = attackerContainer.get<BlockedComponent>() ?: continue
            if (blockedBy.blockerIds.isEmpty()) continue
            val liveBlockers = blockedBy.blockerIds.filter { it in state.getBattlefield() }
            if (liveBlockers.isEmpty()) continue

            // Already has a manual assignment (decision already made)
            if (attackerContainer.get<DamageAssignmentComponent>() != null) continue

            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId) ?: continue
            val hasAssignAsUnblocked = cardDef.staticAbilities.any { it is AssignCombatDamageAsUnblocked }
            if (!hasAssignAsUnblocked) continue

            if (!dealsDamageThisStep(projected, attackerId, firstStrike)) continue

            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower <= 0) continue

            val attackingPlayer = projected.getController(attackerId) ?: continue
            val decisionId = UUID.randomUUID().toString()

            val decision = YesNoDecision(
                id = decisionId,
                playerId = attackingPlayer,
                prompt = "Assign ${attackerCard.name}'s combat damage as though it weren't blocked?",
                context = DecisionContext(
                    sourceId = attackerId,
                    sourceName = attackerCard.name,
                    phase = DecisionPhase.COMBAT
                ),
                yesText = "Assign to player",
                noText = "Assign to blockers"
            )

            val continuation = AssignAsUnblockedContinuation(
                decisionId = decisionId,
                attackerId = attackerId,
                defendingPlayerId = attackingComponent.defenderId,
                firstStrike = firstStrike
            )

            val pausedState = state
                .withPendingDecision(decision)
                .pushContinuation(continuation)

            return ExecutionResult.paused(pausedState, decision)
        }

        // Pre-check: if any attacker with DivideCombatDamageFreely needs a distribution
        // decision, pause before processing ANY damage.
        for ((attackerId, attackingComponent) in attackers) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId) ?: continue
            val hasDivideDamageFreely = cardDef.staticAbilities.any { it is DivideCombatDamageFreely }
            if (!hasDivideDamageFreely) continue

            val hasFirstStrike = projected.hasKeyword(attackerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(attackerId, Keyword.DOUBLE_STRIKE)
            val attackerDealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }
            if (!attackerDealsDamageThisStep) continue

            val allDamagePrevented = DamageUtils.isAllDamageFromSourcePrevented(state, attackerId)
            val groupPrevented = isCombatDamagePreventedByGroupFilter(state, attackerId, projected)
            val toAndByPrevented = isCombatDamageToAndByPrevented(state, attackerId)
            if (allDamagePrevented || groupPrevented || toAndByPrevented) continue

            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower <= 0) continue

            if (attackerContainer.get<DamageAssignmentComponent>() != null) continue

            val blockedBy = attackerContainer.get<BlockedComponent>()
            val defenderId = attackingComponent.defenderId

            val targets = mutableListOf<EntityId>()
            if (blockedBy != null && blockedBy.blockerIds.isNotEmpty()) {
                val blockersOnBattlefield = blockedBy.blockerIds.filter { it in state.getBattlefield() }
                if (blockersOnBattlefield.isEmpty()) {
                    continue
                }
                targets.addAll(blockersOnBattlefield)
            } else if (blockedBy == null) {
                val defendingCreatures = state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    projected.getController(entityId) == defenderId &&
                        container.get<CardComponent>()?.typeLine?.isCreature == true
                }
                targets.addAll(defendingCreatures)
            }
            targets.add(defenderId)

            if (targets.size <= 1) continue

            val decisionId = UUID.randomUUID().toString()
            val attackingPlayer = projected.getController(attackerId) ?: continue

            val decision = DistributeDecision(
                id = decisionId,
                playerId = attackingPlayer,
                prompt = "Divide ${attackerCard.name}'s $attackerPower combat damage among targets",
                context = DecisionContext(
                    sourceId = attackerId,
                    sourceName = attackerCard.name,
                    phase = DecisionPhase.COMBAT
                ),
                totalAmount = attackerPower,
                targets = targets,
                minPerTarget = 0
            )

            val continuation = DamageAssignmentContinuation(
                decisionId = decisionId,
                attackerId = attackerId,
                defendingPlayerId = defenderId,
                firstStrike = firstStrike
            )

            val pausedState = state
                .withPendingDecision(decision)
                .pushContinuation(continuation)

            return ExecutionResult.paused(pausedState, decision)
        }

        // Pre-check: if any regular attacker needs manual damage assignment, pause before
        // processing ANY damage.
        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in state.getBattlefield()) continue

            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue

            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId)
            val hasDivideDamageFreely = cardDef?.staticAbilities?.any { it is DivideCombatDamageFreely } == true
            if (hasDivideDamageFreely) continue

            if (attackerContainer.get<DamageAssignmentComponent>() != null) continue

            val hasFirstStrike = projected.hasKeyword(attackerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(attackerId, Keyword.DOUBLE_STRIKE)
            val attackerDealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }
            if (!attackerDealsDamageThisStep) continue

            val blockedBy = attackerContainer.get<BlockedComponent>()
            if (blockedBy == null || blockedBy.blockerIds.isEmpty()) continue

            if (!damageCalculator.requiresManualAssignment(state, attackerId)) continue

            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower <= 0) continue

            val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
                ?: blockedBy.blockerIds

            val liveBlockers = orderedBlockers.filter { it in state.getBattlefield() }
            if (liveBlockers.isEmpty()) continue

            val hasTrample = projected.hasKeyword(attackerId, Keyword.TRAMPLE)
            val hasDeathtouch = projected.hasKeyword(attackerId, Keyword.DEATHTOUCH)
            val defenderId = attackingComponent.defenderId
            val attackingPlayer = projected.getController(attackerId) ?: continue

            val minimumAssignments = damageCalculator.getMinimumAssignments(state, attackerId)
            val autoDistribution = damageCalculator.calculateAutoDamageDistribution(state, attackerId)

            val decisionId = UUID.randomUUID().toString()
            val decision = AssignDamageDecision(
                id = decisionId,
                playerId = attackingPlayer,
                prompt = "Assign ${attackerCard.name}'s $attackerPower combat damage to blockers" +
                    if (hasTrample) " (trample)" else "",
                context = DecisionContext(
                    sourceId = attackerId,
                    sourceName = attackerCard.name,
                    phase = DecisionPhase.COMBAT
                ),
                attackerId = attackerId,
                availablePower = attackerPower,
                orderedTargets = liveBlockers,
                defenderId = if (hasTrample) defenderId else null,
                minimumAssignments = minimumAssignments,
                defaultAssignments = autoDistribution.assignments,
                hasTrample = hasTrample,
                hasDeathtouch = hasDeathtouch
            )

            val continuation = DamageAssignmentContinuation(
                decisionId = decisionId,
                attackerId = attackerId,
                defendingPlayerId = defenderId,
                firstStrike = firstStrike
            )

            val pausedState = state
                .withPendingDecision(decision)
                .pushContinuation(continuation)

            return ExecutionResult.paused(pausedState, decision)
        }

        // Pre-check: damage prevention choice (CR 615.7)
        val preventionResult = checkDamagePreventionChoice(state, attackers, projected, firstStrike)
        if (preventionResult != null) return preventionResult

        // =====================================================================
        // DAMAGE PIPELINE
        // =====================================================================

        // Phase 1: Propose
        val proposedAssignments = proposeDamageAssignments(state, projected, firstStrike)

        // Phase 2: Modify
        var finalAssignments = proposedAssignments
        for (modifier in damageModifiers) {
            finalAssignments = modifier.modify(state, projected, finalAssignments)
        }

        // Phase 3: Apply
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (assignment in finalAssignments) {
            newState = applySingleAssignment(newState, assignment, events)
        }

        // Consume one-shot redirect effects for creatures that dealt damage
        newState = consumeUsedRedirectEffects(newState, finalAssignments)

        // Lifelink
        val lifelinkResult = applyLifelinkFromDamageEvents(newState, events, projected)
        newState = lifelinkResult.first
        events.addAll(lifelinkResult.second)

        // Check for lethal damage
        val (postDamageState, deathEvents) = checkLethalDamage(newState)
        newState = postDamageState
        events.addAll(deathEvents)

        return ExecutionResult.success(newState, events)
    }

    // =========================================================================
    // Phase 1: Propose Damage Assignments
    // =========================================================================

    private fun proposeDamageAssignments(
        state: GameState,
        projected: ProjectedState,
        firstStrike: Boolean
    ): List<CombatDamageAssignment> {
        val assignments = mutableListOf<CombatDamageAssignment>()
        val attackers = state.findEntitiesWith<AttackingComponent>()

        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in state.getBattlefield()) continue
            val attackerContainer = state.getEntity(attackerId) ?: continue
            attackerContainer.get<CardComponent>() ?: continue

            val blockedBy = attackerContainer.get<BlockedComponent>()
            val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
                ?: blockedBy?.blockerIds
                ?: emptyList()

            // Attacker damage
            if (dealsDamageThisStep(projected, attackerId, firstStrike)) {
                val power = projected.getPower(attackerId) ?: 0
                if (power > 0) {
                    val manualAssignment = attackerContainer.get<DamageAssignmentComponent>()
                    when {
                        manualAssignment != null && manualAssignment.assignments.isNotEmpty() -> {
                            for ((targetId, damage) in manualAssignment.assignments) {
                                if (damage > 0) {
                                    assignments.add(CombatDamageAssignment(attackerId, targetId, damage))
                                }
                            }
                        }
                        blockedBy == null -> {
                            assignments.add(CombatDamageAssignment(attackerId, attackingComponent.defenderId, power))
                        }
                        else -> {
                            val liveBlockers = blockedBy.blockerIds.filter { it in state.getBattlefield() }
                            if (liveBlockers.isNotEmpty()) {
                                val autoDist = damageCalculator.calculateAutoDamageDistribution(state, attackerId)
                                for ((targetId, damage) in autoDist.assignments) {
                                    if (damage > 0) {
                                        assignments.add(CombatDamageAssignment(attackerId, targetId, damage))
                                    }
                                }
                            } else if (projected.hasKeyword(attackerId, Keyword.TRAMPLE)) {
                                // CR 702.19c: Blocked attacker with trample and no remaining blockers
                                // assigns all damage to the defending player/planeswalker.
                                assignments.add(CombatDamageAssignment(attackerId, attackingComponent.defenderId, power))
                            }
                        }
                    }
                }
            }
        }

        // Blocker counterattack damage — each blocker divides its damage among attackers it blocks.
        // Per CR 510.1c, lethal damage checks must account for damage being assigned by other
        // creatures in the same combat damage step, so we track pending damage across all blockers.
        val pendingDamage = mutableMapOf<EntityId, Int>()
        val processedBlockers = mutableSetOf<EntityId>()
        for ((attackerId, _) in attackers) {
            if (attackerId !in state.getBattlefield()) continue
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val blockedBy = attackerContainer.get<BlockedComponent>()
            val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
                ?: blockedBy?.blockerIds
                ?: emptyList()

            for (blockerId in orderedBlockers) {
                if (blockerId in processedBlockers) continue
                if (blockerId !in state.getBattlefield()) continue
                val blockerContainer = state.getEntity(blockerId) ?: continue
                blockerContainer.get<CardComponent>() ?: continue
                if (!dealsDamageThisStep(projected, blockerId, firstStrike)) continue
                val blockerPower = projected.getPower(blockerId) ?: 0
                if (blockerPower <= 0) continue

                processedBlockers.add(blockerId)

                val blockingComponent = blockerContainer.get<BlockingComponent>()
                val blockedAttackerIds = blockingComponent?.blockedAttackerIds ?: listOf(attackerId)

                if (blockedAttackerIds.size <= 1) {
                    // Blocking a single attacker — deal full damage
                    val targetId = blockedAttackerIds.firstOrNull() ?: attackerId
                    if (targetId in state.getBattlefield()) {
                        assignments.add(CombatDamageAssignment(blockerId, targetId, blockerPower))
                        pendingDamage[targetId] = (pendingDamage[targetId] ?: 0) + blockerPower
                    }
                } else {
                    // Blocking multiple attackers — divide damage among them in order
                    val autoDist = damageCalculator.calculateBlockerDamageDistribution(
                        state, blockerId, pendingDamage
                    )
                    for ((targetId, damage) in autoDist.assignments) {
                        if (damage > 0) {
                            assignments.add(CombatDamageAssignment(blockerId, targetId, damage))
                            pendingDamage[targetId] = (pendingDamage[targetId] ?: 0) + damage
                        }
                    }
                }
            }
        }

        return assignments
    }

    private fun dealsDamageThisStep(projected: ProjectedState, creatureId: EntityId, firstStrike: Boolean): Boolean {
        val hasFirstStrike = projected.hasKeyword(creatureId, Keyword.FIRST_STRIKE)
        val hasDoubleStrike = projected.hasKeyword(creatureId, Keyword.DOUBLE_STRIKE)
        return if (firstStrike) {
            hasFirstStrike || hasDoubleStrike
        } else {
            !hasFirstStrike || hasDoubleStrike
        }
    }

    // =========================================================================
    // Phase 3: Apply Damage Assignments
    // =========================================================================

    private fun applySingleAssignment(
        state: GameState,
        assignment: CombatDamageAssignment,
        events: MutableList<GameEvent>
    ): GameState {
        if (assignment.amount <= 0) return state

        val targetContainer = state.getEntity(assignment.targetId) ?: return state
        val isPlayer = targetContainer.get<LifeTotalComponent>() != null &&
                       targetContainer.get<CardComponent>() == null
        val projected = state.projectedState
        val isPlaneswalker = !isPlayer && projected.isPlaneswalker(assignment.targetId)

        val amplifiedAmount = DamageUtils.applyStaticDamageAmplification(
            state, assignment.targetId, assignment.amount, assignment.sourceId, isCombatDamage = true
        )

        return when {
            isPlayer -> applyDamageToPlayer(state, assignment.sourceId, assignment.targetId, amplifiedAmount, assignment.amount, events)
            isPlaneswalker -> applyDamageToPlaneswalker(state, assignment.sourceId, assignment.targetId, amplifiedAmount, events)
            else -> applyDamageToCreature(state, assignment.sourceId, assignment.targetId, amplifiedAmount, events)
        }
    }

    private fun applyDamageToPlayer(
        state: GameState,
        sourceId: EntityId,
        targetId: EntityId,
        amplifiedAmount: Int,
        originalAmount: Int,
        events: MutableList<GameEvent>
    ): GameState {
        var newState = state

        // Replace with counters (Force Bubble)
        val counterResult = DamageUtils.applyReplaceDamageWithCounters(newState, targetId, amplifiedAmount, sourceId)
        if (counterResult != null) {
            newState = counterResult.state
            events.addAll(counterResult.events)
            return newState
        }

        // Deflection shields (Deflecting Palm) — prevent + deal back to source's controller
        val deflectResult = DamageUtils.checkDeflectDamageShield(newState, targetId, amplifiedAmount, sourceId)
        if (deflectResult != null) {
            newState = deflectResult.state
            events.addAll(deflectResult.events)
            return newState
        }

        // Prevention shields
        val (shieldState, effectiveAmount) = DamageUtils.applyDamagePreventionShields(
            newState, targetId, amplifiedAmount, isCombatDamage = true, sourceId = sourceId
        )
        newState = shieldState
        if (effectiveAmount <= 0) return newState

        // Damage redirection (Glarecaster, Zealous Inquisitor)
        val (redirectState, redirectTargetId, redirectAmount) = DamageUtils.checkDamageRedirection(
            newState, targetId, effectiveAmount
        )
        newState = redirectState
        if (redirectTargetId != null && redirectAmount > 0) {
            newState = dealFinalDamage(newState, sourceId, redirectTargetId, redirectAmount, events)
            val remaining = effectiveAmount - redirectAmount
            if (remaining > 0) {
                newState = dealFinalDamage(newState, sourceId, targetId, remaining, events)
            }
            return newState
        }

        // Reduce life
        val currentLife = newState.getEntity(targetId)?.get<LifeTotalComponent>()?.life ?: return newState
        val newLife = currentLife - effectiveAmount
        newState = newState.updateEntity(targetId) { container ->
            container.with(LifeTotalComponent(newLife))
        }
        newState = DamageUtils.trackDamageReceivedByPlayer(newState, targetId, effectiveAmount)

        // Track combat damage: source dealt damage + dealt combat damage to player
        if (sourceId in newState.getBattlefield()) {
            newState = newState.updateEntity(sourceId) { container ->
                container.with(HasDealtDamageComponent).with(HasDealtCombatDamageToPlayerComponent)
            }
        }
        // Track that player was dealt combat damage this turn
        newState = newState.updateEntity(targetId) { container ->
            container.with(WasDealtCombatDamageThisTurnComponent)
        }

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
        events.add(DamageDealtEvent(sourceId, targetId, effectiveAmount, true,
            sourceName = sourceName, targetName = "Player", targetIsPlayer = true))
        events.add(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.DAMAGE))

        // Reflection (Harsh Justice)
        newState = applyDamageReflection(newState, sourceId, targetId, originalAmount, events)

        return newState
    }

    /**
     * Apply combat damage to a planeswalker by removing loyalty counters (Rule 119.3a).
     * SBA will handle putting it into graveyard if loyalty reaches 0.
     */
    private fun applyDamageToPlaneswalker(
        state: GameState,
        sourceId: EntityId,
        targetId: EntityId,
        amplifiedAmount: Int,
        events: MutableList<GameEvent>
    ): GameState {
        if (targetId !in state.getBattlefield()) return state
        if (amplifiedAmount <= 0) return state
        var newState = state

        // Prevention shields
        val (shieldState, effectiveAmount) = DamageUtils.applyDamagePreventionShields(
            newState, targetId, amplifiedAmount, isCombatDamage = true, sourceId = sourceId
        )
        newState = shieldState
        if (effectiveAmount <= 0) return newState

        // Remove loyalty counters equal to damage dealt
        val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
        val currentLoyalty = counters.getCount(com.wingedsheep.sdk.core.CounterType.LOYALTY)
        newState = newState.updateEntity(targetId) { container ->
            container.with(counters.withRemoved(com.wingedsheep.sdk.core.CounterType.LOYALTY, effectiveAmount))
        }

        // Track that source dealt damage
        if (sourceId in newState.getBattlefield()) {
            newState = newState.updateEntity(sourceId) { container ->
                container.with(HasDealtDamageComponent)
            }
        }

        val sourceName = newState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
        val targetName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Planeswalker"
        events.add(DamageDealtEvent(sourceId, targetId, effectiveAmount, true,
            sourceName = sourceName, targetName = targetName, targetIsPlayer = false))
        val newLoyalty = (currentLoyalty - effectiveAmount).coerceAtLeast(0)
        events.add(LoyaltyChangedEvent(targetId, targetName, -(effectiveAmount.coerceAtMost(currentLoyalty))))

        return newState
    }

    private fun applyDamageToCreature(
        state: GameState,
        sourceId: EntityId,
        targetId: EntityId,
        amplifiedAmount: Int,
        events: MutableList<GameEvent>
    ): GameState {
        if (targetId !in state.getBattlefield()) return state
        var newState = state

        // Prevention shields
        val (shieldState, effectiveAmount) = DamageUtils.applyDamagePreventionShields(
            newState, targetId, amplifiedAmount, isCombatDamage = true, sourceId = sourceId
        )
        newState = shieldState
        if (effectiveAmount <= 0) return newState

        // Damage redirection (Glarecaster, Zealous Inquisitor)
        val (redirectState, redirectTargetId, redirectAmount) = DamageUtils.checkDamageRedirection(
            newState, targetId, effectiveAmount
        )
        newState = redirectState
        if (redirectTargetId != null && redirectAmount > 0) {
            newState = dealFinalDamage(newState, sourceId, redirectTargetId, redirectAmount, events)
            val remaining = effectiveAmount - redirectAmount
            if (remaining > 0) {
                newState = dealFinalDamage(newState, sourceId, targetId, remaining, events)
            }
            return newState
        }

        return dealFinalDamage(newState, sourceId, targetId, effectiveAmount, events)
    }

    /**
     * Final damage dealing — no more modification. Marks damage on creature or reduces player life.
     * Handles both player and creature targets (needed for damage redirection to players).
     */
    private fun dealFinalDamage(
        state: GameState,
        sourceId: EntityId,
        targetId: EntityId,
        amount: Int,
        events: MutableList<GameEvent>
    ): GameState {
        if (amount <= 0) return state
        var newState = state

        val targetContainer = newState.getEntity(targetId) ?: return newState
        val isPlayer = targetContainer.get<LifeTotalComponent>() != null &&
                       targetContainer.get<CardComponent>() == null
        val projected = newState.projectedState
        val isPlaneswalker = !isPlayer && projected.isPlaneswalker(targetId)

        if (isPlayer) {
            val currentLife = targetContainer.get<LifeTotalComponent>()?.life ?: return newState
            val newLife = currentLife - amount
            newState = newState.updateEntity(targetId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            newState = DamageUtils.trackDamageReceivedByPlayer(newState, targetId, amount)
            // Track combat damage: source dealt damage + dealt combat damage to player
            if (sourceId in newState.getBattlefield()) {
                newState = newState.updateEntity(sourceId) { container ->
                    container.with(HasDealtDamageComponent).with(HasDealtCombatDamageToPlayerComponent)
                }
            }
            // Track that player was dealt combat damage this turn
            newState = newState.updateEntity(targetId) { container ->
                container.with(WasDealtCombatDamageThisTurnComponent)
            }
            val sourceName = newState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
            events.add(DamageDealtEvent(sourceId, targetId, amount, true,
                sourceName = sourceName, targetName = "Player", targetIsPlayer = true))
            events.add(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.DAMAGE))
        } else if (isPlaneswalker) {
            if (targetId !in newState.getBattlefield()) return newState
            val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            val currentLoyalty = counters.getCount(com.wingedsheep.sdk.core.CounterType.LOYALTY)
            newState = newState.updateEntity(targetId) { container ->
                container.with(counters.withRemoved(com.wingedsheep.sdk.core.CounterType.LOYALTY, amount))
            }
            // Track that source dealt damage
            if (sourceId in newState.getBattlefield()) {
                newState = newState.updateEntity(sourceId) { container ->
                    container.with(HasDealtDamageComponent)
                }
            }
            val sourceName = newState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
            val targetName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Planeswalker"
            events.add(DamageDealtEvent(sourceId, targetId, amount, true,
                sourceName = sourceName, targetName = targetName, targetIsPlayer = false))
            events.add(LoyaltyChangedEvent(targetId, targetName, -(amount.coerceAtMost(currentLoyalty))))
        } else {
            if (targetId !in newState.getBattlefield()) return newState
            val existingDamage = newState.getEntity(targetId)?.get<DamageComponent>()
            val currentDamage = existingDamage?.amount ?: 0
            val projected = newState.projectedState
            val hasDeathtouch = projected.hasKeyword(sourceId, Keyword.DEATHTOUCH)
            newState = newState.updateEntity(targetId) { container ->
                container.with(DamageComponent(
                    amount = currentDamage + amount,
                    deathtouchDamageReceived = hasDeathtouch || (existingDamage?.deathtouchDamageReceived == true)
                ))
            }
            // Mark creature as having been dealt damage this turn
            newState = newState.updateEntity(targetId) { container ->
                container.with(WasDealtDamageThisTurnComponent)
            }
            // Track that source dealt damage
            if (sourceId in newState.getBattlefield()) {
                newState = newState.updateEntity(sourceId) { container ->
                    container.with(HasDealtDamageComponent)
                }
            }
            newState = DamageUtils.trackDamageDealtToCreature(newState, sourceId, targetId)
            val sourceName = newState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
            val targetName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Creature"
            val targetIsFaceDown = newState.getEntity(targetId)?.has<FaceDownComponent>() == true
            events.add(DamageDealtEvent(sourceId, targetId, amount, true,
                sourceName = sourceName, targetName = targetName, targetIsPlayer = false, targetWasFaceDown = targetIsFaceDown))
        }

        return newState
    }

    // =========================================================================
    // Damage Reflection (Harsh Justice)
    // =========================================================================

    private fun applyDamageReflection(
        state: GameState,
        sourceId: EntityId,
        targetPlayerId: EntityId,
        originalAmount: Int,
        events: MutableList<GameEvent>
    ): GameState {
        if (!hasReflectCombatDamage(state, targetPlayerId)) return state

        val projected = state.projectedState
        val attackerController = projected.getController(sourceId) ?: return state
        if (attackerController == targetPlayerId) return state

        val attackerControllerLife = state.getEntity(attackerController)?.get<LifeTotalComponent>()?.life ?: return state
        val newLife = attackerControllerLife - originalAmount
        var newState = state.updateEntity(attackerController) { container ->
            container.with(LifeTotalComponent(newLife))
        }
        newState = DamageUtils.trackDamageReceivedByPlayer(newState, attackerController, originalAmount)
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
        events.add(DamageDealtEvent(sourceId, attackerController, originalAmount, true,
            sourceName = sourceName, targetName = "Player", targetIsPlayer = true))
        events.add(LifeChangedEvent(attackerController, attackerControllerLife, newLife, LifeChangeReason.DAMAGE))

        return newState
    }

    // =========================================================================
    // Redirect Effect Cleanup
    // =========================================================================

    private fun consumeUsedRedirectEffects(state: GameState, assignments: List<CombatDamageAssignment>): GameState {
        val sourcesWithDamage = assignments.filter { it.amount > 0 }.map { it.sourceId }.toSet()
        val redirectedCreatures = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.RedirectCombatDamageToController }
            .flatMap { it.effect.affectedEntities }
            .filter { it in sourcesWithDamage }
            .toSet()
        if (redirectedCreatures.isEmpty()) return state

        var newState = state
        for (creatureId in redirectedCreatures) {
            newState = consumeRedirectCombatDamageToController(newState, creatureId)
        }
        return newState
    }

    // =========================================================================
    // Damage Prevention Choice (CR 615.7)
    // =========================================================================

    private fun checkDamagePreventionChoice(
        state: GameState,
        attackers: List<Pair<EntityId, AttackingComponent>>,
        projected: ProjectedState,
        firstStrike: Boolean
    ): ExecutionResult? {
        val incomingDamage = mutableMapOf<EntityId, MutableMap<EntityId, Int>>()

        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in state.getBattlefield()) continue
            val attackerContainer = state.getEntity(attackerId) ?: continue
            attackerContainer.get<CardComponent>() ?: continue

            val hasFirstStrike = projected.hasKeyword(attackerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(attackerId, Keyword.DOUBLE_STRIKE)
            val dealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }
            if (!dealsDamageThisStep) continue

            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower <= 0) continue

            val blockedBy = attackerContainer.get<BlockedComponent>()

            if (blockedBy == null) {
                val defenderId = attackingComponent.defenderId
                if (!isProtectedFromAttackingCreatureDamage(state, defenderId) &&
                    !isCombatDamagePreventedByGroupFilter(state, attackerId, projected)) {
                    val amplified = DamageUtils.applyStaticDamageAmplification(state, defenderId, attackerPower, attackerId, isCombatDamage = true)
                    incomingDamage.getOrPut(defenderId) { mutableMapOf() }
                        .merge(attackerId, amplified) { a, b -> a + b }
                }
            } else {
                val manualAssignment = attackerContainer.get<DamageAssignmentComponent>()
                val damageDistribution = if (manualAssignment != null) {
                    manualAssignment.assignments
                } else {
                    damageCalculator.calculateAutoDamageDistribution(state, attackerId).assignments
                }

                for ((targetId, damage) in damageDistribution) {
                    if (damage <= 0) continue
                    val targetContainer = state.getEntity(targetId)
                    val isPlayer = targetContainer?.get<LifeTotalComponent>() != null &&
                        targetContainer.get<CardComponent>() == null
                    val amplified = DamageUtils.applyStaticDamageAmplification(state, targetId, damage, attackerId, isCombatDamage = true)
                    if (isPlayer) {
                        incomingDamage.getOrPut(targetId) { mutableMapOf() }
                            .merge(attackerId, amplified) { a, b -> a + b }
                    } else {
                        val damageCantBePrevented = DamageUtils.isDamagePreventionDisabled(state)
                        val attackerColors = projected.getColors(attackerId)
                        val attackerSubtypes = projected.getSubtypes(attackerId)
                        val blockerProtected = !damageCantBePrevented && (attackerColors.any {
                            projected.hasKeyword(targetId, "PROTECTION_FROM_$it")
                        } || attackerSubtypes.any {
                            projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${it.uppercase()}")
                        })
                        if (!blockerProtected) {
                            incomingDamage.getOrPut(targetId) { mutableMapOf() }
                                .merge(attackerId, amplified) { a, b -> a + b }
                        }
                    }
                }
            }
        }

        for ((recipientId, sourcesDamage) in incomingDamage) {
            if (sourcesDamage.size < 2) continue

            val totalDamage = sourcesDamage.values.sum()

            for (floatingEffect in state.floatingEffects) {
                val mod = floatingEffect.effect.modification
                if (mod !is SerializableModification.PreventNextDamage) continue
                if (mod.onlyFromSource != null) continue
                if (recipientId !in floatingEffect.effect.affectedEntities) continue

                val shieldAmount = mod.remainingAmount
                if (shieldAmount >= totalDamage) continue

                val decisionId = UUID.randomUUID().toString()

                val decision = DistributeDecision(
                    id = decisionId,
                    playerId = recipientId,
                    prompt = "Distribute $shieldAmount damage prevention among attacking creatures",
                    context = DecisionContext(
                        sourceId = floatingEffect.sourceId,
                        sourceName = floatingEffect.sourceName,
                        phase = DecisionPhase.COMBAT
                    ),
                    totalAmount = shieldAmount,
                    targets = sourcesDamage.keys.toList(),
                    minPerTarget = 0,
                    maxPerTarget = sourcesDamage
                )

                val continuation = DamagePreventionContinuation(
                    decisionId = decisionId,
                    recipientId = recipientId,
                    shieldEffectId = floatingEffect.id,
                    shieldAmount = shieldAmount,
                    damageBySource = sourcesDamage,
                    firstStrike = firstStrike
                )

                val pausedState = state
                    .withPendingDecision(decision)
                    .pushContinuation(continuation)

                return ExecutionResult.paused(pausedState, decision)
            }
        }

        return null
    }

    // =========================================================================
    // Lifelink
    // =========================================================================

    private fun applyLifelinkFromDamageEvents(
        state: GameState,
        existingEvents: List<GameEvent>,
        projected: ProjectedState
    ): Pair<GameState, List<GameEvent>> {
        val damageBySource = mutableMapOf<EntityId, Int>()
        for (event in existingEvents) {
            if (event !is DamageDealtEvent) continue
            val sourceId = event.sourceId ?: continue
            if (!projected.hasKeyword(sourceId, Keyword.LIFELINK.name)) continue
            damageBySource[sourceId] = (damageBySource[sourceId] ?: 0) + event.amount
        }

        if (damageBySource.isEmpty()) return state to emptyList()

        var newState = state
        val lifelinkEvents = mutableListOf<GameEvent>()

        for ((sourceId, totalDamage) in damageBySource) {
            val controllerId = projected.getController(sourceId) ?: continue
            if (DamageUtils.isLifeGainPrevented(newState, controllerId)) continue

            val currentLife = newState.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: continue
            val newLife = currentLife + totalDamage
            newState = newState.updateEntity(controllerId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            newState = DamageUtils.markLifeGainedThisTurn(newState, controllerId)
            lifelinkEvents.add(LifeChangedEvent(controllerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
        }

        return newState to lifelinkEvents
    }

    // =========================================================================
    // Lethal Damage Check
    // =========================================================================

    private fun checkLethalDamage(state: GameState): Pair<GameState, List<GameEvent>> {
        val newState = state
        val events = mutableListOf<GameEvent>()

        val projected = state.projectedState

        for ((entityId, container) in state.entities) {
            container.get<CardComponent>() ?: continue
            if (!projected.isCreature(entityId)) continue

            val damageComponent = container.get<DamageComponent>() ?: continue
            val damage = damageComponent.amount
            if (damage <= 0) continue

            projected.getToughness(entityId) ?: continue
        }

        return newState to events
    }

    // =========================================================================
    // Damage Prevention Helpers (used by pre-checks)
    // =========================================================================

    private fun isAllCombatDamagePrevented(state: GameState): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventAllCombatDamage
        }
    }

    private fun isProtectedFromAttackingCreatureDamage(state: GameState, playerId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventDamageFromAttackingCreatures &&
                playerId in floatingEffect.effect.affectedEntities
        }
    }

    private fun isCombatDamageToAndByPrevented(state: GameState, creatureId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventCombatDamageToAndBy &&
                creatureId in floatingEffect.effect.affectedEntities
        }
    }

    private fun isCombatDamagePreventedByGroupFilter(
        state: GameState,
        creatureId: EntityId,
        projected: ProjectedState
    ): Boolean {
        val predicateEvaluator = PredicateEvaluator()
        return state.floatingEffects.any { floatingEffect ->
            val modification = floatingEffect.effect.modification
            if (modification is SerializableModification.PreventCombatDamageFromGroup) {
                val controllerId = floatingEffect.controllerId
                val predicateContext = PredicateContext(controllerId = controllerId)
                predicateEvaluator.matchesWithProjection(
                    state, projected, creatureId, modification.filter, predicateContext
                )
            } else {
                false
            }
        }
    }

    // =========================================================================
    // Damage Redirect/Reflect Helpers
    // =========================================================================

    private fun hasReflectCombatDamage(state: GameState, playerId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            val modification = floatingEffect.effect.modification
            modification is SerializableModification.ReflectCombatDamage &&
                modification.protectedPlayerId == playerId.toString()
        }
    }

    private fun hasCombatDamageRedirectToController(state: GameState, creatureId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.RedirectCombatDamageToController &&
                creatureId in floatingEffect.effect.affectedEntities
        }
    }

    private fun consumeRedirectCombatDamageToController(state: GameState, creatureId: EntityId): GameState {
        return state.copy(
            floatingEffects = state.floatingEffects.filterNot { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.RedirectCombatDamageToController &&
                    creatureId in floatingEffect.effect.affectedEntities
            }
        )
    }
}
