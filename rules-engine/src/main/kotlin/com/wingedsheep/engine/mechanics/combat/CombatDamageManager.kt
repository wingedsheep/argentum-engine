package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DivideCombatDamageFreely
import java.util.UUID

/**
 * Handles combat damage calculation and application.
 *
 * Responsibilities:
 * - First strike and regular combat damage steps
 * - Damage assignment decisions (manual, trample, divide freely)
 * - Damage prevention (shields, group filters, combat-specific)
 * - Damage redirection and reflection
 * - Lifelink processing
 * - Lethal damage detection
 */
internal class CombatDamageManager(
    private val cardRegistry: CardRegistry?,
    private val damageCalculator: DamageCalculator,
) {

    /**
     * Calculate and apply combat damage.
     *
     * @param firstStrike If true, only creatures with first strike/double strike deal damage
     */
    fun applyCombatDamage(state: GameState, firstStrike: Boolean = false): ExecutionResult {
        if (isAllCombatDamagePrevented(state)) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        val projected = state.projectedState
        val attackers = state.findEntitiesWith<AttackingComponent>()

        // Pre-check: if any attacker with DivideCombatDamageFreely needs a distribution
        // decision, pause before processing ANY damage.
        for ((attackerId, attackingComponent) in attackers) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: continue
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

            val allDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(state, attackerId)
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

            val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId)
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

        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in state.getBattlefield()) continue

            val attackerContainer = state.getEntity(attackerId) ?: continue
            attackerContainer.get<CardComponent>() ?: continue

            val hasFirstStrike = projected.hasKeyword(attackerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(attackerId, Keyword.DOUBLE_STRIKE)

            val attackerDealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }

            val blockedBy = attackerContainer.get<BlockedComponent>()

            val attackerCard = attackerContainer.get<CardComponent>()!!
            val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId)
            val hasDivideDamageFreely = cardDef?.staticAbilities?.any { it is DivideCombatDamageFreely } == true

            if (hasDivideDamageFreely) {
                val blockerIdList = blockedBy?.blockerIds ?: emptyList()
                val (dmgState, dmgEvents) = dealDividedCombatDamage(
                    newState, attackerId, blockerIdList,
                    attackingComponent.defenderId, firstStrike, attackerDealsDamageThisStep
                )
                newState = dmgState
                events.addAll(dmgEvents)
            } else if (blockedBy == null) {
                if (!attackerDealsDamageThisStep) continue

                val power = projected.getPower(attackerId) ?: 0
                if (power <= 0) continue

                val defenderId = attackingComponent.defenderId

                val allDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(newState, attackerId)
                val isProtected = isProtectedFromAttackingCreatureDamage(newState, defenderId)
                val groupPrevented = isCombatDamagePreventedByGroupFilter(newState, attackerId, projected)
                val toAndByPrevented = isCombatDamageToAndByPrevented(newState, attackerId)
                if (!isProtected && !allDamagePrevented && !groupPrevented && !toAndByPrevented) {
                    val redirectToController = hasCombatDamageRedirectToController(newState, attackerId)
                    val damageTargetId = if (redirectToController) {
                        projected.getController(attackerId) ?: defenderId
                    } else {
                        defenderId
                    }
                    val damageResult = dealDamageToPlayer(newState, damageTargetId, power, attackerId)
                    newState = damageResult.newState
                    events.addAll(damageResult.events)
                    if (redirectToController) {
                        newState = consumeRedirectCombatDamageToController(newState, attackerId)
                    }
                }
            } else {
                val liveBlockerIds = blockedBy.blockerIds.filter { it in newState.getBattlefield() }
                val (attackerDamageState, attackerEvents) = dealCombatDamageBetweenCreatures(
                    newState, attackerId, liveBlockerIds, firstStrike, attackerDealsDamageThisStep
                )
                newState = attackerDamageState
                events.addAll(attackerEvents)
            }
        }

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
                    val amplified = EffectExecutorUtils.applyStaticDamageAmplification(state, defenderId, attackerPower, attackerId)
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
                    val amplified = EffectExecutorUtils.applyStaticDamageAmplification(state, targetId, damage, attackerId)
                    if (isPlayer) {
                        incomingDamage.getOrPut(targetId) { mutableMapOf() }
                            .merge(attackerId, amplified) { a, b -> a + b }
                    } else {
                        val attackerColors = projected.getColors(attackerId)
                        val attackerSubtypes = projected.getSubtypes(attackerId)
                        val blockerProtected = attackerColors.any {
                            projected.hasKeyword(targetId, "PROTECTION_FROM_$it")
                        } || attackerSubtypes.any {
                            projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${it.uppercase()}")
                        }
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
    // Damage to Players
    // =========================================================================

    private fun dealDamageToPlayer(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        sourceId: EntityId
    ): ExecutionResult {
        val amplifiedAmount = EffectExecutorUtils.applyStaticDamageAmplification(state, playerId, amount, sourceId)
        val counterResult = EffectExecutorUtils.applyReplaceDamageWithCounters(state, playerId, amplifiedAmount, sourceId)
        if (counterResult != null) return counterResult
        val (shieldState, effectiveAmount) = EffectExecutorUtils.applyDamagePreventionShields(state, playerId, amplifiedAmount, isCombatDamage = true, sourceId = sourceId)
        if (effectiveAmount <= 0) return ExecutionResult.success(shieldState)

        val playerContainer = shieldState.getEntity(playerId)
            ?: return ExecutionResult.error(shieldState, "Player not found: $playerId")

        val currentLife = playerContainer.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(shieldState, "Player has no life total")

        val newLife = currentLife - effectiveAmount
        var newState = shieldState.updateEntity(playerId) { container ->
            container.with(LifeTotalComponent(newLife))
        }
        newState = EffectExecutorUtils.trackDamageReceivedByPlayer(newState, playerId, effectiveAmount)

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
        val events = mutableListOf<GameEvent>(
            DamageDealtEvent(sourceId, playerId, effectiveAmount, true, sourceName = sourceName, targetName = "Player", targetIsPlayer = true),
            LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.DAMAGE)
        )

        // Check for damage reflection (Harsh Justice)
        val hasReflection = hasReflectCombatDamage(state, playerId)
        if (hasReflection) {
            val projected = state.projectedState
            val attackerController = projected.getController(sourceId)

            if (attackerController != null && attackerController != playerId) {
                val attackerControllerContainer = newState.getEntity(attackerController)
                val attackerControllerLife = attackerControllerContainer?.get<LifeTotalComponent>()?.life

                if (attackerControllerLife != null) {
                    val reflectedNewLife = attackerControllerLife - amount
                    newState = newState.updateEntity(attackerController) { container ->
                        container.with(LifeTotalComponent(reflectedNewLife))
                    }
                    newState = EffectExecutorUtils.trackDamageReceivedByPlayer(newState, attackerController, amount)
                    val reflectSourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
                    events.add(DamageDealtEvent(sourceId, attackerController, amount, true, sourceName = reflectSourceName, targetName = "Player", targetIsPlayer = true))
                    events.add(LifeChangedEvent(attackerController, attackerControllerLife, reflectedNewLife, LifeChangeReason.DAMAGE))
                }
            }
        }

        return ExecutionResult.success(newState, events)
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
            if (EffectExecutorUtils.isLifeGainPrevented(newState, controllerId)) continue

            val currentLife = newState.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: continue
            val newLife = currentLife + totalDamage
            newState = newState.updateEntity(controllerId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            lifelinkEvents.add(LifeChangedEvent(controllerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
        }

        return newState to lifelinkEvents
    }

    // =========================================================================
    // Damage Between Creatures (Blocked Combat)
    // =========================================================================

    private fun dealCombatDamageBetweenCreatures(
        state: GameState,
        attackerId: EntityId,
        blockerIds: List<EntityId>,
        firstStrike: Boolean,
        attackerDealsDamageThisStep: Boolean
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()

        val attackerContainer = newState.getEntity(attackerId) ?: return newState to events
        attackerContainer.get<CardComponent>() ?: return newState to events

        val projected = newState.projectedState

        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: blockerIds

        val manualAssignment = attackerContainer.get<DamageAssignmentComponent>()

        val damageDistribution = if (manualAssignment != null) {
            manualAssignment.assignments
        } else {
            damageCalculator.calculateAutoDamageDistribution(newState, attackerId).assignments
        }

        // Apply attacker's damage to blockers
        val attackerDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(newState, attackerId)
        val attackerGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, attackerId, projected)
        val attackerToAndByPrevented = isCombatDamageToAndByPrevented(newState, attackerId)
        if (attackerDealsDamageThisStep && !attackerDamagePrevented && !attackerGroupPrevented && !attackerToAndByPrevented) {
            val attackerRedirectToController = hasCombatDamageRedirectToController(newState, attackerId)
            if (attackerRedirectToController) {
                val totalDamage = damageDistribution.values.sum()
                if (totalDamage > 0) {
                    val controllerId = projected.getController(attackerId)
                    if (controllerId != null) {
                        val amplifiedDamage = EffectExecutorUtils.applyStaticDamageAmplification(newState, controllerId, totalDamage, attackerId)
                        val (shieldState, effectiveDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, controllerId, amplifiedDamage, isCombatDamage = true, sourceId = attackerId)
                        newState = shieldState
                        if (effectiveDamage > 0) {
                            val currentLife = newState.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
                            val newLife = currentLife - effectiveDamage
                            newState = newState.updateEntity(controllerId) { container ->
                                container.with(LifeTotalComponent(newLife))
                            }
                            newState = EffectExecutorUtils.trackDamageReceivedByPlayer(newState, controllerId, effectiveDamage)
                            val sourceName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                            events.add(DamageDealtEvent(attackerId, controllerId, effectiveDamage, true, sourceName = sourceName, targetName = "Player", targetIsPlayer = true))
                            events.add(LifeChangedEvent(controllerId, currentLife, newLife, LifeChangeReason.DAMAGE))
                        }
                    }
                }
                newState = consumeRedirectCombatDamageToController(newState, attackerId)
            } else {
            for ((targetId, damage) in damageDistribution) {
                if (damage <= 0) continue

                val targetContainer = newState.getEntity(targetId)
                val isPlayer = targetContainer?.get<LifeTotalComponent>() != null &&
                               targetContainer.get<CardComponent>() == null

                if (isPlayer) {
                    val amplifiedTrampleDamage = EffectExecutorUtils.applyStaticDamageAmplification(newState, targetId, damage, attackerId)
                    val (shieldState, effectiveTrampleDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, targetId, amplifiedTrampleDamage, isCombatDamage = true, sourceId = attackerId)
                    newState = shieldState
                    if (effectiveTrampleDamage > 0) {
                        val currentLife = newState.getEntity(targetId)?.get<LifeTotalComponent>()?.life ?: 0
                        val newLife = currentLife - effectiveTrampleDamage
                        newState = newState.updateEntity(targetId) { container ->
                            container.with(LifeTotalComponent(newLife))
                        }
                        newState = EffectExecutorUtils.trackDamageReceivedByPlayer(newState, targetId, effectiveTrampleDamage)
                        val trampleSourceName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                        events.add(DamageDealtEvent(attackerId, targetId, effectiveTrampleDamage, true, sourceName = trampleSourceName, targetName = "Player", targetIsPlayer = true))
                        events.add(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.DAMAGE))
                    }
                } else {
                    val blockerTargetToAndByPrevented = isCombatDamageToAndByPrevented(newState, targetId)
                    val attackerColors = projected.getColors(attackerId)
                    val attackerSubtypes = projected.getSubtypes(attackerId)
                    val blockerProtected = attackerColors.any { colorName ->
                        projected.hasKeyword(targetId, "PROTECTION_FROM_$colorName")
                    } || attackerSubtypes.any { subtype ->
                        projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")
                    }
                    if (!blockerProtected && !blockerTargetToAndByPrevented) {
                        val amplifiedDamage = EffectExecutorUtils.applyStaticDamageAmplification(newState, targetId, damage, attackerId)
                        val (shieldState, effectiveDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, targetId, amplifiedDamage, isCombatDamage = true, sourceId = attackerId)
                        newState = shieldState
                        if (effectiveDamage > 0) {
                            newState = dealCombatDamageToCreature(newState, targetId, effectiveDamage, attackerId, events)
                        }
                    }
                }
            }
            } // end else (no redirect to controller)
        }

        // Each blocker deals damage to attacker
        if (attackerId !in newState.getBattlefield()) {
            return newState to events
        }

        for (blockerId in orderedBlockers) {
            val blockerContainer = newState.getEntity(blockerId) ?: continue
            blockerContainer.get<CardComponent>() ?: continue

            if (blockerId !in newState.getBattlefield()) continue

            val hasFirstStrike = projected.hasKeyword(blockerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(blockerId, Keyword.DOUBLE_STRIKE)

            val dealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }

            if (!dealsDamageThisStep) continue

            val blockerPower = projected.getPower(blockerId) ?: 0
            if (blockerPower > 0) {
                val blockerDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(newState, blockerId)
                val blockerGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, blockerId, projected)
                val blockerToAndByPrevented = isCombatDamageToAndByPrevented(newState, blockerId)
                val attackerToAndByPrevented = isCombatDamageToAndByPrevented(newState, attackerId)

                val blockerColors = projected.getColors(blockerId)
                val blockerSubtypes = projected.getSubtypes(blockerId)
                val attackerProtected = blockerColors.any { colorName ->
                    projected.hasKeyword(attackerId, "PROTECTION_FROM_$colorName")
                } || blockerSubtypes.any { subtype ->
                    projected.hasKeyword(attackerId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")
                }
                if (!attackerProtected && !blockerDamagePrevented && !blockerGroupPrevented && !blockerToAndByPrevented && !attackerToAndByPrevented) {
                    val blockerRedirectToController = hasCombatDamageRedirectToController(newState, blockerId)
                    if (blockerRedirectToController) {
                        val blockerControllerId = projected.getController(blockerId)
                        if (blockerControllerId != null) {
                            val amplifiedDamage = EffectExecutorUtils.applyStaticDamageAmplification(newState, blockerControllerId, blockerPower, blockerId)
                            val (shieldState, effectiveDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, blockerControllerId, amplifiedDamage, isCombatDamage = true, sourceId = blockerId)
                            newState = shieldState
                            if (effectiveDamage > 0) {
                                val currentLife = newState.getEntity(blockerControllerId)?.get<LifeTotalComponent>()?.life ?: 0
                                val newLife = currentLife - effectiveDamage
                                newState = newState.updateEntity(blockerControllerId) { container ->
                                    container.with(LifeTotalComponent(newLife))
                                }
                                newState = EffectExecutorUtils.trackDamageReceivedByPlayer(newState, blockerControllerId, effectiveDamage)
                                val blockerSourceName = newState.getEntity(blockerId)?.get<CardComponent>()?.name ?: "Creature"
                                events.add(DamageDealtEvent(blockerId, blockerControllerId, effectiveDamage, true, sourceName = blockerSourceName, targetName = "Player", targetIsPlayer = true))
                                events.add(LifeChangedEvent(blockerControllerId, currentLife, newLife, LifeChangeReason.DAMAGE))
                            }
                        }
                        newState = consumeRedirectCombatDamageToController(newState, blockerId)
                    } else {
                    val amplifiedBlockerDamage = EffectExecutorUtils.applyStaticDamageAmplification(newState, attackerId, blockerPower, blockerId)
                    val (shieldState, effectiveBlockerDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, attackerId, amplifiedBlockerDamage, isCombatDamage = true, sourceId = blockerId)
                    newState = shieldState
                    if (effectiveBlockerDamage > 0) {
                        newState = dealCombatDamageToCreature(newState, attackerId, effectiveBlockerDamage, blockerId, events)
                    }
                    }
                }
            }
        }

        return newState to events
    }

    // =========================================================================
    // Divided Combat Damage (Butcher Orgg)
    // =========================================================================

    private fun dealDividedCombatDamage(
        state: GameState,
        attackerId: EntityId,
        blockerIds: List<EntityId>,
        defenderId: EntityId,
        firstStrike: Boolean,
        attackerDealsDamageThisStep: Boolean
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()

        val attackerContainer = newState.getEntity(attackerId) ?: return newState to events
        attackerContainer.get<CardComponent>() ?: return newState to events

        val projected = newState.projectedState

        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: blockerIds

        val damageAssignment = attackerContainer.get<DamageAssignmentComponent>()

        if (attackerDealsDamageThisStep) {
            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower > 0) {

                if (blockerIds.isNotEmpty()) {
                    val hasBlockerOnBattlefield = orderedBlockers.any { it in newState.getBattlefield() }
                    if (!hasBlockerOnBattlefield) {
                        return dealBlockerCounterattack(newState, events, attackerId, orderedBlockers, firstStrike, projected)
                    }
                }

                val attackerGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, attackerId, projected)
                val attackerAllDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(newState, attackerId)
                val attackerToAndByPrevented = isCombatDamageToAndByPrevented(newState, attackerId)

                if (attackerGroupPrevented || attackerAllDamagePrevented || attackerToAndByPrevented) {
                    // Combat damage from this attacker is prevented
                } else if (damageAssignment != null) {
                    for ((targetId, damage) in damageAssignment.assignments) {
                        if (damage <= 0) continue

                        val targetContainer = newState.getEntity(targetId)
                        val isPlayer = targetContainer?.get<LifeTotalComponent>() != null &&
                            targetContainer.get<CardComponent>() == null

                        if (isPlayer) {
                            val isProtected = isProtectedFromAttackingCreatureDamage(newState, targetId)
                            if (!isProtected) {
                                val amplifiedPlayerDmg = EffectExecutorUtils.applyStaticDamageAmplification(newState, targetId, damage, attackerId)
                                val (shieldState, effectivePlayerDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, targetId, amplifiedPlayerDmg, isCombatDamage = true, sourceId = attackerId)
                                newState = shieldState
                                if (effectivePlayerDamage > 0) {
                                    val currentLife = newState.getEntity(targetId)?.get<LifeTotalComponent>()?.life ?: 0
                                    val newLife = currentLife - effectivePlayerDamage
                                    newState = newState.updateEntity(targetId) { container ->
                                        container.with(LifeTotalComponent(newLife))
                                    }
                                    newState = EffectExecutorUtils.trackDamageReceivedByPlayer(newState, targetId, effectivePlayerDamage)
                                    val freeSourceName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                                    events.add(DamageDealtEvent(attackerId, targetId, effectivePlayerDamage, true, sourceName = freeSourceName, targetName = "Player", targetIsPlayer = true))
                                    events.add(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.DAMAGE))
                                }
                            }
                        } else if (targetId in newState.getBattlefield()) {
                            val attackerColors = projected.getColors(attackerId)
                            val attackerSubtypes = projected.getSubtypes(attackerId)
                            val creatureProtected = attackerColors.any { colorName ->
                                projected.hasKeyword(targetId, "PROTECTION_FROM_$colorName")
                            } || attackerSubtypes.any { subtype ->
                                projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")
                            }

                            if (!creatureProtected) {
                                val amplifiedCreatureDmg = EffectExecutorUtils.applyStaticDamageAmplification(newState, targetId, damage, attackerId)
                                val (shieldState, effectiveDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, targetId, amplifiedCreatureDmg, isCombatDamage = true, sourceId = attackerId)
                                newState = shieldState
                                if (effectiveDamage > 0) {
                                    newState = dealCombatDamageToCreature(newState, targetId, effectiveDamage, attackerId, events)
                                }
                            }
                        }
                    }
                } else {
                    var remainingDamage = attackerPower

                    for (blockerId in orderedBlockers) {
                        if (remainingDamage <= 0) break
                        if (blockerId !in newState.getBattlefield()) continue

                        val lethalInfo = damageCalculator.calculateLethalDamage(newState, blockerId, attackerId)
                        val damageToAssign = minOf(remainingDamage, lethalInfo.lethalAmount)

                        val attackerColors = projected.getColors(attackerId)
                        val attackerSubtypes = projected.getSubtypes(attackerId)
                        val blockerProtected = attackerColors.any { colorName ->
                            projected.hasKeyword(blockerId, "PROTECTION_FROM_$colorName")
                        } || attackerSubtypes.any { subtype ->
                            projected.hasKeyword(blockerId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")
                        }

                        if (!blockerProtected) {
                            val amplifiedBlockerDmg = EffectExecutorUtils.applyStaticDamageAmplification(newState, blockerId, damageToAssign, attackerId)
                            val (shieldState, effectiveDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, blockerId, amplifiedBlockerDmg, isCombatDamage = true, sourceId = attackerId)
                            newState = shieldState
                            if (effectiveDamage > 0) {
                                newState = dealCombatDamageToCreature(newState, blockerId, effectiveDamage, attackerId, events)
                            }
                        }

                        remainingDamage -= damageToAssign
                    }

                    if (remainingDamage > 0) {
                        val isProtected = isProtectedFromAttackingCreatureDamage(newState, defenderId)
                        if (!isProtected) {
                            val amplifiedRemainDmg = EffectExecutorUtils.applyStaticDamageAmplification(newState, defenderId, remainingDamage, attackerId)
                            val (shieldState, effectivePlayerDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, defenderId, amplifiedRemainDmg, isCombatDamage = true, sourceId = attackerId)
                            newState = shieldState
                            if (effectivePlayerDamage > 0) {
                                val currentLife = newState.getEntity(defenderId)?.get<LifeTotalComponent>()?.life ?: 0
                                val newLife = currentLife - effectivePlayerDamage
                                newState = newState.updateEntity(defenderId) { container ->
                                    container.with(LifeTotalComponent(newLife))
                                }
                                newState = EffectExecutorUtils.trackDamageReceivedByPlayer(newState, defenderId, effectivePlayerDamage)
                                val remainAtkName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                                events.add(DamageDealtEvent(attackerId, defenderId, effectivePlayerDamage, true, sourceName = remainAtkName, targetName = "Player", targetIsPlayer = true))
                                events.add(LifeChangedEvent(defenderId, currentLife, newLife, LifeChangeReason.DAMAGE))
                            }
                        }
                    }
                }
            }
        }

        return dealBlockerCounterattack(newState, events, attackerId, orderedBlockers, firstStrike, projected)
    }

    // =========================================================================
    // Blocker Counterattack
    // =========================================================================

    private fun dealBlockerCounterattack(
        state: GameState,
        events: MutableList<GameEvent>,
        attackerId: EntityId,
        orderedBlockers: List<EntityId>,
        firstStrike: Boolean,
        projected: ProjectedState
    ): Pair<GameState, List<GameEvent>> {
        var newState = state

        if (attackerId !in newState.getBattlefield()) {
            return newState to events
        }

        for (blockerId in orderedBlockers) {
            val blockerContainer = newState.getEntity(blockerId) ?: continue
            blockerContainer.get<CardComponent>() ?: continue

            if (blockerId !in newState.getBattlefield()) continue

            val hasFirstStrike = projected.hasKeyword(blockerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(blockerId, Keyword.DOUBLE_STRIKE)

            val dealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }

            if (!dealsDamageThisStep) continue

            val blockerPower = projected.getPower(blockerId) ?: 0
            if (blockerPower > 0) {
                val counterGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, blockerId, projected)
                val blockerColors = projected.getColors(blockerId)
                val blockerSubtypes = projected.getSubtypes(blockerId)
                val attackerProtected = blockerColors.any { colorName ->
                    projected.hasKeyword(attackerId, "PROTECTION_FROM_$colorName")
                } || blockerSubtypes.any { subtype ->
                    projected.hasKeyword(attackerId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")
                }
                if (!attackerProtected && !counterGroupPrevented) {
                    val amplifiedCounterDmg = EffectExecutorUtils.applyStaticDamageAmplification(newState, attackerId, blockerPower, blockerId)
                    val (shieldState, effectiveBlockerDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, attackerId, amplifiedCounterDmg, isCombatDamage = true, sourceId = blockerId)
                    newState = shieldState
                    if (effectiveBlockerDamage > 0) {
                        newState = dealCombatDamageToCreature(newState, attackerId, effectiveBlockerDamage, blockerId, events)
                    }
                }
            }
        }

        return newState to events
    }

    // =========================================================================
    // Damage to Creatures
    // =========================================================================

    private fun dealCombatDamageToCreature(
        state: GameState,
        targetId: EntityId,
        damage: Int,
        sourceId: EntityId,
        events: MutableList<GameEvent>
    ): GameState {
        var newState = state

        val (redirectState, redirectTargetId, redirectAmount) = EffectExecutorUtils.checkDamageRedirection(newState, targetId, damage)
        newState = redirectState

        if (redirectTargetId != null && redirectAmount > 0) {
            newState = dealCombatDamageToCreature(newState, redirectTargetId, redirectAmount, sourceId, events)
            val remainingDamage = damage - redirectAmount
            if (remainingDamage <= 0) return newState
            return dealCombatDamageToCreature(newState, targetId, remainingDamage, sourceId, events)
        }

        val currentDamage = newState.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
        newState = newState.updateEntity(targetId) { container ->
            container.with(DamageComponent(currentDamage + damage))
        }
        newState = EffectExecutorUtils.trackDamageDealtToCreature(newState, sourceId, targetId)
        val sourceName = newState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Creature"
        val targetContainer = newState.getEntity(targetId)
        val targetName = targetContainer?.get<CardComponent>()?.name ?: "Creature"
        val targetIsFaceDown = targetContainer?.has<FaceDownComponent>() == true
        events.add(DamageDealtEvent(sourceId, targetId, damage, true, sourceName = sourceName, targetName = targetName, targetIsPlayer = false, targetWasFaceDown = targetIsFaceDown))

        return newState
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
    // Damage Prevention Helpers
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
