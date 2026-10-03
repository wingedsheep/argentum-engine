package com.wingedsheep.gym.contract

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.BatchYesNoResponse
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.stack.AbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Converts `(GameState, perspectivePlayerId)` into a [TrainingObservation].
 *
 * ## Information hiding
 *
 * Identity visibility comes from the engine's [Visibility] authority, so a hidden zone is not
 * necessarily an empty one. A card a hand-peek effect has shown this perspective stays visible to
 * it, and so does a top card the perspective may legitimately see, which means [ZoneView.cards]
 * carries the subset this player knows while [ZoneView.size] stays the true count and
 * [ZoneView.hidden] stays true. A face-down object reports only what the player looking at it may
 * know (CR 708.5). Set [revealAll] to `true` to disable masking — only appropriate for debug
 * scripts; never for real self-play training.
 *
 * ## Projected vs. base state
 *
 * All per-entity fields (types, subtypes, colors, keywords, power, toughness,
 * controller) are read from [GameState.projectedState] so Rule 613 continuous
 * effects are reflected. The zone a card sits in still comes from the base
 * zone map (control-changing effects don't move cards between owner-keyed
 * zones — see `GameState.getBattlefield`).
 */
class ObservationBuilder(
    cardRegistry: CardRegistry,
    private val schemaHash: String = SchemaHash.CURRENT
) {
    private val visibility = Visibility(cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)

    fun build(
        state: GameState,
        perspectivePlayerId: EntityId,
        legalActions: List<LegalAction>,
        revealAll: Boolean = false
    ): ObservationResult {
        val projected = state.projectedState

        val players = state.turnOrder.map { buildPlayerView(state, it, perspectivePlayerId) }

        val zones = buildZones(state, perspectivePlayerId, revealAll)

        val stack = state.stack.map { entityId ->
            buildStackItem(state, entityId, perspectivePlayerId, revealAll)
        }

        val pendingDecisionAndRegistry = state.pendingDecision
            ?.let { buildPendingDecision(it) }
        val canSeePlay = state.pendingDecision?.let { revealAll || perspectivePlayerId == it.playerId ||
            perspectivePlayerId == state.actorFor(it.playerId) } ?: false
        val pendingDecisionView = pendingDecisionAndRegistry?.first?.let {
            if (canSeePlay) it else it.copy(subjectEntityId = null)
        }
        val decisionRegistry = pendingDecisionAndRegistry?.second ?: ActionRegistry.EMPTY

        // Build legal-action views and their registry. When mid-decision the
        // engine's `legalActions` is empty — we use the decision options instead.
        val legalActionViews: List<LegalActionView>
        val actionRegistry: ActionRegistry
        if (state.pendingDecision is com.wingedsheep.engine.core.PlayCardDecision) {
            val play = state.pendingDecision as com.wingedsheep.engine.core.PlayCardDecision
            val visiblePlays = if (canSeePlay) legalActions else emptyList()
            legalActionViews = visiblePlays.mapIndexed { idx, la -> legalActionToView(idx, la).copy(isDecisionOption = true) }
            actionRegistry = ActionRegistry.ofDecisionResponses(visiblePlays.map {
                com.wingedsheep.engine.core.PlayCardResponse(play.id, it.action)
            })
        } else if (state.pendingDecision != null) {
            val responses = decisionRegistry.decisionResponses.map { it.second }
            legalActionViews = buildDecisionOptionViews(state.pendingDecision!!, responses)
            actionRegistry = decisionRegistry
        } else {
            legalActionViews = legalActions.mapIndexed { idx, la -> legalActionToView(idx, la) }
            actionRegistry = ActionRegistry.ofLegalActions(legalActions)
        }

        val obs = TrainingObservation(
            schemaHash = schemaHash,
            perspectivePlayerId = perspectivePlayerId,
            agentToAct = state.pendingDecision?.playerId ?: state.priorityPlayerId,
            turnNumber = state.turnNumber,
            phase = state.phase,
            step = state.step,
            activePlayerId = state.activePlayerId,
            priorityPlayerId = state.priorityPlayerId,
            players = players,
            zones = zones,
            stack = stack,
            pendingDecision = pendingDecisionView,
            legalActions = legalActionViews,
            terminated = state.gameOver,
            winnerId = state.winnerId,
            stateDigest = ""
        )
        val digested = obs.copy(stateDigest = StateDigest.compute(obs))
        return ObservationResult(digested, actionRegistry)
    }

    // =========================================================================
    // Players
    // =========================================================================

    private fun buildPlayerView(
        state: GameState,
        playerId: EntityId,
        perspectivePlayerId: EntityId
    ): PlayerView {
        val container = state.getEntity(playerId)
        val playerComp = container?.get<PlayerComponent>()
        // Through the resolver — a 2HG team's shared life lives on one member (CR 810.9a).
        val life = if (container?.get<LifeTotalComponent>() != null) state.lifeTotal(playerId) else 0
        val manaPool = container?.get<ManaPoolComponent>()
        val hasLost = container?.get<PlayerLostComponent>() != null

        return PlayerView(
            id = playerId,
            name = playerComp?.name ?: playerId.value,
            lifeTotal = life,
            handSize = state.getHand(playerId).size,
            librarySize = state.getLibrary(playerId).size,
            graveyardSize = state.getGraveyard(playerId).size,
            exileSize = state.getExile(playerId).size,
            manaPool = manaPool?.let {
                ManaPoolView(
                    white = it.white,
                    blue = it.blue,
                    black = it.black,
                    red = it.red,
                    green = it.green,
                    colorless = it.colorless
                )
            } ?: ManaPoolView(),
            isPerspective = playerId == perspectivePlayerId,
            isActive = playerId == state.activePlayerId,
            hasPriority = playerId == state.priorityPlayerId,
            hasLost = hasLost
        )
    }

    // =========================================================================
    // Zones
    // =========================================================================

    private fun buildZones(
        state: GameState,
        perspectivePlayerId: EntityId,
        revealAll: Boolean
    ): List<ZoneView> {
        // Emit a view for every modeled (player, zone) in the contract's stable order so trainers
        // see a consistent shape regardless of whether a zone happens to be empty. The stack has
        // its own ordered representation above; see [TRAINING_OBSERVATION_ZONE_ORDER] for why the
        // order is spelled out rather than taken from the enum.
        val views = mutableListOf<ZoneView>()
        for (playerId in state.turnOrder) {
            for (zone in TRAINING_OBSERVATION_ZONE_ORDER) {
                val key = ZoneKey(playerId, zone)
                val ids = state.getZone(key)
                val wholeZoneVisible = revealAll ||
                    visibility.isZoneVisibleTo(state, key, perspectivePlayerId)
                // A hidden zone still exposes what this perspective may know — a card a hand-peek
                // effect showed it, or a top card it is entitled to see. Which of those apply is
                // the engine's question, not this builder's.
                val cards = ids.mapNotNull { entityId ->
                    val identityVisible = revealAll || visibility.isCardIdentityVisibleTo(
                        state,
                        key,
                        entityId,
                        perspectivePlayerId,
                    )
                    if (!wholeZoneVisible && !identityVisible) null
                    else buildEntityFeatures(state, entityId, zone, identityVisible)
                }
                views += ZoneView(
                    ownerId = playerId,
                    zoneType = zone,
                    hidden = !wholeZoneVisible,
                    size = ids.size,
                    cards = cards
                )
            }
        }
        return views
    }

    // =========================================================================
    // Entities
    // =========================================================================

    private fun buildEntityFeatures(
        state: GameState,
        entityId: EntityId,
        zone: Zone,
        identityVisible: Boolean,
    ): EntityFeatures {
        val container = state.getEntity(entityId) ?: ComponentContainer.EMPTY
        val card = container.get<CardComponent>()
        val projected = state.projectedState
        val pv = projected.getProjectedValues(entityId)

        val onBattlefield = zone == Zone.BATTLEFIELD

        // CR 708.5 — only a player entitled to look at a face-down object learns which card it
        // is, and the caller already asked the engine's [Visibility] authority who that is.
        // Projection masks a face-down permanent's characteristics down to the 2/2 with no name of
        // CR 708.2a, but the printed fields below read the card underneath directly and have no
        // projection entry to hide behind, so they are gated here. A face-down card in exile has
        // no projection entry at all — hence the gate on the base-state fallbacks too.
        val identityHidden = !identityVisible

        val types: Set<String> = when {
            pv != null -> pv.types.toSet()
            identityHidden -> emptySet()
            card != null -> card.typeLine.cardTypes.mapTo(mutableSetOf()) { it.name }
            else -> emptySet()
        }
        val subtypes: Set<String> = when {
            pv != null -> pv.subtypes.toSet()
            identityHidden -> emptySet()
            card != null -> card.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }
            else -> emptySet()
        }
        val colors: Set<String> = when {
            pv != null -> pv.colors.toSet()
            identityHidden -> emptySet()
            card != null -> card.colors.mapTo(mutableSetOf()) { it.name }
            else -> emptySet()
        }
        val keywords: Set<String> = when {
            pv != null -> pv.keywords.toSet()
            identityHidden -> emptySet()
            card != null -> card.baseKeywords.mapTo(mutableSetOf()) { it.name }
            else -> emptySet()
        }

        return EntityFeatures(
            entityId = entityId,
            cardDefinitionId = if (identityHidden) null else card?.cardDefinitionId,
            name = if (identityHidden) {
                nameVisibleToAll(state, entityId, "")
            } else {
                pv?.name ?: card?.name ?: ""
            },
            zone = zone,
            ownerId = container.get<OwnerComponent>()?.playerId ?: card?.ownerId,
            controllerId = if (onBattlefield) projected.getController(entityId) else null,
            types = types,
            subtypes = subtypes,
            colors = colors,
            keywords = keywords,
            manaCost = if (identityHidden) "" else card?.manaCost?.toString() ?: "",
            manaValue = if (identityHidden) 0 else card?.manaValue ?: 0,
            oracleText = if (identityHidden) "" else card?.oracleText ?: "",
            power = if (onBattlefield && projected.isCreature(entityId)) {
                projected.getPower(entityId)
            } else {
                null
            },
            toughness = if (onBattlefield && projected.isCreature(entityId)) {
                projected.getToughness(entityId)
            } else {
                null
            },
            tapped = onBattlefield && container.get<TappedComponent>() != null,
            // Only creatures meaningfully suffer summoning sickness — the engine attaches the
            // marker to every entering permanent so Vehicles / animated lands inherit the
            // restriction when they become creatures, but for non-creatures the marker is a
            // no-op (all {T}/attack gates are creature-conditional). Reporting it on a freshly
            // played Mountain would mislead the agent into thinking it can't tap for mana.
            summoningSick = onBattlefield
                && container.get<SummoningSicknessComponent>() != null
                && projected.isCreature(entityId)
                && !projected.hasKeyword(entityId, Keyword.HASTE),
            faceDown = container.get<FaceDownComponent>() != null,
            damageMarked = container.get<DamageComponent>()?.amount ?: 0,
            counters = container.get<CountersComponent>()?.counters
                ?.mapKeys { it.key.name } ?: emptyMap(),
            attachedTo = container.get<AttachedToComponent>()?.targetId,
            attachments = container.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        )
    }

    // =========================================================================
    // Stack
    // =========================================================================

    private fun buildStackItem(
        state: GameState,
        entityId: EntityId,
        perspectivePlayerId: EntityId,
        revealAll: Boolean,
    ): StackItemView {
        val container = state.getEntity(entityId)
        val card = container?.get<CardComponent>()
        val triggered = container?.get<TriggeredAbilityOnStackComponent>()
        val activated = container?.get<ActivatedAbilityOnStackComponent>()
        val legacyAbility = container?.get<AbilityOnStackComponent>()
        // Abilities first: an ability's entity carries no CardComponent of its own, so the card
        // branch is the general one and must not shadow the specific ones.
        val kind = when {
            triggered != null -> StackItemKind.TRIGGERED_ABILITY
            activated != null || legacyAbility != null -> StackItemKind.ACTIVATED_ABILITY
            card != null -> StackItemKind.SPELL
            else -> StackItemKind.OTHER
        }
        // An ability's entity holds only its stack component — `StackResolver` builds it from that
        // alone, with no CardComponent — so the source name and description are the only identity
        // an agent can read for it.
        val name = card?.name ?: triggered?.sourceName ?: activated?.sourceName ?: ""
        val text = card?.oracleText
            ?: triggered?.let { it.descriptionOverride ?: it.description }
            ?: activated?.descriptionOverride
            ?: ""
        // A spell cast face down is one its opponents may not read either (CR 708.4/708.5). The
        // stack is not part of `state.zones`, so there is no key to pass — the visibility authority
        // derives one from the spell's caster rather than us inventing an owner.
        val identityHidden = !revealAll && !visibility.isCardIdentityVisibleTo(
            state,
            Zone.STACK,
            entityId,
            perspectivePlayerId,
        )
        return StackItemView(
            entityId = entityId,
            // A stack object never gets a Layer-4 projection entry, so `projectedState` has no
            // controller for one. Each stack component carries its own; a spell's is its
            // ControllerComponent, else the caster — the fallback `ProjectedState` uses too.
            controllerId = triggered?.controllerId
                ?: activated?.controllerId
                ?: legacyAbility?.controllerId
                ?: container?.get<ControllerComponent>()?.playerId
                ?: container?.get<SpellOnStackComponent>()?.casterId,
            name = if (identityHidden) nameVisibleToAll(state, entityId, name) else name,
            kind = kind,
            oracleText = if (identityHidden) "" else text,
            targets = container?.get<TargetsComponent>()?.targets.orEmpty().map(::targetEntityId)
        )
    }

    /**
     * The entity a chosen target names. A player target contributes the player's own entity id —
     * players are entities here, so the flattened list stays unambiguous.
     */
    private fun targetEntityId(target: ChosenTarget): EntityId = when (target) {
        is ChosenTarget.Player -> target.playerId
        is ChosenTarget.Permanent -> target.entityId
        is ChosenTarget.Card -> target.cardId
        is ChosenTarget.Spell -> target.spellEntityId
    }

    // =========================================================================
    // Legal actions
    // =========================================================================

    private fun legalActionToView(actionId: Int, la: LegalAction): LegalActionView {
        return LegalActionView(
            actionId = actionId,
            kind = la.actionType,
            description = la.description,
            affordable = la.affordable,
            sourceEntityId = null,
            targetEntityIds = la.validTargets ?: emptyList(),
            manaCost = la.manaCostString,
            hasXCost = la.hasXCost,
            maxAffordableX = la.maxAffordableX,
            minTargets = la.minTargets,
            maxTargets = la.targetCount,
            requiresDamageDistribution = la.requiresDamageDistribution,
            isManaAbility = la.isManaAbility,
            // Combat candidates. The enumerator offers one DeclareAttackers / DeclareBlockers action
            // carrying an empty map, so without these the caller has the action but no way to know
            // what it could declare — and `ActionParams` has nothing to be built from. The two
            // blocker constraints ride along for the same reason: a declaration that exceeds a
            // blocker's limit or skips a required block is rejected, and a caller that can't see
            // them can only discover that as a 400 it had no way to predict.
            validAttackers = la.validAttackers.orEmpty(),
            mandatoryAttackers = la.mandatoryAttackers.orEmpty(),
            validAttackTargets = la.validAttackTargets.orEmpty(),
            validBlockers = la.validBlockers.orEmpty(),
            blockerMaxBlockCounts = la.blockerMaxBlockCounts.orEmpty(),
            mandatoryBlockerAssignments = la.mandatoryBlockerAssignments.orEmpty(),
            isDecisionOption = false
        )
    }

    // =========================================================================
    // Pending decisions
    // =========================================================================

    /**
     * For simple decisions (yes/no, choose-number, choose-mode, choose-color,
     * choose-option, single-select cards) we enumerate every concrete response
     * into the unified action-ID space. For complex decisions (targets,
     * distribute, order, split, search, reorder, damage, mana sources) we emit
     * [PendingDecisionView.requiresStructuredResponse] = true; the trainer
     * submits a `DecisionResponse` via a dedicated endpoint (Phase 3).
     */
    private fun buildPendingDecision(
        decision: PendingDecision
    ): Pair<PendingDecisionView, ActionRegistry> {
        val ctx = decision.context
        val baseShape = DecisionShape()

        return when (decision) {
            is YesNoDecision -> {
                val responses = listOf(
                    YesNoResponse(decision.id, true),
                    YesNoResponse(decision.id, false)
                )
                val view = baseView(decision, PendingDecisionKind.YES_NO, baseShape, structured = false)
                view to ActionRegistry.ofDecisionResponses(responses)
            }
            is BatchYesNoDecision -> {
                // Folded to two whole-run actions (yes-to-all / no-to-all); peel-off isn't an
                // observation action. Reuses the YES_NO encoding kind.
                val responses = listOf(
                    BatchYesNoResponse(decision.id, choice = true, applyToAll = true),
                    BatchYesNoResponse(decision.id, choice = false, applyToAll = true)
                )
                val view = baseView(decision, PendingDecisionKind.YES_NO, baseShape, structured = false)
                view to ActionRegistry.ofDecisionResponses(responses)
            }
            is ChooseNumberDecision -> {
                val responses = (decision.minValue..decision.maxValue).map {
                    NumberChosenResponse(decision.id, it)
                }
                val shape = DecisionShape(
                    numericMin = decision.minValue,
                    numericMax = decision.maxValue
                )
                val view = baseView(decision, PendingDecisionKind.CHOOSE_NUMBER, shape, structured = false)
                view to ActionRegistry.ofDecisionResponses(responses)
            }
            is ChooseModeDecision -> {
                // Folds only single-mode choices into IDs; multi-mode uses structured response.
                if (decision.minModes == 1 && decision.maxModes == 1) {
                    val responses = decision.modes
                        .filter { it.available }
                        .map { ModesChosenResponse(decision.id, listOf(it.index)) }
                    val shape = DecisionShape(
                        minSelections = decision.minModes,
                        maxSelections = decision.maxModes
                    )
                    val view = baseView(decision, PendingDecisionKind.CHOOSE_MODE, shape, structured = false)
                    view to ActionRegistry.ofDecisionResponses(responses)
                } else {
                    val shape = DecisionShape(
                        minSelections = decision.minModes,
                        maxSelections = decision.maxModes
                    )
                    baseView(decision, PendingDecisionKind.CHOOSE_MODE, shape, structured = true) to
                        ActionRegistry.EMPTY
                }
            }
            is ChooseColorDecision -> {
                val responses = decision.availableColors.map {
                    ColorChosenResponse(decision.id, it)
                }
                val shape = DecisionShape(availableColors = decision.availableColors)
                val view = baseView(decision, PendingDecisionKind.CHOOSE_COLOR, shape, structured = false)
                view to ActionRegistry.ofDecisionResponses(responses)
            }
            is com.wingedsheep.engine.core.PlayCardDecision ->
                baseView(decision, PendingDecisionKind.PLAY_CARD, baseShape, structured = true)
                    .copy(subjectEntityId = decision.cardId) to ActionRegistry.EMPTY
            is ChooseOptionDecision -> {
                val responses = decision.options.indices.map {
                    OptionChosenResponse(decision.id, it)
                }
                val view = baseView(decision, PendingDecisionKind.CHOOSE_OPTION, baseShape, structured = false)
                view to ActionRegistry.ofDecisionResponses(responses)
            }
            is ChooseReplacementDecision ->
                // Two-index (from, to) pick — emitted as a structured decision (trainer submits the
                // DecisionResponse directly rather than via the flat action-ID space).
                baseView(decision, PendingDecisionKind.CHOOSE_REPLACEMENT, baseShape, structured = true) to
                    ActionRegistry.EMPTY
            is SelectCardsDecision -> {
                if (decision.minSelections == 1 && decision.maxSelections == 1 && !decision.ordered) {
                    val responses = decision.options.map {
                        CardsSelectedResponse(decision.id, listOf(it))
                    }
                    val shape = DecisionShape(
                        minSelections = decision.minSelections,
                        maxSelections = decision.maxSelections
                    )
                    val view = baseView(decision, PendingDecisionKind.SELECT_CARDS, shape, structured = false)
                    view to ActionRegistry.ofDecisionResponses(responses)
                } else {
                    val shape = DecisionShape(
                        minSelections = decision.minSelections,
                        maxSelections = decision.maxSelections
                    )
                    baseView(decision, PendingDecisionKind.SELECT_CARDS, shape, structured = true) to
                        ActionRegistry.EMPTY
                }
            }
            is BudgetModalDecision -> {
                val shape = DecisionShape(budget = decision.budget)
                baseView(decision, PendingDecisionKind.BUDGET_MODAL, shape, structured = true) to
                    ActionRegistry.EMPTY
            }
            is ChooseTargetsDecision ->
                baseView(decision, PendingDecisionKind.CHOOSE_TARGETS, baseShape, structured = true) to
                    ActionRegistry.EMPTY
            is DistributeDecision -> {
                val shape = DecisionShape(totalToDistribute = decision.totalAmount)
                baseView(decision, PendingDecisionKind.DISTRIBUTE, shape, structured = true) to
                    ActionRegistry.EMPTY
            }
            is OrderObjectsDecision ->
                baseView(decision, PendingDecisionKind.ORDER_OBJECTS, baseShape, structured = true) to
                    ActionRegistry.EMPTY
            is SplitPilesDecision ->
                baseView(decision, PendingDecisionKind.SPLIT_PILES, baseShape, structured = true) to
                    ActionRegistry.EMPTY
            is SearchLibraryDecision -> {
                val shape = DecisionShape(
                    minSelections = decision.minSelections,
                    maxSelections = decision.maxSelections
                )
                baseView(decision, PendingDecisionKind.SEARCH_LIBRARY, shape, structured = true) to
                    ActionRegistry.EMPTY
            }
            is ReorderLibraryDecision ->
                baseView(decision, PendingDecisionKind.REORDER_LIBRARY, baseShape, structured = true) to
                    ActionRegistry.EMPTY
            is AssignDamageDecision ->
                baseView(decision, PendingDecisionKind.ASSIGN_DAMAGE, baseShape, structured = true) to
                    ActionRegistry.EMPTY
            is CombatResolutionDecision ->
                baseView(decision, PendingDecisionKind.COMBAT_RESOLUTION, baseShape, structured = true) to
                    ActionRegistry.EMPTY
            is SelectManaSourcesDecision ->
                baseView(decision, PendingDecisionKind.SELECT_MANA_SOURCES, baseShape, structured = true) to
                    ActionRegistry.EMPTY
        }
    }

    private fun baseView(
        decision: PendingDecision,
        kind: PendingDecisionKind,
        shape: DecisionShape,
        structured: Boolean
    ): PendingDecisionView {
        val ctx = decision.context
        return PendingDecisionView(
            decisionId = decision.id,
            kind = kind,
            playerId = decision.playerId,
            prompt = decision.prompt,
            sourceEntityId = ctx.sourceId,
            sourceName = ctx.sourceName,
            triggeringEntityId = ctx.triggeringEntityId,
            effectHint = ctx.effectHint,
            requiresStructuredResponse = structured,
            shape = shape
        )
    }

    private fun buildDecisionOptionViews(
        decision: PendingDecision,
        responses: List<DecisionResponse>
    ): List<LegalActionView> {
        return responses.mapIndexed { idx, response ->
            LegalActionView(
                actionId = idx,
                kind = "DECISION",
                description = describeResponse(decision, response),
                affordable = true,
                isDecisionOption = true
            )
        }
    }

    private fun describeResponse(decision: PendingDecision, response: DecisionResponse): String = when (response) {
        is YesNoResponse -> if (response.choice) (decision as? YesNoDecision)?.yesText ?: "Yes" else
            (decision as? YesNoDecision)?.noText ?: "No"
        is NumberChosenResponse -> response.number.toString()
        is ModesChosenResponse -> response.selectedModes.joinToString(",") { idx ->
            (decision as? ChooseModeDecision)?.modes?.getOrNull(idx)?.text ?: idx.toString()
        }
        is ColorChosenResponse -> response.color.name
        is OptionChosenResponse ->
            (decision as? ChooseOptionDecision)?.options?.getOrNull(response.optionIndex)
                ?: response.optionIndex.toString()
        is CardsSelectedResponse -> response.selectedCards.joinToString(",") { it.value }
        else -> response.toString()
    }
}

/**
 * Build output pairing an [Observation] with its server-side [ActionRegistry].
 * The observation is safe to serialize; the registry must be retained on the
 * server so it can resolve incoming action IDs. Both game envs ([TrainingObservation])
 * and deckbuild envs ([DeckbuildObservation]) produce this shape.
 */
data class ObservationResult(
    val observation: Observation,
    val registry: ActionRegistry
)
