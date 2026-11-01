package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackedThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisTurnComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.combat.MustAttackNextCombatComponent
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.mechanics.combat.rules.AttackCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.AttackDefenderRule
import com.wingedsheep.engine.mechanics.combat.rules.AttackRestrictionRule
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.ExertedThisTurnComponent
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.engine.mechanics.battle.Battles
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
    private val predicateEvaluator: PredicateEvaluator
) {
    private val conditionEvaluator = predicateEvaluator.conditions

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
        // CR 805.10a/b — a creature attacks the opposing team; never a teammate. [getOpponents]
        // excludes the whole attacking team (in a non-team game just the attacker) and — unlike the
        // raw turn order — every seat that has left the game (CR 800.4a): a departed player is no
        // longer anyone's opponent, so they can't be attacked. The enumerator already reads the
        // same set; the handler has to agree with it because actions are client-supplied.
        val attackingTeam = state.teamOf(attackingPlayer)
        val opponents = state.getOpponents(attackingPlayer)

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
            // Validate the defender: an opponent player, a planeswalker controlled by an opponent,
            // or a battle protected by an opponent. A battle is keyed off its *protector*, never
            // its controller (CR 310.9b), which is what lets a player attack a Siege they control.
            if (defenderId !in opponents) {
                val isAttackableBattle = projected.isBattle(defenderId) &&
                    Battles.canBeAttackedBy(state, defenderId, attackingPlayer, opponents.toSet())
                val isAttackablePlaneswalker = projected.isPlaneswalker(defenderId) &&
                    projected.getController(defenderId) !in attackingTeam
                if (!isAttackableBattle && !isAttackablePlaneswalker) {
                    return ExecutionResult.error(
                        state,
                        "Invalid attack target: must be an opponent, their planeswalker, or a battle they protect"
                    )
                }
                if (defenderId !in state.getBattlefield()) {
                    return ExecutionResult.error(state, "Attacked permanent is not on the battlefield")
                }
            }
            // Check per-defender restrictions (CantAttackUnless, CantBeAttackedBy, etc.)
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

        // "Can only attack alone" (Master of Cruelties, CR 506.5 / 508.1c), and the attack
        // requirements a declaration obeying it may leave unobeyed (CR 508.1d).
        val loneOnly = LoneAttackers.of(state, getValidAttackers(state, attackingPlayer) + attackers.keys, cardRegistry, projected).toSet()
        LoneAttackers.violator(attackers.keys, loneOnly)?.let { violator ->
            val name = state.getEntity(violator)?.get<CardComponent>()?.name ?: "Creature"
            return ExecutionResult.error(state, "$name can only attack alone")
        }
        val excused = (if (loneOnly.isEmpty()) emptySet() else LoneAttackers.excusedRequirements(
            attackers.keys, requiredAttackers(state, attackingPlayer), loneOnly
        )) + attackersOnlyAtACost(state, attackingPlayer, (unexcusedRequiredAttackers(state, attackingPlayer).keys - attackers.keys).toList())

        // Check attacker-count caps — global (Dueling Grounds — "No more than one creature can
        // attack each combat") and per-defender (Tomik, Orzhov Lawmage). Both apply to the whole
        // declared attacker set, not per creature.
        val attackerCountValidation = validateAttackerCountLimits(state, projected, attackers)
        if (attackerCountValidation != null) {
            return ExecutionResult.error(state, attackerCountValidation)
        }

        // Check must-attack requirements (Taunt)
        val mustAttackValidation = validateMustAttackRequirements(state, attackingPlayer, attackers, excused)
        if (mustAttackValidation != null) {
            return ExecutionResult.error(state, mustAttackValidation)
        }

        // Check must-attack-this-turn requirements (Walking Desecration)
        val mustAttackThisTurnValidation = validateMustAttackThisTurnRequirements(state, attackingPlayer, attackers, excused)
        if (mustAttackThisTurnValidation != null) {
            return ExecutionResult.error(state, mustAttackThisTurnValidation)
        }

        // Check projected must-attack requirements (Grand Melee)
        val projectedMustAttackValidation = validateProjectedMustAttackRequirements(state, attackingPlayer, attackers, excused)
        if (projectedMustAttackValidation != null) {
            return ExecutionResult.error(state, projectedMustAttackValidation)
        }

        // Check "attacks a player each combat if able" (Nahiri, the Unforgiving)
        val mustAttackPlayerValidation = validateMustAttackAPlayerRequirements(
            state, attackingPlayer, attackers, projected, opponents
        )
        if (mustAttackPlayerValidation != null) {
            return ExecutionResult.error(state, mustAttackPlayerValidation)
        }

        // Check requirements naming the player to attack (Public Enemy)
        val directedValidation = validateDirectedMustAttackRequirements(
            state, attackingPlayer, attackers, projected, opponents, excused
        )
        if (directedValidation != null) {
            return ExecutionResult.error(state, directedValidation)
        }

        // Check goaded requirements (CR 701.15b–c)
        val goadValidation = validateGoadedRequirements(state, attackingPlayer, attackers, projected, opponents, excused)
        if (goadValidation != null) {
            return ExecutionResult.error(state, goadValidation)
        }

        // Check "each opponent must attack you … with at least one creature" (Trove of Temptation)
        val attackYouValidation = validateOpponentsMustAttackYou(state, attackingPlayer, attackers, projected)
        if (attackYouValidation != null) {
            return ExecutionResult.error(state, attackYouValidation)
        }

        // Declaration taps precede optional attack costs. Keep an immutable checkpoint so a
        // cancelled attack-tax question can roll back the declaration without leaving tap triggers.
        var tappedState = state
        val events = mutableListOf<GameEvent>()
        for (attacker in attackers.keys) {
            // Vigilance, or a rule modification saying attacking doesn't tap it (Johan).
            if (!projected.hasKeyword(attacker, Keyword.VIGILANCE) && !projected.attacksWithoutTapping(attacker)) {
                val (next, tapped) = tap(tappedState, attacker, reason = TapReason.ATTACK)
                tappedState = next
                events.addAll(tapped)
            }
        }
        val enlistInstances = EnlistAttackCosts.instances(tappedState, attackers.keys)
        // Only a cancellable tax window needs rollback state or a routing handle.
        val rollback = if (calculateTotalAttackTax(tappedState, attackers, tappedState.projectedState) > 0) {
            val (declarationId, allocated) = tappedState.newRoutingId()
            tappedState = allocated
            AttackDeclarationCheckpoint.capture(declarationId, state, tappedState, attackers.keys, events)
        } else null
        return chooseEnlist(tappedState, attackingPlayer, attackers, enlistInstances, emptyList(), bands, rollback, events)
    }

    internal fun chooseEnlist(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        remaining: List<EntityId>,
        chosen: List<EnlistPayment>,
        bands: List<Set<EntityId>>,
        rollback: AttackDeclarationCheckpoint? = null,
        carryEvents: List<GameEvent> = emptyList(),
    ): ExecutionResult {
        for ((index, attacker) in remaining.withIndex()) {
            val options = EnlistAttackCosts.eligible(state, attacker, attackers.keys, chosen)
            if (options.isEmpty()) continue
            val controller = state.projectedState.getController(attacker) ?: attackingPlayer
            val name = com.wingedsheep.engine.state.nameVisibleToAll(state, attacker, state.getEntity(attacker)?.get<CardComponent>()?.name ?: "Creature")
            return state.suspendForDecision(
                question = { id -> SelectCardsDecision(
                    id = id, playerId = controller,
                    prompt = "Enlist with $name: choose a creature to tap, or choose none",
                    context = DecisionContext(sourceId = attacker, sourceName = name, phase = DecisionPhase.COMBAT),
                    options = options, minSelections = 0, maxSelections = 1, useTargetingUI = true,
                ) },
                answer = AttackEnlistSelectionContinuation(attackingPlayer, attackers, remaining.drop(index), chosen, rollback, bands),
                events = carryEvents,
            )
        }
        return beginAttackPayments(state, attackingPlayer, attackers, chosen, bands, rollback, carryEvents)
    }

    private fun beginAttackPayments(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        enlistments: List<EnlistPayment>,
        bands: List<Set<EntityId>>,
        rollback: AttackDeclarationCheckpoint? = null,
        carryEvents: List<GameEvent> = emptyList(),
    ): ExecutionResult {
        val projected = state.projectedState
        // Calculate (but don't pay) the attack tax. If non-zero, pause for the attacking
        // player to confirm before we tap any of their mana — otherwise auto-tapping the
        // pool would steal sources they were saving for instants/post-combat plays.
        val totalTax = calculateTotalAttackTax(state, attackers, projected)
        if (totalTax > 0) {
            return pauseForAttackTaxConfirmation(state, attackingPlayer, attackers, totalTax, bands, enlistments, rollback, carryEvents)
        }

        return payEnlistAndContinue(state, attackingPlayer, attackers, enlistments, bands, carryEvents)
    }

    internal fun payEnlistAndContinue(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        enlistments: List<EnlistPayment>,
        bands: List<Set<EntityId>>,
        taxEvents: List<GameEvent>,
    ): ExecutionResult {
        val paid = EnlistAttackCosts.pay(state, enlistments)
        if (paid.error != null) return paid
        val paidState = paid.state
        val paidEvents = taxEvents + paid.events
        // Non-mana attack costs: "can't attack unless you sacrifice two Islands" (Leviathan). The
        // clause is a *restriction* checked at CR 508.1c, and the cost it names is determined and
        // paid at CR 508.1h–j — not an optional "as it attacks" cost (CR 508.1g), which the player
        // may always decline. Affordability was already enforced by CantAttackUnlessSacrificeRule,
        // so reaching here means the cost *can* be paid; what remains is choosing what to sacrifice.
        val sacrificeCosts = AttackSacrificeCosts.requirementsFor(paidState, attackers.keys, cardRegistry)
        if (sacrificeCosts.isNotEmpty()) {
            return pauseForAttackSacrifice(paidState, attackingPlayer, attackers, sacrificeCosts, bands, paidEvents)
        }

        return commitAttackDeclaration(paidState, attackingPlayer, attackers, paidState.projectedState, taxEvents = paidEvents, bands = bands)
    }

    /**
     * Apply the post-tax commitment for a declared attack: stamp [AttackingComponent],
     * mark tracking components, and emit the
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
        // Optional costs to attack (CR 508.1g): "you may exert this creature as it attacks"
        // (CR 701.43d). Asked after any mandatory attack cost is settled — exert never interacts
        // with mana or sacrifices, and a cancelled tax then never leaves a stray exert prompt. No
        // player gets priority in between, so the order is unobservable.
        val exertable = attackers.keys.filter { canExertAsItAttacks(state, projected, it) }
        if (exertable.isNotEmpty()) {
            return state.suspendForDecision(
                question = { decisionId ->
                    SelectCardsDecision(
                        id = decisionId,
                        playerId = attackingPlayer,
                        prompt = "Choose attackers to exert (they won't untap during your next untap step)",
                        context = DecisionContext(phase = DecisionPhase.COMBAT),
                        options = exertable,
                        minSelections = 0,
                        maxSelections = exertable.size,
                        useTargetingUI = true,
                    )
                },
                answer = AttackExertSelectionContinuation(
                    attackingPlayer = attackingPlayer,
                    attackers = attackers,
                    exertable = exertable,
                    bands = bands,
                ),
                events = taxEvents,
            )
        }
        return finishAttackDeclaration(state, attackingPlayer, attackers, projected, taxEvents, bands, exerted = emptySet())
    }

    /**
     * Whether [attackerId] carries [ExertAsItAttacks]. Read off the card definition like
     * [AttackSacrificeCosts.requirementFor]; a face-down creature (CR 708.2) or one that has lost
     * all abilities has no such option. A once-per-turn exert
     * ([ExertAsItAttacks.onlyIfNotExertedThisTurn]) isn't offered once the creature has been
     * exerted this turn — in an earlier combat phase or by an exert cost.
     */
    private fun canExertAsItAttacks(state: GameState, projected: ProjectedState, attackerId: EntityId): Boolean {
        val container = state.getEntity(attackerId) ?: return false
        if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(attackerId)) return false
        val cardDef = container.get<CardComponent>()?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return false
        val exertedThisTurn = container.has<ExertedThisTurnComponent>()
        return cardDef.staticAbilities.any {
            it is ExertAsItAttacks && !(it.onlyIfNotExertedThisTurn && exertedThisTurn)
        }
    }

    /**
     * Stamp the declaration once every attack cost, mandatory and optional, has been chosen and
     * paid. [exerted] are the attackers the player chose to exert as they attack (CR 701.43d);
     * each is exerted even if already exerted (CR 701.43b) and emits an [ExertedEvent] with
     * `asItAttacks`, which fires its linked "when you do" trigger (CR 607.2h).
     */
    internal fun finishAttackDeclaration(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
        taxEvents: List<com.wingedsheep.engine.core.GameEvent>,
        bands: List<Set<EntityId>>,
        exerted: Set<EntityId>,
    ): ExecutionResult {
        var newState = state
        val exertEvents = exerted.map { attackerId ->
            newState = newState.updateEntity(attackerId) { it.with(ExertedComponent).with(ExertedThisTurnComponent) }
            ExertedEvent(
                attackerId,
                state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature",
                asItAttacks = true,
            )
        }
        // Assign each band a shared id, then map every banded attacker to it (CR 702.22).
        val bandIdByAttacker: Map<EntityId, String> = buildMap {
            for (band in bands) {
                val (bandId, bandState) = newState.newRoutingId()
                newState = bandState
                for (attackerId in band) put(attackerId, bandId)
            }
        }

        for ((attackerId, defenderId) in attackers) {
            newState = newState.updateEntity(attackerId) { container ->
                container.with(AttackingComponent(defenderId, bandIdByAttacker[attackerId], defendingPlayerId = CombatDefenders.defendingPlayerOf(newState, defenderId)))
                    .with(AttackedThisCombatComponent)
                    .with(
                        newState.objectRef(attackerId).let { current ->
                            val previous = container.get<com.wingedsheep.engine.state.components.combat.TimesAttackedThisTurnComponent>()
                            com.wingedsheep.engine.state.components.combat.TimesAttackedThisTurnComponent(
                                turnNumber = newState.turnNumber,
                                count = (previous?.countFor(newState.turnNumber, current) ?: 0) + 1,
                                attackerObject = current,
                                // Only a player chosen as the defender is "attacked" (CR 508.1);
                                // `in turnOrder` is the player-identity idiom used below.
                                attackedPlayerIds = previous?.attackedPlayersFor(newState.turnNumber, current).orEmpty() +
                                    listOfNotNull(defenderId.takeIf { it in newState.turnOrder })
                            )
                        }
                    )
            }
            newState = AttackedPermanents.markAttacked(newState, defenderId)
        }

        val attackingMembers = state.sharedTurnTeam(attackingPlayer)
        // Creature-specific first attacks span the combined team declaration, even though each
        // player's Raid and opponent history records only their own creatures.
        val previousAttackersThisTurn = attackingMembers.flatMapTo(mutableSetOf()) { member ->
            state.getEntity(member)?.get<PlayerAttackersThisTurnComponent>()?.attackerIds.orEmpty()
        }
        val firstTimeAttackers = attackers.keys - previousAttackersThisTurn

        // Attackers whose declared defender is a *player* (CR 508.1), not a planeswalker or
        // battle. Backs AttackPredicate.DefenderIsPlayer ("attacks an opponent"); the defender
        // kind is fixed at declaration and the event carries no per-attacker defender identity,
        // so the fact is stamped here. `defenderId in turnOrder` is the player-identity idiom.
        val attackersAgainstPlayer = attackers.filterValues { it in state.turnOrder }.keys
        // "Attacks a player alone" (CR 506.6): the only creature declared as attacking its player,
        // whatever else attacks other players, planeswalkers or battles. Backs
        // AttackPredicate.AloneAgainstPlayer.
        val attackersAloneAgainstPlayer = attackers.entries
            .filter { it.key in attackersAgainstPlayer }
            .groupBy({ it.value }, { it.key })
            .values.mapNotNullTo(mutableSetOf()) { it.singleOrNull() }
        // The battle-defender sibling: backs AttackPredicate.DefenderIsBattle ("attacks a battle")
        // and the per-turn record behind StatePredicate.AttackedABattleThisTurn (War Historian).
        val attackersAgainstBattle = attackers.filterValues { Battles.isBattle(state, it) }.keys

        // The team declares once, but "you attacked" and attacked-opponent history belong to
        // the controller of each declared creature. Use the declaration's projected control.
        for (member in attackingMembers) {
            val memberAttackers = attackers.filterKeys { projected.getController(it) == member }
            val attackedPlayers = memberAttackers.values.filterTo(mutableSetOf()) { it in state.turnOrder }
            newState = newState.updateEntity(member) { container ->
                // Both markers are stamped even for an empty declaration: they record that the step
                // happened, which is what tells "declared nothing" apart from "never got the chance".
                var updated = container
                    .with(AttackersDeclaredThisCombatComponent)
                    .with(AttackersDeclaredThisTurnComponent)
                if (memberAttackers.isNotEmpty()) {
                    updated = updated.with(PlayerAttackedThisTurnComponent)
                    val previous = container.get<PlayerAttackersThisTurnComponent>()
                    updated = updated.with(
                        PlayerAttackersThisTurnComponent(
                            attackerIds = (previous?.attackerIds ?: emptySet()) + memberAttackers.keys,
                            battleAttackerIds = (previous?.battleAttackerIds ?: emptySet()) + (attackersAgainstBattle intersect memberAttackers.keys),
                            declaredAs = previous?.declaredAs.orEmpty() +
                                declaredAttackerSnapshots(state, memberAttackers.keys.toList())
                        )
                    )
                    if (attackedPlayers.isNotEmpty()) {
                        val previousAttackedPlayers = container
                            .get<com.wingedsheep.engine.state.components.combat.PlayerAttackedPlayersThisTurnComponent>()
                            ?.defendingPlayerIds ?: emptySet()
                        updated = updated.with(
                            com.wingedsheep.engine.state.components.combat.PlayerAttackedPlayersThisTurnComponent(
                                previousAttackedPlayers + attackedPlayers
                            )
                        )
                    }
                }
                updated
            }
        }

        val attackerNames = attackers.keys.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
        return ExecutionResult.success(
            newState,
            taxEvents + exertEvents + listOf(
                AttackersDeclaredEvent(
                    attackers.keys.toList(),
                    attackerNames,
                    attackingPlayer,
                    firstTimeAttackers = firstTimeAttackers,
                    attackersAgainstPlayer = attackersAgainstPlayer,
                    attackersAgainstBattle = attackersAgainstBattle,
                    attackersAloneAgainstPlayer = attackersAloneAgainstPlayer
                )
            )
        )
    }

    private fun pauseForAttackTaxConfirmation(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        totalTax: Int,
        bands: List<Set<EntityId>> = emptyList(),
        enlistments: List<EnlistPayment> = emptyList(),
        rollback: AttackDeclarationCheckpoint? = null,
        carryEvents: List<GameEvent> = emptyList(),
    ): ExecutionResult {
        val manaCost = com.wingedsheep.sdk.core.ManaCost(
            List(totalTax) { com.wingedsheep.sdk.core.ManaSymbol.generic(1) }
        )
        val manaSolver = com.wingedsheep.engine.mechanics.mana.ManaSolver(cardRegistry, predicateEvaluator)
        val reserved = enlistments.mapTo(mutableSetOf()) { it.enlistedId }
        // Sources with a "tap another permanent" sub-cost (Springleaf Drum, Gene Pollinator) stay
        // off the menu: the combat-tax resumer can't ask for the extra permanent. The player can
        // still activate them inside the payment window (CR 605.3a), as ManaPaymentWindow does.
        val sources = manaSolver.findAvailableManaSources(state, attackingPlayer)
            .filter { it.entityId !in reserved && it.tapPermanentsSubCost == null }
        val sourceOptions = sources.map { source ->
            com.wingedsheep.engine.core.ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless,
                requiresSacrifice = source.requiresSacrifice,
                manaAmount = source.manaAmount,
            )
        }
        val solution = manaSolver.solve(state, attackingPlayer, manaCost, excludeSources = reserved)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        val attackerNames = attackers.keys.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }
        val attackerListing = when (attackerNames.size) {
            0 -> "your attackers"
            1 -> attackerNames.single()
            else -> attackerNames.dropLast(1).joinToString(", ") + " and " + attackerNames.last()
        }
        val continuation = com.wingedsheep.engine.core.AttackTaxManaSelectionContinuation(
            attackingPlayer = attackingPlayer,
            attackers = attackers,
            manaCost = manaCost,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion,
            enlistments = enlistments,
            rollback = rollback,
            bands = bands,
        )
        return state.suspendForDecision(
            question = { decisionId ->
                com.wingedsheep.engine.core.SelectManaSourcesDecision(
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
            },
            answer = continuation,
            events = carryEvents,
        )
    }

    /**
     * Pause the declare-attackers step for the first unpaid sacrifice cost, queueing the rest.
     *
     * One decision per paying attacker rather than one combined pile: the costs are separate (two
     * Leviathans owe two Islands each, not four between them), and a combined prompt could not say
     * which creature a given Island was paying for.
     */
    private fun pauseForAttackSacrifice(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        costs: List<Pair<EntityId, com.wingedsheep.sdk.scripting.CantAttackUnlessSacrifice>>,
        bands: List<Set<EntityId>>,
        carryEvents: List<GameEvent>,
    ): ExecutionResult {
        val (payingAttacker, requirement) = costs.first()
        val eligible = AttackSacrificeCosts.eligiblePermanents(
            state, attackingPlayer, payingAttacker, requirement,
            predicateEvaluator = predicateEvaluator
        )
        val attackerName = state.getEntity(payingAttacker)?.get<CardComponent>()?.name ?: "your attacker"
        val continuation = com.wingedsheep.engine.core.AttackSacrificeSelectionContinuation(
            attackingPlayer = attackingPlayer,
            attackers = attackers,
            payingAttacker = payingAttacker,
            count = requirement.count,
            remaining = costs.drop(1).map { (id, req) ->
                com.wingedsheep.engine.core.PendingAttackSacrifice(id, req.count)
            },
            bands = bands,
        )
        return state.suspendForDecision(
            question = { decisionId ->
                com.wingedsheep.engine.core.SelectCardsDecision(
                    id = decisionId,
                    playerId = attackingPlayer,
                    prompt = "Sacrifice ${requirement.count} ${requirement.sacrificeFilter.description} to attack with $attackerName",
                    context = com.wingedsheep.engine.core.DecisionContext(
                        sourceId = payingAttacker,
                        sourceName = attackerName,
                        phase = com.wingedsheep.engine.core.DecisionPhase.COMBAT,
                    ),
                    options = eligible,
                    minSelections = requirement.count,
                    maxSelections = requirement.count,
                )
            },
            answer = continuation,
            events = carryEvents,
        )
    }

    /**
     * Ask for the next queued attack sacrifice after a previous one was paid. Same decision as
     * [pauseForAttackSacrifice], but carrying the earlier payment's events forward so the
     * sacrifices are all reported even though only the last resume commits the declaration.
     */
    internal fun pauseForNextAttackSacrifice(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        payingAttacker: EntityId,
        count: Int,
        remaining: List<com.wingedsheep.engine.core.PendingAttackSacrifice>,
        bands: List<Set<EntityId>>,
        carryEvents: List<com.wingedsheep.engine.core.GameEvent>,
    ): ExecutionResult {
        val requirement = AttackSacrificeCosts.requirementFor(state, payingAttacker, cardRegistry)
            ?: return commitAttackDeclaration(
                state, attackingPlayer, attackers, state.projectedState, carryEvents, bands
            )
        val eligible = AttackSacrificeCosts.eligiblePermanents(
            state, attackingPlayer, payingAttacker, requirement,
            predicateEvaluator = predicateEvaluator
        )
        val attackerName = state.getEntity(payingAttacker)?.get<CardComponent>()?.name ?: "your attacker"
        val continuation = com.wingedsheep.engine.core.AttackSacrificeSelectionContinuation(
            attackingPlayer = attackingPlayer,
            attackers = attackers,
            payingAttacker = payingAttacker,
            count = count,
            remaining = remaining,
            bands = bands,
        )
        return state.suspendForDecision(
            question = { decisionId ->
                com.wingedsheep.engine.core.SelectCardsDecision(
                    id = decisionId,
                    playerId = attackingPlayer,
                    prompt = "Sacrifice $count ${requirement.sacrificeFilter.description} to attack with $attackerName",
                    context = com.wingedsheep.engine.core.DecisionContext(
                        sourceId = payingAttacker,
                        sourceName = attackerName,
                        phase = com.wingedsheep.engine.core.DecisionPhase.COMBAT,
                    ),
                    options = eligible,
                    minSelections = count,
                    maxSelections = count,
                )
            },
            answer = continuation,
            events = carryEvents
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

    /**
     * The members of [candidates] (players, planeswalkers, battles) [attackerId] may legally attack
     * under the per-defender rules — the same [attackDefenderRules] [declareAttackers] enforces, so
     * a defender this returns never makes a declaration fail on its own (CR 508.1b–c: each attacker
     * chooses its own defender, and a restriction such as "can't attack unless defending player
     * controls a Mountain" or "creatures can't attack you" is about one particular defender).
     * Order is preserved.
     *
     * It also drops the defenders an "attacks a player other than X if able" requirement on this
     * creature rules out (goad, CR 701.15b; [MustAttackDefender.PlayerOtherThanYou]): that check
     * depends on this creature alone, so a declaration aimed at one of them fails whatever the
     * other attackers do. A player some other attackable opponent is named less often than always
     * remains, so the list never empties on this account.
     */
    fun legalDefendersFor(
        state: GameState,
        attackerId: EntityId,
        attackingPlayer: EntityId,
        candidates: List<EntityId>,
    ): List<EntityId> {
        val projected = state.projectedState
        val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
        val legal = candidates.filter { defenderId -> attackDefenderRules.all { it.check(ctx, defenderId) == null } }
        val avoided = GoadRules(state, cardRegistry, predicateEvaluator).playersToAttackOtherThan(attackerId)
        if (avoided.isEmpty()) return legal
        val opponents = state.getOpponents(attackingPlayer)
        return legal.filterNot { defenderId ->
            breaksAvoidableRequirement(
                state, projected, attackingPlayer, attackerId,
                defenderControllerOf(state, projected, defenderId), avoided, opponents
            )
        }
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
            for (restriction in coAttackerRestrictions(state, attackerId)) {
                if (!coAttackerAvailable(state, projected, attackerId, restriction, attackerIds)) {
                    return "${cardComponent.name} ${restriction.description}"
                }
            }
        }
        return null
    }

    /**
     * The self-scoped [CantAttackUnlessCoAttacker] restrictions on [attackerId]. Tokens have no
     * CardDefinition, so their restrictions arrive via grantedStaticAbilities (CreateTokenExecutor);
     * both sources are unioned so the "can't attack alone" half of Toby's Beast token is enforced
     * alongside printed restrictions (Scarred Puma).
     */
    private fun coAttackerRestrictions(state: GameState, attackerId: EntityId): List<CantAttackUnlessCoAttacker> {
        val cardComponent = state.getEntity(attackerId)?.get<CardComponent>() ?: return emptyList()
        val printed = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?.staticAbilities.orEmpty()
        val granted = state.grantedStaticAbilities
            .filter { it.entityId == attackerId }
            .map { it.ability }
        return (printed + granted)
            .filterIsInstance<CantAttackUnlessCoAttacker>()
            .filter { it.filter.scope is Scope.Self }
    }

    /** Whether some creature in [candidates] other than [attackerId] satisfies [restriction]. */
    private fun coAttackerAvailable(
        state: GameState,
        projected: ProjectedState,
        attackerId: EntityId,
        restriction: CantAttackUnlessCoAttacker,
        candidates: Collection<EntityId>
    ): Boolean {
        val context = PredicateContext(controllerId = projected.getController(attackerId) ?: attackerId)
        return candidates.any { otherId ->
            otherId != attackerId &&
                predicateEvaluator.matches(state, projected, otherId, restriction.coAttackerFilter, context)
        }
    }

    /**
     * Validate [AttackerCountLimit] caps, all three shapes:
     *  - **global** (Dueling Grounds): the total number of declared attackers across all players
     *    may not exceed the smallest such cap;
     *  - **per-defender** (`defenders` set, Tomik, Orzhov Lawmage): each attacked permanent that
     *    matches `defenders` — read relative to the limiting permanent's controller, against
     *    projected state — may be attacked by at most `maxAttackers` creatures;
     *  - **per-player** (`defendingPlayers` set, Mirri, Weatherlight Duelist): each matching
     *    player may be attacked by at most `maxAttackers` creatures — attacks on their
     *    planeswalkers and battles don't count.
     * Read through [CombatStaticScan], so a conditional cap ("as long as Mirri is tapped") binds
     * only while its condition holds, and a face-down (CR 708.2) or ability-stripped holder imposes
     * none. Returns an error message when violated.
     */
    private fun validateAttackerCountLimits(
        state: GameState,
        projected: ProjectedState,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val limits = CombatStaticScan.collect(state, cardRegistry, predicateEvaluator) { it as? AttackerCountLimit }
        if (limits.isEmpty()) return null
        var cap: Int? = null
        var capDescription = ""
        val attackersPerDefender = attackers.values.groupingBy { it }.eachCount()
        for ((permId, controllerId, ability) in limits) {
            val creatures = "No more than ${ability.maxAttackers} creature${if (ability.maxAttackers == 1) "" else "s"}"
            val defenders = ability.defenders
            val defendingPlayers = ability.defendingPlayers
            when {
                defendingPlayers != null -> for ((defenderId, count) in attackersPerDefender) {
                    if (count <= ability.maxAttackers) continue
                    if (defenderId !in state.turnOrder) continue
                    val covers = when (defendingPlayers) {
                        is Player.Each -> true
                        is Player.You -> defenderId == controllerId
                        is Player.EachOpponent -> state.isOpponentOf(defenderId, controllerId)
                        else -> false
                    }
                    if (!covers) continue
                    val defenderName = state.getEntity(defenderId)?.get<PlayerComponent>()?.name ?: "that player"
                    return "$creatures can attack $defenderName each combat"
                }
                defenders != null -> {
                    val context = PredicateContext(controllerId = controllerId, sourceId = permId)
                    for ((defenderId, count) in attackersPerDefender) {
                        if (count <= ability.maxAttackers) continue
                        if (defenderId !in state.getBattlefield()) continue
                        if (defenders.excludeSelf && defenderId == permId) continue
                        if (!predicateEvaluator.matches(state, projected, defenderId, defenders.baseFilter, context)) continue
                        val defenderName = state.getEntity(defenderId)?.get<CardComponent>()?.name ?: "that permanent"
                        return "$creatures can attack $defenderName each combat"
                    }
                }
                else -> if (cap == null || ability.maxAttackers < cap) {
                    cap = ability.maxAttackers
                    capDescription = ability.description
                }
            }
        }
        if (cap != null && attackers.size > cap) {
            return capDescription
        }
        return null
    }

    /**
     * Validate band declarations per CR 702.22c:
     * - Each band must have at least two creatures.
     * - Each creature in a band must also be one of the declared attackers.
     * - All creatures in a band must attack the same defender.
     * - At most one creature in a band may lack the [Keyword.BANDING] keyword — unless every
     *   member shares a quality one of them "bands with other" ([BandsWithOtherRules.formsBand]).
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
            }
            // CR 702.22c: either banding's form (at most one member without banding) or the
            // "bands with other [quality]" form (every member has the quality and at least one of
            // them has "bands with other [quality]").
            val nonBandingCount = band.count { !projected.hasKeyword(it, Keyword.BANDING) }
            if (nonBandingCount > 1 && !BandsWithOtherRules.formsBand(state, projected, band)) {
                return "A band needs every creature but one to have banding, or every creature to share a quality one of them \"bands with other\""
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
        attackers: Map<EntityId, EntityId>,
        excused: Set<EntityId>
    ): String? {
        // Taunt marks the player it was aimed at; in a shared team turn the team's one combined
        // attack (CR 805.10b) has to satisfy a requirement placed on either head.
        val mustAttack = state.sharedTurnTeam(attackingPlayer)
            .firstNotNullOfOrNull { member -> state.getEntity(member)?.get<MustAttackPlayerComponent>() }
            ?: return null

        if (!mustAttack.activeThisTurn) {
            return null
        }

        val requiredDefender = mustAttack.defenderId
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val projected = state.projectedState
        // CR 508.1d: a creature that could attack the taunter only by paying a cost isn't bound.
        fun bound(attackerId: EntityId) =
            attacksFreely(state, projected, attackingPlayer, attackerId, requiredDefender)

        for (attackerId in validAttackers) {
            if (attackerId !in attackers.keys && attackerId !in excused && bound(attackerId)) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn (Taunt)"
            }
        }

        for ((attackerId, defenderId) in attackers) {
            if (defenderId != requiredDefender && bound(attackerId)) {
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
        attackers: Map<EntityId, EntityId>,
        excused: Set<EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)

        for (attackerId in validAttackers) {
            val container = state.getEntity(attackerId) ?: continue
            if (!hasMarkedAttackRequirement(container)) continue

            if (attackerId !in attackers.keys && attackerId !in excused) {
                val cardName = container.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn"
            }
        }

        return null
    }

    /**
     * A requirement a resolved effect put on the creature itself: "attacks this turn if able"
     * (Walking Desecration), or "attacks during its controller's next combat phase if able"
     * (Sizzling Soloist) unless it is waiting out a combat whose attackers were already declared.
     */
    private fun hasMarkedAttackRequirement(container: ComponentContainer): Boolean =
        container.has<MustAttackThisTurnComponent>() ||
            container.get<MustAttackNextCombatComponent>()?.waitsOutCurrentCombat == false

    /**
     * Validate requirements naming the player to attack — "All creatures attack enchanted
     * creature's controller each combat if able" (Public Enemy). A creature bound by one
     * ([bindingDirectedDefenders]) must attack, and must attack one of the players it names: a
     * player, not their planeswalker or battle.
     */
    private fun validateDirectedMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
        opponents: List<EntityId>,
        excused: Set<EntityId>
    ): String? {
        for ((attackerId, bound) in boundAttackDefenders(state, attackingPlayer, projected, opponents)) {
            if (attackers[attackerId] in bound) continue
            if (attackerId !in attackers && attackerId in excused) continue
            val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
            val playerName = bound.singleOrNull()?.let { state.getEntity(it)?.get<PlayerComponent>()?.name }
                ?: "the player it's required to attack"
            return "$cardName must attack $playerName this combat if able"
        }
        return null
    }

    /**
     * Each creature [attackingPlayer] could attack with that a directed requirement binds, mapped
     * to the players it must attack one of (Public Enemy) — empty in the overwhelmingly common
     * case. A creature is bound to a named opponent it could attack at no cost and without
     * breaking its goad ([canAttackFreely]) — the "if able" of CR 508.1d; it is never bound by a
     * player who controls it, nor one it can't attack. An active Taunt aimed at another player
     * wins, as it does over Trove of Temptation in [validateOpponentsMustAttackYou]: every creature
     * attacking the taunter already obeys as many requirements as any declaration can.
     */
    fun boundAttackDefenders(
        state: GameState,
        attackingPlayer: EntityId,
        projected: ProjectedState = state.projectedState,
        opponents: List<EntityId> = state.getOpponents(attackingPlayer),
        goadRules: GoadRules? = null,
    ): Map<EntityId, Set<EntityId>> {
        val directed = DirectedAttackRequirements(state, cardRegistry, predicateEvaluator)
        if (directed.isEmpty) return emptyMap()
        val taunted = activeTaunt(state, attackingPlayer)?.defenderId
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val goad = goadRules ?: GoadRules(state, cardRegistry, predicateEvaluator)
        val bound = mutableMapOf<EntityId, Set<EntityId>>()
        for (attackerId in validAttackers) {
            val named = directed.namedDefendersOf(attackerId)
            if (named.isEmpty() || (taunted != null && taunted !in named)) continue
            val players = named.filterTo(mutableSetOf()) { player ->
                player in opponents && canAttackFreely(
                    state, projected, attackingPlayer, attackerId, listOf(player), player, opponents, validAttackers, goad
                )
            }
            if (players.isNotEmpty()) bound[attackerId] = players
        }
        return bound
    }

    /** The Taunt requirement on [attackingPlayer]'s team this turn, if one is active. */
    private fun activeTaunt(state: GameState, attackingPlayer: EntityId): MustAttackPlayerComponent? =
        state.sharedTurnTeam(attackingPlayer)
            .firstNotNullOfOrNull { member -> state.getEntity(member)?.get<MustAttackPlayerComponent>() }
            ?.takeIf { it.activeThisTurn }

    /**
     * Validate goaded-creature requirements (CR 701.15b–c), and the "attacks a player other than
     * you if able" requirement that shares goad's second half without goading
     * ([MustAttackDefender.PlayerOtherThanYou], Kardur, Doomscourge).
     *
     * Per CR 701.15b a goaded creature has two combat requirements:
     *   1. It attacks each combat if able. We enforce this here by requiring every
     *      valid attacker [GoadRules] reports goaded to appear in [attackers]
     *      — same shape as the must-attack-this-turn check, just keyed off the
     *      component instead. (A Kardur-style requirement projects `mustAttack`, which
     *      [validateProjectedMustAttackRequirements] enforces.)
     *   2. It attacks a player other than each of its goaders if able. CR 701.15b
     *      phrases this requirement in terms of a *player* — not a planeswalker — so
     *      the "if able" lookup considers only opponent players, not their
     *      planeswalkers. CR 701.15c stacks the requirement per goader, and every
     *      "other than X" requirement stacks with them ([GoadRules.playersToAttackOtherThan]).
     *      The attacked player must be named by no more of these requirements than some
     *      other opponent player the creature could legally attack (CR 508.1d, counted
     *      over the "other than X" requirements only — a Trove of Temptation or Public
     *      Enemy requirement is weighed by its own check). When every such player is named
     *      equally often (in two-player, the only opponent), the creature may attack any of them.
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
        opponents: List<EntityId>,
        excused: Set<EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val goadRules = GoadRules(state, cardRegistry, predicateEvaluator)

        for (attackerId in validAttackers) {
            val goaderIds = goadRules.goadersOf(attackerId)
            val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"

            if (goaderIds.isNotEmpty() && attackerId !in attackers.keys && attackerId !in excused) {
                return "$cardName is goaded and must attack this combat if able"
            }

            val chosenDefenderId = attackers[attackerId] ?: continue
            val avoided = goadRules.playersToAttackOtherThan(attackerId)
            if (avoided.isEmpty()) continue
            val chosenDefenderController = defenderControllerOf(state, projected, chosenDefenderId)
            if (!breaksAvoidableRequirement(state, projected, attackingPlayer, attackerId, chosenDefenderController, avoided, opponents)) {
                continue
            }
            val avoidedName = state.getEntity(chosenDefenderController)?.get<PlayerComponent>()?.name
                ?: "that player"
            return if (chosenDefenderController in goaderIds) {
                "$cardName is goaded and must attack a player other than $avoidedName if able"
            } else {
                "$cardName must attack a player other than $avoidedName if able"
            }
        }

        return null
    }

    /**
     * Whether attacking [player] (or something of theirs) breaks more of [attackerId]'s "attacks a
     * player other than X if able" requirements ([avoided], one entry per requirement) than
     * attacking some other opponent *player* it could legally attack would — CR 508.1d's "as many
     * requirements as possible", over goad's player-only alternatives (CR 701.15b).
     */
    private fun breaksAvoidableRequirement(
        state: GameState,
        projected: ProjectedState,
        attackingPlayer: EntityId,
        attackerId: EntityId,
        player: EntityId,
        avoided: List<EntityId>,
        opponents: List<EntityId>
    ): Boolean {
        val broken = avoided.count { it == player }
        if (broken == 0) return false
        return opponents.any { other ->
            other != player && avoided.count { it == other } < broken &&
                attacksFreely(state, projected, attackingPlayer, attackerId, other)
        }
    }

    /**
     * Validate [com.wingedsheep.sdk.scripting.OpponentsMustAttackYou] (Trove of Temptation): for
     * each player [OpponentsMustAttackYouRequirement] says must be attacked, the declaration has
     * to send at least one creature at that player or a planeswalker they control — if able.
     *
     * "Able" follows CR 508.1d: some creature could attack one of those defenders without its
     * controller paying a cost (an attack tax or a "can't attack unless you sacrifice" cost), and
     * without breaking a requirement the creature already carries. A goaded creature whose goaders
     * include this player is not counted while it has a non-goader player to attack (obeying one
     * requirement by breaking another gains nothing), and an active Taunt aimed at a different
     * player — every creature attacks the taunter — already obeys as many requirements as any
     * declaration can, so it wins.
     */
    private fun validateOpponentsMustAttackYou(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState
    ): String? {
        val requiring = OpponentsMustAttackYouRequirement.requiringPlayers(
            state, cardRegistry, predicateEvaluator, attackingPlayer
        )
        if (requiring.isEmpty()) return null
        val taunt = activeTaunt(state, attackingPlayer)
        val opponents = state.getOpponents(attackingPlayer)
        val validAttackers by lazy { getValidAttackers(state, attackingPlayer) }
        val goadRules by lazy { GoadRules(state, cardRegistry, predicateEvaluator) }
        val directed by lazy { boundAttackDefenders(state, attackingPlayer, projected, opponents, goadRules) }
        for (player in requiring) {
            if (attackers.values.any { OpponentsMustAttackYouRequirement.isAttackOn(state, projected, it, player) }) {
                continue
            }
            if (taunt != null && taunt.defenderId != player) continue
            val defenders = OpponentsMustAttackYouRequirement.defendersOf(state, projected, player)
            // A creature a directed requirement binds to someone else isn't free to come here:
            // obeying one requirement by breaking another gains nothing, as with goad.
            val able = validAttackers.any { attackerId ->
                directed[attackerId].let { it == null || player in it } &&
                    canAttackFreely(state, projected, attackingPlayer, attackerId, defenders, player, opponents, validAttackers, goadRules)
            }
            if (able) {
                val name = state.getEntity(player)?.get<PlayerComponent>()?.name ?: "that player"
                return "At least one creature must attack $name or a planeswalker they control this combat"
            }
        }
        return null
    }

    /**
     * Whether [attackerId] could attack one of [defenders] (all belonging to [player]) at no cost
     * and without breaking its goad (or other "a player other than X") requirement — the "if able" of
     * [validateOpponentsMustAttackYou]. A creature that can't attack unless another creature
     * also attacks counts only if one of [validAttackers] could be that co-attacker; otherwise a
     * lone Scarred Puma would leave no legal declaration at all.
     */
    private fun canAttackFreely(
        state: GameState,
        projected: ProjectedState,
        attackingPlayer: EntityId,
        attackerId: EntityId,
        defenders: List<EntityId>,
        player: EntityId,
        opponents: List<EntityId>,
        validAttackers: List<EntityId>,
        goadRules: GoadRules
    ): Boolean {
        if (AttackSacrificeCosts.requirementFor(state, attackerId, cardRegistry) != null) return false
        if (coAttackerRestrictions(state, attackerId).any {
                !coAttackerAvailable(state, projected, attackerId, it, validAttackers)
            }) return false
        val avoided = goadRules.playersToAttackOtherThan(attackerId)
        if (breaksAvoidableRequirement(state, projected, attackingPlayer, attackerId, player, avoided, opponents)) {
            return false
        }
        return defenders.any { defenderId -> attacksFreely(state, projected, attackingPlayer, attackerId, defenderId) }
    }

    /**
     * The player an attack aimed at [defenderId] is really aimed at: the player themselves, a
     * planeswalker's controller, or — for a battle — its protector rather than its controller
     * (CR 310.9d).
     */
    private fun defenderControllerOf(
        state: GameState,
        projected: ProjectedState,
        defenderId: EntityId
    ): EntityId {
        if (state.getEntity(defenderId)?.has<LifeTotalComponent>() == true) return defenderId
        Battles.protectorOf(state, defenderId)?.let { return it }
        return projected.getController(defenderId) ?: defenderId
    }

    /**
     * Whether [attackerId] is required to attack this combat by a "must attack" static — either the
     * projected one (printed `MustAttack`: Valley Dasher, Grand Melee) or an entity-scoped
     * `MustAttack` granted at runtime and stored in [GameState.grantedStaticAbilities] (Carnage's
     * reanimated target: "attacks each combat if able"). Granted statics never reach projection, so
     * they must be consulted here at the point of use, alongside the projected value.
     */
    private fun mustAttackThisCombat(state: GameState, attackerId: EntityId): Boolean {
        if (state.projectedState.mustAttack(attackerId)) return true
        return state.grantedStaticAbilities.any {
            it.entityId == attackerId && it.ability is MustAttack &&
                it.ability.defender != MustAttackDefender.EnchantedCreatureController &&
                it.ability.filter.scope is Scope.Self
        }
    }

    /**
     * Whether [attackerId]'s must-attack requirement can only be met by attacking a *player*
     * ([MustAttackDefender.APlayer]) — projected from a printed static, or granted at runtime (Nahiri,
     * the Unforgiving's +1: "until your next turn, up to one target creature attacks a player each
     * combat if able").
     */
    private fun mustAttackAPlayer(state: GameState, attackerId: EntityId): Boolean {
        if (state.projectedState.mustAttackPlayer(attackerId)) return true
        return state.grantedStaticAbilities.any {
            it.entityId == attackerId && it.ability is MustAttack &&
                it.ability.defender == MustAttackDefender.APlayer && it.ability.filter.scope is Scope.Self
        }
    }

    /**
     * Validate "attacks a player each combat if able". Attacking at all is enforced by
     * [validateProjectedMustAttackRequirements]; this adds the defender half: a creature under the
     * requirement that attacks a planeswalker or battle is illegal while some opponent *player* is
     * a legal defender for it (CR 508.1d — obey as many requirements as possible). Mirrors the
     * player-only half of goad (CR 701.15b).
     */
    private fun validateMustAttackAPlayerRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
        opponents: List<EntityId>
    ): String? {
        for ((attackerId, defenderId) in attackers) {
            if (state.getEntity(defenderId)?.has<LifeTotalComponent>() == true) continue
            if (!mustAttackAPlayer(state, attackerId)) continue
            val canAttackAPlayer = opponents.any { playerId ->
                attacksFreely(state, projected, attackingPlayer, attackerId, playerId)
            }
            if (canAttackAPlayer) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack a player this combat if able"
            }
        }
        return null
    }

    /**
     * Validate projected "must attack" requirements (e.g., from Grand Melee).
     */
    private fun validateProjectedMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        excused: Set<EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)

        for (attackerId in validAttackers) {
            if (!mustAttackThisCombat(state, attackerId)) continue

            if (attackerId !in attackers.keys && attackerId !in excused) {
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
     * and projected mustAttack (e.g., from static MustAttack ability like Valley Dasher) — only
     * those every declaration obeying the most requirements attacks with, so a creature that can
     * only attack alone is listed only when nothing else is required, and is never locked in
     * beside one ([LoneAttackers.mandatory]).
     */
    fun getMandatoryAttackers(state: GameState, attackingPlayer: EntityId): List<EntityId> {
        val required = requiredAttackers(state, attackingPlayer)
        if (required.isEmpty()) return emptyList()
        return LoneAttackers.mandatory(required, LoneAttackers.of(state, required.keys, cardRegistry).toSet())
    }

    /**
     * Every creature some attack requirement names, before [LoneAttackers] weighs them against
     * "can only attack alone" (CR 508.1d) — see [getMandatoryAttackers].
     */
    private fun requiredAttackers(state: GameState, attackingPlayer: EntityId): Map<EntityId, Int> {
        val mandatory = unexcusedRequiredAttackers(state, attackingPlayer).toMutableMap()
        mandatory.keys.removeAll(attackersOnlyAtACost(state, attackingPlayer, mandatory.keys.toList()))
        return mandatory
    }

    /** [requiredAttackers] before the CR 508.1d cost excusal ([attackersOnlyAtACost]). */
    private fun unexcusedRequiredAttackers(state: GameState, attackingPlayer: EntityId): Map<EntityId, Int> {
        val validAttackers = getValidAttackers(state, attackingPlayer)
        // Creature → how many "attacks if able" requirements it carries (CR 508.1d counts
        // requirements, not creatures).
        val mandatory = linkedMapOf<EntityId, Int>()
        fun require(attackerId: EntityId, count: Int = 1) {
            mandatory[attackerId] = (mandatory[attackerId] ?: 0) + count
        }

        // 1. MustAttackPlayerComponent (Taunt effect) — all valid attackers must attack
        val mustAttackPlayer = state.getEntity(attackingPlayer)?.get<MustAttackPlayerComponent>()
        if (mustAttackPlayer != null && mustAttackPlayer.activeThisTurn) {
            // Only creatures that could attack the taunter without paying a cost (CR 508.1d).
            val projected = state.projectedState
            validAttackers.filter { attacksFreely(state, projected, attackingPlayer, it, mustAttackPlayer.defenderId) }
                .forEach { require(it) }
        }

        // 2. A requirement marked on the creature (Walking Desecration, Sizzling Soloist)
        for (attackerId in validAttackers) {
            val container = state.getEntity(attackerId) ?: continue
            if (hasMarkedAttackRequirement(container)) {
                require(attackerId)
            }
        }

        // 3. Projected mustAttack (static ability like Valley Dasher, Grand Melee) or a granted
        // entity-scoped MustAttack (Carnage's reanimated target), which never reaches projection.
        // Projection folds every such static into one flag, so they weigh as one requirement.
        for (attackerId in validAttackers) {
            if (mustAttackThisCombat(state, attackerId)) {
                require(attackerId)
            }
        }

        // 4. Goaded (CR 701.15b) — a resolved goad designation or a Goaded static; each goader
        // adds its own requirement (CR 701.15c)
        val goadRules = GoadRules(state, cardRegistry, predicateEvaluator)
        for (attackerId in validAttackers) {
            val goaders = goadRules.goadersOf(attackerId).size
            if (goaders > 0) require(attackerId, goaders)
        }

        // 5. A requirement naming the player to attack (Public Enemy), where it binds
        boundAttackDefenders(state, attackingPlayer, goadRules = goadRules).keys.forEach { require(it) }

        return mandatory
    }

    /**
     * The members of [candidates] that could attack only by paying for it: every defender they may
     * legally attack charges an attack tax, or attacking at all demands a sacrifice. CR 508.1d — a
     * player isn't required to pay a cost to obey an attack requirement — so an "attacks each
     * combat if able" creature facing Ghostly Prison, or Summon: Yojimbo's floating tax, may stay
     * home unless it has some untaxed defender to attack. A creature with no legal defender at all
     * is not listed: it simply can't attack, which every requirement already accounts for.
     */
    private fun attackersOnlyAtACost(
        state: GameState,
        attackingPlayer: EntityId,
        candidates: List<EntityId>,
    ): Set<EntityId> {
        if (candidates.isEmpty()) return emptySet()
        val projected = state.projectedState
        val defenders = CombatDefenders.attackTargets(state, projected, attackingPlayer)
        return candidates.filterTo(mutableSetOf()) { attackerId ->
            if (AttackSacrificeCosts.requirementFor(state, attackerId, cardRegistry) != null) return@filterTo true
            val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
            val legal = defenders.filter { defenderId -> attackDefenderRules.all { it.check(ctx, defenderId) == null } }
            legal.isNotEmpty() && legal.none { calculateTotalAttackTax(state, mapOf(attackerId to it), projected) == 0 }
        }
    }

    /**
     * Whether [attackerId] may attack [defenderId] at no cost — legal under the per-defender rules,
     * untaxed, and with no sacrifice demanded. CR 508.1d: only such an attack counts as "able" for
     * a requirement, so every check that weighs one defender against another ("a player other than
     * X", "a player", Taunt's player) asks this rather than legality alone.
     */
    private fun attacksFreely(
        state: GameState,
        projected: ProjectedState,
        attackingPlayer: EntityId,
        attackerId: EntityId,
        defenderId: EntityId,
    ): Boolean {
        if (AttackSacrificeCosts.requirementFor(state, attackerId, cardRegistry) != null) return false
        val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
        return attackDefenderRules.all { it.check(ctx, defenderId) == null } &&
            calculateTotalAttackTax(state, mapOf(attackerId to defenderId), projected) == 0
    }

    // =========================================================================
    // Attack Taxes
    // =========================================================================

    /**
     * Compute the total generic-mana attack tax owed for [attackers] without paying it.
     * Used by [declareAttackers] to decide whether to pause for player confirmation
     * before tapping any mana. Shared with the AI via [CombatTaxes], which prices a
     * proposed attack before proposing it.
     */
    internal fun calculateTotalAttackTax(
        state: GameState,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState
    ): Int = CombatTaxes.attackTax(state, cardRegistry, attackers, projected, predicateEvaluator = predicateEvaluator)

    /**
     * Each attacker's characteristics as declared (CR 508.1), read off the pre-declaration
     * [state] — the projection the declaration was legal against. Kept on the attack record so
     * "you attacked with a Rogue this turn" survives the Rogue leaving or ceasing to be one.
     */
    private fun declaredAttackerSnapshots(state: GameState, ids: List<EntityId>): List<EntitySnapshot> {
        val projected = state.projectedState
        return captureEntitySnapshots(ids, projected, state).map { snapshot ->
            snapshot.copy(
                keywords = projected.getKeywords(snapshot.entityId),
                wasToken = state.getEntity(snapshot.entityId)?.has<TokenComponent>() ?: false,
            )
        }
    }
}
