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
import com.wingedsheep.engine.state.components.combat.AttackerOrderComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.CounterType
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
    private val features: EngineFeatures = EngineFeatures(),
) {

    private data class PlanCandidate(
        val attackerId: EntityId,
        val attackerName: String,
        val attackingComponent: AttackingComponent,
        val chooser: EntityId,
        val availablePower: Int,
        val liveBlockers: List<EntityId>,
        val hasTrample: Boolean,
        val hasDeathtouch: Boolean,
        val minimumAssignments: Map<EntityId, Int>,
        val defaultAssignments: Map<EntityId, Int>,
        /** True for AssignCombatDamageAsUnblocked (Thorn Elemental). Disables lethal-first on this attacker's edges and adds a non-trample player drain. */
        val hasAssignAsUnblocked: Boolean = false,
        /** True for DivideCombatDamageFreely (Butcher Orgg). All edges have minimum=0; defaults split evenly across blockers + defender. */
        val hasDivideDamageFreely: Boolean = false,
        /**
         * True when CR 702.22j applies: at least one of this attacker's blockers has
         * banding, so the defender chooses the damage split *and* can ignore the
         * damage-assignment order. All ATK→BLK edges from this attacker drop their
         * lethal-first minimum to 0.
         */
        val bandingBypassesOrder: Boolean = false,
    )

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
        // When the resolution-board flag is on this whole loop is bypassed — those
        // attackers flow into the plan-candidate gather instead and surface a non-trample
        // ATTACKER_TO_PLAYER drain edge on the board, replacing the separate Y/N modal.
        if (!features.combatResolutionBoardEnabled) {
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

                val attackerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
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
        }

        // Pre-check: if any attacker with DivideCombatDamageFreely needs a distribution
        // decision, pause before processing ANY damage. When the resolution board is on,
        // *blocked* DivideCombatDamageFreely attackers fold into the board instead; the
        // *unblocked* variant (e.g. Butcher Orgg dumping damage onto an arbitrary set of
        // defending creatures + defender) still uses the legacy DistributeDecision flow
        // since the board doesn't yet surface arbitrary defending-creature targets.
        for ((attackerId, attackingComponent) in attackers) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId) ?: continue
            val hasDivideDamageFreely = cardDef.staticAbilities.any { it is DivideCombatDamageFreely }
            if (!hasDivideDamageFreely) continue
            if (features.combatResolutionBoardEnabled) {
                val blocked = attackerContainer.get<BlockedComponent>()?.blockerIds
                    ?.any { it in state.getBattlefield() } == true
                if (blocked) continue  // handled by the resolution board's plan-candidate gather
            }

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

            val attackerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
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
        // processing ANY damage. Instead of one AssignDamageDecision per attacker, we
        // gather every attacker that needs manual assignment, group them by the
        // *chooser* (attacker controller by default; defender via CR 702.21e when a
        // banding blocker is present), and emit a single CombatDamagePlanDecision per
        // chooser. The most common case — a band of N attackers blocked by ≥2 blockers
        // each — collapses from N modals to 1.
        //
        // With the resolution board on, also force-include any attacker whose
        // blockers include one that blocks ≥2 attackers (an Ironfist-style
        // bipartite). Without this the blocker's damage division would silently
        // auto-resolve — the decision wouldn't emit because no individual attacker
        // requires manual assignment on its own. See plan §10 "Engine gaps".
        val attackersWithBipartiteBlocker: Set<EntityId> = if (!features.combatResolutionBoardEnabled) {
            emptySet()
        } else {
            val result = mutableSetOf<EntityId>()
            for ((attackerId, _) in attackers) {
                val blockerIds = state.getEntity(attackerId)?.get<BlockedComponent>()
                    ?.blockerIds.orEmpty().filter { it in state.getBattlefield() }
                val anyBipartite = blockerIds.any { blockerId ->
                    val liveBlocked = state.getEntity(blockerId)
                        ?.get<BlockingComponent>()
                        ?.blockedAttackerIds.orEmpty()
                        .count { it in state.getBattlefield() }
                    liveBlocked >= 2
                }
                if (anyBipartite) result += attackerId
            }
            result
        }
        val planCandidates = mutableListOf<PlanCandidate>()
        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in state.getBattlefield()) continue

            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue

            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId)
            val hasDivideDamageFreely = cardDef?.staticAbilities?.any { it is DivideCombatDamageFreely } == true
            val hasAssignAsUnblocked = cardDef?.staticAbilities?.any { it is AssignCombatDamageAsUnblocked } == true
            // When the resolution board is on, DivideCombatDamageFreely + AssignAsUnblocked
            // both fold into the board (the standalone pre-checks are skipped above for
            // AssignAsUnblocked; DivideCombatDamageFreely is handled by its own pre-check
            // below when the flag is off). With the flag on, we route these through plan
            // candidates so they share the board UI with vanilla manual assignment.
            if (!features.combatResolutionBoardEnabled && hasDivideDamageFreely) continue

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

            // CR 702.21e: when a banding blocker is present and there are multiple blockers,
            // force a manual assignment decision so the *defender* can choose the division —
            // the normal auto-distribution skips creatures with exact lethal damage.
            val liveBlockerIds = blockedBy.blockerIds.filter { it in state.getBattlefield() }
            val bandingOverride = liveBlockerIds.size >= 2 &&
                liveBlockerIds.any { projected.hasKeyword(it, Keyword.BANDING) }
            // AssignAsUnblocked / DivideCombatDamageFreely always require manual assignment
            // (the player must choose how to split). When the flag is on they're folded into
            // the board even when normal lethal-first wouldn't surface a decision.
            val forceManual = features.combatResolutionBoardEnabled &&
                (hasAssignAsUnblocked || hasDivideDamageFreely)
            val bipartitePull = attackerId in attackersWithBipartiteBlocker
            if (!damageCalculator.requiresManualAssignment(state, attackerId) &&
                !bandingOverride && !forceManual && !bipartitePull) continue

            val attackerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
            if (attackerPower <= 0) continue

            val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
                ?: blockedBy.blockerIds

            val liveBlockers = orderedBlockers.filter { it in state.getBattlefield() }
            if (liveBlockers.isEmpty()) continue

            val hasTrample = projected.hasKeyword(attackerId, Keyword.TRAMPLE)
            val hasDeathtouch = projected.hasKeyword(attackerId, Keyword.DEATHTOUCH)
            val attackingPlayer = projected.getController(attackerId) ?: continue

            // CR 702.21e: when a defending creature with banding is blocking, the *defending*
            // player divides the attacker's combat damage among the blockers.
            val chooser = CombatDamageUtils.damageAssignmentChooser(
                state, projected, attackerId, defaultChooser = attackingPlayer
            )

            // Two paths bypass CR 510.1c's lethal-first on the ATK→BLK side:
            //   - CR 702.21e (blocker has banding → chooser flips to defender, which also
            //     lifts order, so `chooser != attackingPlayer` is sufficient).
            //   - CR 702.22j (attacker's band contains a banding member → active player
            //     still chooses, but may divide damage among the band's blockers ignoring
            //     order). The chooser doesn't flip here, so it needs its own check.
            val bandingBypassesOrder = chooser != attackingPlayer ||
                CombatDamageUtils.attackerBandHasBanding(state, projected, attackerId)

            val minimumAssignments = damageCalculator.getMinimumAssignments(state, attackerId)
            val autoDistribution = damageCalculator.calculateAutoDamageDistribution(state, attackerId)

            planCandidates.add(
                PlanCandidate(
                    attackerId = attackerId,
                    attackerName = attackerCard.name,
                    attackingComponent = attackingComponent,
                    chooser = chooser,
                    availablePower = attackerPower,
                    liveBlockers = liveBlockers,
                    hasTrample = hasTrample,
                    hasDeathtouch = hasDeathtouch,
                    minimumAssignments = minimumAssignments,
                    defaultAssignments = autoDistribution.assignments,
                    hasAssignAsUnblocked = hasAssignAsUnblocked,
                    hasDivideDamageFreely = hasDivideDamageFreely,
                    bandingBypassesOrder = bandingBypassesOrder,
                )
            )
        }

        if (planCandidates.isNotEmpty()) {
            // Group by chooser to keep one plan per acting player. In a 2-player game
            // with no banding-driven inversion mid-step there's only one group.
            val byChooser = planCandidates.groupBy { it.chooser }
            val (chooser, group) = byChooser.entries.first()

            if (features.combatResolutionBoardEnabled) {
                // Phase 2: unify all chooser groups into a single decision. The primary
                // chooser acts first; CR 702.22j/k co-choosers follow in order via the
                // continuation's pendingChoosers queue.
                val choosersInOrder = byChooser.keys.toList()
                return emitCombatResolutionDecision(
                    state = state,
                    projected = projected,
                    choosersInOrder = choosersInOrder,
                    allCandidates = planCandidates,
                    firstStrike = firstStrike,
                    previousAmounts = emptyMap(),
                )
            }

            val entries = group.map { c ->
                CombatDamagePlanEntry(
                    attackerId = c.attackerId,
                    attackerName = c.attackerName,
                    availablePower = c.availablePower,
                    orderedTargets = c.liveBlockers,
                    defenderId = if (c.hasTrample) c.attackingComponent.defenderId else null,
                    minimumAssignments = c.minimumAssignments,
                    defaultAssignments = c.defaultAssignments,
                    hasTrample = c.hasTrample,
                    hasDeathtouch = c.hasDeathtouch,
                    bandId = state.getEntity(c.attackerId)?.get<AttackingComponent>()?.bandId,
                )
            }
            val decisionId = UUID.randomUUID().toString()
            val prompt = if (entries.size == 1) {
                "Assign ${entries[0].attackerName}'s ${entries[0].availablePower} combat damage"
            } else {
                "Assign combat damage for ${entries.size} attackers"
            }
            val decision = CombatDamagePlanDecision(
                id = decisionId,
                playerId = chooser,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = entries.firstOrNull()?.attackerId,
                    sourceName = if (entries.size == 1) entries[0].attackerName else "Combat damage",
                    phase = DecisionPhase.COMBAT,
                ),
                entries = entries,
                firstStrike = firstStrike,
            )
            val continuation = CombatDamagePlanContinuation(
                decisionId = decisionId,
                firstStrike = firstStrike,
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
                val power = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
                if (power > 0) {
                    val manualAssignment = attackerContainer.get<DamageAssignmentComponent>()
                    when {
                        manualAssignment != null && manualAssignment.assignments.isNotEmpty() -> {
                            // A DamageAssignmentComponent set during the first-strike damage step
                            // persists into the regular damage step. If first-strike damage killed
                            // a blocker, that blocker is no longer on the battlefield. Per CR 702.19c,
                            // an attacker with trample whose blocker has been removed from combat
                            // assigns the freed-up damage to the defending player/planeswalker.
                            // Without trample, damage assigned to a removed blocker is lost.
                            val defenderId = attackingComponent.defenderId
                            val hasTrample = projected.hasKeyword(attackerId, Keyword.TRAMPLE)
                            var trampleRedirect = 0
                            for ((targetId, damage) in manualAssignment.assignments) {
                                if (damage <= 0) continue
                                val targetIsLive = targetId in state.getBattlefield() || targetId == defenderId
                                if (targetIsLive) {
                                    assignments.add(CombatDamageAssignment(attackerId, targetId, damage))
                                } else if (hasTrample) {
                                    trampleRedirect += damage
                                }
                            }
                            if (trampleRedirect > 0) {
                                assignments.add(CombatDamageAssignment(attackerId, defenderId, trampleRedirect))
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
                val blockerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, blockerId, cardRegistry)
                if (blockerPower <= 0) continue

                processedBlockers.add(blockerId)

                val blockingComponent = blockerContainer.get<BlockingComponent>()
                val blockedAttackerIds = blockingComponent?.blockedAttackerIds ?: listOf(attackerId)

                // Honor a manual blocker assignment (CR 510.1d): when the new resolution
                // board exposes blocker→attacker edges, the resumer writes the player's
                // chosen amounts into the blocker's DamageAssignmentComponent. Targets that
                // are no longer on the battlefield are silently dropped (mirroring the
                // attacker-side first-strike pruning above).
                val manualBlockerAssignment = blockerContainer.get<DamageAssignmentComponent>()
                if (manualBlockerAssignment != null && manualBlockerAssignment.assignments.isNotEmpty()) {
                    for ((targetId, damage) in manualBlockerAssignment.assignments) {
                        if (damage <= 0) continue
                        if (targetId !in state.getBattlefield()) continue
                        assignments.add(CombatDamageAssignment(blockerId, targetId, damage))
                        pendingDamage[targetId] = (pendingDamage[targetId] ?: 0) + damage
                    }
                    continue
                }

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

        // Reduce life. Per CR 119.3, damage causes life loss, so life-loss
        // replacements (Bloodletter of Aclazotz) modify the life total reduction here.
        // Lifelink and tracking still see the unmodified `effectiveAmount`.
        val currentLife = newState.getEntity(targetId)?.get<LifeTotalComponent>()?.life ?: return newState
        val lifeLossAmount = DamageUtils.applyStaticLifeLossModification(newState, targetId, effectiveAmount)
        val newLife = currentLife - lifeLossAmount
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

        // Commander damage (CR 903.10a)
        newState = accumulateCommanderDamage(newState, sourceId, targetId, effectiveAmount)

        val toxicAmount = getToxicAmount(newState, newState.projectedState, sourceId)
        if (toxicAmount > 0) {
            val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            newState = newState.updateEntity(targetId) { container ->
                container.with(counters.withAdded(CounterType.POISON, toxicAmount))
            }
            events.add(CountersAddedEvent(targetId, CounterType.POISON.name, toxicAmount, "Player"))
        }

        // Reflection (Harsh Justice)
        newState = applyDamageReflection(newState, sourceId, targetId, originalAmount, events)

        return newState
    }

    /**
     * Apply combat damage to a planeswalker by removing loyalty counters (Rule 120.3c).
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

            // Commander damage (CR 903.10a)
            newState = accumulateCommanderDamage(newState, sourceId, targetId, amount)

            val toxicAmount = getToxicAmount(newState, projected, sourceId)
            if (toxicAmount > 0) {
                val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
                newState = newState.updateEntity(targetId) { container ->
                    container.with(counters.withAdded(CounterType.POISON, toxicAmount))
                }
                events.add(CountersAddedEvent(targetId, CounterType.POISON.name, toxicAmount, "Player"))
            }
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
            val projected = newState.projectedState
            val hasWither = projected.hasKeyword(sourceId, Keyword.WITHER)
            if (hasWither) {
                // Wither (CR 702.80): damage to creatures is dealt in the form of -1/-1 counters
                val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
                newState = newState.updateEntity(targetId) { container ->
                    container.with(counters.withAdded(com.wingedsheep.sdk.core.CounterType.MINUS_ONE_MINUS_ONE, amount))
                }
                events.add(CountersAddedEvent(targetId, com.wingedsheep.sdk.core.CounterType.MINUS_ONE_MINUS_ONE.name, amount,
                    newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Creature"))
            } else {
                val existingDamage = newState.getEntity(targetId)?.get<DamageComponent>()
                val currentDamage = existingDamage?.amount ?: 0
                val hasDeathtouch = projected.hasKeyword(sourceId, Keyword.DEATHTOUCH)
                newState = newState.updateEntity(targetId) { container ->
                    container.with(DamageComponent(
                        amount = currentDamage + amount,
                        deathtouchDamageReceived = hasDeathtouch || (existingDamage?.deathtouchDamageReceived == true)
                    ))
                }
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

    /**
     * Toxic N total — printed and granted flow through the same projected `TOXIC_<n>`
     * keyword form (see [com.wingedsheep.engine.state.components.identity.ToxicComponent]
     * + [com.wingedsheep.engine.mechanics.layers.StateProjector]). Sums per-instance counts
     * across all `TOXIC_<n>` strings. A bare `TOXIC` keyword without a count contributes
     * zero, by design — only `KeywordAbility.Numeric(Keyword.TOXIC, n)` (or a granted toxic effect) grants
     * combat poison.
     */
    private fun getToxicAmount(state: GameState, projected: ProjectedState, sourceId: EntityId): Int {
        if (sourceId !in state.getBattlefield()) return 0
        val sourceContainer = state.getEntity(sourceId) ?: return 0
        if (sourceContainer.has<FaceDownComponent>()) return 0

        return projected.getKeywords(sourceId).sumOf { parseToxic(it) }
    }

    private fun parseToxic(keyword: String): Int {
        if (!keyword.startsWith("TOXIC_")) return 0
        return keyword.removePrefix("TOXIC_").toIntOrNull() ?: 0
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

        // Commander damage (CR 903.10a) — reflection still counts as combat damage from the commander
        newState = accumulateCommanderDamage(newState, sourceId, attackerController, originalAmount)

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

            val attackerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
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
                        val protectedFromOpponent = !damageCantBePrevented &&
                            projected.hasKeyword(targetId, "PROTECTION_FROM_EACH_OPPONENT") &&
                            run {
                                val srcController = projected.getController(attackerId)
                                val tgtController = projected.getController(targetId)
                                srcController != null && tgtController != null && srcController != tgtController
                            }
                        val blockerProtected = !damageCantBePrevented && (attackerColors.any {
                            projected.hasKeyword(targetId, "PROTECTION_FROM_$it")
                        } || attackerSubtypes.any {
                            projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${it.uppercase()}")
                        }) || protectedFromOpponent
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
            newState = DamageUtils.markLifeGainedThisTurn(newState, controllerId, totalDamage)
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
                predicateEvaluator.matches(
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

    /**
     * Accumulate post-prevention combat damage onto `state.commanderDamage` when [sourceId] is a
     * non-token commander dealing combat damage to a player (CR 903.10a). Token copies are NOT
     * the commander and never carry [CommanderComponent], so the [TokenComponent] guard is a
     * defense-in-depth check rather than a substantive gate. Returns [state] unchanged when the
     * source isn't a commander or the amount is non-positive.
     */
    private fun accumulateCommanderDamage(
        state: GameState,
        sourceId: EntityId,
        targetPlayerId: EntityId,
        amount: Int,
    ): GameState {
        if (amount <= 0) return state
        val container = state.getEntity(sourceId) ?: return state
        if (container.has<TokenComponent>()) return state
        container.get<CommanderComponent>() ?: return state
        return state.recordCommanderDamage(sourceId, targetPlayerId, amount)
    }

    // =========================================================================
    // Combat Resolution Board (Phase 1)
    //
    // Builds and pauses on a [CombatResolutionDecision] — the bipartite
    // attacker/blocker/defender graph that replaces the per-attacker
    // [CombatDamagePlanDecision] when [EngineFeatures.combatResolutionBoardEnabled]
    // is set. Scope-wise this Phase 1 emitter mirrors the legacy emitter: it
    // surfaces only attacker→target edges (blockers + trample drains) for the
    // single chooser group the legacy path would have emitted. Phase 2 will add
    // blocker→attacker edges and merge multiple chooser groups into a single
    // decision for the banding two-actor flow.
    // =========================================================================

    /**
     * Build and pause on a [CombatResolutionDecision] covering every attacker in
     * [allCandidates] (across all choosers, for the banding two-actor case).
     *
     * The primary chooser is `choosersInOrder.first()` and gets prompted first;
     * remaining choosers queue up on the continuation's `pendingChoosers`. Each
     * edge's `editableBy` matches the candidate's chooser. When a subsequent
     * chooser is prompted (after the primary submits), [previousAmounts] carries
     * the already-confirmed values so the new decision shows them baked in.
     */
    private fun emitCombatResolutionDecision(
        state: GameState,
        projected: ProjectedState,
        choosersInOrder: List<EntityId>,
        allCandidates: List<PlanCandidate>,
        firstStrike: Boolean,
        previousAmounts: Map<String, Int>,
    ): ExecutionResult {
        val chooser = choosersInOrder.first()
        val group = allCandidates
        val blockerIds = group.flatMap { it.liveBlockers }.toSet()
        val defenderIds = group.map { it.attackingComponent.defenderId }.toSet()

        val attackers = group.map { c ->
            val attackerEntity = state.getEntity(c.attackerId)
            val attackerCard = attackerEntity?.get<CardComponent>()
            ResolutionAttacker(
                id = c.attackerId,
                name = c.attackerName,
                power = projected.getPower(c.attackerId) ?: c.availablePower,
                toughness = projected.getToughness(c.attackerId) ?: 0,
                hasTrample = c.hasTrample,
                hasTrampleOverPlaneswalkers = false,
                hasDeathtouch = c.hasDeathtouch,
                hasFirstStrike = projected.hasKeyword(c.attackerId, Keyword.FIRST_STRIKE),
                hasDoubleStrike = projected.hasKeyword(c.attackerId, Keyword.DOUBLE_STRIKE),
                dealsDamageThisStep = true,
                bandId = c.attackingComponent.bandId,
                attackedDefenderId = c.attackingComponent.defenderId,
                blockedByIds = c.liveBlockers,
                markedDamage = attackerEntity?.get<DamageComponent>()?.amount ?: 0,
            )
        }

        val blockers = blockerIds.mapNotNull { blockerId ->
            val container = state.getEntity(blockerId) ?: return@mapNotNull null
            val card = container.get<CardComponent>() ?: return@mapNotNull null
            val blocking = container.get<BlockingComponent>()
            ResolutionBlocker(
                id = blockerId,
                name = card.name,
                power = projected.getPower(blockerId) ?: 0,
                toughness = projected.getToughness(blockerId) ?: 0,
                hasDeathtouch = projected.hasKeyword(blockerId, Keyword.DEATHTOUCH),
                hasFirstStrike = projected.hasKeyword(blockerId, Keyword.FIRST_STRIKE),
                hasDoubleStrike = projected.hasKeyword(blockerId, Keyword.DOUBLE_STRIKE),
                dealsDamageThisStep = true,
                blockedAttackerIds = blocking?.blockedAttackerIds.orEmpty(),
                orderedAttackers = container.get<AttackerOrderComponent>()?.orderedAttackers
                    ?: blocking?.blockedAttackerIds.orEmpty(),
                markedDamage = container.get<DamageComponent>()?.amount ?: 0,
            )
        }

        val defenders = defenderIds.mapNotNull { defenderId ->
            val container = state.getEntity(defenderId) ?: return@mapNotNull null
            val isPlayer = container.get<LifeTotalComponent>() != null &&
                container.get<CardComponent>() == null
            when {
                isPlayer -> ResolutionDefender(
                    id = defenderId,
                    kind = ResolutionTargetKind.PLAYER,
                    name = "Player",
                    lifeOrLoyaltyOrDefense = container.get<LifeTotalComponent>()?.life,
                )
                projected.isPlaneswalker(defenderId) -> ResolutionDefender(
                    id = defenderId,
                    kind = ResolutionTargetKind.PLANESWALKER,
                    name = container.get<CardComponent>()?.name ?: "Planeswalker",
                    lifeOrLoyaltyOrDefense = container.get<CountersComponent>()
                        ?.getCount(CounterType.LOYALTY),
                )
                else -> ResolutionDefender(
                    id = defenderId,
                    kind = ResolutionTargetKind.BATTLE,
                    name = container.get<CardComponent>()?.name ?: "Battle",
                    lifeOrLoyaltyOrDefense = null,
                )
            }
        }

        val edges = mutableListOf<DamageEdge>()
        for (c in group) {
            var unlockOrder = 0
            // Free-assignment attackers (AssignCombatDamageAsUnblocked / DivideCombatDamageFreely)
            // bypass lethal-first: blocker edges have minimum=0 and a non-trample defender drain
            // is always present, so the player can drag damage anywhere within the power budget.
            val isFreeAssignment = c.hasAssignAsUnblocked || c.hasDivideDamageFreely
            val bypassLethalFirst = isFreeAssignment || c.bandingBypassesOrder
            for (blockerId in c.liveBlockers) {
                val default = if (c.hasAssignAsUnblocked) {
                    // Default for AssignAsUnblocked: dump everything on defender, nothing on blockers.
                    0
                } else if (c.hasDivideDamageFreely) {
                    // Default for DivideDamageFreely: even split across blockers + defender.
                    c.availablePower / (c.liveBlockers.size + 1)
                } else {
                    c.defaultAssignments[blockerId] ?: 0
                }
                // lethalThreshold drives the validator's CR 510.1d relational check.
                // null = this edge bypasses damage-assignment order (free assignment,
                // banding). Otherwise the validator enforces "later blocker can't
                // receive damage until this one has at least lethalThreshold marked".
                // Per-edge `minimum` is always 0 — the rule is relational, not
                // per-edge: an attacker whose power is below the first blocker's
                // lethal must still be able to assign its full power to that blocker.
                // The threshold must reflect the blocker's actual lethal need (not
                // the power-capped `minimumAssignments`), or relational gating would
                // pass on under-lethal assignments.
                val lethal = if (bypassLethalFirst) {
                    null
                } else if (c.hasDeathtouch) {
                    1
                } else {
                    damageCalculator
                        .calculateLethalDamage(state, blockerId, c.attackerId)
                        .lethalAmount
                        .coerceAtLeast(1)
                }
                edges.add(
                    DamageEdge(
                        id = "${c.attackerId}->${blockerId}",
                        sourceId = c.attackerId,
                        targetId = blockerId,
                        direction = DamageEdgeDirection.ATTACKER_TO_BLOCKER,
                        amount = default,
                        minimum = 0,
                        maximum = c.availablePower,
                        isTrampleDrain = false,
                        lethalThreshold = lethal,
                        editableBy = c.chooser,
                        unlockOrder = unlockOrder++,
                    )
                )
            }
            // Add defender drain when the attacker can route damage there: trample (lethal-first
            // gated), or free-assignment (no gating). Free-assignment defaults pre-fill the
            // drain with the full power for AssignAsUnblocked or the even-split remainder for
            // DivideDamageFreely.
            if (c.hasTrample || isFreeAssignment) {
                val defenderId = c.attackingComponent.defenderId
                val defenderContainer = state.getEntity(defenderId)
                val defenderIsPlayer = defenderContainer?.get<LifeTotalComponent>() != null &&
                    defenderContainer.get<CardComponent>() == null
                val direction = when {
                    defenderIsPlayer -> DamageEdgeDirection.ATTACKER_TO_PLAYER
                    projected.isPlaneswalker(defenderId) -> DamageEdgeDirection.ATTACKER_TO_PLANESWALKER
                    else -> DamageEdgeDirection.ATTACKER_TO_BATTLE
                }
                val drainDefault = when {
                    c.hasAssignAsUnblocked -> c.availablePower
                    c.hasDivideDamageFreely -> c.availablePower - (c.availablePower / (c.liveBlockers.size + 1)) * c.liveBlockers.size
                    else -> c.defaultAssignments[defenderId] ?: 0
                }
                edges.add(
                    DamageEdge(
                        id = "${c.attackerId}->${defenderId}",
                        sourceId = c.attackerId,
                        targetId = defenderId,
                        direction = direction,
                        amount = drainDefault,
                        minimum = 0,
                        maximum = c.availablePower,
                        // isTrampleDrain governs lethal-first gating in the validator. Free
                        // assignment is allowed to drain regardless of blocker lethality.
                        isTrampleDrain = c.hasTrample && !isFreeAssignment,
                        lethalThreshold = null,
                        editableBy = c.chooser,
                        unlockOrder = unlockOrder++,
                    )
                )
            }
        }

        // Blocker→attacker edges (CR 510.1d): surface the reverse-direction graph for any
        // blocker in `blockers` that blocks 2+ attackers. Pre-fill via
        // `calculateBlockerDamageDistribution` (already lethal-first / pending-damage aware).
        // editableBy follows CR 702.22k: defaults to the blocker's controller; flipped to the
        // active player when any blocked attacker has banding.
        //
        // Walk blockers in a deterministic order (matching the order of the `blockers` list
        // built above) and feed each blocker's defaults through a running pendingDamage map
        // so successive blockers see the damage already going at each attacker (mirrors what
        // `proposeDamageAssignments` does at apply time).
        val activePlayerId = state.activePlayerId
        val pendingForBlockerEdges = mutableMapOf<EntityId, Int>()
        // Seed pendingDamage with the attacker→blocker side we just emitted, so blockers can
        // see what attackers are already taking from each other (e.g. Ironfist crossover).
        for (edge in edges) {
            if (edge.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER) {
                pendingForBlockerEdges[edge.targetId] =
                    (pendingForBlockerEdges[edge.targetId] ?: 0) + edge.amount
            }
        }
        for (blocker in blockers) {
            if (blocker.blockedAttackerIds.size < 2) continue
            val blockerContainer = state.getEntity(blocker.id) ?: continue
            val defaultChooser = projected.getController(blocker.id) ?: continue
            val chooser = CombatDamageUtils.blockerDamageAssignmentChooser(
                state, projected, blocker.id, defaultChooser, activePlayerId ?: defaultChooser
            )
            // CR 702.22k: when any attacker this blocker is blocking has banding, the active
            // player chooses the division *and* can ignore the damage-assignment order. Same
            // condition that flips the chooser, so detect via `chooser != defaultChooser`.
            val bandingBypassesOrder = chooser != defaultChooser
            val orderedTargets = blockerContainer
                .get<AttackerOrderComponent>()
                ?.orderedAttackers
                ?: blocker.blockedAttackerIds
            val liveOrdered = orderedTargets.filter { it in state.getBattlefield() }
            if (liveOrdered.isEmpty()) continue
            val blockerPower = blocker.power
            if (blockerPower <= 0) continue
            val defaults = damageCalculator.calculateBlockerDamageDistribution(
                state, blocker.id, pendingForBlockerEdges
            ).assignments
            var blockerUnlockOrder = 0
            for (attackerId in liveOrdered) {
                val default = defaults[attackerId] ?: 0
                val lethalInfo = damageCalculator.calculateLethalDamage(state, attackerId, blocker.id)
                // Same model as attacker→blocker: minimum is always 0, the
                // CR 510.1d order constraint is enforced relationally in the
                // validator via lethalThreshold. Banding (CR 702.22k) lets the
                // chooser ignore order → signal by lethalThreshold = null.
                val lethal = if (bandingBypassesOrder) {
                    null
                } else if (blocker.hasDeathtouch) {
                    1
                } else {
                    lethalInfo.lethalAmount
                }
                edges.add(
                    DamageEdge(
                        id = "${blocker.id}->${attackerId}",
                        sourceId = blocker.id,
                        targetId = attackerId,
                        direction = DamageEdgeDirection.BLOCKER_TO_ATTACKER,
                        amount = default,
                        minimum = 0,
                        maximum = blockerPower,
                        isTrampleDrain = false,
                        lethalThreshold = lethal,
                        editableBy = chooser,
                        unlockOrder = blockerUnlockOrder++,
                    )
                )
                pendingForBlockerEdges[attackerId] =
                    (pendingForBlockerEdges[attackerId] ?: 0) + default
            }
        }

        // Bake previously-confirmed amounts into the edges. Empty on the primary
        // chooser's prompt; populated when re-emitting for a co-chooser so the prior
        // choice is shown as the current edge amount.
        val finalEdges = if (previousAmounts.isEmpty()) edges else edges.map { edge ->
            previousAmounts[edge.id]?.let { edge.copy(amount = it) } ?: edge
        }

        val decisionId = UUID.randomUUID().toString()
        val prompt = if (group.size == 1) {
            "Assign ${group[0].attackerName}'s ${group[0].availablePower} combat damage"
        } else {
            "Assign combat damage for ${group.size} attackers"
        }
        val coChooserId = choosersInOrder.drop(1).firstOrNull()
        val decision = CombatResolutionDecision(
            id = decisionId,
            playerId = chooser,
            prompt = prompt,
            context = DecisionContext(
                sourceId = group.firstOrNull()?.attackerId,
                sourceName = if (group.size == 1) group[0].attackerName else "Combat damage",
                phase = DecisionPhase.COMBAT,
            ),
            firstStrike = firstStrike,
            attackers = attackers,
            blockers = blockers,
            defenders = defenders,
            edges = finalEdges,
            coChooserId = coChooserId,
        )
        val continuation = CombatResolutionContinuation(
            decisionId = decisionId,
            firstStrike = firstStrike,
            pendingChoosers = choosersInOrder,
            decisionShape = decision,
        )
        val pausedState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)
        return ExecutionResult.paused(pausedState, decision)
    }

    /**
     * Re-pause the same logical combat-damage step with a new chooser. Used by the
     * resumer when the two-actor banding flow needs to hand off from one chooser to
     * the next without re-running the pre-check or recomputing the graph.
     *
     * Returns a paused [ExecutionResult] carrying the updated decision (same
     * attackers/blockers/defenders/edges but with the next chooser as
     * [PendingDecision.playerId] and the prior chooser's amounts baked in).
     */
    internal fun repauseCombatResolution(
        state: GameState,
        previous: CombatResolutionDecision,
        remainingChoosers: List<EntityId>,
        latestAmounts: Map<String, Int>,
        firstStrike: Boolean,
    ): ExecutionResult {
        val nextChooser = remainingChoosers.first()
        val nextCoChooser = remainingChoosers.drop(1).firstOrNull()
        val updatedEdges = previous.edges.map { edge ->
            latestAmounts[edge.id]?.let { edge.copy(amount = it) } ?: edge
        }
        val decisionId = UUID.randomUUID().toString()
        val newDecision = previous.copy(
            id = decisionId,
            playerId = nextChooser,
            coChooserId = nextCoChooser,
            edges = updatedEdges,
        )
        val continuation = CombatResolutionContinuation(
            decisionId = decisionId,
            firstStrike = firstStrike,
            pendingChoosers = remainingChoosers,
            decisionShape = newDecision,
        )
        val pausedState = state
            .withPendingDecision(newDecision)
            .pushContinuation(continuation)
        return ExecutionResult.paused(pausedState, newDecision)
    }
}
