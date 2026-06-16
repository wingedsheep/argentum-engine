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

            val attackerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
            if (attackerPower <= 0) continue

            if (attackerContainer.get<DamageAssignmentComponent>() != null) continue

            val blockedBy = attackerContainer.get<BlockedComponent>()
            val defenderId = attackingComponent.defenderId

            // CR 509.1h / 510.1c: a creature that was blocked but has no creatures still blocking
            // it as the combat damage step begins assigns no combat damage (Butcher Orgg has no
            // trample). The BlockedComponent persists with its blocker list cleared once the
            // blockers leave, so "blocked with no live blocker" is distinct from "never blocked"
            // (blockedBy == null). In that case the divide-freely ability never applies, so skip
            // the decision entirely and let the normal pipeline assign nothing.
            if (blockedBy != null) {
                val liveBlockers = blockedBy.blockerIds.filter { it in state.getBattlefield() }
                if (liveBlockers.isEmpty()) {
                    continue
                }
            }

            // Otherwise (unblocked, or blocked by at least one live creature) the damage may be
            // divided freely among the defending player and ANY number of creatures they control —
            // not just the creatures blocking Butcher Orgg.
            val targets = mutableListOf<EntityId>()
            val defendingCreatures = state.getBattlefield().filter { entityId ->
                projected.getController(entityId) == defenderId && projected.isCreature(entityId)
            }
            targets.addAll(defendingCreatures)
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

        // Pre-check: bundle every attacker that needs a manual damage-assignment choice into a
        // single CombatResolutionDecision (the bipartite combat-damage board). This replaces the
        // old per-attacker AssignDamageDecision chain and the standalone blocker/attacker ordering
        // pre-step. An attacker enters the board when it has a real division choice — trample, or
        // multiple blockers with excess power (CR 510.1c) — or when banding/bipartite blockers
        // mean another player must choose part of the graph (CR 702.22j/k).
        val boardResult = checkCombatResolutionBoard(state, projected, attackers, firstStrike)
        if (boardResult != null) return boardResult

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
    // Combat resolution board (CR 510 / 702.22)
    //
    // Builds and pauses on a single [CombatResolutionDecision] — the bipartite
    // attacker/blocker/defender damage graph — covering every attacker that needs a
    // manual damage-assignment choice this step, plus the blocker→attacker edges for
    // any blocker that blocks two or more attackers. Banding flips edge ownership and
    // lifts ordering via [CombatDamageUtils.combatDamageChooser] (CR 702.22j/k).
    // =========================================================================

    /** One attacker's contribution to the board: its blockers, defaults, chooser, and ordering. */
    private data class PlanCandidate(
        val attackerId: EntityId,
        val attackerName: String,
        val attackingComponent: AttackingComponent,
        val chooser: EntityId,
        val orderConstrained: Boolean,
        val availablePower: Int,
        val liveBlockers: List<EntityId>,
        val hasTrample: Boolean,
    )

    /** Stable wire id for a damage edge. The resumer reads source/target off the edge, never parses this. */
    private fun edgeId(sourceId: EntityId, targetId: EntityId): String = "$sourceId->$targetId"

    /**
     * Gather the attackers that need a manual damage-assignment choice and, if any, pause on a
     * single [CombatResolutionDecision]. Returns null when no board is needed (combat resolves
     * automatically through the damage pipeline).
     *
     * An attacker is pulled onto the board when:
     * - it genuinely has a division choice ([DamageCalculator.requiresManualAssignment] — trample,
     *   or multiple blockers with excess power), or
     * - it has 2+ blockers and one of them has banding, so the defending player must divide the
     *   damage (CR 702.22j), or
     * - one of its blockers also blocks another attacker (a bipartite blocker), so that blocker's
     *   own damage division (CR 510.1c / 702.22k) needs to surface even if no single attacker
     *   would otherwise require a manual choice.
     */
    private fun checkCombatResolutionBoard(
        state: GameState,
        projected: ProjectedState,
        attackers: List<Pair<EntityId, AttackingComponent>>,
        firstStrike: Boolean,
    ): ExecutionResult? {
        val battlefield = state.getBattlefield()

        // Attackers whose blockers include one that blocks 2+ attackers (an Ironfist-style
        // bipartite). Without this pull the blocker's division would silently auto-resolve.
        val attackersWithBipartiteBlocker: Set<EntityId> = buildSet {
            for ((attackerId, _) in attackers) {
                val blockerIds = state.getEntity(attackerId)?.get<BlockedComponent>()
                    ?.blockerIds.orEmpty().filter { it in battlefield }
                val anyBipartite = blockerIds.any { blockerId ->
                    (state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds
                        .orEmpty().count { it in battlefield }) >= 2
                }
                if (anyBipartite) add(attackerId)
            }
        }

        val candidates = mutableListOf<PlanCandidate>()
        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in battlefield) continue
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue

            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId)
            // DivideCombatDamageFreely (Butcher Orgg) keeps its own DistributeDecision pre-check.
            if (cardDef?.staticAbilities?.any { it is DivideCombatDamageFreely } == true) continue
            // AssignAsUnblocked, once answered, has written a DamageAssignmentComponent → skip.
            if (attackerContainer.get<DamageAssignmentComponent>() != null) continue
            if (!dealsDamageThisStep(projected, attackerId, firstStrike)) continue

            val blockedBy = attackerContainer.get<BlockedComponent>()
            if (blockedBy == null || blockedBy.blockerIds.isEmpty()) continue
            val liveBlockerIds = blockedBy.blockerIds.filter { it in battlefield }
            if (liveBlockerIds.isEmpty()) continue

            // CR 702.22j: a banding blocker hands the division to the defender even at exact
            // lethal (where requiresManualAssignment would normally say "no choice").
            val bandingOverride = liveBlockerIds.size >= 2 &&
                liveBlockerIds.any { projected.hasKeyword(it, Keyword.BANDING) }
            val bipartitePull = attackerId in attackersWithBipartiteBlocker
            if (!damageCalculator.requiresManualAssignment(state, attackerId) &&
                !bandingOverride && !bipartitePull) continue

            val attackerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
            if (attackerPower <= 0) continue

            val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
                ?: blockedBy.blockerIds
            val liveBlockers = orderedBlockers.filter { it in battlefield }
            if (liveBlockers.isEmpty()) continue

            val attackingPlayer = projected.getController(attackerId) ?: continue
            val chooser = CombatDamageUtils.combatDamageChooser(
                state, projected, attackerId, CombatDamageUtils.CombatSide.ATTACKER,
                defaultChooser = attackingPlayer, activePlayerId = state.activePlayerId ?: attackingPlayer,
            )

            candidates.add(
                PlanCandidate(
                    attackerId = attackerId,
                    attackerName = attackerCard.name,
                    attackingComponent = attackingComponent,
                    chooser = chooser.playerId,
                    orderConstrained = chooser.orderConstrained,
                    availablePower = attackerPower,
                    liveBlockers = liveBlockers,
                    hasTrample = projected.hasKeyword(attackerId, Keyword.TRAMPLE),
                )
            )
        }

        if (candidates.isEmpty()) return null
        return emitCombatResolutionDecision(state, projected, candidates, firstStrike)
    }

    private fun emitCombatResolutionDecision(
        state: GameState,
        projected: ProjectedState,
        candidates: List<PlanCandidate>,
        firstStrike: Boolean,
    ): ExecutionResult {
        val battlefield = state.getBattlefield()
        val activePlayerId = state.activePlayerId ?: candidates.first().chooser
        val blockerIds = candidates.flatMap { it.liveBlockers }.toSet()
        val defenderIds = candidates.map { it.attackingComponent.defenderId }.toSet()

        val attackerNodes = candidates.map { c ->
            val container = state.getEntity(c.attackerId)
            ResolutionAttacker(
                id = c.attackerId,
                name = c.attackerName,
                power = projected.getPower(c.attackerId) ?: c.availablePower,
                toughness = projected.getToughness(c.attackerId) ?: 0,
                hasTrample = c.hasTrample,
                hasDeathtouch = projected.hasKeyword(c.attackerId, Keyword.DEATHTOUCH),
                hasFirstStrike = projected.hasKeyword(c.attackerId, Keyword.FIRST_STRIKE),
                hasDoubleStrike = projected.hasKeyword(c.attackerId, Keyword.DOUBLE_STRIKE),
                dealsDamageThisStep = true,
                bandId = c.attackingComponent.bandId,
                attackedDefenderId = c.attackingComponent.defenderId,
                blockedByIds = c.liveBlockers,
                markedDamage = container?.get<DamageComponent>()?.amount ?: 0,
            )
        }

        val blockerNodes = blockerIds.mapNotNull { blockerId ->
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
                dealsDamageThisStep = dealsDamageThisStep(projected, blockerId, firstStrike),
                blockedAttackerIds = blocking?.blockedAttackerIds.orEmpty(),
                orderedAttackers = container.get<AttackerOrderComponent>()?.orderedAttackers
                    ?: blocking?.blockedAttackerIds.orEmpty(),
                markedDamage = container.get<DamageComponent>()?.amount ?: 0,
            )
        }

        val defenderNodes = defenderIds.mapNotNull { defenderId ->
            val container = state.getEntity(defenderId) ?: return@mapNotNull null
            val isPlayer = container.get<LifeTotalComponent>() != null && container.get<CardComponent>() == null
            when {
                isPlayer -> ResolutionDefender(defenderId, ResolutionTargetKind.PLAYER, "Player",
                    state.lifeTotal(defenderId)) // CR 810.9a — team's shared total
                projected.isPlaneswalker(defenderId) -> ResolutionDefender(defenderId, ResolutionTargetKind.PLANESWALKER,
                    container.get<CardComponent>()?.name ?: "Planeswalker",
                    container.get<CountersComponent>()?.getCount(CounterType.LOYALTY))
                else -> ResolutionDefender(defenderId, ResolutionTargetKind.BATTLE,
                    container.get<CardComponent>()?.name ?: "Battle", null)
            }
        }

        val edges = mutableListOf<DamageEdge>()

        // Attacker -> blocker edges, plus a trample drain to the defender. Defaults come from the
        // lethal-first auto distribution (CR 510.1c order); the player may re-divide within budget.
        for (c in candidates) {
            val auto = damageCalculator.calculateAutoDamageDistribution(state, c.attackerId).assignments
            for (blockerId in c.liveBlockers) {
                edges.add(
                    DamageEdge(
                        id = edgeId(c.attackerId, blockerId),
                        sourceId = c.attackerId,
                        targetId = blockerId,
                        direction = DamageEdgeDirection.ATTACKER_TO_BLOCKER,
                        amount = auto[blockerId] ?: 0,
                        maximum = c.availablePower,
                        lethal = damageCalculator.calculateLethalDamage(state, blockerId, c.attackerId)
                            .lethalAmount.coerceAtLeast(1),
                        orderConstrained = c.orderConstrained,
                        isTrampleDrain = false,
                        editableBy = c.chooser,
                    )
                )
            }
            if (c.hasTrample) {
                val defenderId = c.attackingComponent.defenderId
                val container = state.getEntity(defenderId)
                val isPlayer = container?.get<LifeTotalComponent>() != null && container.get<CardComponent>() == null
                val direction = when {
                    isPlayer -> DamageEdgeDirection.ATTACKER_TO_PLAYER
                    projected.isPlaneswalker(defenderId) -> DamageEdgeDirection.ATTACKER_TO_PLANESWALKER
                    else -> DamageEdgeDirection.ATTACKER_TO_BATTLE
                }
                edges.add(
                    DamageEdge(
                        id = edgeId(c.attackerId, defenderId),
                        sourceId = c.attackerId,
                        targetId = defenderId,
                        direction = direction,
                        amount = auto[defenderId] ?: 0,
                        maximum = c.availablePower,
                        lethal = 0,
                        orderConstrained = false,
                        isTrampleDrain = true,
                        editableBy = c.chooser,
                    )
                )
            }
        }

        // Blocker -> attacker edges for any blocker that blocks 2+ attackers (CR 510.1c). Seed the
        // running pending-damage map with the attacker-side amounts so successive blockers see what
        // each attacker is already taking (Ironfist crossover).
        val pendingDamage = mutableMapOf<EntityId, Int>()
        for (edge in edges) {
            if (edge.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER) {
                pendingDamage.merge(edge.targetId, edge.amount, Int::plus)
            }
        }
        for (blocker in blockerNodes) {
            if (blocker.blockedAttackerIds.size < 2) continue
            val defaultChooser = projected.getController(blocker.id) ?: continue
            val chooser = CombatDamageUtils.combatDamageChooser(
                state, projected, blocker.id, CombatDamageUtils.CombatSide.BLOCKER,
                defaultChooser = defaultChooser, activePlayerId = activePlayerId,
            )
            val orderedTargets = blocker.orderedAttackers.filter { it in battlefield }
            if (orderedTargets.isEmpty()) continue
            val blockerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, blocker.id, cardRegistry)
            if (blockerPower <= 0) continue
            val defaults = damageCalculator.calculateBlockerDamageDistribution(state, blocker.id, pendingDamage).assignments
            for (attackerId in orderedTargets) {
                val default = defaults[attackerId] ?: 0
                edges.add(
                    DamageEdge(
                        id = edgeId(blocker.id, attackerId),
                        sourceId = blocker.id,
                        targetId = attackerId,
                        direction = DamageEdgeDirection.BLOCKER_TO_ATTACKER,
                        amount = default,
                        maximum = blockerPower,
                        lethal = damageCalculator.calculateLethalDamage(state, attackerId, blocker.id)
                            .lethalAmount.coerceAtLeast(1),
                        orderConstrained = chooser.orderConstrained,
                        isTrampleDrain = false,
                        editableBy = chooser.playerId,
                    )
                )
                pendingDamage.merge(attackerId, default, Int::plus)
            }
        }

        // CR 510.1c sequences assignment: attacker-side choosers act first, then any blocker-side
        // editor that isn't already queued. The resumer hands off to each chooser in turn.
        val attackerChoosers = candidates.map { it.chooser }.distinct()
        val blockerChoosers = edges
            .filter { it.direction == DamageEdgeDirection.BLOCKER_TO_ATTACKER }
            .map { it.editableBy }
            .distinct()
            .filterNot { it in attackerChoosers }
        val choosers = attackerChoosers + blockerChoosers

        val decisionId = UUID.randomUUID().toString()
        val prompt = if (candidates.size == 1) {
            "Assign ${candidates[0].attackerName}'s ${candidates[0].availablePower} combat damage"
        } else {
            "Assign combat damage for ${candidates.size} attackers"
        }
        val decision = CombatResolutionDecision(
            id = decisionId,
            playerId = choosers.first(),
            prompt = prompt,
            context = DecisionContext(
                sourceId = candidates.firstOrNull()?.attackerId,
                sourceName = if (candidates.size == 1) candidates[0].attackerName else "Combat damage",
                phase = DecisionPhase.COMBAT,
            ),
            firstStrike = firstStrike,
            attackers = attackerNodes,
            blockers = blockerNodes,
            defenders = defenderNodes,
            edges = edges,
            coChooserId = choosers.getOrNull(1),
        )
        val continuation = CombatResolutionContinuation(
            decisionId = decisionId,
            firstStrike = firstStrike,
            pendingChoosers = choosers,
            decisionShape = decision,
        )
        return ExecutionResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
        )
    }

    /**
     * Re-pause the same logical combat-damage step for the next chooser, carrying the prior
     * chooser's locked-in amounts into the edges. Used by the resumer to sequence CR 510.1c
     * (attacker side then blocker side) and the CR 702.22j/k two-actor banding flow without
     * recomputing the graph.
     */
    internal fun repauseCombatResolution(
        state: GameState,
        previous: CombatResolutionDecision,
        remainingChoosers: List<EntityId>,
        latestAmounts: Map<String, Int>,
        firstStrike: Boolean,
    ): ExecutionResult {
        val nextChooser = remainingChoosers.first()
        val updatedEdges = previous.edges.map { edge ->
            latestAmounts[edge.id]?.let { edge.copy(amount = it) } ?: edge
        }
        val decisionId = UUID.randomUUID().toString()
        val newDecision = previous.copy(
            id = decisionId,
            playerId = nextChooser,
            coChooserId = remainingChoosers.getOrNull(1),
            edges = updatedEdges,
        )
        val continuation = CombatResolutionContinuation(
            decisionId = decisionId,
            firstStrike = firstStrike,
            pendingChoosers = remainingChoosers,
            decisionShape = newDecision,
        )
        return ExecutionResult.paused(
            state.withPendingDecision(newDecision).pushContinuation(continuation),
            newDecision,
        )
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

                // Honor a manual blocker assignment (CR 510.1c): the combat resolution board's
                // resumer writes the chosen blocker→attacker amounts into the blocker's
                // DamageAssignmentComponent. Targets no longer on the battlefield are dropped,
                // mirroring the attacker-side handling above.
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

        // Deflection / reflection shields (Deflecting Palm prevents; Eye for an Eye reflects but
        // lets the damage proceed).
        when (val deflect = DamageUtils.checkDeflectDamageShield(newState, targetId, amplifiedAmount, sourceId)) {
            is com.wingedsheep.engine.handlers.effects.DeflectOutcome.Prevented -> {
                newState = deflect.result.state
                events.addAll(deflect.result.events)
                return newState
            }
            is com.wingedsheep.engine.handlers.effects.DeflectOutcome.Reflected -> {
                newState = deflect.state
                events.addAll(deflect.events)
                // Damage is not prevented — fall through and keep applying it below.
            }
            null -> {}
        }

        // "Prevent all damage from chosen source" shields (Samite Ministration)
        val preventFromSourceResult = DamageUtils.checkPreventFromSourceShield(newState, targetId, amplifiedAmount, sourceId)
        if (preventFromSourceResult != null) {
            newState = preventFromSourceResult.state
            events.addAll(preventFromSourceResult.events)
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

        // Reduce life. Per CR 120.3a, damage to a player without infect causes that
        // player to lose that much life, so life-loss replacements (Bloodletter of
        // Aclazotz) modify the life total reduction here. Lifelink and tracking still
        // see the unmodified `effectiveAmount`.
        if (newState.getEntity(targetId)?.get<LifeTotalComponent>() == null) return newState
        // CR 810.9 — combat damage applies to the team's shared life total.
        val currentLife = newState.lifeTotal(targetId)
        var lifeLossAmount = DamageUtils.applyStaticLifeLossModification(newState, targetId, effectiveAmount)
        lifeLossAmount = DamageUtils.applyLifeLossFloors(newState, targetId, currentLife, lifeLossAmount)
        val newLife = currentLife - lifeLossAmount
        newState = newState.withLifeTotal(targetId, newLife)
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
            // CR 810.9 — applies to the team's shared total (isPlayer already guards presence).
            val currentLife = newState.lifeTotal(targetId)
            val newLife = currentLife - amount
            newState = newState.withLifeTotal(targetId, newLife)
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
            // Excess damage (CR 120.4a) is only computed for the non-wither path below.
            // Wither damage (dealt as -1/-1 counters per CR 702.80a), planeswalker (above
            // loyalty), and battle (above defense) excess paths are not yet modelled.
            var excess = 0
            if (hasWither) {
                // Wither (CR 702.80): damage to creatures is dealt in the form of -1/-1 counters
                val counters = newState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
                newState = newState.updateEntity(targetId) { container ->
                    container.with(counters.withAdded(com.wingedsheep.sdk.core.CounterType.MINUS_ONE_MINUS_ONE, amount))
                }
                val (afterMark, firstThisTurn) =
                    com.wingedsheep.engine.handlers.effects.DamageUtils.recordCounterPlacement(newState, targetId)
                newState = afterMark
                events.add(CountersAddedEvent(targetId, com.wingedsheep.sdk.core.CounterType.MINUS_ONE_MINUS_ONE.name, amount,
                    newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Creature", firstThisTurn))
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
                // Excess damage (CR 120.4a): damage past lethal needed. With deathtouch any
                // damage greater than 1 is excess — lethal collapses to a flat 1 regardless
                // of marked damage (CR 120.4a refs 702.2). Trample is already removed by
                // combat damage assignment, so any damage that reaches this creature was
                // assigned to it — the leftover above lethal is excess.
                val toughness = projected.getToughness(targetId) ?: 0
                val lethalNeeded = if (hasDeathtouch) 1
                else (toughness - currentDamage).coerceAtLeast(0)
                excess = (amount - lethalNeeded).coerceAtLeast(0)
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
            // Capture the recipient's controller + creature-ness now, while it's still on the
            // battlefield. Combat-damage SBAs strip the dead creature's ControllerComponent
            // before trigger detection, so recipient-based triggers ("a creature you control /
            // an opponent controls is dealt damage") rely on this LKI to still match (CR 603.10).
            val targetControllerId = projected.getController(targetId)
            val targetWasCreature = projected.isCreature(targetId)
            events.add(DamageDealtEvent(sourceId, targetId, amount, true,
                sourceName = sourceName, targetName = targetName, targetIsPlayer = false, targetWasFaceDown = targetIsFaceDown,
                targetControllerId = targetControllerId, targetWasCreature = targetWasCreature, excessAmount = excess))
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

        if (state.getEntity(attackerController)?.get<LifeTotalComponent>() == null) return state
        // CR 810.9 — applies to the attacking player's team's shared total.
        val attackerControllerLife = state.lifeTotal(attackerController)
        val newLife = attackerControllerLife - originalAmount
        var newState = state.withLifeTotal(attackerController, newLife)
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
            // Route through the shared primitive so prevention (Sulfuric Vortex) and the
            // ModifyLifeGain pipeline (Alhammarret's Archive, Leyline of Hope) apply to combat
            // lifelink the same way they do to noncombat lifelink and direct GainLife effects.
            val (gainedState, gainEvent) = DamageUtils.gainLife(newState, controllerId, totalDamage)
            newState = gainedState
            if (gainEvent != null) lifelinkEvents.add(gainEvent)
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
}
