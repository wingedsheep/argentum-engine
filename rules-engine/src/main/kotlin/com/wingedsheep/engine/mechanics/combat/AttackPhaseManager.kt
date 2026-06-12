package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.mechanics.combat.rules.AttackCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.AttackDefenderRule
import com.wingedsheep.engine.mechanics.combat.rules.AttackRestrictionRule
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.PredicateContext

/**
 * Handles the declare attackers step of combat.
 *
 * Responsibilities:
 * - Validating individual attackers via [AttackRestrictionRule] and [AttackDefenderRule]
 * - Must-attack requirements (Taunt, Walking Desecration, Grand Melee)
 * - Attack taxes (Ghostly Prison, Windborn Muse, Whipgrass Entangler)
 * - Applying attacker components and tapping
 */
internal class AttackPhaseManager(
    private val cardRegistry: CardRegistry,
    private val attackRestrictionRules: List<AttackRestrictionRule>,
    private val attackDefenderRules: List<AttackDefenderRule>,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
) {

    private val dynamicAmountEvaluator = com.wingedsheep.engine.handlers.DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Validate and declare attackers.
     *
     * @param attackers Map of attacker entity ID to defender (player or planeswalker)
     * @param bands Optional band groupings (CR 702.22). Each set is one band of attackers.
     */
    fun declareAttackers(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        bands: List<Set<EntityId>> = emptyList()
    ): ExecutionResult {
        // Validate each attacker
        val projected = state.projectedState
        val opponents = state.turnOrder.filter { it != attackingPlayer }

        // Validate band declarations (CR 702.22c).
        val bandValidation = validateBands(state, attackers, bands, projected)
        if (bandValidation != null) {
            return ExecutionResult.error(state, bandValidation)
        }
        for ((attackerId, defenderId) in attackers) {
            val validation = validateAttacker(state, attackingPlayer, attackerId)
            if (validation != null) {
                return ExecutionResult.error(state, validation)
            }
            // Validate defender is either an opponent player or a planeswalker controlled by an opponent
            if (defenderId !in opponents) {
                if (!projected.isPlaneswalker(defenderId) || projected.getController(defenderId) in listOf(attackingPlayer)) {
                    return ExecutionResult.error(state, "Invalid attack target: must be an opponent or their planeswalker")
                }
                if (defenderId !in state.getBattlefield()) {
                    return ExecutionResult.error(state, "Planeswalker is not on the battlefield")
                }
            }
            // Check per-defender restrictions (CantAttackUnless, CantBeAttackedWithout, etc.)
            val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
            for (rule in attackDefenderRules) {
                val error = rule.check(ctx, defenderId)
                if (error != null) {
                    return ExecutionResult.error(state, error)
                }
            }
        }

        // Check co-attacker requirements (Scarred Puma — "can't attack unless a black or
        // green creature also attacks"). Depends on the whole proposed attacker group, not
        // the defender, so it's validated here rather than via an AttackDefenderRule.
        val coAttackerValidation = validateCoAttackerRequirements(state, projected, attackers.keys)
        if (coAttackerValidation != null) {
            return ExecutionResult.error(state, coAttackerValidation)
        }

        // Check global attacker-count caps (Dueling Grounds — "No more than one creature can
        // attack each combat"). Applies to the whole declared attacker set, not per creature.
        val attackerCountValidation = validateGlobalAttackerCount(state, attackers.keys)
        if (attackerCountValidation != null) {
            return ExecutionResult.error(state, attackerCountValidation)
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

        // Check goaded requirements (CR 701.15b–c)
        val goadValidation = validateGoadedRequirements(state, attackingPlayer, attackers, projected, opponents)
        if (goadValidation != null) {
            return ExecutionResult.error(state, goadValidation)
        }

        // Calculate (but don't pay) the attack tax. If non-zero, pause for the attacking
        // player to confirm before we tap any of their mana — otherwise auto-tapping the
        // pool would steal sources they were saving for instants/post-combat plays.
        val totalTax = calculateTotalAttackTax(state, attackingPlayer, attackers, projected)
        if (totalTax > 0) {
            return pauseForAttackTaxConfirmation(state, attackingPlayer, attackers, totalTax, bands)
        }

        return commitAttackDeclaration(state, attackingPlayer, attackers, projected, taxEvents = emptyList(), bands = bands)
    }

    /**
     * Apply the post-tax commitment for a declared attack: stamp [AttackingComponent],
     * tap attackers (unless vigilance), mark tracking components, and emit the
     * [AttackersDeclaredEvent]. Callable both from the synchronous (no-tax) path in
     * [declareAttackers] and from [com.wingedsheep.engine.handlers.continuations.CombatTaxContinuationResumer]
     * after the player confirms and the tax is paid.
     *
     * @param taxEvents Events from the tax payment (auto-tap [TappedEvent]s, etc.) to emit
     *   before the [AttackersDeclaredEvent].
     * @param bands Validated band groupings (CR 702.22); each attacker in a band is stamped
     *   with a shared [AttackingComponent.bandId].
     */
    internal fun commitAttackDeclaration(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
        taxEvents: List<com.wingedsheep.engine.core.GameEvent>,
        bands: List<Set<EntityId>> = emptyList()
    ): ExecutionResult {
        // Assign each band a shared id, then map every banded attacker to it (CR 702.22).
        val bandIdByAttacker: Map<EntityId, String> = buildMap {
            for (band in bands) {
                val bandId = java.util.UUID.randomUUID().toString()
                for (attackerId in band) put(attackerId, bandId)
            }
        }

        var newState = state
        val tapEvents = mutableListOf<TappedEvent>()
        for ((attackerId, defenderId) in attackers) {
            val hasVigilance = projected.hasKeyword(attackerId, Keyword.VIGILANCE)
            newState = newState.updateEntity(attackerId) { container ->
                var updated = container.with(AttackingComponent(defenderId, bandIdByAttacker[attackerId]))
                if (!hasVigilance) {
                    updated = updated.with(TappedComponent)
                }
                updated
            }
            if (!hasVigilance) {
                val attackerName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                tapEvents.add(TappedEvent(attackerId, attackerName))
            }
        }

        newState = newState.updateEntity(attackingPlayer) { container ->
            var updated = container.with(AttackersDeclaredThisCombatComponent)
            if (attackers.isNotEmpty()) {
                updated = updated.with(PlayerAttackedThisTurnComponent)
                val previous = container.get<PlayerAttackersThisTurnComponent>()?.attackerIds ?: emptySet()
                updated = updated.with(PlayerAttackersThisTurnComponent(previous + attackers.keys))
            }
            updated
        }

        val attackerNames = attackers.keys.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
        return ExecutionResult.success(
            newState,
            taxEvents + tapEvents + listOf(AttackersDeclaredEvent(attackers.keys.toList(), attackerNames, attackingPlayer))
        )
    }

    private fun pauseForAttackTaxConfirmation(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        totalTax: Int,
        bands: List<Set<EntityId>> = emptyList(),
    ): ExecutionResult {
        val manaCost = com.wingedsheep.sdk.core.ManaCost(
            List(totalTax) { com.wingedsheep.sdk.core.ManaSymbol.generic(1) }
        )
        val manaSolver = com.wingedsheep.engine.mechanics.mana.ManaSolver(cardRegistry)
        val sources = manaSolver.findAvailableManaSources(state, attackingPlayer)
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
        val solution = manaSolver.solve(state, attackingPlayer, manaCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        val decisionId = java.util.UUID.randomUUID().toString()
        val attackerNames = attackers.keys.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }
        val attackerListing = when (attackerNames.size) {
            0 -> "your attackers"
            1 -> attackerNames.single()
            else -> attackerNames.dropLast(1).joinToString(", ") + " and " + attackerNames.last()
        }
        val decision = com.wingedsheep.engine.core.SelectManaSourcesDecision(
            id = decisionId,
            playerId = attackingPlayer,
            prompt = "Pay {$totalTax} to attack with $attackerListing",
            context = com.wingedsheep.engine.core.DecisionContext(
                sourceId = null,
                sourceName = "Attack tax",
                phase = com.wingedsheep.engine.core.DecisionPhase.COMBAT,
            ),
            availableSources = sourceOptions,
            requiredCost = manaCost.toString(),
            autoPaySuggestion = autoPaySuggestion,
            canDecline = true,
        )
        val continuation = com.wingedsheep.engine.core.AttackTaxManaSelectionContinuation(
            decisionId = decisionId,
            attackingPlayer = attackingPlayer,
            attackers = attackers,
            manaCost = manaCost,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion,
            bands = bands,
        )
        return ExecutionResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
        )
    }

    /**
     * Check if a creature passes all per-creature attack restrictions.
     * Does NOT check per-defender restrictions.
     */
    fun isValidAttacker(state: GameState, attackerId: EntityId, attackingPlayer: EntityId): Boolean {
        val projected = state.projectedState
        val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
        return attackRestrictionRules.all { it.check(ctx) == null }
    }

    /**
     * Check if a creature is restricted from attacking all opponents by per-defender rules.
     * Returns true if the creature cannot attack any opponent.
     */
    fun isRestrictedFromAllDefenders(state: GameState, attackerId: EntityId, attackingPlayer: EntityId): Boolean {
        val projected = state.projectedState
        val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
        return attackDefenderRules.any { it.restrictsAllDefenders(ctx) }
    }

    // =========================================================================
    // Private validation
    // =========================================================================

    /**
     * Validate that a creature can attack.
     * Delegates to registered [AttackRestrictionRule] instances.
     */
    private fun validateAttacker(
        state: GameState,
        attackingPlayer: EntityId,
        attackerId: EntityId
    ): String? {
        state.getEntity(attackerId) ?: return "Attacker not found: $attackerId"
        state.getEntity(attackerId)?.get<CardComponent>() ?: return "Not a card: $attackerId"

        val ctx = AttackCheckContext(
            state = state,
            projected = state.projectedState,
            attackerId = attackerId,
            attackingPlayer = attackingPlayer,
            cardRegistry = cardRegistry
        )
        for (rule in attackRestrictionRules) {
            val error = rule.check(ctx)
            if (error != null) return error
        }
        return null
    }

    /**
     * Validate "can't attack unless [X] also attacks" restrictions ([CantAttackUnlessCoAttacker]).
     *
     * For each proposed attacker carrying the restriction, at least one *other* attacker in the
     * same declaration must match the restriction's filter (evaluated with projected state so
     * color/type-changing effects are honored). Self never counts as its own co-attacker.
     */
    private fun validateCoAttackerRequirements(
        state: GameState,
        projected: ProjectedState,
        attackerIds: Set<EntityId>
    ): String? {
        for (attackerId in attackerIds) {
            val cardComponent = state.getEntity(attackerId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val restrictions = cardDef.staticAbilities
                .filterIsInstance<CantAttackUnlessCoAttacker>()
                .filter { it.filter.scope is Scope.Self }
            for (restriction in restrictions) {
                val context = PredicateContext(controllerId = projected.getController(attackerId) ?: attackerId)
                val satisfied = attackerIds.any { otherId ->
                    otherId != attackerId &&
                        predicateEvaluator.matches(state, projected, otherId, restriction.coAttackerFilter, context)
                }
                if (!satisfied) {
                    return "${cardComponent.name} ${restriction.description}"
                }
            }
        }
        return null
    }

    /**
     * Validate global attacker-count caps. While any permanent with [AttackerCountLimit] is on
     * the battlefield (e.g. Dueling Grounds), the total number of declared attackers across all
     * players may not exceed the smallest such cap. Returns an error message when violated.
     */
    private fun validateGlobalAttackerCount(
        state: GameState,
        attackerIds: Set<EntityId>
    ): String? {
        var cap: Int? = null
        var capDescription = ""
        for (permId in state.getBattlefield()) {
            val cardComponent = state.getEntity(permId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities.filterIsInstance<AttackerCountLimit>()) {
                if (cap == null || ability.maxAttackers < cap) {
                    cap = ability.maxAttackers
                    capDescription = ability.description
                }
            }
        }
        if (cap != null && attackerIds.size > cap) {
            return capDescription
        }
        return null
    }

    /**
     * Validate band declarations per CR 702.22c:
     * - Each band must have at least two creatures.
     * - Each creature in a band must also be one of the declared attackers.
     * - All creatures in a band must attack the same defender.
     * - At most one creature in a band may lack the [Keyword.BANDING] keyword.
     * - A creature may appear in at most one band.
     *
     * Returns an error message if any constraint is violated, or null when valid (including
     * the common no-bands case).
     */
    private fun validateBands(
        state: GameState,
        attackers: Map<EntityId, EntityId>,
        bands: List<Set<EntityId>>,
        projected: ProjectedState,
    ): String? {
        if (bands.isEmpty()) return null

        val seen = mutableSetOf<EntityId>()
        for (band in bands) {
            if (band.size < 2) {
                return "A band must contain at least two creatures"
            }
            var nonBandingCount = 0
            var sharedDefender: EntityId? = null
            for (creatureId in band) {
                if (creatureId in seen) {
                    val name = state.getEntity(creatureId)?.get<CardComponent>()?.name ?: "Creature"
                    return "$name cannot be in more than one band"
                }
                seen += creatureId

                val defenderId = attackers[creatureId]
                    ?: run {
                        val name = state.getEntity(creatureId)?.get<CardComponent>()?.name ?: "Creature"
                        return "$name is in a band but is not declared as an attacker"
                    }

                if (sharedDefender == null) {
                    sharedDefender = defenderId
                } else if (sharedDefender != defenderId) {
                    return "All creatures in a band must attack the same defender"
                }

                if (!projected.hasKeyword(creatureId, Keyword.BANDING)) {
                    nonBandingCount += 1
                    if (nonBandingCount > 1) {
                        return "A band may contain at most one creature without banding"
                    }
                }
            }
        }
        return null
    }

    /**
     * Validate "must attack" requirements (Taunt effect).
     */
    private fun validateMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val mustAttack = state.getEntity(attackingPlayer)?.get<MustAttackPlayerComponent>()
            ?: return null

        if (!mustAttack.activeThisTurn) {
            return null
        }

        val requiredDefender = mustAttack.defenderId
        val validAttackers = getValidAttackers(state, attackingPlayer)

        for (attackerId in validAttackers) {
            if (attackerId !in attackers.keys) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn (Taunt)"
            }
        }

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

            if (attackerId !in attackers.keys) {
                val cardName = container.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn"
            }
        }

        return null
    }

    /**
     * Validate goaded-creature requirements (CR 701.15b–c).
     *
     * Per CR 701.15b a goaded creature has two combat requirements:
     *   1. It attacks each combat if able. We enforce this here by requiring every
     *      valid attacker that carries [GoadedComponent] to appear in [attackers]
     *      — same shape as the must-attack-this-turn check, just keyed off the
     *      component instead.
     *   2. It attacks a player other than each of its goaders if able. CR 701.15b
     *      phrases this requirement in terms of a *player* — not a planeswalker — so
     *      the "if able" lookup considers only opponent players, not their
     *      planeswalkers. CR 701.15c stacks the requirement per goader. If every
     *      opponent player the creature could legally attack is a goader, the
     *      requirement is unsatisfiable, so the creature may attack a goader (and
     *      must attack something, per requirement 1).
     *
     * The chosen defender may itself be a planeswalker; [defenderControllerOf] maps
     * it to its controller so attacking a goader's planeswalker is still caught as
     * "attacking the goader" when an unaffected player was available.
     */
    private fun validateGoadedRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
        opponents: List<EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)

        for (attackerId in validAttackers) {
            val goaded = state.getEntity(attackerId)?.get<GoadedComponent>() ?: continue
            val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"

            if (attackerId !in attackers.keys) {
                return "$cardName is goaded and must attack this combat if able"
            }

            // Is there a non-goader *player* this creature could legally attack right
            // now (honoring per-defender restrictions)? Per CR 701.15b the "attack a
            // player other than the goader" requirement only ever points at players,
            // so planeswalkers are not part of this alternative pool.
            val ctx = com.wingedsheep.engine.mechanics.combat.rules.AttackCheckContext(
                state, projected, attackerId, attackingPlayer, cardRegistry
            )
            val hasNonGoaderPlayer = opponents.any { playerId ->
                playerId !in goaded.goaderIds &&
                    attackDefenderRules.all { rule -> rule.check(ctx, playerId) == null }
            }
            if (!hasNonGoaderPlayer) continue

            val chosenDefenderId = attackers[attackerId] ?: continue
            val chosenDefenderController = defenderControllerOf(state, projected, chosenDefenderId)
            if (chosenDefenderController in goaded.goaderIds) {
                val goaderName = state.getEntity(chosenDefenderController)?.get<PlayerComponent>()?.name
                    ?: "their goader"
                return "$cardName is goaded and must attack a player other than $goaderName if able"
            }
        }

        return null
    }

    private fun defenderControllerOf(
        state: GameState,
        projected: ProjectedState,
        defenderId: EntityId
    ): EntityId {
        if (state.getEntity(defenderId)?.has<LifeTotalComponent>() == true) return defenderId
        return projected.getController(defenderId) ?: defenderId
    }

    /**
     * Validate projected "must attack" requirements (e.g., from Grand Melee).
     */
    private fun validateProjectedMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val projected = state.projectedState

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
     * Get all creatures that can legally attack for a player.
     */
    private fun getValidAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        val projected = state.projectedState

        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false

            val ctx = AttackCheckContext(state, projected, entityId, playerId, cardRegistry)

            if (attackRestrictionRules.any { it.check(ctx) != null }) return@filter false
            if (attackDefenderRules.any { it.restrictsAllDefenders(ctx) }) return@filter false

            true
        }
    }

    /**
     * Get creatures that must attack this combat (for UI pre-selection).
     * Includes creatures required by MustAttackPlayerComponent, MustAttackThisTurnComponent,
     * and projected mustAttack (e.g., from static MustAttack ability like Valley Dasher).
     */
    fun getMandatoryAttackers(state: GameState, attackingPlayer: EntityId): List<EntityId> {
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val projected = state.projectedState
        val mandatory = mutableSetOf<EntityId>()

        // 1. MustAttackPlayerComponent (Taunt effect) — all valid attackers must attack
        val mustAttackPlayer = state.getEntity(attackingPlayer)?.get<MustAttackPlayerComponent>()
        if (mustAttackPlayer != null && mustAttackPlayer.activeThisTurn) {
            mandatory.addAll(validAttackers)
        }

        // 2. MustAttackThisTurnComponent (Walking Desecration) — individual creatures
        for (attackerId in validAttackers) {
            val container = state.getEntity(attackerId) ?: continue
            if (container.has<MustAttackThisTurnComponent>()) {
                mandatory.add(attackerId)
            }
        }

        // 3. Projected mustAttack (static ability like Valley Dasher, Grand Melee)
        for (attackerId in validAttackers) {
            if (projected.mustAttack(attackerId)) {
                mandatory.add(attackerId)
            }
        }

        // 4. GoadedComponent (CR 701.15b) — individual creatures
        for (attackerId in validAttackers) {
            val container = state.getEntity(attackerId) ?: continue
            if (container.has<GoadedComponent>()) {
                mandatory.add(attackerId)
            }
        }

        return mandatory.toList()
    }

    // =========================================================================
    // Attack Taxes
    // =========================================================================

    /**
     * Compute the total generic-mana attack tax owed for [attackers] without paying it.
     * Used by [declareAttackers] to decide whether to pause for player confirmation
     * before tapping any mana.
     */
    internal fun calculateTotalAttackTax(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState
    ): Int {
        if (attackers.isEmpty()) return 0
        val attackersPerDefender = mutableMapOf<EntityId, Int>()
        for ((_, defenderId) in attackers) {
            val defenderPlayerId = if (state.turnOrder.contains(defenderId)) {
                defenderId
            } else {
                projected.getController(defenderId)
            }
            if (defenderPlayerId != null) {
                attackersPerDefender[defenderPlayerId] = (attackersPerDefender[defenderPlayerId] ?: 0) + 1
            }
        }

        var totalGenericTax = 0
        for ((defenderId, attackerCount) in attackersPerDefender) {
            val defenderPermanents = projected.getBattlefieldControlledBy(defenderId)
            for (entityId in defenderPermanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
                for (ability in cardDef.staticAbilities) {
                    if (ability is AttackTax) {
                        val ctx = com.wingedsheep.engine.handlers.EffectContext(
                            sourceId = entityId,
                            controllerId = defenderId,
                        )
                        val taxPerAttacker = maxOf(0, dynamicAmountEvaluator.evaluate(state, ability.amountPerAttacker, ctx, projected))
                        totalGenericTax += taxPerAttacker * attackerCount
                    }
                }
            }
        }

        totalGenericTax += calculatePerCreatureTax(state, attackers.keys, projected)
        return totalGenericTax
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
