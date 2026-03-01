package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword
import com.wingedsheep.sdk.scripting.CantBeBlockedByPower
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptByKeyword
import com.wingedsheep.sdk.scripting.CantBeBlockedUnlessDefenderSharesCreatureType
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CantBlockCreaturesWithGreaterPower
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.CombatCondition
import com.wingedsheep.sdk.scripting.DivideCombatDamageFreely
import com.wingedsheep.sdk.scripting.StaticTarget
import java.util.UUID

/**
 * Manages combat flow: attackers, blockers, damage.
 *
 * Combat proceeds through these steps:
 * 1. Beginning of combat step
 * 2. Declare attackers step
 * 3. Declare blockers step
 * 4. Combat damage step (first strike, then regular)
 * 5. End of combat step
 */
class CombatManager(
    private val cardRegistry: CardRegistry? = null,
    private val damageCalculator: DamageCalculator = DamageCalculator(),
    private val stateProjector: StateProjector = StateProjector()
) {

    // =========================================================================
    // Declare Attackers
    // =========================================================================

    /**
     * Validate and declare attackers.
     *
     * @param attackers Map of attacker entity ID to defender (player or planeswalker)
     */
    fun declareAttackers(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): ExecutionResult {
        // Validate each attacker
        val projected = stateProjector.project(state)
        for ((attackerId, defenderId) in attackers) {
            val validation = validateAttacker(state, attackingPlayer, attackerId)
            if (validation != null) {
                return ExecutionResult.error(state, validation)
            }
            // Check creature count attack restrictions (e.g., Goblin Goon)
            val creatureCountValidation = validateCantAttackUnless(
                state, attackerId, attackingPlayer, defenderId, projected
            )
            if (creatureCountValidation != null) {
                return ExecutionResult.error(state, creatureCountValidation)
            }
        }

        // Check must-attack requirements (Taunt)
        val mustAttackValidation = validateMustAttackRequirements(state, attackingPlayer, attackers)
        if (mustAttackValidation != null) {
            return ExecutionResult.error(state, mustAttackValidation)
        }

        // Check must-attack-this-turn requirements (Walking Desecration)
        val mustAttackThisTurnValidation = validateMustAttackThisTurnRequirements(state, attackingPlayer, attackers)
        if (mustAttackThisTurnValidation != null) {
            return ExecutionResult.error(state, mustAttackThisTurnValidation)
        }

        // Check projected must-attack requirements (Grand Melee)
        val projectedMustAttackValidation = validateProjectedMustAttackRequirements(state, attackingPlayer, attackers)
        if (projectedMustAttackValidation != null) {
            return ExecutionResult.error(state, projectedMustAttackValidation)
        }

        // Apply attacker components and tap attacking creatures
        var newState = state
        for ((attackerId, defenderId) in attackers) {
            newState = newState.updateEntity(attackerId) { container ->
                var updated = container.with(AttackingComponent(defenderId))
                // Tap attacking creatures (unless they have vigilance)
                val hasVigilance = projected.hasKeyword(attackerId, Keyword.VIGILANCE)
                if (!hasVigilance) {
                    updated = updated.with(TappedComponent)
                }
                updated
            }
        }

        // Mark that attackers have been declared this combat (even if empty)
        newState = newState.updateEntity(attackingPlayer) { container ->
            var updated = container.with(AttackersDeclaredThisCombatComponent)
            // Track that this player attacked this turn (for Raid and similar mechanics)
            if (attackers.isNotEmpty()) {
                updated = updated.with(PlayerAttackedThisTurnComponent)
            }
            updated
        }

        val attackerNames = attackers.keys.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
        return ExecutionResult.success(
            newState,
            listOf(AttackersDeclaredEvent(attackers.keys.toList(), attackerNames, attackingPlayer))
        )
    }

    /**
     * Validate that a creature can attack.
     */
    private fun validateAttacker(
        state: GameState,
        attackingPlayer: EntityId,
        attackerId: EntityId
    ): String? {
        val container = state.getEntity(attackerId)
            ?: return "Attacker not found: $attackerId"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: $attackerId"

        // Use projected state for controller, keywords, and types (includes floating effects like animate land)
        val projected = stateProjector.project(state)

        // Must be a creature (use projected types to handle animated lands etc.)
        if (!projected.isCreature(attackerId)) {
            return "Only creatures can attack: ${cardComponent.name}"
        }

        // Must be controlled by attacking player (use projected controller for control-changing effects)
        val controller = projected.getController(attackerId)
        if (controller != attackingPlayer) {
            return "You don't control ${cardComponent.name}"
        }

        // Must be untapped
        if (container.has<TappedComponent>()) {
            return "${cardComponent.name} is tapped and cannot attack"
        }

        // Cannot have summoning sickness (unless it has haste)
        val hasHaste = projected.hasKeyword(attackerId, Keyword.HASTE)
        if (!hasHaste && container.has<SummoningSicknessComponent>()) {
            return "${cardComponent.name} has summoning sickness"
        }

        // Cannot have defender
        if (projected.hasKeyword(attackerId, Keyword.DEFENDER)) {
            return "${cardComponent.name} has defender and cannot attack"
        }

        // Cannot have "can't attack" (e.g., from Pacifism)
        if (projected.cantAttack(attackerId)) {
            return "${cardComponent.name} can't attack"
        }

        // Cannot be already attacking
        if (container.has<AttackingComponent>()) {
            return "${cardComponent.name} is already attacking"
        }

        return null
    }

    /**
     * Validate "must attack" requirements (Taunt effect).
     *
     * When a player has MustAttackPlayerComponent active:
     * - All creatures that CAN attack MUST be declared as attackers
     * - All declared attackers MUST attack the specified defender
     *
     * @return Error message if requirements not met, null if valid
     */
    private fun validateMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val mustAttack = state.getEntity(attackingPlayer)?.get<MustAttackPlayerComponent>()
            ?: return null  // No must-attack requirement

        // Only enforce if active this turn
        if (!mustAttack.activeThisTurn) {
            return null
        }

        val requiredDefender = mustAttack.defenderId

        // Get all creatures that can legally attack
        val validAttackers = getValidAttackers(state, attackingPlayer)

        // All valid attackers must be declared
        for (attackerId in validAttackers) {
            if (attackerId !in attackers.keys) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn (Taunt)"
            }
        }

        // All declared attackers must attack the required defender
        for ((attackerId, defenderId) in attackers) {
            if (defenderId != requiredDefender) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                val defenderName = state.getEntity(requiredDefender)?.get<CardComponent>()?.name ?: "that player"
                return "$cardName must attack $defenderName (Taunt)"
            }
        }

        return null
    }

    /**
     * Validate "must attack this turn" requirements for individual creatures.
     *
     * When creatures have MustAttackThisTurnComponent (e.g., from Walking Desecration):
     * - Each such creature that CAN attack MUST be declared as an attacker
     * - They can attack any legal defender (unlike Taunt which requires a specific one)
     *
     * @return Error message if requirements not met, null if valid
     */
    private fun validateMustAttackThisTurnRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)

        for (attackerId in validAttackers) {
            val container = state.getEntity(attackerId) ?: continue
            if (!container.has<MustAttackThisTurnComponent>()) continue

            // This creature must attack this turn
            if (attackerId !in attackers.keys) {
                val cardName = container.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn"
            }
        }

        return null
    }

    /**
     * Validate projected "must attack" requirements (e.g., from Grand Melee).
     * Creatures with projected mustAttack that CAN attack MUST be declared as attackers.
     */
    private fun validateProjectedMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val projected = stateProjector.project(state)

        for (attackerId in validAttackers) {
            if (!projected.mustAttack(attackerId)) continue

            if (attackerId !in attackers.keys) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this combat if able"
            }
        }

        return null
    }

    /**
     * Validate projected "must block" requirements (e.g., from Grand Melee).
     * Creatures with projected mustBlock that CAN block an attacker MUST be declared as blockers.
     */
    private fun validateProjectedMustBlockRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val projected = stateProjector.project(state)
        val potentialBlockers = findPotentialBlockers(state, blockingPlayer)

        for (blockerId in potentialBlockers) {
            if (!projected.mustBlock(blockerId)) continue

            // Check if this creature can legally block any attacker
            val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }
            val canBlockAny = attackers.any { attackerId ->
                canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
            }

            if (!canBlockAny) continue

            // This creature must block but isn't blocking anything
            if (blockerId !in blockers.keys) {
                val cardName = state.getEntity(blockerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must block this combat if able"
            }
        }

        return null
    }

    /**
     * Get all creatures that can legally attack for a player.
     * Used for validating must-attack requirements.
     */
    private fun getValidAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        val battlefield = state.getBattlefield()
        val projected = stateProjector.project(state)

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controller = projected.getController(entityId)

            // Must be a creature controlled by the player (use projected types for animated lands etc.)
            if (!projected.isCreature(entityId) || controller != playerId) {
                return@filter false
            }

            // Must be untapped
            if (container.has<TappedComponent>()) {
                return@filter false
            }

            // Check projected keywords for Haste/Defender
            val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
            val hasDefender = projected.hasKeyword(entityId, Keyword.DEFENDER)

            // Must not have summoning sickness (unless it has haste)
            if (!hasHaste && container.has<SummoningSicknessComponent>()) {
                return@filter false
            }

            // Must not have defender or "can't attack"
            if (hasDefender || projected.cantAttack(entityId)) {
                return@filter false
            }

            // Check creature count attack restriction (e.g., Goblin Goon)
            if (hasCantAttackUnlessRestriction(state, entityId, playerId, projected)) {
                return@filter false
            }

            true
        }
    }

    // =========================================================================
    // Declare Blockers
    // =========================================================================

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

        // Apply blocker components
        var newState = state
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

        // Per MTG CR 509.2: After blockers are declared, the attacking player must
        // declare damage assignment order for each attacker blocked by 2+ creatures
        val attackersNeedingOrder = findAttackersWithMultipleBlockers(newState)
        if (attackersNeedingOrder.isNotEmpty()) {
            // The active player is always the attacking player during combat
            val attackingPlayer = state.activePlayerId!!
            return createBlockerOrderDecision(
                newState,
                attackingPlayer = attackingPlayer,
                firstAttacker = attackersNeedingOrder.first(),
                remainingAttackers = attackersNeedingOrder.drop(1),
                precedingEvents = listOf(blockersEvent)
            )
        }

        return ExecutionResult.success(
            newState,
            listOf(blockersEvent)
        )
    }

    /**
     * Find all attackers that are blocked by 2 or more creatures.
     * These attackers need damage assignment order to be declared.
     */
    private fun findAttackersWithMultipleBlockers(state: GameState): List<EntityId> {
        return state.findEntitiesWith<BlockedComponent>()
            .filter { (_, blocked) -> blocked.blockerIds.size >= 2 }
            .map { it.first }
    }

    /**
     * Create a pending decision for the attacking player to order blockers.
     *
     * Per MTG CR 509.2, after blockers are declared, the attacking player must
     * declare the damage assignment order for each attacking creature that's
     * blocked by multiple creatures.
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
        val blockedComponent = attackerContainer.get<BlockedComponent>()!!
        val blockerIds = blockedComponent.blockerIds

        // Build card info for blockers (for UI display)
        // Face-down creatures must not reveal their identity
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
            prompt = "Order damage assignment for ${attackerCard.name}",
            context = DecisionContext(
                sourceId = firstAttacker,
                sourceName = attackerCard.name,
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

        // Must be controlled by blocking player (use projected controller for control-changing effects)
        val projected = stateProjector.project(state)

        // Must be a creature (use projected types to handle animated lands etc.)
        if (!projected.isCreature(blockerId)) {
            return "Only creatures can block: ${cardComponent.name}"
        }
        val controller = projected.getController(blockerId)
        if (controller != blockingPlayer) {
            return "You don't control ${cardComponent.name}"
        }

        // Must be untapped
        if (container.has<TappedComponent>()) {
            return "${cardComponent.name} is tapped and cannot block"
        }

        // Cannot be already blocking
        if (container.has<BlockingComponent>()) {
            return "${cardComponent.name} is already blocking"
        }

        // Check if the blocker has "can't block" restriction (e.g., Craven Giant, Jungle Lion)
        // Face-down creatures have no abilities, so they can block
        val isFaceDown = container.has<FaceDownComponent>()
        if (!isFaceDown) {
            val cantBlockValidation = validateCantBlock(cardComponent)
            if (cantBlockValidation != null) {
                return cantBlockValidation
            }
        }

        // Check projected "can't block" (e.g., from Pacifism aura)
        if (projected.cantBlock(blockerId)) {
            return "${cardComponent.name} can't block"
        }

        // Check CantBlockUnless restriction (e.g., Goblin Goon)
        if (!isFaceDown) {
            val cantBlockUnlessError = validateCantBlockUnless(state, blockerId, blockingPlayer, projected)
            if (cantBlockUnlessError != null) return cantBlockUnlessError
        }

        // Face-down creatures lose all abilities, so keyword/power block restrictions don't apply
        if (!isFaceDown) {
            // Check if the blocker can only block creatures with a specific keyword (e.g., Cloud Pirates)
            val canOnlyBlockValidation = validateCanOnlyBlockWithKeyword(state, cardComponent, attackerIds, projected)
            if (canOnlyBlockValidation != null) {
                return canOnlyBlockValidation
            }

            // Check if blocker can't block creatures with greater power (e.g., Spitfire Handler)
            val cantBlockGreaterPowerValidation = validateCantBlockCreaturesWithGreaterPower(
                state, blockerId, cardComponent, attackerIds, projected
            )
            if (cantBlockGreaterPowerValidation != null) {
                return cantBlockGreaterPowerValidation
            }
        }

        // Check if creature is trying to block multiple attackers without CanBlockAnyNumber
        if (attackerIds.size > 1) {
            val canBlockMultiple = if (!isFaceDown) {
                val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
                cardDef?.staticAbilities?.any { it is CanBlockAnyNumber } == true
            } else false
            if (!canBlockMultiple) {
                return "${cardComponent.name} can only block one creature"
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
     */
    private fun validateCanBlock(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        blockingPlayer: EntityId
    ): String? {
        val blockerContainer = state.getEntity(blockerId)!!
        val attackerContainer = state.getEntity(attackerId)
            ?: return "Attacker not found: $attackerId"

        val blockerCard = blockerContainer.get<CardComponent>()!!
        val attackerCard = attackerContainer.get<CardComponent>()
            ?: return "Not a card: $attackerId"

        // Use projected keywords (includes floating effects)
        val projected = stateProjector.project(state)

        // Unblockable: Cannot be blocked at all
        if (projected.hasKeyword(attackerId, com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_BLOCKED)) {
            return "${attackerCard.name} can't be blocked"
        }

        // Flying: Can only be blocked by creatures with flying or reach
        if (projected.hasKeyword(attackerId, Keyword.FLYING)) {
            val canBlockFlying = projected.hasKeyword(blockerId, Keyword.FLYING) ||
                projected.hasKeyword(blockerId, Keyword.REACH)
            if (!canBlockFlying) {
                return "${blockerCard.name} cannot block ${attackerCard.name} (flying)"
            }
        }

        // Horsemanship: Can only be blocked by creatures with horsemanship
        if (projected.hasKeyword(attackerId, Keyword.HORSEMANSHIP)) {
            if (!projected.hasKeyword(blockerId, Keyword.HORSEMANSHIP)) {
                return "${blockerCard.name} cannot block ${attackerCard.name} (horsemanship)"
            }
        }

        // Shadow: Can only be blocked by creatures with shadow
        if (projected.hasKeyword(attackerId, Keyword.SHADOW)) {
            if (!projected.hasKeyword(blockerId, Keyword.SHADOW)) {
                return "${blockerCard.name} cannot block ${attackerCard.name} (shadow)"
            }
        }

        // Landwalk: Cannot be blocked if defending player controls land of that type
        val landwalkValidation = validateLandwalk(state, attackerId, attackerCard, blockingPlayer, projected)
        if (landwalkValidation != null) {
            return landwalkValidation
        }

        // CantBeBlockedByPower: Cannot be blocked by creatures with power >= N
        val powerRestrictionValidation = validateCantBeBlockedByPower(
            attackerId, attackerCard, blockerId, blockerCard, projected
        )
        if (powerRestrictionValidation != null) {
            return powerRestrictionValidation
        }

        // CantBeBlockedExceptByColor: Can only be blocked by creatures of the specified color
        val colorRestrictionValidation = validateCantBeBlockedExceptByColor(
            state, attackerId, attackerCard, blockerId, blockerCard
        )
        if (colorRestrictionValidation != null) {
            return colorRestrictionValidation
        }

        // CantBeBlockedExceptByKeyword: Can only be blocked by creatures with a specific keyword
        val keywordEvasionValidation = validateCantBeBlockedExceptByKeyword(
            attackerId, attackerCard, blockerId, blockerCard, projected
        )
        if (keywordEvasionValidation != null) {
            return keywordEvasionValidation
        }

        // CantBeBlockedUnlessDefenderSharesCreatureType: e.g. Graxiplon
        val sharedTypeRestrictionValidation = validateCantBeBlockedUnlessDefenderSharesCreatureType(
            state, attackerId, attackerCard, blockingPlayer, projected
        )
        if (sharedTypeRestrictionValidation != null) {
            return sharedTypeRestrictionValidation
        }

        // Skulk: Cannot be blocked by creatures with greater power
        // TODO: Implement skulk

        // Fear: Can only be blocked by artifact creatures or black creatures
        if (projected.hasKeyword(attackerId, Keyword.FEAR)) {
            val isArtifactCreature = blockerCard.typeLine.isArtifactCreature
            val isBlackCreature = Color.BLACK in blockerCard.colors
            if (!isArtifactCreature && !isBlackCreature) {
                return "${blockerCard.name} cannot block ${attackerCard.name} (fear)"
            }
        }

        // Intimidate: Can only be blocked by artifact creatures or creatures sharing a color
        // TODO: Implement intimidate

        // Protection from color: Can't be blocked by creatures of the stated color (Rule 702.16)
        for (colorName in projected.getColors(blockerId)) {
            if (projected.hasKeyword(attackerId, "PROTECTION_FROM_$colorName")) {
                return "${attackerCard.name} has protection from ${colorName.lowercase()} and can't be blocked by ${blockerCard.name}"
            }
        }

        // Protection from creature subtype: Can't be blocked by creatures of the stated subtype
        for (subtype in projected.getSubtypes(blockerId)) {
            if (projected.hasKeyword(attackerId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                return "${attackerCard.name} has protection from ${subtype.lowercase()}s and can't be blocked by ${blockerCard.name}"
            }
        }

        return null
    }

    /**
     * Check if attacker has landwalk and defending player controls the corresponding land type.
     * Returns an error message if the attacker cannot be blocked due to landwalk, null otherwise.
     */
    private fun validateLandwalk(
        state: GameState,
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockingPlayer: EntityId,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState
    ): String? {
        // Map of landwalk keywords to their corresponding land subtypes
        val landwalkToSubtype = mapOf(
            Keyword.FORESTWALK to com.wingedsheep.sdk.core.Subtype.FOREST,
            Keyword.SWAMPWALK to com.wingedsheep.sdk.core.Subtype.SWAMP,
            Keyword.ISLANDWALK to com.wingedsheep.sdk.core.Subtype.ISLAND,
            Keyword.MOUNTAINWALK to com.wingedsheep.sdk.core.Subtype.MOUNTAIN,
            Keyword.PLAINSWALK to com.wingedsheep.sdk.core.Subtype.PLAINS
        )

        for ((landwalkKeyword, landSubtype) in landwalkToSubtype) {
            if (projected.hasKeyword(attackerId, landwalkKeyword)) {
                // Check if defending player controls a land with this subtype
                if (playerControlsLandWithSubtype(state, blockingPlayer, landSubtype)) {
                    return "${attackerCard.name} has ${landwalkKeyword.displayName} and cannot be blocked"
                }
            }
        }

        return null
    }

    /**
     * Check if a player controls a land with the given subtype.
     */
    private fun playerControlsLandWithSubtype(
        state: GameState,
        playerId: EntityId,
        landSubtype: com.wingedsheep.sdk.core.Subtype
    ): Boolean {
        val projected = stateProjector.project(state)
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            val cardComponent = container.get<CardComponent>() ?: return@any false
            val controller = projected.getController(entityId)

            controller == playerId &&
                cardComponent.typeLine.isLand &&
                cardComponent.typeLine.hasSubtype(landSubtype)
        }
    }

    /**
     * Check if attacker has CantBeBlockedByPower restriction and blocker's power meets/exceeds it.
     * Returns an error message if the blocker cannot block due to power restriction, null otherwise.
     *
     * Uses projected power to account for spells that modify power (e.g., Giant Growth).
     */
    private fun validateCantBeBlockedByPower(
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockerId: EntityId,
        blockerCard: CardComponent,
        projected: ProjectedState
    ): String? {
        // Get attacker's static abilities from card definition
        val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
        val powerRestriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedByPower>().firstOrNull()
            ?: return null

        // Get blocker's projected power (includes buffs from spells like Giant Growth)
        val blockerPower = projected.getPower(blockerId) ?: 0

        if (blockerPower >= powerRestriction.minPower) {
            return "${blockerCard.name} cannot block ${attackerCard.name} (power $blockerPower or greater)"
        }

        return null
    }

    /**
     * Check if attacker has CantBeBlockedByPower restriction and blocker's power meets/exceeds it.
     * Returns false if the blocker cannot block due to power restriction.
     *
     * Uses projected power to account for spells that modify power.
     */
    private fun canBlockDespitePowerRestriction(
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockerId: EntityId,
        projected: ProjectedState
    ): Boolean {
        // Get attacker's static abilities from card definition
        val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return true
        val powerRestriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedByPower>().firstOrNull()
            ?: return true

        // Get blocker's projected power (includes buffs from spells like Giant Growth)
        val blockerPower = projected.getPower(blockerId) ?: 0

        return blockerPower < powerRestriction.minPower
    }

    /**
     * Check if attacker has CantBeBlockedExceptByColor restriction from a floating effect.
     * Returns an error message if the blocker cannot block due to color restriction, null otherwise.
     */
    private fun validateCantBeBlockedExceptByColor(
        state: GameState,
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockerId: EntityId,
        blockerCard: CardComponent
    ): String? {
        // Find floating effects with CantBeBlockedExceptByColor that affect this attacker
        val colorRestriction = state.floatingEffects
            .filter { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.CantBeBlockedExceptByColor &&
                    attackerId in floatingEffect.effect.affectedEntities
            }
            .map { it.effect.modification as SerializableModification.CantBeBlockedExceptByColor }
            .firstOrNull()
            ?: return null

        // Check if blocker has the required color
        val requiredColor = com.wingedsheep.sdk.core.Color.valueOf(colorRestriction.color)
        if (!blockerCard.colors.contains(requiredColor)) {
            return "${blockerCard.name} cannot block ${attackerCard.name} (can only be blocked by ${requiredColor.displayName.lowercase()} creatures)"
        }

        return null
    }

    /**
     * Check if blocker has the required color to block an attacker with CantBeBlockedExceptByColor.
     * Returns true if the blocker can block despite the color restriction.
     */
    private fun canBlockDespiteColorRestriction(
        state: GameState,
        attackerId: EntityId,
        blockerId: EntityId
    ): Boolean {
        val blockerCard = state.getEntity(blockerId)?.get<CardComponent>() ?: return true

        // Find floating effects with CantBeBlockedExceptByColor that affect this attacker
        val colorRestriction = state.floatingEffects
            .filter { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.CantBeBlockedExceptByColor &&
                    attackerId in floatingEffect.effect.affectedEntities
            }
            .map { it.effect.modification as SerializableModification.CantBeBlockedExceptByColor }
            .firstOrNull()
            ?: return true

        // Check if blocker has the required color
        val requiredColor = com.wingedsheep.sdk.core.Color.valueOf(colorRestriction.color)
        return blockerCard.colors.contains(requiredColor)
    }

    /**
     * Check if attacker has CantBeBlockedExceptByKeyword restriction and blocker lacks the keyword.
     * Returns an error message if the blocker cannot block due to keyword evasion, null otherwise.
     */
    private fun validateCantBeBlockedExceptByKeyword(
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockerId: EntityId,
        blockerCard: CardComponent,
        projected: ProjectedState
    ): String? {
        val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
        val keywordRestriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedExceptByKeyword>().firstOrNull()
            ?: return null

        val requiredKeyword = keywordRestriction.requiredKeyword

        // Check if blocker has the required keyword
        if (projected.hasKeyword(blockerId, requiredKeyword)) return null

        // Per MTG rules, reach allows blocking creatures with "can't be blocked except by flying"
        if (requiredKeyword == Keyword.FLYING && projected.hasKeyword(blockerId, Keyword.REACH)) return null

        return "${blockerCard.name} cannot block ${attackerCard.name} (can only be blocked by creatures with ${requiredKeyword.displayName.lowercase()})"
    }

    /**
     * Check if blocker can block despite CantBeBlockedExceptByKeyword evasion.
     * Returns true if the blocker can block.
     */
    private fun canBlockDespiteKeywordEvasion(
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockerId: EntityId,
        projected: ProjectedState
    ): Boolean {
        val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return true
        val keywordRestriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedExceptByKeyword>().firstOrNull()
            ?: return true

        val requiredKeyword = keywordRestriction.requiredKeyword

        if (projected.hasKeyword(blockerId, requiredKeyword)) return true

        // Per MTG rules, reach allows blocking creatures with "can't be blocked except by flying"
        if (requiredKeyword == Keyword.FLYING && projected.hasKeyword(blockerId, Keyword.REACH)) return true

        return false
    }

    /**
     * Check if attacker has CantBeBlockedUnlessDefenderSharesCreatureType restriction.
     * The attacker can't be blocked unless the defending player controls N or more
     * creatures that share a creature type (e.g., three Goblins or three Elves).
     *
     * Returns an error message if blocking is not allowed, null otherwise.
     */
    private fun validateCantBeBlockedUnlessDefenderSharesCreatureType(
        state: GameState,
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockingPlayer: EntityId,
        projected: ProjectedState
    ): String? {
        val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantBeBlockedUnlessDefenderSharesCreatureType>().firstOrNull()
            ?: return null

        if (!defenderHasEnoughSharedCreatureTypes(state, blockingPlayer, restriction.minSharedCount, projected)) {
            return "${attackerCard.name} can't be blocked unless you control ${restriction.minSharedCount} or more creatures that share a creature type"
        }

        return null
    }

    /**
     * Check if the defending player controls enough creatures sharing a creature type.
     * Returns true if any creature subtype appears on [minCount] or more creatures
     * controlled by [playerId].
     */
    private fun defenderHasEnoughSharedCreatureTypes(
        state: GameState,
        playerId: EntityId,
        minCount: Int,
        projected: ProjectedState
    ): Boolean {
        val creatures = projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
            card.typeLine.isCreature
        }

        // Count occurrences of each creature subtype
        val subtypeCounts = mutableMapOf<String, Int>()
        for (entityId in creatures) {
            for (subtype in projected.getSubtypes(entityId)) {
                subtypeCounts[subtype] = (subtypeCounts[subtype] ?: 0) + 1
            }
        }

        return subtypeCounts.values.any { it >= minCount }
    }

    /**
     * Check if a blocker can block despite CantBeBlockedUnlessDefenderSharesCreatureType.
     * Returns true if the defending player meets the shared creature type requirement.
     */
    private fun canBlockDespiteSharedCreatureTypeRestriction(
        state: GameState,
        attackerId: EntityId,
        attackerCard: CardComponent,
        blockingPlayer: EntityId,
        projected: ProjectedState
    ): Boolean {
        val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return true
        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantBeBlockedUnlessDefenderSharesCreatureType>().firstOrNull()
            ?: return true

        return defenderHasEnoughSharedCreatureTypes(state, blockingPlayer, restriction.minSharedCount, projected)
    }

    /**
     * Check if a creature has "can't block" ability (e.g., Craven Giant, Jungle Lion).
     * Returns an error message if the creature cannot block, null otherwise.
     */
    private fun validateCantBlock(blockerCard: CardComponent): String? {
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return null
        val cantBlockAbility = cardDef.staticAbilities.filterIsInstance<CantBlock>().firstOrNull()
            ?: return null

        // CantBlock with StaticTarget.SourceCreature means this creature can't block
        if (cantBlockAbility.target == StaticTarget.SourceCreature) {
            return "${blockerCard.name} can't block"
        }

        return null
    }

    /**
     * Check if a creature has "can't block" ability.
     * Returns false if the creature cannot block.
     */
    private fun hasCantBlockAbility(blockerCard: CardComponent): Boolean {
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return false
        val cantBlockAbility = cardDef.staticAbilities.filterIsInstance<CantBlock>().firstOrNull()
            ?: return false

        return cantBlockAbility.target == StaticTarget.SourceCreature
    }

    /**
     * Check if a creature has "can only block creatures with X" restriction.
     * Returns an error message if any attacker lacks the required keyword, null otherwise.
     *
     * Used for Cloud Pirates, Cloud Spirit, etc: "can block only creatures with flying."
     */
    private fun validateCanOnlyBlockWithKeyword(
        state: GameState,
        blockerCard: CardComponent,
        attackerIds: List<EntityId>,
        projected: ProjectedState
    ): String? {
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities.filterIsInstance<CanOnlyBlockCreaturesWithKeyword>().firstOrNull()
            ?: return null

        // Check if the restriction applies to this creature
        if (restriction.target != StaticTarget.SourceCreature) {
            return null
        }

        // Check each attacker - all must have the required keyword
        for (attackerId in attackerIds) {
            val attackerCard = state.getEntity(attackerId)?.get<CardComponent>() ?: continue

            if (!projected.hasKeyword(attackerId, restriction.keyword)) {
                return "${blockerCard.name} can block only creatures with ${restriction.keyword.displayName.lowercase()}"
            }
        }

        return null
    }

    /**
     * Check if a creature can block despite "can only block creatures with X" restriction.
     * Returns true if there's no restriction or the attacker has the required keyword.
     */
    private fun canBlockDespiteKeywordRestriction(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        projected: ProjectedState
    ): Boolean {
        val blockerCard = state.getEntity(blockerId)?.get<CardComponent>() ?: return true
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return true
        val restriction = cardDef.staticAbilities.filterIsInstance<CanOnlyBlockCreaturesWithKeyword>().firstOrNull()
            ?: return true

        if (restriction.target != StaticTarget.SourceCreature) {
            return true
        }

        return projected.hasKeyword(attackerId, restriction.keyword)
    }

    /**
     * Check if blocker has "can't block creatures with greater power" restriction.
     * Returns an error message if any attacker has power greater than the blocker's power, null otherwise.
     *
     * Used for Spitfire Handler: "This creature can't block creatures with power greater than
     * this creature's power."
     */
    private fun validateCantBlockCreaturesWithGreaterPower(
        state: GameState,
        blockerId: EntityId,
        blockerCard: CardComponent,
        attackerIds: List<EntityId>,
        projected: ProjectedState
    ): String? {
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities.filterIsInstance<CantBlockCreaturesWithGreaterPower>().firstOrNull()
            ?: return null

        if (restriction.target != StaticTarget.SourceCreature) return null

        val blockerPower = projected.getPower(blockerId) ?: 0

        for (attackerId in attackerIds) {
            val attackerCard = state.getEntity(attackerId)?.get<CardComponent>() ?: continue
            val attackerPower = projected.getPower(attackerId) ?: 0

            if (attackerPower > blockerPower) {
                return "${blockerCard.name} can't block ${attackerCard.name} (power $attackerPower is greater than ${blockerCard.name}'s power $blockerPower)"
            }
        }

        return null
    }

    /**
     * Check if blocker can block despite "can't block creatures with greater power" restriction.
     * Returns true if the blocker can block.
     */
    private fun canBlockDespiteGreaterPowerRestriction(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        projected: ProjectedState
    ): Boolean {
        val blockerCard = state.getEntity(blockerId)?.get<CardComponent>() ?: return true
        val cardDef = cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return true
        val restriction = cardDef.staticAbilities.filterIsInstance<CantBlockCreaturesWithGreaterPower>().firstOrNull()
            ?: return true

        if (restriction.target != StaticTarget.SourceCreature) return true

        val blockerPower = projected.getPower(blockerId) ?: 0
        val attackerPower = projected.getPower(attackerId) ?: 0

        return attackerPower <= blockerPower
    }

    /**
     * Validate menace requirements (must be blocked by 2+ creatures).
     */
    private fun validateMenaceRequirements(
        state: GameState,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        // Build a map of attackers to their blockers
        val attackerToBlockers = mutableMapOf<EntityId, MutableList<EntityId>>()
        for ((blockerId, attackerIds) in blockers) {
            for (attackerId in attackerIds) {
                attackerToBlockers.getOrPut(attackerId) { mutableListOf() }.add(blockerId)
            }
        }

        // Use projected keywords (includes floating effects)
        val projected = stateProjector.project(state)

        // Check each attacker with menace
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
    // Combat Damage
    // =========================================================================

    /**
     * Calculate and apply combat damage.
     *
     * @param firstStrike If true, only creatures with first strike/double strike deal damage
     */
    fun applyCombatDamage(state: GameState, firstStrike: Boolean = false): ExecutionResult {
        // Check if all combat damage is prevented this turn
        if (isAllCombatDamagePrevented(state)) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Use projected values for power and keywords (includes floating effects like +4/+4)
        val projected = stateProjector.project(state)

        // Find all attackers
        val attackers = state.findEntitiesWith<AttackingComponent>()

        // Pre-check: if any attacker with DivideCombatDamageFreely needs a distribution
        // decision, pause before processing ANY damage.
        for ((attackerId, attackingComponent) in attackers) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: continue
            val hasDivideDamageFreely = cardDef.staticAbilities.any { it is DivideCombatDamageFreely }
            if (!hasDivideDamageFreely) continue

            // Check if this attacker deals damage in this step
            val hasFirstStrike = projected.hasKeyword(attackerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(attackerId, Keyword.DOUBLE_STRIKE)
            val attackerDealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }
            if (!attackerDealsDamageThisStep) continue

            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower <= 0) continue

            // Already has a player-assigned distribution? Skip.
            if (attackerContainer.get<DamageAssignmentComponent>() != null) continue

            val blockedBy = attackerContainer.get<BlockedComponent>()
            val defenderId = attackingComponent.defenderId

            // Build targets: when blocked, blockers + defending player;
            // when unblocked, defending creatures + defending player (per ruling 2).
            val targets = mutableListOf<EntityId>()
            if (blockedBy != null && blockedBy.blockerIds.isNotEmpty()) {
                val blockersOnBattlefield = blockedBy.blockerIds.filter { it in state.getBattlefield() }
                if (blockersOnBattlefield.isEmpty()) {
                    // All blockers removed — per ruling 1, can't deal combat damage
                    continue
                }
                targets.addAll(blockersOnBattlefield)
            } else if (blockedBy == null) {
                // Unblocked: can assign to any creature the defending player controls
                val defendingCreatures = state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    projected.getController(entityId) == defenderId &&
                        container.get<CardComponent>()?.typeLine?.isCreature == true
                }
                targets.addAll(defendingCreatures)
            }
            targets.add(defenderId)

            // If only the defending player is a target, skip the decision (auto-assign all to player)
            if (targets.size <= 1) continue

            // Create distribution decision
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
        // processing ANY damage. This handles trample and multiple-blocker situations.
        for ((attackerId, attackingComponent) in attackers) {
            if (attackerId !in state.getBattlefield()) continue

            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue

            // Skip DivideCombatDamageFreely attackers (handled above)
            val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId)
            val hasDivideDamageFreely = cardDef?.staticAbilities?.any { it is DivideCombatDamageFreely } == true
            if (hasDivideDamageFreely) continue

            // Skip if already has a player-assigned distribution
            if (attackerContainer.get<DamageAssignmentComponent>() != null) continue

            // Only check attackers that deal damage this step
            val hasFirstStrike = projected.hasKeyword(attackerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(attackerId, Keyword.DOUBLE_STRIKE)
            val attackerDealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }
            if (!attackerDealsDamageThisStep) continue

            // Only check blocked attackers
            val blockedBy = attackerContainer.get<BlockedComponent>()
            if (blockedBy == null || blockedBy.blockerIds.isEmpty()) continue

            // Check if manual assignment is required
            if (!damageCalculator.requiresManualAssignment(state, attackerId)) continue

            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower <= 0) continue

            val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
                ?: blockedBy.blockerIds

            // Only include blockers still on the battlefield
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
        // If a recipient has a PreventNextDamage shield that can't cover all incoming
        // simultaneous combat damage from multiple sources, the defender chooses distribution.
        val preventionResult = checkDamagePreventionChoice(state, attackers, projected, firstStrike)
        if (preventionResult != null) return preventionResult

        for ((attackerId, attackingComponent) in attackers) {
            // Skip attackers no longer on the battlefield (killed by first strike damage)
            if (attackerId !in state.getBattlefield()) continue

            val attackerContainer = state.getEntity(attackerId) ?: continue
            attackerContainer.get<CardComponent>() ?: continue

            // Check if this attacker deals damage in this step
            val hasFirstStrike = projected.hasKeyword(attackerId, Keyword.FIRST_STRIKE)
            val hasDoubleStrike = projected.hasKeyword(attackerId, Keyword.DOUBLE_STRIKE)

            val attackerDealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }

            // Check if blocked
            val blockedBy = attackerContainer.get<BlockedComponent>()

            // Check for "divide combat damage freely" ability (Butcher Orgg)
            val attackerCard = attackerContainer.get<CardComponent>()!!
            val cardDef = cardRegistry?.getCard(attackerCard.cardDefinitionId)
            val hasDivideDamageFreely = cardDef?.staticAbilities?.any { it is DivideCombatDamageFreely } == true

            if (hasDivideDamageFreely) {
                // Divide damage freely: handles both blocked and unblocked cases
                val blockerIdList = blockedBy?.blockerIds ?: emptyList()
                val (dmgState, dmgEvents) = dealDividedCombatDamage(
                    newState, attackerId, blockerIdList,
                    attackingComponent.defenderId, firstStrike, attackerDealsDamageThisStep
                )
                newState = dmgState
                events.addAll(dmgEvents)
            } else if (blockedBy == null) {
                // Unblocked - only deal damage if attacker deals damage this step
                if (!attackerDealsDamageThisStep) continue

                // Get attacker's power (projected value includes buffs like +4/+4)
                val power = projected.getPower(attackerId) ?: 0
                if (power <= 0) continue

                val defenderId = attackingComponent.defenderId

                // Check if all damage from this creature is prevented (Chain of Silence)
                val allDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(newState, attackerId)

                // Check if the defending player is protected from combat damage by attacking creatures
                val isProtected = isProtectedFromAttackingCreatureDamage(newState, defenderId)

                // Check if combat damage from this creature is prevented by a group filter
                val groupPrevented = isCombatDamagePreventedByGroupFilter(newState, attackerId, projected)

                // Check if combat damage to and by this creature is prevented (Deftblade Elite)
                val toAndByPrevented = isCombatDamageToAndByPrevented(newState, attackerId)
                if (!isProtected && !allDamagePrevented && !groupPrevented && !toAndByPrevented) {
                    // Check if combat damage is redirected to controller (Goblin Psychopath)
                    val redirectToController = hasCombatDamageRedirectToController(newState, attackerId)
                    val damageTargetId = if (redirectToController) {
                        val controllerId = projected.getController(attackerId) ?: defenderId
                        controllerId
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
                // If protected, damage is prevented - no events emitted
            } else {
                // Blocked - filter to live blockers only
                val liveBlockerIds = blockedBy.blockerIds.filter { it in newState.getBattlefield() }
                val (attackerDamageState, attackerEvents) = dealCombatDamageBetweenCreatures(
                    newState, attackerId, liveBlockerIds, firstStrike, attackerDealsDamageThisStep
                )
                newState = attackerDamageState
                events.addAll(attackerEvents)
            }
        }

        // Check for lethal damage (state-based actions)
        val (postDamageState, deathEvents) = checkLethalDamage(newState)
        newState = postDamageState
        events.addAll(deathEvents)

        return ExecutionResult.success(newState, events)
    }

    /**
     * Check if any damage recipient needs to choose how to distribute prevention
     * among multiple simultaneous combat damage sources (CR 615.7).
     *
     * Returns an ExecutionResult.paused if a choice is needed, null otherwise.
     */
    private fun checkDamagePreventionChoice(
        state: GameState,
        attackers: List<Pair<EntityId, AttackingComponent>>,
        projected: ProjectedState,
        firstStrike: Boolean
    ): ExecutionResult? {
        // Build map: recipient → (source → damage amount) for all incoming combat damage
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
                // Unblocked: damage goes to defending player
                val defenderId = attackingComponent.defenderId
                if (!isProtectedFromAttackingCreatureDamage(state, defenderId) &&
                    !isCombatDamagePreventedByGroupFilter(state, attackerId, projected)) {
                    val amplified = EffectExecutorUtils.applyStaticDamageAmplification(state, defenderId, attackerPower, attackerId)
                    incomingDamage.getOrPut(defenderId) { mutableMapOf() }
                        .merge(attackerId, amplified) { a, b -> a + b }
                }
            } else {
                // Blocked: check for manual assignment or auto-distribution
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
                        // Check protection from attacker's colors/subtypes
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

        // For each recipient, check if they have a prevention shield requiring a choice
        for ((recipientId, sourcesDamage) in incomingDamage) {
            if (sourcesDamage.size < 2) continue // Single source: no choice needed

            val totalDamage = sourcesDamage.values.sum()

            // Find PreventNextDamage shields on this recipient (skip source-specific ones — already allocated)
            for (floatingEffect in state.floatingEffects) {
                val mod = floatingEffect.effect.modification
                if (mod !is SerializableModification.PreventNextDamage) continue
                if (mod.onlyFromSource != null) continue // Already allocated via prevention choice
                if (recipientId !in floatingEffect.effect.affectedEntities) continue

                val shieldAmount = mod.remainingAmount
                if (shieldAmount >= totalDamage) continue // Shield covers all: no choice needed

                // Shield can't cover all damage from multiple sources — player must choose
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

    /**
     * Deal damage to a player.
     */
    private fun dealDamageToPlayer(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        sourceId: EntityId
    ): ExecutionResult {
        // Apply damage amplification (e.g., Gratuitous Violence)
        val amplifiedAmount = EffectExecutorUtils.applyStaticDamageAmplification(state, playerId, amount, sourceId)
        // Apply damage prevention shields
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
            // Get the attacker's controller (use projected state for control-changing effects)
            val projected = stateProjector.project(state)
            val attackerController = projected.getController(sourceId)

            // Only reflect if attacker is controlled by a different player
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

    /**
     * Check if a player has damage reflection active (Harsh Justice).
     * This reflects combat damage from attacking creatures back to their controllers.
     */
    private fun hasReflectCombatDamage(state: GameState, playerId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            val modification = floatingEffect.effect.modification
            modification is SerializableModification.ReflectCombatDamage &&
                modification.protectedPlayerId == playerId.toString()
        }
    }

    /**
     * Check if a creature has a RedirectCombatDamageToController floating effect.
     * Used by Goblin Psychopath: if the coin flip is lost, combat damage is redirected to controller.
     */
    private fun hasCombatDamageRedirectToController(state: GameState, creatureId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.RedirectCombatDamageToController &&
                creatureId in floatingEffect.effect.affectedEntities
        }
    }

    /**
     * Consume (remove) the RedirectCombatDamageToController floating effect for a creature.
     * Called after combat damage has been redirected, since it's a one-shot effect.
     */
    private fun consumeRedirectCombatDamageToController(state: GameState, creatureId: EntityId): GameState {
        return state.copy(
            floatingEffects = state.floatingEffects.filterNot { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.RedirectCombatDamageToController &&
                    creatureId in floatingEffect.effect.affectedEntities
            }
        )
    }

    /**
     * Deal combat damage between an attacker and blockers.
     *
     * This uses the damage assignment order (if set) and applies damage according to CR 510:
     * - Damage must be assigned in order
     * - Each blocker must receive lethal before moving to the next
     * - Excess damage with trample goes to defending player
     */
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

        // Use projected values for power and keywords (includes floating effects like +4/+4)
        val projected = stateProjector.project(newState)
        val attackerPower = projected.getPower(attackerId) ?: 0
        val hasTrample = projected.hasKeyword(attackerId, Keyword.TRAMPLE)

        // Get blockers in damage assignment order (or default order)
        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: blockerIds

        // Check for manual damage assignment component
        val manualAssignment = attackerContainer.get<DamageAssignmentComponent>()

        // Calculate damage distribution
        val damageDistribution = if (manualAssignment != null) {
            // Use player-specified assignment
            manualAssignment.assignments
        } else {
            // Auto-calculate optimal distribution
            damageCalculator.calculateAutoDamageDistribution(newState, attackerId).assignments
        }

        // Apply attacker's damage to blockers (and potentially defending player with trample)
        // Check if all damage from attacker is prevented (Chain of Silence)
        val attackerDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(newState, attackerId)
        // Check if combat damage from attacker is prevented by a group filter
        val attackerGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, attackerId, projected)
        // Check if combat damage to and by attacker is prevented (Deftblade Elite)
        val attackerToAndByPrevented = isCombatDamageToAndByPrevented(newState, attackerId)
        if (attackerDealsDamageThisStep && !attackerDamagePrevented && !attackerGroupPrevented && !attackerToAndByPrevented) {
            // Check if attacker's combat damage is redirected to its controller (Goblin Psychopath)
            val attackerRedirectToController = hasCombatDamageRedirectToController(newState, attackerId)
            if (attackerRedirectToController) {
                // Sum all damage and redirect to controller
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

                // Check if target is a player or creature
                val targetContainer = newState.getEntity(targetId)
                val isPlayer = targetContainer?.get<LifeTotalComponent>() != null &&
                               targetContainer.get<CardComponent>() == null

                if (isPlayer) {
                    // Deal damage to defending player (trample) - apply amplification then shields
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
                    // Check if combat damage to and by the blocker is prevented (prevents damage TO blocker)
                    val blockerTargetToAndByPrevented = isCombatDamageToAndByPrevented(newState, targetId)
                    // Check protection: blocker protected from attacker's colors or subtypes?
                    val attackerColors = projected.getColors(attackerId)
                    val attackerSubtypes = projected.getSubtypes(attackerId)
                    val blockerProtected = attackerColors.any { colorName ->
                        projected.hasKeyword(targetId, "PROTECTION_FROM_$colorName")
                    } || attackerSubtypes.any { subtype ->
                        projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")
                    }
                    if (!blockerProtected && !blockerTargetToAndByPrevented) {
                        // Apply damage amplification then prevention shields
                        val amplifiedDamage = EffectExecutorUtils.applyStaticDamageAmplification(newState, targetId, damage, attackerId)
                        val (shieldState, effectiveDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, targetId, amplifiedDamage, isCombatDamage = true, sourceId = attackerId)
                        newState = shieldState
                        if (effectiveDamage > 0) {
                            val currentDamage = newState.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
                            newState = newState.updateEntity(targetId) { container ->
                                container.with(DamageComponent(currentDamage + effectiveDamage))
                            }
                            newState = EffectExecutorUtils.trackDamageDealtToCreature(newState, attackerId, targetId)
                            val atkSourceName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                            val blockerTargetName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Creature"
                            events.add(DamageDealtEvent(attackerId, targetId, effectiveDamage, true, sourceName = atkSourceName, targetName = blockerTargetName, targetIsPlayer = false))
                        }
                    }
                }
            }
            } // end else (no redirect to controller)
        }

        // Each blocker deals damage to attacker (only if attacker is still on the battlefield)
        // Per CR 510.1d: if the creature a blocker is blocking ceases to exist,
        // the blocker doesn't deal combat damage.
        if (attackerId !in newState.getBattlefield()) {
            return newState to events
        }

        for (blockerId in orderedBlockers) {
            val blockerContainer = newState.getEntity(blockerId) ?: continue
            blockerContainer.get<CardComponent>() ?: continue

            // Skip blockers no longer on the battlefield (killed by first strike damage)
            if (blockerId !in newState.getBattlefield()) continue

            // Check if blocker deals damage in this step (using projected keywords)
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
                // Check if all damage from this blocker is prevented (Chain of Silence)
                val blockerDamagePrevented = EffectExecutorUtils.isAllDamageFromSourcePrevented(newState, blockerId)
                // Check if combat damage from this blocker is prevented by a group filter
                val blockerGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, blockerId, projected)
                // Check if combat damage to and by blocker is prevented (prevents damage dealt BY blocker)
                val blockerToAndByPrevented = isCombatDamageToAndByPrevented(newState, blockerId)
                // Check if combat damage to and by attacker is prevented (prevents damage dealt TO attacker)
                val attackerToAndByPrevented = isCombatDamageToAndByPrevented(newState, attackerId)

                // Check protection: attacker protected from blocker's colors or subtypes?
                val blockerColors = projected.getColors(blockerId)
                val blockerSubtypes = projected.getSubtypes(blockerId)
                val attackerProtected = blockerColors.any { colorName ->
                    projected.hasKeyword(attackerId, "PROTECTION_FROM_$colorName")
                } || blockerSubtypes.any { subtype ->
                    projected.hasKeyword(attackerId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")
                }
                if (!attackerProtected && !blockerDamagePrevented && !blockerGroupPrevented && !blockerToAndByPrevented && !attackerToAndByPrevented) {
                    // Check if blocker's combat damage is redirected to its controller (Goblin Psychopath)
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
                    // Apply damage amplification then prevention shields
                    val amplifiedBlockerDamage = EffectExecutorUtils.applyStaticDamageAmplification(newState, attackerId, blockerPower, blockerId)
                    val (shieldState, effectiveBlockerDamage) = EffectExecutorUtils.applyDamagePreventionShields(newState, attackerId, amplifiedBlockerDamage, isCombatDamage = true, sourceId = blockerId)
                    newState = shieldState
                    if (effectiveBlockerDamage > 0) {
                        val currentDamage = newState.getEntity(attackerId)?.get<DamageComponent>()?.amount ?: 0
                        newState = newState.updateEntity(attackerId) { container ->
                            container.with(DamageComponent(currentDamage + effectiveBlockerDamage))
                        }
                        newState = EffectExecutorUtils.trackDamageDealtToCreature(newState, blockerId, attackerId)
                        val blockerSourceName = newState.getEntity(blockerId)?.get<CardComponent>()?.name ?: "Creature"
                        val attackerTargetName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                        events.add(DamageDealtEvent(blockerId, attackerId, effectiveBlockerDamage, true, sourceName = blockerSourceName, targetName = attackerTargetName, targetIsPlayer = false))
                    }
                    }
                }
            }
        }

        return newState to events
    }

    /**
     * Deal combat damage for a creature with "divide combat damage freely" ability (Butcher Orgg).
     *
     * When a DamageAssignmentComponent is present (player chose distribution),
     * applies the player's chosen distribution. Otherwise auto-assigns lethal
     * damage to each blocker in order, with remainder to the defending player.
     *
     * Handles both blocked and unblocked cases. When unblocked (empty blockerIds),
     * the player may distribute damage among defending creatures and/or the
     * defending player via the DamageAssignmentComponent.
     *
     * Blockers still deal damage back to the attacker normally.
     */
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

        val projected = stateProjector.project(newState)

        // Get blockers in damage assignment order (or default order)
        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: blockerIds

        val damageAssignment = attackerContainer.get<DamageAssignmentComponent>()

        // Attacker deals damage
        if (attackerDealsDamageThisStep) {
            val attackerPower = projected.getPower(attackerId) ?: 0
            if (attackerPower > 0) {

                // If blocked, check that at least one blocker is still on the battlefield.
                // Per ruling: if all blockers are removed before combat damage,
                // the creature can't deal combat damage at all.
                if (blockerIds.isNotEmpty()) {
                    val hasBlockerOnBattlefield = orderedBlockers.any { it in newState.getBattlefield() }
                    if (!hasBlockerOnBattlefield) {
                        // Fall through to blocker counterattack only (no attacker damage)
                        return dealBlockerCounterattack(newState, events, attackerId, orderedBlockers, firstStrike, projected)
                    }
                }

                // Check if combat damage from this attacker is prevented by a group filter
                val attackerGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, attackerId, projected)

                if (attackerGroupPrevented) {
                    // Combat damage from this attacker is prevented; skip to blocker counterattack
                } else if (damageAssignment != null) {
                    // Use player's chosen distribution
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
                            // Deal damage to creature
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
                                    val currentDamage = newState.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
                                    newState = newState.updateEntity(targetId) { container ->
                                        container.with(DamageComponent(currentDamage + effectiveDamage))
                                    }
                                    newState = EffectExecutorUtils.trackDamageDealtToCreature(newState, attackerId, targetId)
                                    val freeAtkName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                                    val freeCreatureName = newState.getEntity(targetId)?.get<CardComponent>()?.name ?: "Creature"
                                    events.add(DamageDealtEvent(attackerId, targetId, effectiveDamage, true, sourceName = freeAtkName, targetName = freeCreatureName, targetIsPlayer = false))
                                }
                            }
                        }
                    }
                } else {
                    // Auto-assign: lethal to each blocker in order, remainder to defending player
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
                                val currentDamage = newState.getEntity(blockerId)?.get<DamageComponent>()?.amount ?: 0
                                newState = newState.updateEntity(blockerId) { container ->
                                    container.with(DamageComponent(currentDamage + effectiveDamage))
                                }
                                newState = EffectExecutorUtils.trackDamageDealtToCreature(newState, attackerId, blockerId)
                                val autoAtkName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                                val autoBlockerName = newState.getEntity(blockerId)?.get<CardComponent>()?.name ?: "Creature"
                                events.add(DamageDealtEvent(attackerId, blockerId, effectiveDamage, true, sourceName = autoAtkName, targetName = autoBlockerName, targetIsPlayer = false))
                            }
                        }

                        remainingDamage -= damageToAssign
                    }

                    // Remaining damage goes to defending player
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

        // Each blocker deals damage to attacker (same as normal combat)
        return dealBlockerCounterattack(newState, events, attackerId, orderedBlockers, firstStrike, projected)
    }

    /**
     * Handle blocker counterattack damage (blockers dealing damage back to attacker).
     * Extracted to avoid duplication between the damage-dealing and blocker-removed paths.
     */
    private fun dealBlockerCounterattack(
        state: GameState,
        events: MutableList<GameEvent>,
        attackerId: EntityId,
        orderedBlockers: List<EntityId>,
        firstStrike: Boolean,
        projected: ProjectedState
    ): Pair<GameState, List<GameEvent>> {
        var newState = state

        // If the attacker is no longer on the battlefield, blockers don't deal combat damage
        if (attackerId !in newState.getBattlefield()) {
            return newState to events
        }

        for (blockerId in orderedBlockers) {
            val blockerContainer = newState.getEntity(blockerId) ?: continue
            blockerContainer.get<CardComponent>() ?: continue

            // Skip blockers no longer on the battlefield
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
                // Check if combat damage from this blocker is prevented by a group filter
                val counterGroupPrevented = isCombatDamagePreventedByGroupFilter(newState, blockerId, projected)
                // Check protection
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
                        val currentDamage = newState.getEntity(attackerId)?.get<DamageComponent>()?.amount ?: 0
                        newState = newState.updateEntity(attackerId) { container ->
                            container.with(DamageComponent(currentDamage + effectiveBlockerDamage))
                        }
                        newState = EffectExecutorUtils.trackDamageDealtToCreature(newState, blockerId, attackerId)
                        val counterBlockerName = newState.getEntity(blockerId)?.get<CardComponent>()?.name ?: "Creature"
                        val counterAttackerName = newState.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                        events.add(DamageDealtEvent(blockerId, attackerId, effectiveBlockerDamage, true, sourceName = counterBlockerName, targetName = counterAttackerName, targetIsPlayer = false))
                    }
                }
            }
        }

        return newState to events
    }

    /**
     * Check for creatures with lethal damage and emit preview events.
     * Actual destruction is handled by StateBasedActionChecker.
     */
    private fun checkLethalDamage(state: GameState): Pair<GameState, List<GameEvent>> {
        val newState = state
        val events = mutableListOf<GameEvent>()

        // Use projected toughness (includes floating effects like +4/+4)
        val projected = stateProjector.project(state)

        for ((entityId, container) in state.entities) {
            container.get<CardComponent>() ?: continue
            if (!projected.isCreature(entityId)) continue

            // Only check creatures that have actually taken damage
            val damageComponent = container.get<DamageComponent>() ?: continue
            val damage = damageComponent.amount
            if (damage <= 0) continue

            // Skip if we can't determine toughness
            val toughness = projected.getToughness(entityId) ?: continue

            if (damage >= toughness) {
                // Creature has lethal damage - will be destroyed by state-based actions
                // Don't emit event here - StateBasedActionChecker handles it
            }
        }

        return newState to events
    }

    // =========================================================================
    // End Combat
    // =========================================================================

    /**
     * Clear combat state at end of combat.
     */
    fun endCombat(state: GameState): ExecutionResult {
        var newState = state

        // Remove all combat-related components from all entities (creatures and players)
        for ((entityId, _) in state.entities) {
            newState = newState.updateEntity(entityId) { container ->
                container
                    .without<AttackingComponent>()
                    .without<BlockingComponent>()
                    .without<BlockedComponent>()
                    .without<DamageAssignmentComponent>()
                    .without<DamageAssignmentOrderComponent>()
                    .without<DealtFirstStrikeDamageComponent>()
                    .without<RequiresManualDamageAssignmentComponent>()
                    .without<AttackersDeclaredThisCombatComponent>()
                    .without<BlockersDeclaredThisCombatComponent>()
            }
        }

        return ExecutionResult.success(newState)
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get all attacking creatures.
     */
    fun getAttackers(state: GameState): List<EntityId> {
        return state.findEntitiesWith<AttackingComponent>().map { it.first }
    }

    /**
     * Get all blocking creatures.
     */
    fun getBlockers(state: GameState): List<EntityId> {
        return state.findEntitiesWith<BlockingComponent>().map { it.first }
    }

    /**
     * Check if any creatures are attacking.
     */
    fun hasAttackers(state: GameState): Boolean {
        return state.findEntitiesWith<AttackingComponent>().isNotEmpty()
    }

    // =========================================================================
    // Must Be Blocked Requirements
    // =========================================================================

    /**
     * Validate "must be blocked" requirements.
     *
     * Per MTG rules, when multiple creatures have "must be blocked by all" effects:
     * - Each creature that can block must block at least one of them
     * - The defending player chooses which one each blocker blocks
     *
     * @return Error message if requirements not met, null if valid
     */
    private fun validateMustBeBlockedRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        // Find all attackers with "must be blocked by all" requirement
        val mustBeBlockedAttackers = findMustBeBlockedAttackers(state)
        if (mustBeBlockedAttackers.isEmpty()) {
            return null  // No "must be blocked" requirements
        }

        val projected = stateProjector.project(state)

        // Find all potential blockers (untapped creatures controlled by blocking player)
        val potentialBlockers = findPotentialBlockers(state, blockingPlayer)

        // Build a map of blockers to the attackers they ARE blocking
        val blockerToAttackers = blockers.mapValues { it.value.toSet() }

        // For each potential blocker, check if they must block a "must be blocked" attacker
        for (blockerId in potentialBlockers) {
            // Find which "must be blocked" attackers this creature CAN legally block
            val canBlockThese = mustBeBlockedAttackers.filter { attackerId ->
                canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
            }

            if (canBlockThese.isEmpty()) {
                // This creature can't block any of the "must be blocked" attackers - that's fine
                continue
            }

            // This creature CAN block at least one "must be blocked" attacker,
            // so it MUST block one of them
            val actuallyBlocking = blockerToAttackers[blockerId] ?: emptySet()
            val blockingMustBeBlocked = actuallyBlocking.intersect(mustBeBlockedAttackers.toSet())

            if (blockingMustBeBlocked.isEmpty()) {
                val blockerCard = state.getEntity(blockerId)?.get<CardComponent>()
                val blockerName = blockerCard?.name ?: "Creature"

                // Build a helpful error message
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
     * For each creature with a MustBlockSpecificAttacker floating effect,
     * if it can legally block the specified attacker, it must be blocking that attacker.
     */
    private fun validateProvokeRequirements(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        val projected = stateProjector.project(state)

        // Find all provoke constraints: blocker -> attacker
        val provokeConstraints = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.MustBlockSpecificAttacker }
            .flatMap { floatingEffect ->
                val modification = floatingEffect.effect.modification as SerializableModification.MustBlockSpecificAttacker
                floatingEffect.effect.affectedEntities.map { blockerId ->
                    blockerId to modification.attackerId
                }
            }

        for ((blockerId, attackerId) in provokeConstraints) {
            // Only check creatures controlled by the blocking player
            val controller = projected.getController(blockerId)
            if (controller != blockingPlayer) continue

            // Check if the blocker is still on the battlefield and untapped
            val blockerContainer = state.getEntity(blockerId) ?: continue
            if (blockerId !in state.getBattlefield()) continue
            if (blockerContainer.has<TappedComponent>()) continue

            // Check if the attacker is still attacking
            val attackerContainer = state.getEntity(attackerId) ?: continue
            if (!attackerContainer.has<AttackingComponent>()) continue

            // Check if this creature can legally block the attacker
            if (!canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)) continue

            // This creature can block the provoke attacker, so it must do so
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
     * Check if all combat damage is prevented this turn.
     * This is used by Leery Fogbeast and similar effects.
     */
    private fun isAllCombatDamagePrevented(state: GameState): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventAllCombatDamage
        }
    }

    /**
     * Check if a player is protected from combat damage by attacking creatures.
     * This is used by Deep Wood and similar effects.
     */
    private fun isProtectedFromAttackingCreatureDamage(state: GameState, playerId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventDamageFromAttackingCreatures &&
                playerId in floatingEffect.effect.affectedEntities
        }
    }

    /**
     * Check if all combat damage to and by a specific creature is prevented.
     * Used by Deftblade Elite and similar effects.
     */
    private fun isCombatDamageToAndByPrevented(state: GameState, creatureId: EntityId): Boolean {
        return state.floatingEffects.any { floatingEffect ->
            floatingEffect.effect.modification is SerializableModification.PreventCombatDamageToAndBy &&
                creatureId in floatingEffect.effect.affectedEntities
        }
    }

    /**
     * Check if a creature's combat damage is prevented by a group filter effect.
     * Used by Frontline Strategist and similar effects that prevent combat damage
     * from creatures matching a filter (e.g., non-Soldier creatures).
     * The creature type is checked at damage time per the rules.
     */
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

    /**
     * Compute mandatory blocker assignments from floating effects.
     * Returns a map of blocker → list of attackers it must block.
     *
     * Considers both:
     * - MustBlockSpecificAttacker (Provoke): blocker must block the specified attacker
     * - MustBeBlockedByAll (Taunting Elf): all able blockers must block the attacker
     */
    fun getMandatoryBlockerAssignments(state: GameState, blockingPlayer: EntityId): Map<EntityId, List<EntityId>> {
        val projected = stateProjector.project(state)
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

    /**
     * Find all attackers that have "must be blocked by all" requirement active.
     */
    private fun findMustBeBlockedAttackers(state: GameState): List<EntityId> {
        // Get all attacking creatures
        val attackers = state.findEntitiesWith<AttackingComponent>().map { it.first }.toSet()

        // Find floating effects with MustBeBlockedByAll modification
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
        val projected = stateProjector.project(state)
        return state.getBattlefield()
            .filter { entityId ->
                val container = state.getEntity(entityId) ?: return@filter false
                container.get<CardComponent>() ?: return@filter false
                val controller = projected.getController(entityId)

                // Must be a creature controlled by blocking player and untapped (use projected types for animated lands)
                projected.isCreature(entityId) &&
                    controller == blockingPlayer &&
                    !container.has<TappedComponent>()
            }
    }

    /**
     * Check if a creature can legally block at least one of the current attackers.
     */
    fun canCreatureBlockAnyAttacker(state: GameState, blockerId: EntityId, blockingPlayer: EntityId): Boolean {
        val blockerContainer = state.getEntity(blockerId) ?: return false
        val blockerCard = blockerContainer.get<CardComponent>() ?: return false

        // Check if blocker has "can't block" restriction (face-down creatures have no abilities)
        val isFaceDown = blockerContainer.has<FaceDownComponent>()
        if (!isFaceDown && hasCantBlockAbility(blockerCard)) return false

        val projected = stateProjector.project(state)

        // Check projected "can't block" (e.g., from Pacifism aura)
        if (projected.cantBlock(blockerId)) return false

        // Check creature count block restriction (e.g., Goblin Goon)
        if (!isFaceDown && hasCantBlockUnlessRestriction(state, blockerId, blockingPlayer, projected)) return false

        val attackers = state.entities.filter { (_, container) -> container.has<AttackingComponent>() }.keys

        return attackers.any { attackerId ->
            canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
        }
    }

    /**
     * Check if a creature can legally block an attacker.
     * This re-uses the evasion checking logic from validateCanBlock.
     */
    private fun canCreatureBlockAttacker(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        blockingPlayer: EntityId,
        projected: ProjectedState
    ): Boolean {
        val blockerContainer = state.getEntity(blockerId) ?: return false
        val attackerContainer = state.getEntity(attackerId) ?: return false

        val blockerCard = blockerContainer.get<CardComponent>() ?: return false
        val attackerCard = attackerContainer.get<CardComponent>() ?: return false

        // Check if blocker has "can't block" restriction (face-down creatures have no abilities)
        val isFaceDown = blockerContainer.has<FaceDownComponent>()
        if (!isFaceDown && hasCantBlockAbility(blockerCard)) {
            return false
        }

        // Check projected "can't block" (e.g., from Pacifism aura)
        if (projected.cantBlock(blockerId)) {
            return false
        }

        // Face-down creatures lose all abilities, so keyword/power block restrictions don't apply
        if (!isFaceDown) {
            // Check if blocker can only block creatures with a specific keyword (e.g., Cloud Pirates)
            if (!canBlockDespiteKeywordRestriction(state, blockerId, attackerId, projected)) {
                return false
            }
        }

        // CantBlockCreaturesWithGreaterPower: Can't block creatures with power > this creature's power
        if (!isFaceDown && !canBlockDespiteGreaterPowerRestriction(state, blockerId, attackerId, projected)) {
            return false
        }

        // Unblockable: Cannot be blocked at all
        if (projected.hasKeyword(attackerId, com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_BLOCKED)) {
            return false
        }

        // Flying: Can only be blocked by creatures with flying or reach
        if (projected.hasKeyword(attackerId, Keyword.FLYING)) {
            val canBlockFlying = projected.hasKeyword(blockerId, Keyword.FLYING) ||
                projected.hasKeyword(blockerId, Keyword.REACH)
            if (!canBlockFlying) return false
        }

        // Horsemanship: Can only be blocked by creatures with horsemanship
        if (projected.hasKeyword(attackerId, Keyword.HORSEMANSHIP)) {
            if (!projected.hasKeyword(blockerId, Keyword.HORSEMANSHIP)) return false
        }

        // Shadow: Can only be blocked by creatures with shadow
        if (projected.hasKeyword(attackerId, Keyword.SHADOW)) {
            if (!projected.hasKeyword(blockerId, Keyword.SHADOW)) return false
        }

        // Fear: Can only be blocked by artifact creatures or black creatures
        if (projected.hasKeyword(attackerId, Keyword.FEAR)) {
            val isArtifactCreature = blockerCard.typeLine.isArtifactCreature
            val isBlackCreature = Color.BLACK in blockerCard.colors
            if (!isArtifactCreature && !isBlackCreature) return false
        }

        // Landwalk: Cannot be blocked if defending player controls land of that type
        val landwalkToSubtype = mapOf(
            Keyword.FORESTWALK to com.wingedsheep.sdk.core.Subtype.FOREST,
            Keyword.SWAMPWALK to com.wingedsheep.sdk.core.Subtype.SWAMP,
            Keyword.ISLANDWALK to com.wingedsheep.sdk.core.Subtype.ISLAND,
            Keyword.MOUNTAINWALK to com.wingedsheep.sdk.core.Subtype.MOUNTAIN,
            Keyword.PLAINSWALK to com.wingedsheep.sdk.core.Subtype.PLAINS
        )

        for ((landwalkKeyword, landSubtype) in landwalkToSubtype) {
            if (projected.hasKeyword(attackerId, landwalkKeyword)) {
                if (playerControlsLandWithSubtype(state, blockingPlayer, landSubtype)) {
                    return false
                }
            }
        }

        // CantBeBlockedByPower: Cannot be blocked by creatures with power >= N
        if (!canBlockDespitePowerRestriction(attackerId, attackerCard, blockerId, projected)) {
            return false
        }

        // CantBeBlockedExceptByColor: Can only be blocked by creatures of the specified color
        if (!canBlockDespiteColorRestriction(state, attackerId, blockerId)) {
            return false
        }

        // CantBeBlockedExceptByKeyword: Can only be blocked by creatures with a specific keyword
        if (!canBlockDespiteKeywordEvasion(attackerId, attackerCard, blockerId, projected)) {
            return false
        }

        // CantBeBlockedUnlessDefenderSharesCreatureType: e.g. Graxiplon
        if (!canBlockDespiteSharedCreatureTypeRestriction(state, attackerId, attackerCard, blockingPlayer, projected)) {
            return false
        }

        return true
    }

    // =========================================================================
    // Conditional Attack/Block Restrictions (CantAttackUnless / CantBlockUnless)
    // =========================================================================

    /**
     * Count creatures on the battlefield controlled by a player.
     */
    private fun countCreatures(state: GameState, playerId: EntityId, projected: ProjectedState): Int {
        return state.getBattlefield().count { entityId ->
            projected.isCreature(entityId) && projected.getController(entityId) == playerId
        }
    }

    /**
     * Evaluate a [CombatCondition] for an attack restriction.
     *
     * @param controllerId The creature's controller
     * @param opponentId The defending player
     * @return true if the condition is satisfied (attack is allowed)
     */
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

    /**
     * Validate CantAttackUnless restrictions for an attacker.
     *
     * @return Error message if restricted, null if allowed
     */
    private fun validateCantAttackUnless(
        state: GameState,
        attackerId: EntityId,
        attackingPlayer: EntityId,
        defenderId: EntityId,
        projected: ProjectedState
    ): String? {
        val container = state.getEntity(attackerId) ?: return null
        if (container.has<FaceDownComponent>()) return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId) ?: return null

        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantAttackUnless>()
            .firstOrNull { it.target == StaticTarget.SourceCreature } ?: return null

        val defendingPlayer = findDefendingPlayer(state, defenderId)

        if (!evaluateCombatCondition(restriction.condition, state, attackingPlayer, defendingPlayer, projected)) {
            return "${cardComponent.name} ${restriction.description}"
        }

        return null
    }

    /**
     * Check if a creature has a CantAttackUnless restriction that prevents it
     * from attacking any opponent. Used for legal action generation.
     *
     * @return true if the creature is restricted from attacking all opponents
     */
    fun hasCantAttackUnlessRestriction(
        state: GameState,
        attackerId: EntityId,
        attackingPlayer: EntityId,
        projected: ProjectedState
    ): Boolean {
        val container = state.getEntity(attackerId) ?: return false
        if (container.has<FaceDownComponent>()) return false
        val cardComponent = container.get<CardComponent>() ?: return false
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId) ?: return false

        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantAttackUnless>()
            .firstOrNull { it.target == StaticTarget.SourceCreature } ?: return false

        // Check against all opponents - if restricted against all, can't attack anyone
        val opponents = state.turnOrder.filter { it != attackingPlayer }
        return opponents.all { opponentId ->
            !evaluateCombatCondition(restriction.condition, state, attackingPlayer, opponentId, projected)
        }
    }

    /**
     * Validate CantBlockUnless restrictions for a blocker.
     *
     * @return Error message if restricted, null if allowed
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
     *
     * @return true if the creature is restricted from blocking
     */
    fun hasCantBlockUnlessRestriction(
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

        // Find the attacking player
        val attackers = state.entities.filter { (_, c) -> c.has<AttackingComponent>() }
        if (attackers.isEmpty()) return false

        val anyAttacker = attackers.keys.first()
        val attackingPlayer = projected.getController(anyAttacker) ?: return false

        return !evaluateCombatCondition(restriction.condition, state, blockingPlayer, attackingPlayer, projected)
    }

    /**
     * Find the defending player given a defender entity (could be a player or planeswalker).
     */
    private fun findDefendingPlayer(state: GameState, defenderId: EntityId): EntityId {
        // If the defender is a player directly, return it
        if (state.getEntity(defenderId)?.has<LifeTotalComponent>() == true) {
            return defenderId
        }
        // Otherwise it's a planeswalker — find its controller
        return state.getEntity(defenderId)?.get<ControllerComponent>()?.playerId ?: defenderId
    }
}
