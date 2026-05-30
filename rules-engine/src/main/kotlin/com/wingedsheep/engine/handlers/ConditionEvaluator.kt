package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.handlers.ConditionEvaluationContext.Projection
import com.wingedsheep.engine.handlers.ConditionEvaluationContext.Resolution
import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredViaAbilityComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.CastRecordComponent
import com.wingedsheep.engine.state.components.battlefield.WasKickedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.WasDealtCombatDamageThisTurnComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype
import com.wingedsheep.sdk.scripting.conditions.YouControlMostOfChosenType
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureIsLegendary
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.IsInPhase
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.OpponentSpellOnStack
import com.wingedsheep.sdk.scripting.conditions.SourceCastForImpending
import com.wingedsheep.sdk.scripting.conditions.SourceIsModified
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.conditions.SourceMatches
import com.wingedsheep.engine.state.components.battlefield.CastForImpendingComponent
import com.wingedsheep.engine.state.components.identity.ChosenModeComponent
import com.wingedsheep.sdk.scripting.conditions.WasCast
import com.wingedsheep.sdk.scripting.conditions.WasCastFromHand
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone
import com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent
import com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentHadSubtype
import com.wingedsheep.sdk.scripting.conditions.AnotherPermanentWithSameNameAsTarget
import com.wingedsheep.sdk.scripting.conditions.TargetMatchesFilter
import com.wingedsheep.sdk.scripting.conditions.TargetSharesMostCommonColor
import com.wingedsheep.sdk.scripting.conditions.ColorIsMostCommon
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityEnteredOrWasCastFromGraveyard
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadMinusOneMinusOneCounter
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasHistoric
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasNotPutByThisSource
import com.wingedsheep.sdk.scripting.conditions.TriggeringSpellHasSingleTarget
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.conditions.IsFirstSpellOfTypeCastThisTurn
import com.wingedsheep.sdk.scripting.conditions.IsFirstSpellPaidWithTreasureManaCastThisTurn
import com.wingedsheep.sdk.scripting.conditions.SourceAbilityResolvedNTimesThisTurn
import com.wingedsheep.sdk.scripting.conditions.ManaSpentToCastIncludes
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.conditions.BlightWasPaid
import com.wingedsheep.sdk.scripting.conditions.SourceIsRingBearer
import com.wingedsheep.sdk.scripting.conditions.YouControlSource
import com.wingedsheep.sdk.scripting.conditions.PlayerAttackedWithCreaturesThisTurn
import com.wingedsheep.sdk.scripting.conditions.PermanentTypeEnteredBattlefieldThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerCastSpellsThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerHasCitysBlessing
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn
import com.wingedsheep.engine.handlers.triggers.CreatureDiedThisTurnConditionEvaluator
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.conditions.VoidCondition
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent

/**
 * Evaluates conditions from the SDK against the game state.
 */
class ConditionEvaluator {

    private val dynamicAmountEvaluator = DynamicAmountEvaluator(this)

    /**
     * Evaluate a condition at resolution time, when a full [EffectContext] is available.
     * Thin wrapper over the dual-mode [evaluate] below.
     */
    fun evaluate(
        state: GameState,
        condition: Condition,
        context: EffectContext
    ): Boolean = evaluate(state, condition, Resolution(context))

    /**
     * Evaluate a condition in either resolution or projection mode.
     *
     * Conditions that need resolution-time facts (targets, triggering entity, cast-from
     * zone, kicker state, etc.) return `false` in projection mode — a static-ability gate
     * never sees those facts.
     */
    internal fun evaluate(
        state: GameState,
        condition: Condition,
        ctx: ConditionEvaluationContext
    ): Boolean {
        // Helper: dispatch a resolution-only branch, returning false when running under projection.
        fun ifResolution(fn: (EffectContext) -> Boolean): Boolean =
            (ctx as? Resolution)?.let { fn(it.effectContext) } ?: false

        return when (condition) {
            // ============================================================
            // Dual-mode primitives (work in both resolution and projection)
            // ============================================================
            is Compare -> evaluateCompareCtx(state, condition, ctx)
            is Exists -> evaluateExistsCtx(state, condition, ctx)

            is IsYourTurn -> ctx.controllerId?.let { state.activePlayerId == it } ?: false
            is IsNotYourTurn -> ctx.controllerId?.let { state.activePlayerId != it } ?: false

            is SourcePlottedOnPriorTurn -> {
                val sourceId = ctx.sourceId
                val plotted = sourceId?.let { state.getEntity(it)?.get<PlottedComponent>() }
                plotted != null && plotted.turnPlotted < state.turnNumber
            }

            is SourceCastForImpending -> {
                val sourceId = ctx.sourceId
                sourceId != null && state.getEntity(sourceId)?.has<CastForImpendingComponent>() == true
            }

            // Generic source-state primitive — predicate-evaluator against the source entity.
            is SourceMatches -> evaluateSourceMatchesCtx(state, condition, ctx)

            // CR 701.52e: the source is your Ring-bearer — it carries your Ring-bearer designation
            // and you still control it. The control half reads the projected controller so a
            // control-changing effect correctly ends the designation.
            is SourceIsRingBearer -> {
                val sourceId = ctx.sourceId
                val controllerId = ctx.controllerId
                val bearer = sourceId?.let { state.getEntity(it)?.get<RingBearerComponent>() }
                bearer != null && controllerId != null &&
                    bearer.ownerId == controllerId &&
                    state.projectedState.getController(sourceId) == controllerId
            }

            // Aura-controller-aware modified check (CR 700.4) — distinct enough from the
            // generic StatePredicate.IsModified to warrant its own branch.
            is SourceIsModified -> evaluateSourceIsModifiedCtx(state, ctx)

            is EnchantedCreatureHasSubtype -> evaluateEnchantedCreatureHasSubtypeCtx(state, condition, ctx)
            is EnchantedCreatureIsLegendary -> evaluateEnchantedCreatureIsLegendaryCtx(state, ctx)

            // Player-relative trackers (resolve [Player] against the current context).
            is PlayerAttackedWithCreaturesThisTurn -> evaluateAttackedWithCreaturesCtx(state, condition, ctx)
            is PlayerCastSpellsThisTurn -> evaluateCastSpellsThisTurnCtx(state, condition, ctx)
            is PlayerHasCitysBlessing -> evaluateHasCitysBlessingCtx(state, condition, ctx)
            is PermanentTypeEnteredBattlefieldThisTurn ->
                evaluatePermanentTypeEnteredBattlefieldThisTurnCtx(state, condition, ctx)

            // Global facts (no controller/source needed).
            is VoidCondition ->
                state.nonlandPermanentLeftBattlefieldThisTurn || state.spellWarpedThisTurn

            // Board-derived only — no targets/triggering/kicker — so it works identically in
            // resolution and projection (required for the djinn `ConditionalStaticAbility` gate).
            is ColorIsMostCommon ->
                condition.color.name in mostCommonColors(state, ctx.projectedStateFor(state))

            // Composites recurse with the same ctx.
            is AllConditions -> condition.conditions.all { evaluate(state, it, ctx) }
            is AnyCondition -> condition.conditions.any { evaluate(state, it, ctx) }
            is NotCondition -> !evaluate(state, condition.condition, ctx)

            // ============================================================
            // Resolution-only conditions — false under projection.
            // ============================================================
            is APlayerControlsMostOfSubtype -> ifResolution { evaluateAPlayerControlsMostOfSubtype(state, condition) }
            is YouControlMostOfChosenType -> ifResolution { evaluateYouControlMostOfChosenType(state, condition, it) }
            is YouControlSource -> ifResolution { evaluateYouControlSource(state, it) }
            is WasCast -> ifResolution { evaluateWasCast(state, it) }
            is WasCastFromHand -> ifResolution { evaluateWasCastFromHand(state, it) }
            is WasCastFromZone -> ifResolution { evaluateWasCastFromZone(state, condition, it) }
            is WasKicked -> ifResolution { evaluateWasKicked(state, it) }
            is BlightWasPaid -> ifResolution { it.wasBlightPaid }
            is ManaSpentToCastIncludes -> ifResolution { evaluateManaSpentToCastIncludes(state, condition, it) }
            is SourceChosenModeIs -> ifResolution {
                val sourceId = it.sourceId
                sourceId != null &&
                    state.getEntity(sourceId)?.get<ChosenModeComponent>()?.modeId == condition.modeId
            }
            is SacrificedPermanentHadSubtype -> ifResolution { evaluateSacrificedPermanentHadSubtype(condition, it) }
            is TriggeringEntityWasHistoric -> ifResolution { evaluateTriggeringEntityWasHistoric(state, it) }
            is TriggeringEntityEnteredOrWasCastFromGraveyard ->
                ifResolution { evaluateTriggeringEntityEnteredOrWasCastFromGraveyard(state, it) }
            is TriggeringEntityHadMinusOneMinusOneCounter ->
                ifResolution { (it.triggerMinusOneMinusOneCounterCount ?: 0) > 0 }
            is TriggeringEntityWasNotPutByThisSource ->
                ifResolution { evaluateTriggeringEntityWasNotPutByThisSource(state, it) }
            is TriggeringSpellHasSingleTarget -> ifResolution { evaluateTriggeringSpellHasSingleTarget(state, it) }
            is TargetMatchesFilter -> ifResolution { evaluateTargetMatchesFilter(state, condition, it) }
            is TargetSharesMostCommonColor -> ifResolution { evaluateTargetSharesMostCommonColor(state, condition, it) }
            is AnotherPermanentWithSameNameAsTarget ->
                ifResolution { evaluateAnotherPermanentWithSameNameAsTarget(state, condition, it) }
            is IsInPhase -> ifResolution { evaluateIsInPhase(state, condition, it) }
            is YouWereAttackedThisStep -> ifResolution { evaluateYouWereAttackedThisStep(state, it) }
            is IsFirstSpellOfTypeCastThisTurn -> ifResolution { evaluateFirstSpellOfType(state, condition, it) }
            is IsFirstSpellPaidWithTreasureManaCastThisTurn ->
                ifResolution { evaluateFirstSpellPaidWithTreasureMana(state, it) }
            is SourceAbilityResolvedNTimesThisTurn -> ifResolution { evaluateSourceAbilityResolvedNTimes(state, condition, it) }
            is OpponentSpellOnStack -> ifResolution { evaluateOpponentSpellOnStack(state, it) }
            is CollectionContainsMatch -> ifResolution { evaluateCollectionContainsMatch(state, condition, it) }
            is CreatureDiedThisTurnCondition ->
                ifResolution { CreatureDiedThisTurnConditionEvaluator().evaluate(state, condition, it) }
        }
    }

    // ================================================================================
    // Dual-mode evaluators (resolve via [ConditionEvaluationContext]).
    // ================================================================================

    private fun syntheticEffectContext(state: GameState, ctx: ConditionEvaluationContext): EffectContext? {
        val controllerId = ctx.controllerId ?: return null
        return EffectContext(
            sourceId = ctx.sourceId,
            controllerId = controllerId,
            opponentId = state.getOpponent(controllerId)
        )
    }

    private fun evaluateCompareCtx(
        state: GameState,
        condition: Compare,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val effectCtx = when (ctx) {
            is Resolution -> ctx.effectContext
            is Projection -> syntheticEffectContext(state, ctx) ?: return false
        }
        val left = dynamicAmountEvaluator.evaluate(state, condition.left, effectCtx)
        val right = dynamicAmountEvaluator.evaluate(state, condition.right, effectCtx)
        return when (condition.operator) {
            ComparisonOperator.LT -> left < right
            ComparisonOperator.LTE -> left <= right
            ComparisonOperator.EQ -> left == right
            ComparisonOperator.NEQ -> left != right
            ComparisonOperator.GT -> left > right
            ComparisonOperator.GTE -> left >= right
        }
    }

    private fun evaluateSourceMatchesCtx(
        state: GameState,
        condition: SourceMatches,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val sourceId = ctx.sourceId ?: return false
        val projected = ctx.projectedStateFor(state)
        val predicateContext = when (ctx) {
            is Resolution -> PredicateContext.fromEffectContext(ctx.effectContext)
            is Projection -> ctx.controllerId?.let { PredicateContext(controllerId = it) }
                ?: PredicateContext(controllerId = sourceId)
        }
        return PredicateEvaluator().matches(state, projected, sourceId, condition.filter, predicateContext)
    }

    private fun evaluateExistsCtx(
        state: GameState,
        condition: Exists,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val predicateEvaluator = PredicateEvaluator()
        val controllerId = ctx.controllerId
        val projected = ctx.projectedStateFor(state)
        val predicateContext = when (ctx) {
            is Resolution -> PredicateContext.fromEffectContext(ctx.effectContext)
            is Projection -> controllerId?.let { PredicateContext(controllerId = it) } ?: return condition.negate
        }

        val playerIds: List<EntityId> = when (condition.player) {
            is Player.You -> controllerId?.let { listOf(it) } ?: emptyList()
            is Player.Opponent -> controllerId?.let { c -> state.turnOrder.filter { it != c } } ?: emptyList()
            is Player.EachOpponent -> controllerId?.let { c -> state.turnOrder.filter { it != c } } ?: emptyList()
            is Player.Each -> state.turnOrder
            is Player.Any -> state.turnOrder
            else -> controllerId?.let { listOf(it) } ?: emptyList()
        }

        val found = playerIds.any { playerId ->
            var entities = if (condition.zone == Zone.BATTLEFIELD) {
                state.getBattlefield().filter { entityId -> projected.getController(entityId) == playerId }
            } else {
                state.getZone(ZoneKey(playerId, condition.zone))
            }
            if (condition.excludeSelf) {
                entities = entities.filter { it != ctx.sourceId }
            }
            if (condition.filter == GameObjectFilter.Any) {
                entities.isNotEmpty()
            } else {
                entities.any { entityId ->
                    predicateEvaluator.matches(state, projected, entityId, condition.filter, predicateContext)
                }
            }
        }
        return if (condition.negate) !found else found
    }

    private fun evaluateSourceIsModifiedCtx(state: GameState, ctx: ConditionEvaluationContext): Boolean {
        val sourceId = ctx.sourceId ?: return false
        val entity = state.getEntity(sourceId) ?: return false
        val controllerId = ctx.controllerId

        val counters = entity.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
        if (counters != null && counters.counters.values.any { it > 0 }) return true

        for (permanentId in state.getBattlefield()) {
            if (permanentId == sourceId) continue
            val container = state.getEntity(permanentId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()?.targetId ?: continue
            if (attachedTo != sourceId) continue
            val card = container.get<CardComponent>() ?: continue

            if (card.typeLine.hasSubtype(Subtype.EQUIPMENT)) return true

            if (card.typeLine.hasSubtype(Subtype.AURA)) {
                val auraController = when (ctx) {
                    is Resolution -> container.get<ControllerComponent>()?.playerId
                    is Projection -> ctx.projectedValues[permanentId]?.controllerId
                        ?: container.get<ControllerComponent>()?.playerId
                }
                if (auraController == controllerId) return true
            }
        }
        return false
    }

    private fun evaluateEnchantedCreatureHasSubtypeCtx(
        state: GameState,
        condition: EnchantedCreatureHasSubtype,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val sourceId = ctx.sourceId ?: return false
        val attached = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
        val creatureId = attached ?: when (ctx) {
            is Resolution -> sourceId  // resolution-mode fallback (granted-ability scope)
            is Projection -> return false
        }
        return ctx.projectedStateFor(state).hasSubtype(creatureId, condition.subtype.value)
    }

    private fun evaluateEnchantedCreatureIsLegendaryCtx(
        state: GameState,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val sourceId = ctx.sourceId ?: return false
        val attached = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
        val creatureId = attached ?: when (ctx) {
            is Resolution -> sourceId
            is Projection -> return false
        }
        return "LEGENDARY" in ctx.projectedStateFor(state).getTypes(creatureId)
    }

    /**
     * Resolve a [Player] reference against the current evaluation context.
     *
     * - [Player.You]: controller-of-source (resolution: effectContext.controllerId;
     *   projection: source's projected controller).
     * - [Player.Opponent]: any opponent of the controller. Returns the first opponent
     *   in turn order — sufficient for per-player tracker reads where each player has
     *   its own bucket.
     * - [Player.TriggeringPlayer]: only resolvable in resolution mode.
     * - Other relational forms are not currently supported by the player-relative
     *   conditions and return `null`.
     */
    private fun resolvePlayer(state: GameState, player: Player, ctx: ConditionEvaluationContext): EntityId? {
        return when (player) {
            is Player.You -> ctx.controllerId
                ?: ctx.sourceId?.let { state.getEntity(it)?.get<ControllerComponent>()?.playerId }
            is Player.Opponent -> {
                val c = ctx.controllerId ?: return null
                state.turnOrder.firstOrNull { it != c }
            }
            is Player.TriggeringPlayer -> (ctx as? Resolution)?.effectContext?.triggeringPlayerId
            else -> null
        }
    }

    private fun evaluateAttackedWithCreaturesCtx(
        state: GameState,
        condition: PlayerAttackedWithCreaturesThisTurn,
        ctx: ConditionEvaluationContext
    ): Boolean {
        if (condition.atLeast <= 0) return true
        val playerId = resolvePlayer(state, condition.player, ctx) ?: return false
        val attackerIds = state.getEntity(playerId)
            ?.get<PlayerAttackersThisTurnComponent>()
            ?.attackerIds
            ?: emptySet()
        if (attackerIds.size < condition.atLeast) return false
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = when (ctx) {
            is Resolution -> PredicateContext.fromEffectContext(ctx.effectContext)
            is Projection -> PredicateContext(controllerId = playerId)
        }
        val projected = ctx.projectedStateFor(state)
        var matches = 0
        for (id in attackerIds) {
            if (predicateEvaluator.matches(state, projected, id, condition.filter, predicateContext)) {
                matches++
                if (matches >= condition.atLeast) return true
            }
        }
        return false
    }

    private fun evaluateCastSpellsThisTurnCtx(
        state: GameState,
        condition: PlayerCastSpellsThisTurn,
        ctx: ConditionEvaluationContext
    ): Boolean {
        if (condition.atLeast <= 0) return true
        val playerId = resolvePlayer(state, condition.player, ctx) ?: return false
        val records = state.spellsCastThisTurnByPlayer[playerId] ?: return false
        if (records.size < condition.atLeast) return false
        if (condition.filter == GameObjectFilter.Any) return true
        val evaluator = PredicateEvaluator()
        var matches = 0
        for (record in records) {
            if (evaluator.matchesFilter(record, condition.filter)) {
                matches++
                if (matches >= condition.atLeast) return true
            }
        }
        return false
    }

    private fun evaluateHasCitysBlessingCtx(
        state: GameState,
        condition: PlayerHasCitysBlessing,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val playerId = resolvePlayer(state, condition.player, ctx) ?: return false
        return state.getEntity(playerId)?.has<PlayerCitysBlessingComponent>() == true
    }

    private fun evaluatePermanentTypeEnteredBattlefieldThisTurnCtx(
        state: GameState,
        condition: PermanentTypeEnteredBattlefieldThisTurn,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val playerId = resolvePlayer(state, condition.player, ctx) ?: return false
        val tracker = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.PermanentTypesEnteredBattlefieldThisTurnComponent>()
            ?: return false
        return condition.cardType in tracker.cardTypes
    }

    private fun evaluateYouControlSource(state: GameState, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        val sourceController = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId ?: return false
        return context.controllerId == sourceController
    }

    private fun evaluateWasCast(state: GameState, context: EffectContext): Boolean {
        // For spells resolving, any non-null castFromZone means the spell is being cast
        if (context.castFromZone != null) return true
        // For permanents (e.g., ETB triggers), check the cast-from-zone markers added
        // when the spell resolved. Reanimated, token, and "put onto battlefield"
        // permanents lack these markers and are correctly excluded.
        val sourceId = context.sourceId ?: return false
        val container = state.getEntity(sourceId) ?: return false
        return container.has<CastFromHandComponent>() || container.has<CastFromGraveyardComponent>()
    }

    private fun evaluateWasCastFromHand(state: GameState, context: EffectContext): Boolean {
        // For spells resolving, check context.castFromZone (set from SpellOnStackComponent)
        if (context.castFromZone == Zone.HAND) return true
        // For permanents (triggered abilities), fall back to battlefield component
        val sourceId = context.sourceId ?: return false
        return state.getEntity(sourceId)?.has<CastFromHandComponent>() == true
    }

    private fun evaluateWasCastFromZone(state: GameState, condition: WasCastFromZone, context: EffectContext): Boolean {
        // For spells resolving, check context.castFromZone (set from SpellOnStackComponent)
        if (context.castFromZone == condition.zone) return true
        // For permanents (triggered abilities), fall back to battlefield components
        val sourceId = context.sourceId ?: return false
        if (condition.zone == Zone.HAND) {
            return state.getEntity(sourceId)?.has<CastFromHandComponent>() == true
        }
        return false
    }

    private fun evaluateWasKicked(state: GameState, context: EffectContext): Boolean {
        // Check the component on the permanent first (for triggered abilities)
        val sourceId = context.sourceId ?: return context.wasKicked
        if (state.getEntity(sourceId)?.has<WasKickedComponent>() == true) return true
        // Fall back to context (for spell resolution, e.g. kicker additional effects)
        return context.wasKicked
    }

    private fun evaluateManaSpentToCastIncludes(
        state: GameState,
        condition: ManaSpentToCastIncludes,
        context: EffectContext
    ): Boolean {
        val sourceId = context.sourceId ?: return false
        val record = state.getEntity(sourceId)?.get<CastRecordComponent>() ?: return false
        return record.whiteSpent >= condition.requiredWhite &&
            record.blueSpent >= condition.requiredBlue &&
            record.blackSpent >= condition.requiredBlack &&
            record.redSpent >= condition.requiredRed &&
            record.greenSpent >= condition.requiredGreen
    }

    private fun evaluateIsInPhase(state: GameState, condition: IsInPhase, context: EffectContext): Boolean {
        if (condition.yoursOnly && state.activePlayerId != context.controllerId) return false
        return state.phase in condition.phases
    }

    private fun evaluateSacrificedPermanentHadSubtype(
        condition: SacrificedPermanentHadSubtype,
        context: EffectContext
    ): Boolean {
        return context.sacrificedPermanents.any { snapshot ->
            snapshot.subtypes.contains(condition.subtype)
        }
    }

    private fun evaluateYouWereAttackedThisStep(state: GameState, context: EffectContext): Boolean {
        return state.entities.any { (_, container) ->
            val attacking = container.get<AttackingComponent>()
            attacking != null && attacking.defenderId == context.controllerId
        }
    }

    private fun evaluateOpponentSpellOnStack(state: GameState, context: EffectContext): Boolean {
        val opponentId = context.opponentId ?: return false
        return state.stack.any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()?.casterId == opponentId
        }
    }

    private fun evaluateAPlayerControlsMostOfSubtype(
        state: GameState,
        condition: APlayerControlsMostOfSubtype
    ): Boolean {
        val counts = state.turnOrder.associateWith { playerId ->
            state.entities.count { (_, container) ->
                container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.typeLine?.hasSubtype(condition.subtype) == true
            }
        }
        val maxCount = counts.values.maxOrNull() ?: return false
        if (maxCount == 0) return false
        val playersWithMax = counts.count { it.value == maxCount }
        return playersWithMax == 1
    }

    private fun evaluateYouControlMostOfChosenType(
        state: GameState,
        condition: YouControlMostOfChosenType,
        context: EffectContext
    ): Boolean {
        val chosenType = context.pipeline.chosenValues[condition.chosenValueKey] ?: return false
        val projected = state.projectedState
        val controllerId = context.controllerId

        val counts = state.turnOrder.associateWith { playerId ->
            state.getBattlefield().count { entityId ->
                val controller = projected.getController(entityId)
                controller == playerId && projected.hasSubtype(entityId, chosenType) &&
                    projected.isCreature(entityId)
            }
        }

        val controllerCount = counts[controllerId] ?: 0
        if (controllerCount == 0) return false

        return counts.filter { it.key != controllerId }.all { controllerCount > it.value }
    }

    private fun evaluateTriggeringEntityWasHistoric(state: GameState, context: EffectContext): Boolean {
        val entityId = context.triggeringEntityId ?: return false
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return false
        return card.typeLine.isHistoric
    }

    private fun evaluateTriggeringEntityEnteredOrWasCastFromGraveyard(
        state: GameState,
        context: EffectContext
    ): Boolean {
        val entityId = context.triggeringEntityId ?: return false
        val entity = state.getEntity(entityId) ?: return false
        return entity.has<com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent>() ||
            entity.has<com.wingedsheep.engine.state.components.battlefield.EnteredFromGraveyardComponent>()
    }

    private fun evaluateTriggeringEntityWasNotPutByThisSource(
        state: GameState,
        context: EffectContext
    ): Boolean {
        val triggeringId = context.triggeringEntityId ?: return true
        val sourceId = context.sourceId ?: return true
        val marker = state.getEntity(triggeringId)?.get<EnteredViaAbilityComponent>()
        return marker?.sourceId != sourceId
    }

    private fun evaluateTriggeringSpellHasSingleTarget(
        state: GameState,
        context: EffectContext
    ): Boolean {
        val entityId = context.triggeringEntityId ?: return false
        val targets = state.getEntity(entityId)
            ?.get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()
            ?.targets
            ?: return false
        return targets.size == 1
    }

    private fun evaluateTargetMatchesFilter(
        state: GameState,
        condition: TargetMatchesFilter,
        context: EffectContext
    ): Boolean {
        val target = context.targets.getOrNull(condition.targetIndex) ?: return false
        val entityId = when (target) {
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> return false
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> target.cardId
        }
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projected = state.projectedState
        return predicateEvaluator.matches(state, projected, entityId, condition.filter, predicateContext)
    }

    /**
     * CR-style "most common color among all permanents": tally each of the five colors across
     * every battlefield permanent (multicolored permanents count once per color), find the
     * highest tally, and check whether the targeted permanent shares any color tied for that
     * highest tally. Uses projected colors so color-changing continuous effects are respected.
     */
    private fun evaluateTargetSharesMostCommonColor(
        state: GameState,
        condition: TargetSharesMostCommonColor,
        context: EffectContext
    ): Boolean {
        val target = context.targets.getOrNull(condition.targetIndex) ?: return false
        val entityId = (target as? com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent)
            ?.entityId ?: return false
        val projected = state.projectedState
        return projected.getColors(entityId).any { it in mostCommonColors(state, projected) }
    }

    /**
     * "If another permanent with the same name as the target is on the battlefield": resolve the
     * target permanent, read its card name, and return true when at least one *other* battlefield
     * permanent shares that exact name. The target itself is excluded so a single copy never
     * satisfies its own check.
     *
     * A face-down permanent has no name (CR 708.2), so it neither matches anything (as the target)
     * nor counts as a same-named permanent (as a candidate) — face-down entities are skipped on
     * both sides even though they retain a hidden [CardComponent.name].
     */
    private fun evaluateAnotherPermanentWithSameNameAsTarget(
        state: GameState,
        condition: AnotherPermanentWithSameNameAsTarget,
        context: EffectContext
    ): Boolean {
        val target = context.targets.getOrNull(condition.targetIndex) ?: return false
        val entityId = (target as? com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent)
            ?.entityId ?: return false
        val targetEntity = state.getEntity(entityId) ?: return false
        if (targetEntity.has<FaceDownComponent>()) return false
        val targetName = targetEntity.get<CardComponent>()?.name ?: return false
        return state.getBattlefield().any { otherId ->
            if (otherId == entityId) return@any false
            val other = state.getEntity(otherId) ?: return@any false
            !other.has<FaceDownComponent>() && other.get<CardComponent>()?.name == targetName
        }
    }

    /**
     * The set of colors tied for most common among all battlefield permanents. Colors are the
     * projected-state color strings (`Color.name`); a multicolored permanent contributes to each
     * of its colors. Empty when no permanent has a color.
     */
    private fun mostCommonColors(state: GameState, projected: ProjectedState): Set<String> {
        val colorCounts = mutableMapOf<String, Int>()
        for (permanentId in state.getBattlefield()) {
            for (color in projected.getColors(permanentId)) {
                colorCounts[color] = (colorCounts[color] ?: 0) + 1
            }
        }
        val maxCount = colorCounts.values.maxOrNull() ?: return emptySet()
        if (maxCount == 0) return emptySet()
        return colorCounts.filterValues { it == maxCount }.keys
    }

    private fun evaluateFirstSpellOfType(
        state: GameState,
        condition: IsFirstSpellOfTypeCastThisTurn,
        context: EffectContext
    ): Boolean {
        // The triggering spell itself must match the filter — otherwise casting an artifact
        // when one instant was already cast this turn would incorrectly match the Instant filter.
        val triggeringId = context.triggeringEntityId ?: return false
        val entity = state.getEntity(triggeringId) ?: return false
        val card = entity.get<CardComponent>() ?: return false
        val triggeringRecord = CastSpellRecord(
            typeLine = card.typeLine,
            manaValue = card.manaValue,
            colors = card.colors,
            isFaceDown = entity.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()
        )
        val evaluator = PredicateEvaluator()
        if (!evaluator.matchesFilter(triggeringRecord, condition.spellFilter)) return false

        val records = state.spellsCastThisTurnByPlayer[context.controllerId] ?: return false
        val count = records.count { evaluator.matchesFilter(it, condition.spellFilter) }
        return count == 1
    }

    private fun evaluateFirstSpellPaidWithTreasureMana(
        state: GameState,
        context: EffectContext
    ): Boolean {
        val records = state.spellsCastThisTurnByPlayer[context.controllerId] ?: return false
        return records.count { it.paidWithTreasureMana } == 1 &&
            records.lastOrNull()?.paidWithTreasureMana == true
    }

    private fun evaluateSourceAbilityResolvedNTimes(
        state: GameState,
        condition: SourceAbilityResolvedNTimesThisTurn,
        context: EffectContext
    ): Boolean {
        val sourceId = context.sourceId ?: return false
        val component = state.getEntity(sourceId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.AbilityResolutionCountThisTurnComponent>()
            ?: return false
        return component.count == condition.count
    }

    private fun evaluateCollectionContainsMatch(
        state: GameState,
        condition: CollectionContainsMatch,
        context: EffectContext
    ): Boolean {
        val collection = context.pipeline.storedCollections[condition.collection] ?: return false
        if (collection.isEmpty()) return false
        if (condition.filter == GameObjectFilter.Any) return true
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext.fromEffectContext(context)
        return collection.any { entityId ->
            predicateEvaluator.matches(state, state.projectedState, entityId, condition.filter, predicateContext)
        }
    }
}
