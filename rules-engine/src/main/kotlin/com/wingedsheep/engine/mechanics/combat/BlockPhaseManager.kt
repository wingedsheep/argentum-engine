package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
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
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.scripting.BlockTax
import com.wingedsheep.sdk.scripting.BlockerCountLimit
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CantBlockUnless
import java.util.UUID

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
) {
    private val conditionEvaluator = ConditionEvaluator()
    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

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

        // Calculate (but don't pay) the block tax. If non-zero, pause for the blocking
        // player to confirm — same reasoning as attack taxes: don't tap their mana
        // without consent.
        val projected = state.projectedState
        val totalBlockTax = calculatePerCreatureTax(state, blockers.keys, projected) +
            calculateBlockTax(state, blockers.keys, projected)
        if (totalBlockTax > 0) {
            return pauseForBlockTaxConfirmation(state, blockingPlayer, blockers, totalBlockTax)
        }

        return commitBlockDeclaration(state, blockingPlayer, blockers, taxEvents = emptyList())
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

        var newState = state
        // Capture legendary-ness of every combatant *now* (at block declaration), so the
        // "blocked or was blocked by a legendary creature this turn" marker (You Cannot Pass!)
        // reflects the pairing-time status even if a legendary partner later leaves or loses
        // legendary-ness (CR: the predicate looks at combat history).
        val projected = state.projectedState
        for ((blockerId, attackerIds) in expandedBlockers) {
            newState = newState.updateEntity(blockerId) { container ->
                container.with(BlockingComponent(attackerIds))
            }

            // Mark attackers as blocked
            for (attackerId in attackerIds) {
                newState = newState.updateEntity(attackerId) { container ->
                    val existing = container.get<BlockedComponent>()?.blockerIds ?: emptyList()
                    container.with(BlockedComponent(existing + blockerId))
                }
            }

            // Stamp the "paired with a legendary in combat this turn" marker on each side
            // whose partner is legendary.
            val blockerIsLegendary = projected.isLegendary(blockerId)
            for (attackerId in attackerIds) {
                if (projected.isLegendary(attackerId)) {
                    newState = newState.updateEntity(blockerId) { container ->
                        container.with(com.wingedsheep.engine.state.components.combat.BlockedOrWasBlockedByLegendaryThisTurnComponent)
                    }
                }
                if (blockerIsLegendary) {
                    newState = newState.updateEntity(attackerId) { container ->
                        container.with(com.wingedsheep.engine.state.components.combat.BlockedOrWasBlockedByLegendaryThisTurnComponent)
                    }
                }
            }
        }

        // Mark that blockers have been declared this combat (even if empty)
        newState = newState.updateEntity(blockingPlayer) { container ->
            container.with(BlockersDeclaredThisCombatComponent)
        }

        val blockerNameMap = expandedBlockers.keys.associateWith { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
        val attackerNameMap = expandedBlockers.values.flatten().distinct().associateWith { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
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
        val blockerCard = blockerContainer.get<CardComponent>() ?: return false

        val isFaceDown = blockerContainer.has<FaceDownComponent>()
        if (!isFaceDown && hasCantBlockAbility(blockerCard)) return false

        val projected = state.projectedState

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
        val controller = projected.getController(blockerId)
        if (controller != blockingPlayer) {
            return "You don't control ${cardComponent.name}"
        }

        if (container.has<TappedComponent>()) {
            return "${cardComponent.name} is tapped and cannot block"
        }

        if (container.has<BlockingComponent>()) {
            return "${cardComponent.name} is already blocking"
        }

        val isFaceDown = container.has<FaceDownComponent>()
        if (!isFaceDown) {
            val cantBlockValidation = validateCantBlock(cardComponent)
            if (cantBlockValidation != null) {
                return cantBlockValidation
            }
        }

        if (projected.cantBlock(blockerId)) {
            return "${cardComponent.name} can't block"
        }

        if (!isFaceDown) {
            val cantBlockUnlessError = validateCantBlockUnless(state, blockerId, blockingPlayer, projected)
            if (cantBlockUnlessError != null) return cantBlockUnlessError
        }

        if (attackerIds.size > 1) {
            val canBlockAny = if (!isFaceDown) {
                val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
                cardDef?.staticAbilities?.any { it is CanBlockAnyNumber } == true
            } else false
            if (!canBlockAny) {
                val additionalBlocks = projected.getAdditionalBlockCount(blockerId)
                val maxBlocks = 1 + additionalBlocks
                if (attackerIds.size > maxBlocks) {
                    val countText = if (maxBlocks == 1) "one creature" else "$maxBlocks creatures"
                    return "${cardComponent.name} can only block $countText"
                }
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
            val attackedDefender = CombatDefenders.defendingPlayerOf(state, attacking.defenderId)
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
     * Check if a creature has "can't block" ability (e.g., Craven Giant, Jungle Lion).
     */
    private fun validateCantBlock(blockerCard: CardComponent): String? {
        val cardDef = cardRegistry.getCard(blockerCard.cardDefinitionId) ?: return null
        val cantBlockAbility = cardDef.staticAbilities.filterIsInstance<CantBlock>().firstOrNull()
            ?: return null

        if (cantBlockAbility.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self) {
            return "${blockerCard.name} can't block"
        }

        return null
    }

    /**
     * Check if a creature has "can't block" ability.
     * Returns true if the creature cannot block.
     */
    private fun hasCantBlockAbility(blockerCard: CardComponent): Boolean {
        val cardDef = cardRegistry.getCard(blockerCard.cardDefinitionId) ?: return false
        val cantBlockAbility = cardDef.staticAbilities.filterIsInstance<CantBlock>().firstOrNull()
            ?: return false

        return cantBlockAbility.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self
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

        val blockerCard = blockerContainer.get<CardComponent>() ?: return false

        val isFaceDown = blockerContainer.has<FaceDownComponent>()
        if (!isFaceDown && hasCantBlockAbility(blockerCard)) {
            return false
        }

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
     * Each attacker with this static ability caps the number of creatures that may block it.
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

        for ((attackerId, count) in attackerToBlockerCount) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            if (attackerContainer.has<FaceDownComponent>()) continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(attackerCard.cardDefinitionId)

            // Printed "can't be blocked by more than N". cardDef may be null for tokens/copies
            // without a registered definition — the granted forms below still apply.
            val staticLimit = cardDef?.staticAbilities
                ?.filterIsInstance<CantBeBlockedByMoreThan>()
                ?.filter { it.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self }
                ?.minOfOrNull { it.maxBlockers }
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
            val limit = listOfNotNull(staticLimit, grantedLimit, flagLimit).minOrNull() ?: continue

            if (count > limit) {
                val countText = if (limit == 1) "more than one creature" else "more than $limit creatures"
                return "${attackerCard.name} can't be blocked by $countText"
            }
        }
        return null
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

        // 2. "Must be blocked if able" (Gaea's Protector): at least one creature must block it
        val mustBeBlockedIfAbleAttackers = findMustBeBlockedIfAbleAttackers(state)
        for (attackerId in mustBeBlockedIfAbleAttackers) {
            val blockersAssigned = attackerToBlockers[attackerId] ?: emptySet()
            if (blockersAssigned.isNotEmpty()) continue

            // Check if any potential blocker can actually block this attacker
            val canBeBlocked = potentialBlockers.any { blockerId ->
                canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
            }
            if (!canBeBlocked) continue

            val attackerName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$attackerName must be blocked if able"
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
            if (blockerContainer.has<TappedComponent>()) continue

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
     * evaluated with the attacker as the source.
     */
    private fun attackersWithMustBeBlockedStatic(state: GameState, allCreatures: Boolean): List<EntityId> {
        val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }
        return attackers.filter { attackerId ->
            val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.cardDefinitionId ?: return@filter false
            val statics = cardRegistry.getCard(cardName)?.staticAbilities.orEmpty()
            statics.any { ability ->
                val unwrapped = if (ability is ConditionalStaticAbility) ability.ability else ability
                if (unwrapped !is MustBeBlocked || unwrapped.allCreatures != allCreatures) return@any false
                if (ability is ConditionalStaticAbility) {
                    val controller = state.projectedState.getController(attackerId) ?: return@any false
                    conditionEvaluator.evaluate(
                        state,
                        ability.condition,
                        EffectContext(
                            sourceId = attackerId,
                            controllerId = controller
                        )
                    )
                } else true
            }
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
     * Find all potential blockers (untapped creatures controlled by the blocking player).
     */
    private fun findPotentialBlockers(state: GameState, blockingPlayer: EntityId): List<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield()
            .filter { entityId ->
                val container = state.getEntity(entityId) ?: return@filter false
                container.get<CardComponent>() ?: return@filter false
                val controller = projected.getController(entityId)

                projected.isCreature(entityId) &&
                    controller == blockingPlayer &&
                    !container.has<TappedComponent>()
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
        if (container.has<FaceDownComponent>()) return null
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
        if (container.has<FaceDownComponent>()) return false
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
        val manaSolver = com.wingedsheep.engine.mechanics.mana.ManaSolver(cardRegistry)
        val sources = manaSolver.findAvailableManaSources(state, blockingPlayer)
        val sourceOptions = sources.map { source ->
            com.wingedsheep.engine.core.ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless,
                requiresSacrifice = source.requiresSacrifice,
                requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null,
            )
        }
        val solution = manaSolver.solve(state, blockingPlayer, manaCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = com.wingedsheep.engine.core.SelectManaSourcesDecision(
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
        val continuation = com.wingedsheep.engine.core.BlockTaxManaSelectionContinuation(
            decisionId = decisionId,
            blockingPlayer = blockingPlayer,
            blockers = blockers,
            manaCost = manaCost,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion,
        )
        return ExecutionResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
        )
    }

    /**
     * Calculate per-creature tax from AttackBlockTaxPerCreatureType floating effects.
     */
    private fun calculatePerCreatureTax(
        state: GameState,
        creatureIds: Set<EntityId>,
        projected: ProjectedState
    ): Int {
        var totalTax = 0
        for (creatureId in creatureIds) {
            for (floatingEffect in state.floatingEffects) {
                val mod = floatingEffect.effect.modification
                if (mod !is SerializableModification.AttackBlockTaxPerCreatureType) continue
                if (creatureId !in floatingEffect.effect.affectedEntities) continue

                val creatureTypeCount = state.getBattlefield().count { entityId ->
                    projected.isCreature(entityId) && projected.hasSubtype(entityId, mod.creatureType)
                }
                val costPerCreature = ManaCost.parse(mod.manaCostPer).cmc
                totalTax += costPerCreature * creatureTypeCount
            }
        }
        return totalTax
    }

    /**
     * Calculate the generic-mana block tax from [BlockTax] static abilities (Archangel of Tithes —
     * "creatures can't block unless their controller pays {1} for each of those creatures").
     *
     * Unlike [AttackTax], this is a global restriction: every permanent on the battlefield with a
     * [BlockTax] ability (whose optional condition holds, e.g. "as long as this creature is
     * attacking") taxes each declared blocker by its per-blocker amount. Multiple sources stack.
     */
    private fun calculateBlockTax(
        state: GameState,
        blockerIds: Set<EntityId>,
        projected: ProjectedState
    ): Int {
        if (blockerIds.isEmpty()) return 0
        var totalTax = 0
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability !is BlockTax) continue
                val controllerId = projected.getController(entityId) ?: continue
                val ctx = EffectContext(
                    sourceId = entityId,
                    controllerId = controllerId,
                )
                val condition = ability.condition
                if (condition != null && !conditionEvaluator.evaluate(state, condition, ctx)) {
                    continue
                }
                val taxPerBlocker = maxOf(0, dynamicAmountEvaluator.evaluate(state, ability.amountPerBlocker, ctx, projected))
                totalTax += taxPerBlocker * blockerIds.size
            }
        }
        return totalTax
    }
}
