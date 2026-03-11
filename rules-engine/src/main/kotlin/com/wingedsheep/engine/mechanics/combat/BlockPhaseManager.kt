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
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.CombatCondition
import com.wingedsheep.sdk.scripting.StaticTarget
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
    private val cardRegistry: CardRegistry?,
    private val blockEvasionRules: List<BlockEvasionRule>,
) {

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

        // Check and pay block taxes from floating effects (Whipgrass Entangler)
        val blockTaxResult = validateAndPayBlockTaxes(state, blockingPlayer, blockers)
        if (!blockTaxResult.isSuccess) {
            return blockTaxResult
        }

        // Apply blocker components
        var newState = blockTaxResult.newState
        for ((blockerId, attackerIds) in blockers) {
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
        }

        // Mark that blockers have been declared this combat (even if empty)
        newState = newState.updateEntity(blockingPlayer) { container ->
            container.with(BlockersDeclaredThisCombatComponent)
        }

        val blockerNameMap = blockers.keys.associateWith { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
        val attackerNameMap = blockers.values.flatten().distinct().associateWith { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
        val blockersEvent = BlockersDeclaredEvent(blockers, blockerNameMap, attackerNameMap)
        val blockTaxEvents = blockTaxResult.events

        // Per MTG CR 509.2: After blockers are declared, the attacking player must
        // declare damage assignment order for each attacker blocked by 2+ creatures
        val attackersNeedingOrder = findAttackersWithMultipleBlockers(newState)
        if (attackersNeedingOrder.isNotEmpty()) {
            val attackingPlayer = state.activePlayerId!!
            return createBlockerOrderDecision(
                newState,
                attackingPlayer = attackingPlayer,
                firstAttacker = attackersNeedingOrder.first(),
                remainingAttackers = attackersNeedingOrder.drop(1),
                precedingEvents = blockTaxEvents + blockersEvent
            )
        }

        // Per MTG CR 509.3: The attacking player must also declare damage assignment
        // order for each blocker that blocks 2+ attacking creatures
        val blockersNeedingOrder = findBlockersWithMultipleAttackers(newState)
        if (blockersNeedingOrder.isNotEmpty()) {
            val attackingPlayer = state.activePlayerId!!
            return createAttackerOrderDecision(
                newState,
                attackingPlayer = attackingPlayer,
                firstBlocker = blockersNeedingOrder.first(),
                remainingBlockers = blockersNeedingOrder.drop(1),
                precedingEvents = blockTaxEvents + blockersEvent
            )
        }

        return ExecutionResult.success(
            newState,
            blockTaxEvents + blockersEvent
        )
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
                val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
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

        // Check evasion abilities of each attacker
        for (attackerId in attackerIds) {
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
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return null
        val cantBlockAbility = cardDef.staticAbilities.filterIsInstance<CantBlock>().firstOrNull()
            ?: return null

        if (cantBlockAbility.target == StaticTarget.SourceCreature) {
            return "${blockerCard.name} can't block"
        }

        return null
    }

    /**
     * Check if a creature has "can't block" ability.
     * Returns true if the creature cannot block.
     */
    private fun hasCantBlockAbility(blockerCard: CardComponent): Boolean {
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return false
        val cantBlockAbility = cardDef.staticAbilities.filterIsInstance<CantBlock>().firstOrNull()
            ?: return false

        return cantBlockAbility.target == StaticTarget.SourceCreature
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

    // =========================================================================
    // Must Be Blocked Requirements
    // =========================================================================

    /**
     * Validate "must be blocked" requirements.
     */
    private fun validateMustBeBlockedRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val mustBeBlockedAttackers = findMustBeBlockedAttackers(state)
        if (mustBeBlockedAttackers.isEmpty()) {
            return null
        }

        val projected = state.projectedState
        val potentialBlockers = findPotentialBlockers(state, blockingPlayer)
        val blockerToAttackers = blockers.mapValues { it.value.toSet() }

        for (blockerId in potentialBlockers) {
            val canBlockThese = mustBeBlockedAttackers.filter { attackerId ->
                canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
            }

            if (canBlockThese.isEmpty()) {
                continue
            }

            val actuallyBlocking = blockerToAttackers[blockerId] ?: emptySet()
            val blockingMustBeBlocked = actuallyBlocking.intersect(mustBeBlockedAttackers.toSet())

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
    // Blocker Order Decision
    // =========================================================================

    /**
     * Find all attackers that are blocked by 2 or more creatures.
     */
    private fun findAttackersWithMultipleBlockers(state: GameState): List<EntityId> {
        return state.findEntitiesWith<BlockedComponent>()
            .filter { (_, blocked) -> blocked.blockerIds.size >= 2 }
            .map { it.first }
    }

    /**
     * Find all blockers that are blocking 2 or more attacking creatures.
     */
    fun findBlockersWithMultipleAttackers(state: GameState): List<EntityId> {
        return state.findEntitiesWith<BlockingComponent>()
            .filter { (_, blocking) -> blocking.blockedAttackerIds.size >= 2 }
            .map { it.first }
    }

    /**
     * Create a pending decision for the attacking player to order blockers.
     */
    private fun createBlockerOrderDecision(
        state: GameState,
        attackingPlayer: EntityId,
        firstAttacker: EntityId,
        remainingAttackers: List<EntityId>,
        precedingEvents: List<GameEvent>
    ): ExecutionResult {
        val attackerContainer = state.getEntity(firstAttacker)!!
        val attackerCard = attackerContainer.get<CardComponent>()!!
        val attackerIsFaceDown = attackerContainer.has<FaceDownComponent>()
        val attackerDisplayName = if (attackerIsFaceDown) "Face-down creature" else attackerCard.name
        val blockedComponent = attackerContainer.get<BlockedComponent>()!!
        val blockerIds = blockedComponent.blockerIds

        val cardInfo = blockerIds.associateWith { blockerId ->
            val blockerContainer = state.getEntity(blockerId)
            val isFaceDown = blockerContainer?.has<FaceDownComponent>() == true
            if (isFaceDown) {
                SearchCardInfo(
                    name = "Morph",
                    manaCost = "{3}",
                    typeLine = "Creature"
                )
            } else {
                val blockerCard = blockerContainer?.get<CardComponent>()
                SearchCardInfo(
                    name = blockerCard?.name ?: "Unknown",
                    manaCost = blockerCard?.manaCost?.toString() ?: "",
                    typeLine = blockerCard?.typeLine?.toString() ?: ""
                )
            }
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = OrderObjectsDecision(
            id = decisionId,
            playerId = attackingPlayer,
            prompt = "Order damage assignment for $attackerDisplayName",
            context = DecisionContext(
                sourceId = firstAttacker,
                sourceName = attackerDisplayName,
                phase = DecisionPhase.COMBAT
            ),
            objects = blockerIds,
            cardInfo = cardInfo
        )

        val continuation = BlockerOrderContinuation(
            decisionId = decisionId,
            attackingPlayerId = attackingPlayer,
            attackerId = firstAttacker,
            attackerName = attackerCard.name,
            remainingAttackers = remainingAttackers
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            precedingEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = attackingPlayer,
                    decisionType = "ORDER_BLOCKERS",
                    prompt = decision.prompt
                )
            )
        )
    }

    // =========================================================================
    // Attacker Order Decision (for blockers blocking multiple attackers)
    // =========================================================================

    /**
     * Create a pending decision for the attacking player to order their attackers
     * for a blocker's damage assignment (CR 509.3).
     */
    fun createAttackerOrderDecision(
        state: GameState,
        attackingPlayer: EntityId,
        firstBlocker: EntityId,
        remainingBlockers: List<EntityId>,
        precedingEvents: List<GameEvent>
    ): ExecutionResult {
        val blockerContainer = state.getEntity(firstBlocker)!!
        val blockerCard = blockerContainer.get<CardComponent>()!!
        val blockerIsFaceDown = blockerContainer.has<FaceDownComponent>()
        val blockerDisplayName = if (blockerIsFaceDown) "Face-down creature" else blockerCard.name
        val blockingComponent = blockerContainer.get<BlockingComponent>()!!
        val attackerIds = blockingComponent.blockedAttackerIds

        val cardInfo = attackerIds.associateWith { attackerId ->
            val attackerContainer = state.getEntity(attackerId)
            val isFaceDown = attackerContainer?.has<FaceDownComponent>() == true
            if (isFaceDown) {
                SearchCardInfo(
                    name = "Morph",
                    manaCost = "{3}",
                    typeLine = "Creature"
                )
            } else {
                val attackerCard = attackerContainer?.get<CardComponent>()
                SearchCardInfo(
                    name = attackerCard?.name ?: "Unknown",
                    manaCost = attackerCard?.manaCost?.toString() ?: "",
                    typeLine = attackerCard?.typeLine?.toString() ?: ""
                )
            }
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = OrderObjectsDecision(
            id = decisionId,
            playerId = attackingPlayer,
            prompt = "Order damage assignment for $blockerDisplayName's blocked attackers",
            context = DecisionContext(
                sourceId = firstBlocker,
                sourceName = blockerDisplayName,
                phase = DecisionPhase.COMBAT
            ),
            objects = attackerIds,
            cardInfo = cardInfo
        )

        val continuation = AttackerOrderContinuation(
            decisionId = decisionId,
            attackingPlayerId = attackingPlayer,
            blockerId = firstBlocker,
            blockerName = blockerDisplayName,
            remainingBlockers = remainingBlockers
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            precedingEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = attackingPlayer,
                    decisionType = "ORDER_ATTACKERS",
                    prompt = decision.prompt
                )
            )
        )
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Find all attackers that have "must be blocked by all" requirement active.
     */
    private fun findMustBeBlockedAttackers(state: GameState): List<EntityId> {
        val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }.toSet()

        return state.floatingEffects
            .filter { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.MustBeBlockedByAll
            }
            .flatMap { floatingEffect ->
                floatingEffect.effect.affectedEntities.filter { it in attackers }
            }
            .distinct()
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
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId) ?: return null

        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantBlockUnless>()
            .firstOrNull { it.target == StaticTarget.SourceCreature } ?: return null

        val attackers = state.entities.filter { (_, c) -> c.has<AttackingComponent>() }
        if (attackers.isEmpty()) return null

        val anyAttacker = attackers.keys.first()
        val attackingPlayer = projected.getController(anyAttacker) ?: return null

        if (!evaluateCombatCondition(restriction.condition, state, blockingPlayer, attackingPlayer, projected)) {
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
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId) ?: return false

        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantBlockUnless>()
            .firstOrNull { it.target == StaticTarget.SourceCreature } ?: return false

        val attackers = state.entities.filter { (_, c) -> c.has<AttackingComponent>() }
        if (attackers.isEmpty()) return false

        val anyAttacker = attackers.keys.first()
        val attackingPlayer = projected.getController(anyAttacker) ?: return false

        return !evaluateCombatCondition(restriction.condition, state, blockingPlayer, attackingPlayer, projected)
    }

    private fun evaluateCombatCondition(
        condition: CombatCondition,
        state: GameState,
        controllerId: EntityId,
        opponentId: EntityId,
        projected: ProjectedState
    ): Boolean = when (condition) {
        is CombatCondition.ControlMoreCreatures -> {
            countCreatures(state, controllerId, projected) > countCreatures(state, opponentId, projected)
        }
        is CombatCondition.OpponentControlsLandType -> {
            state.getBattlefield().any { entityId ->
                projected.getController(entityId) == opponentId &&
                    projected.getProjectedValues(entityId)?.subtypes
                        ?.any { it.equals(condition.landType, ignoreCase = true) } == true
            }
        }
    }

    private fun countCreatures(state: GameState, playerId: EntityId, projected: ProjectedState): Int {
        return state.getBattlefield().count { entityId ->
            projected.isCreature(entityId) && projected.getController(entityId) == playerId
        }
    }

    // =========================================================================
    // Block Taxes
    // =========================================================================

    /**
     * Validate and pay block taxes from per-creature floating effects (Whipgrass Entangler).
     */
    private fun validateAndPayBlockTaxes(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): ExecutionResult {
        if (blockers.isEmpty()) return ExecutionResult.success(state)

        val projected = state.projectedState
        val totalGenericTax = calculatePerCreatureTax(state, blockers.keys, projected)

        if (totalGenericTax <= 0) return ExecutionResult.success(state)

        return payManaTax(state, blockingPlayer, totalGenericTax, "block")
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
}
