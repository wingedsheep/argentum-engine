package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.mechanics.combat.rules.TappedBlockBypass
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.mechanics.combat.rules.BlockCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.BlockEvasionRule
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.scripting.BlockerCountLimit
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent

/**
 * Handles the declare blockers step of combat.
 *
 * Responsibilities:
 * - Validating individual blockers (creature eligibility, evasion, can't block)
 * - Menace requirements
 * - Must-be-blocked requirements (Alluring Scent, Taunting Elf)
 * - Provoke requirements
 * - Projected must-block requirements (Grand Melee)
 * - Block taxes (Whipgrass Entangler)
 * - Blocker order decisions for multiple blockers
 * - Mandatory blocker assignment queries
 */
internal class BlockPhaseManager(
    private val cardRegistry: CardRegistry,
    private val blockEvasionRules: List<BlockEvasionRule>,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
    private val predicateEvaluator: PredicateEvaluator
) {
    private val conditionEvaluator = predicateEvaluator.conditions
    /**
     * Validate and declare blockers.
     *
     * @param blockers Map of blocker entity ID to list of attackers being blocked
     */
    fun declareBlockers(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): ExecutionResult {
        // Validate each blocker
        for ((blockerId, attackerIds) in blockers) {
            val validation = validateBlocker(state, blockingPlayer, blockerId, attackerIds)
            if (validation != null) {
                return ExecutionResult.error(state, validation)
            }
        }

        // Check menace requirements
        val menaceValidation = validateMenaceRequirements(state, blockers)
        if (menaceValidation != null) {
            return ExecutionResult.error(state, menaceValidation)
        }

        // Check "can't be blocked except by N or more creatures" (Troll of Khazad-dûm)
        val minBlockersValidation = validateMinBlockersRequirements(state, blockers)
        if (minBlockersValidation != null) {
            return ExecutionResult.error(state, minBlockersValidation)
        }

        // Check max-blocker restrictions on attackers (CantBeBlockedByMoreThan)
        val maxBlockersValidation = validateMaxBlockersRequirements(state, blockers)
        if (maxBlockersValidation != null) {
            return ExecutionResult.error(state, maxBlockersValidation)
        }

        // Check global blocker-count caps (Dueling Grounds — "No more than one creature can
        // block each combat"). Counts distinct blocking creatures across all players.
        val blockerCountValidation = validateGlobalBlockerCount(state, blockers.keys)
        if (blockerCountValidation != null) {
            return ExecutionResult.error(state, blockerCountValidation)
        }

        // Check co-blocker requirements (CR 509.1b — "can't block alone" / "can't block unless an
        // X also blocks"). Depends on the whole proposed blocker group, not the attacker, so it's
        // validated here rather than per-blocker. Mirrors the co-attacker check in declare-attackers.
        val coBlockerValidation = validateCoBlockerRequirements(state, state.projectedState, blockers.keys)
        if (coBlockerValidation != null) {
            return ExecutionResult.error(state, coBlockerValidation)
        }

        val eachRequirements = eachAttackerRequirements(state, blockingPlayer, blockers)
        if (eachRequirements != null) {
            if (eachRequirements != blockers) {
                return ExecutionResult.error(state, "Creatures must obey the maximum possible blocking requirements")
            }
        } else {
            // Check "must be blocked" requirements (Alluring Scent, etc.)
            val mustBeBlockedValidation = validateMustBeBlockedRequirements(state, blockingPlayer, blockers)
            if (mustBeBlockedValidation != null) {
                return ExecutionResult.error(state, mustBeBlockedValidation)
            }

            // Check provoke "must block specific attacker" requirements
            val provokeValidation = validateProvokeRequirements(state, blockingPlayer, blockers)
            if (provokeValidation != null) {
                return ExecutionResult.error(state, provokeValidation)
            }

            // Check projected must-block requirements (Grand Melee)
            val projectedMustBlockValidation = validateProjectedMustBlockRequirements(state, blockingPlayer, blockers)
            if (projectedMustBlockValidation != null) {
                return ExecutionResult.error(state, projectedMustBlockValidation)
            }
        }

        // Calculate (but don't pay) the block tax. If non-zero, pause for the blocking
        // player to confirm — same reasoning as attack taxes: don't tap their mana
        // without consent.
        val projected = state.projectedState
        val totalBlockTax = CombatTaxes.blockTax(state, cardRegistry, blockers.keys, projected, predicateEvaluator = predicateEvaluator)
        if (totalBlockTax > 0) {
            return pauseForBlockTaxConfirmation(state, blockingPlayer, blockers, totalBlockTax)
        }

        return commitBlockDeclaration(state, blockingPlayer, blockers, taxEvents = emptyList())
    }

    fun beginBlockerPiles(state: GameState, blockingPlayer: EntityId): ExecutionResult {
        val attackers = state.getBattlefield().filter { id ->
            val attack = state.getEntity(id)?.get<AttackingComponent>() ?: return@filter false
            CombatDefenders.defendingPlayerOf(state, attack, state.projectedState) in state.sharedTurnTeam(blockingPlayer)
        }
        val projected = state.projectedState
        val creatures = state.getBattlefield().filter {
            projected.isCreature(it) && projected.getController(it) == blockingPlayer
        }
        // Nothing to divide: declare "no blocks" rather than ask an empty question.
        if (attackers.isEmpty() || creatures.isEmpty()) return commitBlockDeclaration(state, blockingPlayer, emptyMap(), emptyList())
        val capacities = creatures.associateWith { blocker -> maxPileMemberships(state, blocker, attackers.size) }
        return state.suspendForDecision(
            question = { id -> SplitPilesDecision(
                id = id,
                playerId = CombatDeclarationControl.declarerFor(state, blockingPlayer) ?: blockingPlayer,
                prompt = "Choose creatures for each pile. Piles are assigned to attackers at random; empty piles are allowed.",
                context = DecisionContext(sourceId = null, sourceName = "Blocker piles", phase = DecisionPhase.COMBAT),
                cards = creatures,
                numberOfPiles = attackers.size,
                allowUnassigned = true,
                maxPileMemberships = capacities,
                useTargetingUI = true,
            ) },
            answer = BlockerPilesContinuation(blockingPlayer, attackers),
        )
    }

    private fun maxPileMemberships(state: GameState, blocker: EntityId, pileCount: Int): Int {
        return minOf(pileCount, BlockStaticRules(state, cardRegistry, predicateEvaluator).maxBlocks(blocker))
    }

    fun resolveBlockerPiles(state: GameState, continuation: BlockerPilesContinuation, response: PilesSplitResponse): ExecutionResult {
        // Shuffle after the response is validated, so an invalid submission cannot reroll combat.
        val (attackers, randomized) = state.nextRandom { shuffle(continuation.attackers) }
        val candidates = linkedMapOf<EntityId, MutableList<EntityId>>()
        for ((index, pile) in response.piles.withIndex()) {
            val attacker = attackers[index]
            for (blocker in pile) {
                if (validateBlocker(randomized, continuation.blockingPlayer, blocker, listOf(attacker)) == null) {
                    candidates.getOrPut(blocker) { mutableListOf() }.add(attacker)
                }
            }
        }
        val blocks = candidates.mapValues { it.value.toList() }
        if (pileRestrictionsSatisfied(randomized, blocks)) return commitBlockDeclaration(randomized, continuation.blockingPlayer, blocks, emptyList())
        val suggested = maximalLegalPileBlocks(randomized, blocks)
        val count = suggested.values.sumOf { it.size }
        if (count == 0) return commitBlockDeclaration(randomized, continuation.blockingPlayer, emptyMap(), emptyList())
        val pileOptions = attackers.mapIndexed { index, attacker -> index to blocks.filterValues { attacker in it }.keys.toList() }.toMap()
        return randomized.suspendForDecision(
            question = { id -> SplitPilesDecision(
                id = id,
                playerId = CombatDeclarationControl.declarerFor(randomized, continuation.blockingPlayer) ?: continuation.blockingPlayer,
                prompt = "Choose $count blocks from the assigned piles. Blocking restrictions prevent all chosen creatures from blocking.",
                context = DecisionContext(sourceId = null, sourceName = "Blocker piles", phase = DecisionPhase.COMBAT),
                cards = blocks.keys.toList(),
                numberOfPiles = attackers.size,
                allowUnassigned = true,
                maxPileMemberships = blocks.mapValues { it.value.size },
                useTargetingUI = true,
                pileLabels = attackers.map { com.wingedsheep.engine.state.nameVisibleToAll(randomized, it,
                    randomized.getEntity(it)?.get<CardComponent>()?.name ?: "Attacker") },
                pileOptions = pileOptions,
                requiredAssignments = count,
                suggestedPiles = attackers.map { attacker -> suggested.filterValues { attacker in it }.keys.toList() },
            ) },
            answer = BlockerPileRestrictionChoiceContinuation(continuation.blockingPlayer, attackers, blocks, count),
        )
    }

    fun resolvePileRestrictions(state: GameState, continuation: BlockerPileRestrictionChoiceContinuation, response: PilesSplitResponse): ExecutionResult {
        val blocks = linkedMapOf<EntityId, MutableList<EntityId>>()
        for ((index, pile) in response.piles.withIndex()) {
            val attacker = continuation.attackers[index]
            for (blocker in pile) {
                if (attacker !in continuation.candidates[blocker].orEmpty()) return ExecutionResult.error(state, "Block was not assigned by the random piles")
                blocks.getOrPut(blocker) { mutableListOf() }.add(attacker)
            }
        }
        if (blocks.values.sumOf { it.size } != continuation.assignmentCount || !pileRestrictionsSatisfied(state, blocks))
            return ExecutionResult.error(state, "Choose the required number of legal blocks")
        return commitBlockDeclaration(state, continuation.blockingPlayer, blocks, emptyList())
    }

    /** Restrictions still apply to "can block"; declaration requirements and costs do not. */
    private fun pileRestrictionsSatisfied(state: GameState, blocks: Map<EntityId, List<EntityId>>): Boolean =
        validateMenaceRequirements(state, blocks) == null &&
            validateMinBlockersRequirements(state, blocks) == null &&
            validateMaxBlockersRequirements(state, blocks) == null &&
            validateGlobalBlockerCount(state, blocks.keys) == null &&
            validateCoBlockerRequirements(state, state.projectedState, blocks.keys) == null

    private fun removeImpossibleBlockEdges(state: GameState, candidates: Map<EntityId, List<EntityId>>): Map<EntityId, List<EntityId>> {
        // A subset cannot repair too few eligible blockers or an absent co-blocker. Remove those
        // impossible edges first; this also keeps large all-menace combats off the subset search.
        var reduced = candidates
        while (true) {
            val impossibleAttackers = reduced.values.flatten().distinct().filter { attacker ->
                val group = reduced.mapValues { (_, attackers) -> attackers.filter { it == attacker } }.filterValues { it.isNotEmpty() }
                validateMenaceRequirements(state, group) != null || validateMinBlockersRequirements(state, group) != null
            }.toSet()
            val impossibleBlockers = reduced.keys.filter { blocker ->
                // Check only this blocker's restriction, against every candidate still in play: if
                // even the full set can't supply its co-blocker, no subset can.
                validateCoBlockerRequirements(state, state.projectedState, reduced.keys, restrictionBlockerIds = setOf(blocker)) != null
            }.toSet()
            val next = reduced.filterKeys { it !in impossibleBlockers }
                .mapValues { (_, attackers) -> attackers.filter { it !in impossibleAttackers } }.filterValues { it.isNotEmpty() }
            if (next == reduced) break
            reduced = next
        }
        return reduced
    }

    private fun maximalLegalPileBlocks(state: GameState, candidates: Map<EntityId, List<EntityId>>): Map<EntityId, List<EntityId>> {
        val reduced = removeImpossibleBlockEdges(state, candidates)
        if (pileRestrictionsSatisfied(state, reduced)) return reduced
        // Menace and minimum/maximum blocker counts are per attacker and count-based (who may block
        // whom was settled per edge above), so each attacker's largest legal count is an upper bound
        // on its share. When the per-attacker trim also satisfies the cross-attacker restrictions
        // it is optimal, and the subset search below is never reached.
        val perAttacker = reduced.flatMap { (blocker, attackers) -> attackers.map { blocker to it } }
            .groupBy { it.second }.values.flatMap { group ->
                val keep = (group.size downTo 0).first { count ->
                    val blocks = group.take(count).associate { it.first to listOf(it.second) }
                    validateMenaceRequirements(state, blocks) == null &&
                        validateMinBlockersRequirements(state, blocks) == null &&
                        validateMaxBlockersRequirements(state, blocks) == null
                }
                group.take(keep)
            }
        val trimmed = linkedMapOf<EntityId, MutableList<EntityId>>()
        for ((blocker, attacker) in perAttacker) trimmed.getOrPut(blocker) { mutableListOf() }.add(attacker)
        if (pileRestrictionsSatisfied(state, trimmed)) return trimmed.mapValues { it.value.toList() }
        // This search runs only for conflicting group restrictions, never during projection or
        // action enumeration. Start with the complete set and examine removals in increasing size;
        // the first successful size does as much of the instruction as possible.
        val edges = reduced.flatMap { (blocker, attackers) -> attackers.map { blocker to it } }
        // Count caps give a safe lower bound. Without it, a pile of many creatures assigned to
        // a one-blocker attacker would enumerate almost every subset before finding its answer.
        val byAttacker = edges.groupBy { it.second }
        val perAttackerMinimum = byAttacker.values.sumOf { group ->
            val maximum = (group.size downTo 0).first { count ->
                validateMaxBlockersRequirements(state, group.take(count).associate { it.first to listOf(it.second) }) == null
            }
            group.size - maximum
        }
        val keys = reduced.keys.toList()
        val globalMaximum = (keys.size downTo 0).first { count -> validateGlobalBlockerCount(state, keys.take(count).toSet()) == null }
        val globalMinimum = edges.size - reduced.values.map { it.size }.sortedDescending().take(globalMaximum).sum()
        val minimumRemoved = maxOf(1, perAttackerMinimum, globalMinimum)
        for (removedCount in minimumRemoved..edges.size) {
            var solution: Map<EntityId, List<EntityId>>? = null
            val removed = BooleanArray(edges.size)
            fun search(start: Int, remaining: Int) {
                if (solution != null) return
                // Edges before [start] are decided. Count caps only get worse as edges are kept, so a
                // kept prefix that already breaks one can't be repaired by later removals.
                val kept = linkedMapOf<EntityId, MutableList<EntityId>>()
                for (index in 0 until start) if (!removed[index]) kept.getOrPut(edges[index].first) { mutableListOf() }.add(edges[index].second)
                if (validateMaxBlockersRequirements(state, kept) != null || validateGlobalBlockerCount(state, kept.keys) != null) return
                if (remaining == 0) {
                    val blocks = linkedMapOf<EntityId, MutableList<EntityId>>()
                    for ((index, edge) in edges.withIndex()) if (!removed[index]) blocks.getOrPut(edge.first) { mutableListOf() }.add(edge.second)
                    val immutable = blocks.mapValues { it.value.toList() }
                    if (pileRestrictionsSatisfied(state, immutable)) solution = immutable
                    return
                }
                for (index in start..edges.size - remaining) {
                    removed[index] = true
                    search(index + 1, remaining - 1)
                    removed[index] = false
                }
            }
            search(0, removedCount)
            solution?.let { return it }
        }
        return emptyMap()
    }

    /**
     * Apply the post-tax commitment for a declared block: stamp [BlockingComponent] /
     * [BlockedComponent], mark the blockers-declared tracking component, emit the
     * [BlockersDeclaredEvent], and queue any blocker-order / attacker-order decisions.
     *
     * Callable from the synchronous (no-tax) path in [declareBlockers] and from
     * [com.wingedsheep.engine.handlers.continuations.CombatTaxContinuationResumer] after
     * the player confirms the tax.
     */
    internal fun commitBlockDeclaration(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>,
        taxEvents: List<com.wingedsheep.engine.core.GameEvent>,
    ): ExecutionResult {
        // CR 702.22h: blocking any member of an attacking band blocks the whole band — a blocker
        // assigned to one band member is treated as blocking every member. Expand the declared
        // assignments before stamping so the rest of combat (ordering, the damage board) sees the
        // full bipartite picture.
        val bandMembers = collectBands(state)
        val expandedBlockers: Map<EntityId, List<EntityId>> = blockers.mapValues { (_, attackerIds) ->
            val expanded = LinkedHashSet<EntityId>()
            for (attackerId in attackerIds) {
                expanded += attackerId
                val bandId = state.getEntity(attackerId)?.get<AttackingComponent>()?.bandId
                if (bandId != null) expanded += bandMembers[bandId] ?: emptySet()
            }
            expanded.toList()
        }

        var newState = BlockingRelationships.establish(state, expandedBlockers)

        // Mark that blockers have been declared this combat (even if empty)
        newState = newState.updateEntity(blockingPlayer) { container ->
            container.with(BlockersDeclaredThisCombatComponent)
        }

        val blockerNameMap = expandedBlockers.keys.associateWith { nameVisibleToAll(state, it, state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature") }
        val attackerNameMap = expandedBlockers.values.flatten().distinct().associateWith { nameVisibleToAll(state, it, state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature") }
        val blockersEvent = BlockersDeclaredEvent(expandedBlockers, blockerNameMap, attackerNameMap)
        val blockTaxEvents = taxEvents

        // Damage-assignment order (CR 510.1c/d) is no longer collected in a standalone
        // OrderObjectsDecision pre-step. The combat resolution board owns ordering: it reads the
        // declaration order (BlockedComponent.blockerIds / BlockingComponent.blockedAttackerIds)
        // as the default and lets the chooser reorder via the response. No pause here.
        return ExecutionResult.success(
            newState,
            blockTaxEvents + blockersEvent
        )
    }

    /**
     * Collect the current attacking bands, keyed by [AttackingComponent.bandId]. Used to expand
     * declared block assignments so a blocker on one band member blocks the whole band (CR 702.22h).
     */
    private fun collectBands(state: GameState): Map<String, Set<EntityId>> {
        val result = mutableMapOf<String, MutableSet<EntityId>>()
        for ((entityId, container) in state.entities) {
            val bandId = container.get<AttackingComponent>()?.bandId ?: continue
            result.getOrPut(bandId) { mutableSetOf() }.add(entityId)
        }
        return result
    }

    /**
     * Check if a creature can legally block at least one of the current attackers.
     */
    fun canCreatureBlockAnyAttacker(state: GameState, blockerId: EntityId, blockingPlayer: EntityId): Boolean {
        val blockerContainer = state.getEntity(blockerId) ?: return false
        blockerContainer.get<CardComponent>() ?: return false

        val isFaceDown = blockerContainer.has<FaceDownComponent>()

        val projected = state.projectedState

        // A creature's own "can't block" reaches the projection as SetCantBlock, which drops out
        // once the creature loses all abilities (CR 604.2) — so read the projection, never the
        // card definition.
        if (projected.cantBlock(blockerId)) return false

        if (!isFaceDown && hasCantBlockUnlessRestriction(state, blockerId, blockingPlayer, projected)) return false

        val attackers = state.entities.filter { (_, container) -> container.has<AttackingComponent>() }.keys

        return attackers.any { attackerId ->
            canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
        }
    }

    /**
     * Compute mandatory blocker assignments from floating effects.
     * Returns a map of blocker → list of attackers it must block.
     */
    fun getMandatoryBlockerAssignments(state: GameState, blockingPlayer: EntityId): Map<EntityId, List<EntityId>> {
        eachAttackerRequirements(state, blockingPlayer)?.let { return it }
        val projected = state.projectedState
        val potentialBlockers = findPotentialBlockers(state, blockingPlayer)
        val result = mutableMapOf<EntityId, MutableList<EntityId>>()

        // 1. MustBlockSpecificAttacker (Provoke)
        val provokeConstraints = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.MustBlockSpecificAttacker }
            .flatMap { floatingEffect ->
                val modification = floatingEffect.effect.modification as SerializableModification.MustBlockSpecificAttacker
                floatingEffect.effect.affectedEntities.map { blockerId ->
                    blockerId to modification.attackerId
                }
            }

        for ((blockerId, attackerId) in provokeConstraints) {
            if (blockerId !in potentialBlockers) continue
            val controller = projected.getController(blockerId)
            if (controller != blockingPlayer) continue
            val attackerContainer = state.getEntity(attackerId) ?: continue
            if (!attackerContainer.has<AttackingComponent>()) continue
            if (!canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)) continue
            result.getOrPut(blockerId) { mutableListOf() }.add(attackerId)
        }

        // 2. MustBeBlockedByAll (Taunting Elf, Alluring Scent)
        val mustBeBlockedAttackers = findMustBeBlockedAttackers(state)
        for (attackerId in mustBeBlockedAttackers) {
            for (blockerId in potentialBlockers) {
                if (canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)) {
                    result.getOrPut(blockerId) { mutableListOf() }.add(attackerId)
                }
            }
        }

        return result.filterValues { it.isNotEmpty() }
    }

    /** A separate requirement per attacker, maximized together with the existing combat demands. */
    private fun eachAttackerRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        submitted: Map<EntityId, List<EntityId>>? = null,
    ): Map<EntityId, List<EntityId>>? {
        val rules = BlockStaticRules(state, cardRegistry, predicateEvaluator)
        val potential = findPotentialBlockers(state, blockingPlayer)
        val each = potential.filter { rules.mustBlockEach(it) }
        if (each.isEmpty()) return null
        val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }
        data class Demand(val blocker: EntityId? = null, val attacker: EntityId? = null)
        val demands = mutableListOf<Demand>()
        for (blocker in each) repeat(rules.eachRequirementCount(blocker)) {
            for (attacker in attackers) demands.add(Demand(blocker, attacker))
        }
        for (blocker in potential) if (state.projectedState.mustBlock(blocker)) {
            demands.add(Demand(blocker = blocker))
        }
        for (attacker in findMustBeBlockedAttackers(state)) for (blocker in potential) {
            demands.add(Demand(blocker, attacker))
        }
        for (attacker in findMustBeBlockedIfAbleAttackers(state)) {
            demands.add(Demand(attacker = attacker))
        }
        for (floating in state.floatingEffects) {
            val modification = floating.effect.modification as? SerializableModification.MustBlockSpecificAttacker ?: continue
            if (modification.attackerId !in attackers) continue
            for (blocker in floating.effect.affectedEntities) if (blocker in potential) {
                demands.add(Demand(blocker, modification.attackerId))
            }
        }
        fun obeyed(demand: Demand, blocks: Map<EntityId, List<EntityId>>): Boolean = when {
            demand.blocker == null -> blocks.values.any { demand.attacker in it }
            demand.attacker == null -> blocks[demand.blocker].orEmpty().isNotEmpty()
            else -> demand.attacker in blocks[demand.blocker].orEmpty()
        }
        fun score(blocks: Map<EntityId, List<EntityId>>) = demands.count { obeyed(it, blocks) }
        // Costs are never compulsory to satisfy a requirement. Voluntary taxed blocks are still
        // validated normally; the hypothetical maximum uses only cost-free blockers.
        val candidates = removeImpossibleBlockEdges(state, potential.filter { blocker ->
            CombatTaxes.blockTax(state, cardRegistry, setOf(blocker), state.projectedState,
                predicateEvaluator = predicateEvaluator) == 0
        }.associateWith { blocker ->
            attackers.filter { validateBlocker(state, blockingPlayer, blocker, listOf(it)) == null }
        }.filterValues { it.isNotEmpty() })
        val capacities = candidates.keys.associateWith(rules::maxBlocks)
        val minimumBlockers = candidates.values.flatten().distinct().associateWith { attacker ->
            val eligible = candidates.filterValues { attacker in it }.keys.toList()
            (1..eligible.size).first { count ->
                val blocks = eligible.take(count).associateWith { listOf(attacker) }
                validateMenaceRequirements(state, blocks) == null && validateMinBlockersRequirements(state, blocks) == null
            }
        }
        fun canComplete(
            blocks: Map<EntityId, List<EntityId>>,
            remaining: List<EntityId>,
            checkedAttackers: Set<EntityId>? = null,
        ): Boolean {
            val counts = blocks.values.flatten()
                .filter { checkedAttackers == null || it in checkedAttackers }
                .groupingBy { it }.eachCount()
            val deficits = counts.mapNotNull { (attacker, count) ->
                (minimumBlockers.getValue(attacker) - count).takeIf { it > 0 }?.let { attacker to it }
            }.toMap()
            if (deficits.any { (attacker, deficit) ->
                remaining.count { attacker in candidates.getValue(it) } < deficit
            }) return false
            val supply = remaining.sumOf { blocker ->
                minOf(capacities.getValue(blocker), candidates.getValue(blocker).count { it in deficits })
            }
            return deficits.values.sum() <= supply
        }
        val byBlocker = demands.filter { it.blocker != null }.groupBy { it.blocker!! }
        val byAttacker = demands.filter { it.blocker == null }
        // Relax only declaration-wide restrictions. Each remaining blocker still has its actual
        // capacity, so a board of one-block creatures does not explore every attacker permutation.
        fun optimistic(blocks: Map<EntityId, List<EntityId>>, remaining: List<EntityId>): Int {
            val possible = blocks + remaining.associateWith { candidates[it]!! }
            val attackerDemands = byAttacker.count { obeyed(it, possible) }
            val blockerDemands = byBlocker.entries.sumOf { (blocker, requirements) ->
                if (blocker !in remaining) requirements.count { obeyed(it, blocks) }
                else {
                    val eligible = candidates[blocker].orEmpty()
                    val weights = eligible.map { attacker -> requirements.count { it.attacker == attacker } }
                    weights.sortedDescending().take(minOf(eligible.size, capacities[blocker]!!)).sum() +
                        requirements.count { it.attacker == null && eligible.isNotEmpty() }
                }
            }
            return attackerDemands + blockerDemands
        }
        val upper = optimistic(emptyMap(), candidates.keys.toList())
        val threshold = submitted?.let(::score)
        if (threshold != null && threshold >= upper) return submitted
        fun legal(blocks: Map<EntityId, List<EntityId>>) =
            blocks.all { (blocker, targets) -> targets.size <= capacities.getValue(blocker) } &&
                pileRestrictionsSatisfied(state, blocks)
        if (legal(candidates)) return candidates
        var best = emptyMap<EntityId, List<EntityId>>()
        var bestScore = 0
        val holders = candidates.keys.sortedByDescending { if (it in each) Int.MAX_VALUE else candidates[it]!!.size }
        val selected = linkedMapOf<EntityId, List<EntityId>>()
        fun search(index: Int) {
            if (bestScore == upper || (threshold != null && bestScore > threshold)) return
            if (!canComplete(selected, holders.drop(index))) return
            if (optimistic(selected, holders.drop(index)) <= maxOf(bestScore, threshold ?: -1)) return
            if (validateGlobalBlockerCount(state, selected.keys) != null ||
                validateMaxBlockersRequirements(state, selected) != null) return
            if (score(selected) == upper && legal(selected)) {
                bestScore = upper; best = selected.toMap(); return
            }
            if (index == holders.size) {
                if (legal(selected)) {
                    val value = score(selected)
                    if (value > bestScore) { bestScore = value; best = selected.toMap() }
                }
                return
            }
            val blocker = holders[index]
            val eligible = candidates[blocker]!!
            val capacity = minOf(eligible.size, capacities.getValue(blocker))
            val choice = mutableListOf<EntityId>()
            fun choose(next: Int, remaining: Int) {
                // This blocker can still join other attackers, but cannot repair the minimum
                // count on an attacker already in its partial choice.
                if (!canComplete(selected + (blocker to choice), holders.drop(index + 1), choice.toSet())) return
                if (remaining == 0) {
                    if (choice.isEmpty()) selected.remove(blocker) else selected[blocker] = choice.toList()
                    search(index + 1)
                    selected.remove(blocker)
                    return
                }
                for (i in next..eligible.size - remaining) {
                    choice.add(eligible[i]); choose(i + 1, remaining - 1); choice.removeAt(choice.lastIndex)
                    if (bestScore == upper || (threshold != null && bestScore > threshold)) return
                }
            }
            for (count in capacity downTo 0) {
                choose(0, count)
                if (bestScore == upper || (threshold != null && bestScore > threshold)) break
            }
        }
        search(0)
        return if (threshold != null && bestScore <= threshold) submitted else best
    }

    // =========================================================================
    // Blocker Validation
    // =========================================================================

    /**
     * Validate that a creature can block.
     */
    private fun validateBlocker(
        state: GameState,
        blockingPlayer: EntityId,
        blockerId: EntityId,
        attackerIds: List<EntityId>
    ): String? {
        val container = state.getEntity(blockerId)
            ?: return "Blocker not found: $blockerId"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: $blockerId"

        val projected = state.projectedState

        if (!projected.isCreature(blockerId)) {
            return "Only creatures can block: ${cardComponent.name}"
        }
        // CR 509.1a / 506.3f: a creature that is also a battle can't block.
        if (projected.isBattle(blockerId)) {
            return "A battle can't block: ${cardComponent.name}"
        }
        val controller = projected.getController(blockerId)
        if (controller != blockingPlayer) {
            return "You don't control ${cardComponent.name}"
        }

        if (TappedBlockBypass.tappedPreventsBlocking(state, blockerId, cardRegistry, predicateEvaluator)) {
            return "${cardComponent.name} is tapped and cannot block"
        }

        if (container.has<BlockingComponent>()) {
            return "${cardComponent.name} is already blocking"
        }

        val isFaceDown = container.has<FaceDownComponent>()

        if (projected.cantBlock(blockerId)) {
            return "${cardComponent.name} can't block"
        }

        if (!isFaceDown) {
            val cantBlockUnlessError = validateCantBlockUnless(state, blockerId, blockingPlayer, projected)
            if (cantBlockUnlessError != null) return cantBlockUnlessError
        }

        if (attackerIds.size > 1) {
            val maxBlocks = maxPileMemberships(state, blockerId, Int.MAX_VALUE)
            if (attackerIds.size > maxBlocks) {
                val countText = if (maxBlocks == 1) "one creature" else "$maxBlocks creatures"
                return "${cardComponent.name} can only block $countText"
            }
        }

        // Check each attacker
        for (attackerId in attackerIds) {
            // CR 509.1b / 805.10d: a creature can only block an attacker that is attacking its
            // controller (or a planeswalker/battle its controller protects). Under shared team turns
            // (Two-Headed Giant) the defending team blocks as one, so a creature may block an attacker
            // aimed at any teammate; without shared team turns (Team vs. Team — CR 808, non-team
            // games) sharedTurnTeam is a singleton, so you can only block attackers aimed at you.
            val attacking = state.getEntity(attackerId)?.get<AttackingComponent>()
                ?: return "${cardComponent.name} can't block: ${attackerId.value} isn't attacking"
            val attackedDefender = CombatDefenders.defendingPlayerOf(state, attacking, projected)
            if (attackedDefender !in state.sharedTurnTeam(blockingPlayer)) {
                return "${cardComponent.name} can't block a creature attacking another player"
            }

            val evasionValidation = validateCanBlock(state, blockerId, attackerId, blockingPlayer)
            if (evasionValidation != null) {
                return evasionValidation
            }
        }

        return null
    }

    /**
     * Validate that a blocker can block a specific attacker (evasion abilities).
     * Delegates to registered [BlockEvasionRule] instances.
     */
    private fun validateCanBlock(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        blockingPlayer: EntityId
    ): String? {
        state.getEntity(attackerId) ?: return "Attacker not found: $attackerId"
        state.getEntity(attackerId)?.get<CardComponent>() ?: return "Not a card: $attackerId"

        val ctx = BlockCheckContext(
            state = state,
            projected = state.projectedState,
            attackerId = attackerId,
            blockerId = blockerId,
            blockingPlayer = blockingPlayer,
            cardRegistry = cardRegistry
        )
        for (rule in blockEvasionRules) {
            val error = rule.check(ctx)
            if (error != null) return error
        }
        return null
    }

    /**
     * Check if a creature can legally block an attacker.
     * Delegates to registered [BlockEvasionRule] instances for evasion checks,
     * plus blocker-level restrictions (can't block, face-down abilities).
     */
    private fun canCreatureBlockAttacker(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        blockingPlayer: EntityId,
        projected: ProjectedState
    ): Boolean {
        val blockerContainer = state.getEntity(blockerId) ?: return false
        state.getEntity(attackerId) ?: return false

        blockerContainer.get<CardComponent>() ?: return false

        if (projected.cantBlock(blockerId)) {
            return false
        }

        val ctx = BlockCheckContext(
            state = state,
            projected = projected,
            attackerId = attackerId,
            blockerId = blockerId,
            blockingPlayer = blockingPlayer,
            cardRegistry = cardRegistry
        )
        return blockEvasionRules.all { it.check(ctx) == null }
    }

    // =========================================================================
    // Menace
    // =========================================================================

    /**
     * Validate menace requirements (must be blocked by 2+ creatures).
     */
    private fun validateMenaceRequirements(
        state: GameState,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val attackerToBlockers = mutableMapOf<EntityId, MutableList<EntityId>>()
        for ((blockerId, attackerIds) in blockers) {
            for (attackerId in attackerIds) {
                attackerToBlockers.getOrPut(attackerId) { mutableListOf() }.add(blockerId)
            }
        }

        val projected = state.projectedState

        for ((attackerId, blockerList) in attackerToBlockers) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue

            if (projected.hasKeyword(attackerId, Keyword.MENACE)) {
                if (blockerList.size < 2) {
                    return "${attackerCard.name} has menace and must be blocked by 2 or more creatures"
                }
            }
        }

        return null
    }

    /**
     * Validate "can't be blocked except by N or more creatures" ([CantBeBlockedByFewerThan]).
     * Generalizes menace: an attacker carrying the static may be left unblocked, but if blocked it
     * must have at least [CantBeBlockedByFewerThan.minBlockers] blockers.
     */
    private fun validateMinBlockersRequirements(
        state: GameState,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val attackerToBlockers = mutableMapOf<EntityId, MutableList<EntityId>>()
        for ((blockerId, attackerIds) in blockers) {
            for (attackerId in attackerIds) {
                attackerToBlockers.getOrPut(attackerId) { mutableListOf() }.add(blockerId)
            }
        }

        for ((attackerId, blockerList) in attackerToBlockers) {
            if (blockerList.isEmpty()) continue
            val attackerContainer = state.getEntity(attackerId) ?: continue
            if (attackerContainer.has<FaceDownComponent>()) continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId) ?: continue

            val minBlockers = cardDef.staticAbilities
                .filterIsInstance<com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan>()
                .filter { it.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self }
                .maxOfOrNull { it.minBlockers } ?: continue

            if (blockerList.size < minBlockers) {
                return "${attackerCard.name} can't be blocked except by $minBlockers or more creatures"
            }
        }

        return null
    }

    /**
     * Validate `CantBeBlockedByMoreThan` restrictions (CR 509.1b).
     * Each attacker with this static ability caps the number of creatures that may block it —
     * whether the ability is its own, granted to it, or projected onto it by a battlefield
     * permanent's group clause ([hostScopedMaxBlockers]).
     */
    private fun validateMaxBlockersRequirements(
        state: GameState,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val attackerToBlockerCount = mutableMapOf<EntityId, Int>()
        for (attackerIds in blockers.values) {
            for (attackerId in attackerIds) {
                attackerToBlockerCount.merge(attackerId, 1, Int::plus)
            }
        }
        val hostLimits = hostScopedMaxBlockers(state, attackerToBlockerCount.keys)

        for ((attackerId, count) in attackerToBlockerCount) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            // A face-down attacker has no printed abilities (CR 708.2a), but a face-up host's
            // group clause still covers it.
            val cardDef = if (attackerContainer.has<FaceDownComponent>() ||
                state.projectedState.hasLostAllAbilities(attackerId)) null
                else cardRegistry.getCard(attackerCard.cardDefinitionId)

            // Printed "can't be blocked by more than N", including the conditional form
            // (Akawalli's descend-8 "can't be blocked by more than one creature") — unwrap a
            // ConditionalStaticAbility and honor it only while its condition currently holds,
            // mirroring the MustBeBlocked handling in attackersWithMustBeBlockedStatic. cardDef
            // may be null for tokens/copies without a registered definition — the granted forms
            // below still apply.
            val attackerController = state.projectedState.getController(attackerId)
            val staticLimit = cardDef?.staticAbilities
                ?.mapNotNull { ability ->
                    val unwrapped = if (ability is ConditionalStaticAbility) ability.ability else ability
                    if (unwrapped !is CantBeBlockedByMoreThan) return@mapNotNull null
                    if (unwrapped.filter.scope !is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self) {
                        return@mapNotNull null
                    }
                    if (ability is ConditionalStaticAbility) {
                        if (attackerController == null) return@mapNotNull null
                        if (!conditionEvaluator.evaluate(
                                state,
                                ability.condition,
                                EffectContext(sourceId = attackerId, controllerId = attackerController)
                            )
                        ) return@mapNotNull null
                    }
                    unwrapped.maxBlockers
                }
                ?.minOrNull()
            // Granted static-ability form: e.g. Full Steam Ahead grants CantBeBlockedByMoreThan(1)
            // until end of turn via grantedStaticAbilities.
            val grantedLimit = state.grantedStaticAbilities
                .filter { it.entityId == attackerId }
                .map { it.ability }
                .filterIsInstance<CantBeBlockedByMoreThan>()
                .filter { it.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self }
                .minOfOrNull { it.maxBlockers }
            // Granted (floating) flag form (CR 509.1b): a temporary "can't be blocked by more than one
            // creature" via Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE) caps at 1.
            val flagLimit = if (
                state.projectedState.hasKeyword(
                    attackerId,
                    com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE
                )
            ) 1 else null
            val limit = listOfNotNull(staticLimit, grantedLimit, flagLimit, hostLimits[attackerId]).minOrNull()
                ?: continue

            if (count > limit) {
                val countText = if (limit == 1) "more than one creature" else "more than $limit creatures"
                return "${nameVisibleToAll(state, attackerId, attackerCard.name)} can't be blocked by $countText"
            }
        }
        return null
    }

    /**
     * The tightest `CantBeBlockedByMoreThan` cap each of [attackers] gets from a battlefield
     * permanent's group clause — Flopsie, Bumi's Buddy's and Challenger Troll's "each creature you
     * control with power 4 or greater can't be blocked by more than one creature", Rocksteady's
     * Boars. The filter is resolved relative to the permanent carrying the static and matched
     * against projected state, so a creature pumped to power 4 after the host arrived is covered
     * and one shrunk below it drops out. `Scope.Self` is left to the attacker's own read; a host
     * that is itself an attacker is covered by its own group clause unless the filter says "other".
     */
    private fun hostScopedMaxBlockers(state: GameState, attackers: Set<EntityId>): Map<EntityId, Int> {
        if (attackers.isEmpty()) return emptyMap()
        val projected = state.projectedState
        val limits = mutableMapOf<EntityId, Int>()
        for (hostId in state.getBattlefield()) {
            val container = state.getEntity(hostId) ?: continue
            if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(hostId)) continue
            val cardId = container.get<CardComponent>()?.cardDefinitionId ?: continue
            for (ability in cardRegistry.getCard(cardId)?.staticAbilities.orEmpty()) {
                val unwrapped = if (ability is ConditionalStaticAbility) ability.ability else ability
                if (unwrapped !is CantBeBlockedByMoreThan || unwrapped.filter.scope is Scope.Self) continue
                val controller = projected.getController(hostId) ?: continue
                if (ability is ConditionalStaticAbility &&
                    !conditionEvaluator.evaluate(
                        state, ability.condition, EffectContext(sourceId = hostId, controllerId = controller)
                    )
                ) continue
                resolveFilteredAttackers(state, projected, hostId, controller, unwrapped.filter, attackers)
                    .filterNot { it == hostId && unwrapped.filter.excludeSelf }
                    .forEach { limits.merge(it, unwrapped.maxBlockers, ::minOf) }
            }
        }
        return limits
    }

    /**
     * Validate global blocker-count caps. While any permanent with [BlockerCountLimit] is on the
     * battlefield (e.g. Dueling Grounds), the total number of distinct blocking creatures across
     * all players may not exceed the smallest such cap. Returns an error message when violated.
     */
    private fun validateGlobalBlockerCount(
        state: GameState,
        blockerIds: Set<EntityId>
    ): String? {
        var cap: Int? = null
        var capDescription = ""
        for (permId in state.getBattlefield()) {
            val cardComponent = state.getEntity(permId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities.filterIsInstance<BlockerCountLimit>()) {
                if (cap == null || ability.maxBlockers < cap) {
                    cap = ability.maxBlockers
                    capDescription = ability.description
                }
            }
        }
        if (cap != null && blockerIds.size > cap) {
            return capDescription
        }
        return null
    }

    /**
     * Validate "can't block unless [X] also blocks" restrictions ([CantBlockUnlessCoBlocker], CR
     * 509.1b). The blocking sibling of [com.wingedsheep.engine.mechanics.combat.AttackPhaseManager]'s
     * co-attacker check.
     *
     * For each proposed blocker carrying the restriction, at least one *other* blocker in the same
     * declaration must match the restriction's filter (evaluated with projected state so
     * color/type-changing effects are honored). The co-blocker need not block the same attacker —
     * it just has to be declared as a blocker this combat. Self never counts as its own co-blocker.
     *
     * Restrictions are read from both the card definition (printed) and grantedStaticAbilities, so
     * the form arrives on a token without a CardDefinition (Toby's Beast token — "This token can't
     * attack or block alone").
     */
    private fun validateCoBlockerRequirements(
        state: GameState,
        projected: ProjectedState,
        blockerIds: Set<EntityId>,
        /** Whose restrictions to check; co-blockers are always drawn from all of [blockerIds]. */
        restrictionBlockerIds: Set<EntityId> = blockerIds,
    ): String? {
        for (blockerId in restrictionBlockerIds) {
            val cardComponent = state.getEntity(blockerId)?.get<CardComponent>() ?: continue
            if (state.getEntity(blockerId)?.has<FaceDownComponent>() == true) continue
            val printed = cardRegistry.getCard(cardComponent.cardDefinitionId)
                ?.staticAbilities.orEmpty()
            val granted = state.grantedStaticAbilities
                .filter { it.entityId == blockerId }
                .map { it.ability }
            val restrictions = (printed + granted)
                .filterIsInstance<CantBlockUnlessCoBlocker>()
                .filter { it.filter.scope is Scope.Self }
            for (restriction in restrictions) {
                val context = PredicateContext(controllerId = projected.getController(blockerId) ?: blockerId)
                val satisfied = blockerIds.any { otherId ->
                    otherId != blockerId &&
                        predicateEvaluator.matches(state, projected, otherId, restriction.coBlockerFilter, context)
                }
                if (!satisfied) {
                    return "${cardComponent.name} ${restriction.description}"
                }
            }
        }
        return null
    }

    // =========================================================================
    // Must Be Blocked Requirements
    // =========================================================================

    /**
     * Validate "must be blocked" requirements.
     * Handles both "must be blocked by all" (Lure) and "must be blocked if able" (Gaea's Protector).
     */
    private fun validateMustBeBlockedRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val projected = state.projectedState
        val potentialBlockers = findPotentialBlockers(state, blockingPlayer)

        // Build reverse map: attacker → set of blockers assigned to it
        val attackerToBlockers = mutableMapOf<EntityId, MutableSet<EntityId>>()
        for ((blockerId, attackerIds) in blockers) {
            for (attackerId in attackerIds) {
                attackerToBlockers.getOrPut(attackerId) { mutableSetOf() }.add(blockerId)
            }
        }

        // 1. "Must be blocked by all" (Lure/Taunting Elf): every blocker that CAN block it MUST block it
        val mustBeBlockedByAllAttackers = findMustBeBlockedAttackers(state)
        if (mustBeBlockedByAllAttackers.isNotEmpty()) {
            val blockerToAttackers = blockers.mapValues { it.value.toSet() }

            for (blockerId in potentialBlockers) {
                val canBlockThese = mustBeBlockedByAllAttackers.filter { attackerId ->
                    canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
                }

                if (canBlockThese.isEmpty()) {
                    continue
                }

                val actuallyBlocking = blockerToAttackers[blockerId] ?: emptySet()
                val blockingMustBeBlocked = actuallyBlocking.intersect(mustBeBlockedByAllAttackers.toSet())

                if (blockingMustBeBlocked.isEmpty()) {
                    val blockerCard = state.getEntity(blockerId)?.get<CardComponent>()
                    val blockerName = blockerCard?.name ?: "Creature"

                    val attackerNames = canBlockThese.mapNotNull { attackerId ->
                        state.getEntity(attackerId)?.get<CardComponent>()?.name
                    }

                    return if (canBlockThese.size == 1) {
                        "$blockerName must block ${attackerNames.first()}"
                    } else {
                        "$blockerName must block one of: ${attackerNames.joinToString(", ")}"
                    }
                }
            }
        }

        // 2. "Must be blocked if able" (Gaea's Protector): at least one creature must block it.
        // Rule 509.1c: the declaration is illegal if the number of requirements being obeyed is
        // fewer than the maximum number that could be obeyed. That maximum is a maximum bipartite
        // matching between the must-be-blocked attackers and the blockers hypothetically free to
        // cover them: a provoke-pinned blocker is only free for its pinned attacker, and a blocker
        // that can block a Lure-style attacker is claimed by that requirement (section 1 forces it
        // there). Per-pair blocking restrictions go through canCreatureBlockAttacker; declaration-
        // wide restrictions (e.g. can't-block-alone) are not modelled, so the computed maximum can
        // only over-count in those corners — never rejecting more than 509.1c would.
        val mustBeBlockedIfAbleAttackers = findMustBeBlockedIfAbleAttackers(state)
        if (mustBeBlockedIfAbleAttackers.isNotEmpty()) {
            val provokePinnedAttackers = state.floatingEffects
                .filter { it.effect.modification is SerializableModification.MustBlockSpecificAttacker }
                .flatMap { floatingEffect ->
                    val modification =
                        floatingEffect.effect.modification as SerializableModification.MustBlockSpecificAttacker
                    floatingEffect.effect.affectedEntities.map { it to modification.attackerId }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { it.value.toSet() }
            val lureClaimedBlockers = potentialBlockers.filter { blockerId ->
                mustBeBlockedByAllAttackers.any { attackerId ->
                    canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
                }
            }.toSet()

            fun canHypotheticallyBlock(blockerId: EntityId, attackerId: EntityId): Boolean {
                if (blockerId in lureClaimedBlockers) return false
                provokePinnedAttackers[blockerId]?.let { pins -> if (attackerId !in pins) return false }
                return canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
            }

            // Maximum bipartite matching (attackers ↔ hypothetically-free blockers): its size is
            // the most requirements that could be simultaneously obeyed. Shared Kuhn's routine.
            val matchedAttackerOfBlocker = com.wingedsheep.engine.mechanics.BipartiteMatching
                .maximumMatching(mustBeBlockedIfAbleAttackers, potentialBlockers) { attackerId, blockerId ->
                    canHypotheticallyBlock(blockerId, attackerId)
                }
            val maxSatisfiable = matchedAttackerOfBlocker.size
            val satisfied = mustBeBlockedIfAbleAttackers.count { !attackerToBlockers[it].isNullOrEmpty() }

            if (satisfied < maxSatisfiable) {
                val matchedAttackers = matchedAttackerOfBlocker.values.toSet()
                val culpritId = mustBeBlockedIfAbleAttackers.first {
                    it in matchedAttackers && attackerToBlockers[it].isNullOrEmpty()
                }
                val attackerName = state.getEntity(culpritId)?.get<CardComponent>()?.name ?: "Creature"
                return "$attackerName must be blocked if able"
            }
        }

        return null
    }

    /**
     * Validate provoke "must block specific attacker" requirements.
     */
    private fun validateProvokeRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val projected = state.projectedState

        val provokeConstraints = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.MustBlockSpecificAttacker }
            .flatMap { floatingEffect ->
                val modification = floatingEffect.effect.modification as SerializableModification.MustBlockSpecificAttacker
                floatingEffect.effect.affectedEntities.map { blockerId ->
                    blockerId to modification.attackerId
                }
            }

        for ((blockerId, attackerId) in provokeConstraints) {
            val controller = projected.getController(blockerId)
            if (controller != blockingPlayer) continue

            val blockerContainer = state.getEntity(blockerId) ?: continue
            if (blockerId !in state.getBattlefield()) continue
            if (TappedBlockBypass.tappedPreventsBlocking(state, blockerId, cardRegistry, predicateEvaluator)) continue

            val attackerContainer = state.getEntity(attackerId) ?: continue
            if (!attackerContainer.has<AttackingComponent>()) continue

            if (!canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)) continue

            val actuallyBlocking = blockers[blockerId] ?: emptyList()
            if (attackerId !in actuallyBlocking) {
                val blockerName = blockerContainer.get<CardComponent>()?.name ?: "Creature"
                val attackerName = attackerContainer.get<CardComponent>()?.name ?: "creature"
                return "$blockerName must block $attackerName (provoke)"
            }
        }

        return null
    }

    /**
     * Validate projected "must block" requirements (e.g., from Grand Melee).
     */
    private fun validateProjectedMustBlockRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val projected = state.projectedState
        val potentialBlockers = findPotentialBlockers(state, blockingPlayer)

        for (blockerId in potentialBlockers) {
            if (!projected.mustBlock(blockerId)) continue

            val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }
            val canBlockAny = attackers.any { attackerId ->
                canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
            }

            if (!canBlockAny) continue

            if (blockerId !in blockers.keys) {
                val cardName = state.getEntity(blockerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must block this combat if able"
            }
        }

        return null
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Find all attackers that have "must be blocked by all" requirement active (Lure effects).
     */
    private fun findMustBeBlockedAttackers(state: GameState): List<EntityId> {
        val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }.toSet()

        val fromFloating = state.floatingEffects
            .filter { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.MustBeBlockedByAll
            }
            .flatMap { floatingEffect ->
                floatingEffect.effect.affectedEntities.filter { it in attackers }
            }
        return (fromFloating + attackersWithMustBeBlockedStatic(state, allCreatures = true)).distinct()
    }

    /**
     * Attackers that carry a [MustBeBlocked] static ability (matching [allCreatures]), including the
     * conditional form (e.g. Frodo Baggins: gated on `SourceIsRingBearer`). The gating condition is
     * evaluated with the static's holder as the source.
     *
     * A holder's *printed* statics count only while it has its abilities: a face-down permanent has
     * none (CR 708.2a) and one that has lost all abilities (projected `lostAllAbilities`) no longer
     * imposes the requirement. Runtime grants ([GameState.grantedStaticAbilities]) are read alongside
     * the printed ones, as the granted "can't be blocked by more than N" form is.
     */
    private fun attackersWithMustBeBlockedStatic(state: GameState, allCreatures: Boolean): List<EntityId> {
        val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }
        if (attackers.isEmpty()) return emptyList()
        val projected = state.projectedState
        val attackerSet = attackers.toSet()
        val battlefield = state.getBattlefield()
        val result = mutableSetOf<EntityId>()

        fun apply(holder: EntityId, ability: StaticAbility) {
            val unwrapped = if (ability is ConditionalStaticAbility) ability.ability else ability
            if (unwrapped !is MustBeBlocked || unwrapped.allCreatures != allCreatures) return
            val controller = projected.getController(holder) ?: return
            if (ability is ConditionalStaticAbility &&
                !conditionEvaluator.evaluate(
                    state, ability.condition, EffectContext(sourceId = holder, controllerId = controller)
                )
            ) return
            val filter = unwrapped.filter
            if (filter == null) {
                // Source-scoped: the holder itself must be blocked (Goblin Fire Fiend, Frodo Baggins).
                if (holder in attackerSet) result.add(holder)
            } else {
                // Projected onto other creatures via a filter, resolved relative to the holder - e.g.
                // The Masamune's "equipped creature ... must be blocked if able".
                result.addAll(resolveFilteredAttackers(state, projected, holder, controller, filter, attackerSet))
            }
        }

        for (holder in battlefield) {
            val container = state.getEntity(holder) ?: continue
            if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(holder)) continue
            val cardName = container.get<CardComponent>()?.cardDefinitionId ?: continue
            cardRegistry.getCard(cardName)?.staticAbilities.orEmpty().forEach { apply(holder, it) }
        }
        for (grant in state.grantedStaticAbilities) {
            if (grant.entityId !in battlefield) continue
            if (!GrantDurationGate.holds(state, grant.entityId, grant.sourceId, grant.duration)) continue
            apply(grant.entityId, grant.ability)
        }

        return result.toList()
    }

    /**
     * Resolve which declared attackers a filtered static (carried by [sourceId]) applies to —
     * [MustBeBlocked] and [CantBeBlockedByMoreThan] both read it. Source-relative scopes resolve against [sourceId]: `AttachedTo` -> the creature it
     * is attached to (equipped creature), `Self` -> the source, `Specific` -> the bound entity;
     * `Battlefield` matches every attacker against the base filter. Only attackers pass, and each
     * must also satisfy the base filter (evaluated with the static's source as context).
     */
    private fun resolveFilteredAttackers(
        state: GameState,
        projected: ProjectedState,
        sourceId: EntityId,
        controllerId: EntityId,
        filter: GroupFilter,
        attackerSet: Set<EntityId>,
    ): List<EntityId> {
        val candidates: List<EntityId> = when (val scope = filter.scope) {
            is Scope.AttachedTo -> listOfNotNull(state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId)
            is Scope.Self -> listOf(sourceId)
            is Scope.SoulbondPair ->
                com.wingedsheep.engine.mechanics.SoulbondPairing.pairOf(state, sourceId).toList()
            is Scope.Specific -> listOf(scope.entityId)
            is Scope.Battlefield -> attackerSet.toList()
        }
        return candidates.filter { id ->
            id in attackerSet &&
                predicateEvaluator.matches(
                    state, projected, id, filter.baseFilter,
                    PredicateContext(sourceId = sourceId, controllerId = controllerId)
                )
        }
    }

    /**
     * Find all attackers that have "must be blocked if able" requirement active.
     * These only require at least one blocker, not all.
     */
    private fun findMustBeBlockedIfAbleAttackers(state: GameState): List<EntityId> {
        val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }.toSet()

        val fromFloating = state.floatingEffects
            .filter { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.MustBeBlockedIfAble
            }
            .flatMap { floatingEffect ->
                floatingEffect.effect.affectedEntities.filter { it in attackers }
            }
        return (fromFloating + attackersWithMustBeBlockedStatic(state, allCreatures = false)).distinct()
    }

    /**
     * Find all potential blockers (untapped creatures controlled by the blocking player, plus tapped
     * ones a [com.wingedsheep.sdk.scripting.CanBlockAsThoughUntapped] covers).
     */
    private fun findPotentialBlockers(state: GameState, blockingPlayer: EntityId): List<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield()
            .filter { entityId ->
                val container = state.getEntity(entityId) ?: return@filter false
                container.get<CardComponent>() ?: return@filter false
                val controller = projected.getController(entityId)

                projected.isCreature(entityId) &&
                    !projected.isBattle(entityId) &&
                    controller == blockingPlayer &&
                    !TappedBlockBypass.tappedPreventsBlocking(state, entityId, cardRegistry, predicateEvaluator)
            }
    }

    // =========================================================================
    // CantBlockUnless
    // =========================================================================

    /**
     * Validate CantBlockUnless restrictions for a blocker.
     */
    private fun validateCantBlockUnless(
        state: GameState,
        blockerId: EntityId,
        blockingPlayer: EntityId,
        projected: ProjectedState
    ): String? {
        val container = state.getEntity(blockerId) ?: return null
        // A face-down creature has no printed abilities (CR 708.2a); one that lost all abilities
        // no longer has this one either (CR 604.2).
        if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(blockerId)) return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null

        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantBlockUnless>()
            .firstOrNull { it.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self } ?: return null

        val attackers = state.entities.filter { (_, c) -> c.has<AttackingComponent>() }
        if (attackers.isEmpty()) return null

        val anyAttacker = attackers.keys.first()
        val attackingPlayer = projected.getController(anyAttacker) ?: return null

        val effectContext = EffectContext(
            sourceId = blockerId,
            controllerId = blockingPlayer,
        )
        if (!conditionEvaluator.evaluate(state, restriction.condition, effectContext)) {
            return "${cardComponent.name} ${restriction.description}"
        }

        return null
    }

    /**
     * Check if a creature has a CantBlockUnless restriction.
     */
    private fun hasCantBlockUnlessRestriction(
        state: GameState,
        blockerId: EntityId,
        blockingPlayer: EntityId,
        projected: ProjectedState
    ): Boolean {
        val container = state.getEntity(blockerId) ?: return false
        if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(blockerId)) return false
        val cardComponent = container.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return false

        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantBlockUnless>()
            .firstOrNull { it.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self } ?: return false

        val attackers = state.entities.filter { (_, c) -> c.has<AttackingComponent>() }
        if (attackers.isEmpty()) return false

        val anyAttacker = attackers.keys.first()
        val attackingPlayer = projected.getController(anyAttacker) ?: return false

        val effectContext = EffectContext(
            sourceId = blockerId,
            controllerId = blockingPlayer,
        )
        return !conditionEvaluator.evaluate(state, restriction.condition, effectContext)
    }

    // =========================================================================
    // Block Taxes
    // =========================================================================

    private fun pauseForBlockTaxConfirmation(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>,
        totalTax: Int,
    ): ExecutionResult {
        val manaCost = com.wingedsheep.sdk.core.ManaCost(
            List(totalTax) { com.wingedsheep.sdk.core.ManaSymbol.generic(1) }
        )
        val manaSolver = com.wingedsheep.engine.mechanics.mana.ManaSolver(cardRegistry, predicateEvaluator)
        val sources = manaSolver.findAvailableManaSources(state, blockingPlayer)
        val sourceOptions = sources.map { source ->
            com.wingedsheep.engine.core.ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless,
                requiresSacrifice = source.requiresSacrifice,
                manaAmount = source.manaAmount,
                requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null,
            )
        }
        val solution = manaSolver.solve(state, blockingPlayer, manaCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        val continuation = com.wingedsheep.engine.core.BlockTaxManaSelectionContinuation(
            blockingPlayer = blockingPlayer,
            blockers = blockers,
            manaCost = manaCost,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion,
        )
        return state.suspendForDecision(
            question = { decisionId ->
                com.wingedsheep.engine.core.SelectManaSourcesDecision(
                    id = decisionId,
                    playerId = blockingPlayer,
                    prompt = "Pay {$totalTax} to block with the declared creatures",
                    context = com.wingedsheep.engine.core.DecisionContext(
                        sourceId = null,
                        sourceName = "Block tax",
                        phase = com.wingedsheep.engine.core.DecisionPhase.COMBAT,
                    ),
                    availableSources = sourceOptions,
                    requiredCost = manaCost.toString(),
                    autoPaySuggestion = autoPaySuggestion,
                    canDecline = true,
                )
            },
            answer = continuation
        )
    }

}
