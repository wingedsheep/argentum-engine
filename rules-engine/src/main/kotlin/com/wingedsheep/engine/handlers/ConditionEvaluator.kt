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
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.CastRecordComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.engine.state.components.battlefield.chosenLandType
import com.wingedsheep.engine.state.components.battlefield.chosenModeId
import com.wingedsheep.engine.state.components.battlefield.wasKickedChoice
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
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
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
import com.wingedsheep.sdk.scripting.conditions.APlayerLifeAtMost
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureIsLegendary
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.IsInPhase
import com.wingedsheep.sdk.scripting.conditions.IsInStep
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.OpponentSpellOnStack
import com.wingedsheep.sdk.scripting.conditions.ControllerTurnsTakenAtMost
import com.wingedsheep.sdk.scripting.conditions.SourceCastForImpending
import com.wingedsheep.sdk.scripting.conditions.SourceIsModified
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade
import com.wingedsheep.sdk.scripting.conditions.CastChoiceIs
import com.wingedsheep.sdk.scripting.conditions.CastTimeFlagSet
import com.wingedsheep.engine.state.components.battlefield.CastForImpendingComponent
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.conditions.WasCast
import com.wingedsheep.sdk.scripting.conditions.WasCastFromHand
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone
import com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent
import com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentHadSubtype
import com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentWasLegendary
import com.wingedsheep.sdk.scripting.conditions.YouSacrificedPermanentThisWay
import com.wingedsheep.sdk.scripting.conditions.AnotherPermanentWithSameNameAsTarget
import com.wingedsheep.sdk.scripting.conditions.TargetMarkedDamageExceedsToughness
import com.wingedsheep.sdk.scripting.conditions.TargetIsPlayer
import com.wingedsheep.sdk.scripting.conditions.TargetSharesMostCommonColor
import com.wingedsheep.sdk.scripting.conditions.ColorIsMostCommon
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityEnteredOrWasCastFromGraveyard
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadCounters
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadMinusOneMinusOneCounter
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasHistoric
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasCast
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasNotPutByThisSource
import com.wingedsheep.sdk.scripting.conditions.TriggeringSpellHasSingleTarget
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.conditions.IsFirstSpellPaidWithTreasureManaCastThisTurn
import com.wingedsheep.sdk.scripting.conditions.SourceAbilityResolvedNTimesThisTurn
import com.wingedsheep.sdk.scripting.conditions.ManaSpentToCastIncludes
import com.wingedsheep.sdk.scripting.conditions.NoManaSpentToCast
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.conditions.BlightWasPaid
import com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid
import com.wingedsheep.sdk.scripting.conditions.SourceIsRingBearer
import com.wingedsheep.sdk.scripting.conditions.YouChoseOtherCreatureAsRingBearer
import com.wingedsheep.sdk.scripting.conditions.YouControlSource
import com.wingedsheep.sdk.scripting.conditions.PlayerAttackedWithCreaturesThisTurn
import com.wingedsheep.sdk.scripting.conditions.PermanentLeftBattlefieldThisTurn
import com.wingedsheep.sdk.scripting.conditions.PermanentTypeEnteredBattlefieldThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerCastSpellsThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerCommittedCrimeThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerHasCitysBlessing
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.conditions.ControlledCreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn
import com.wingedsheep.engine.handlers.triggers.CreatureDiedThisTurnConditionEvaluator
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.conditions.VoidCondition
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent

/**
 * Evaluates conditions from the SDK against the game state.
 *
 * [defaultProjection] is forwarded to this evaluator's [DynamicAmountEvaluator] for battlefield
 * reads when a caller doesn't thread a projection. Resolution-time callers leave the default
 * (the canonical lazy [GameState.projectedState]). A mid-projection caller — the
 * [com.wingedsheep.engine.mechanics.layers.EffectApplicator] evaluating a source condition while
 * the projection is still being computed — must swap in a non-reentrant projection, otherwise an
 * aggregate `Compare` (e.g. "while you control N+ creatures") re-enters the lazy `projectedState`
 * initializer and recurses until the stack overflows.
 */
class ConditionEvaluator(
    defaultProjection: (GameState) -> ProjectedState = { it.projectedState }
) {

    private val dynamicAmountEvaluator = DynamicAmountEvaluator(this, defaultProjection)

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

            // Board-derived (current step + active player), so it works identically at resolution
            // and under projection — used as a ConditionalStaticAbility gate (Zurgo's end step).
            is IsInStep -> {
                if (condition.yoursOnly && state.activePlayerId != ctx.controllerId) false
                else state.step in condition.steps
            }

            // Reads the controller's PlayerTurnsTakenComponent (incremented in
            // TurnManager.startTurn). Returns false when there's no controller
            // (e.g. some projection-time evaluations) — the unless-branch then
            // falls through, which matches "enters tapped" for the default case.
            is ControllerTurnsTakenAtMost -> {
                val controllerId = ctx.controllerId
                val taken = controllerId?.let {
                    state.getEntity(it)?.get<PlayerTurnsTakenComponent>()?.count
                }
                taken != null && taken <= condition.threshold
            }

            is SourcePlottedOnPriorTurn -> {
                val sourceId = ctx.sourceId
                val plotted = sourceId?.let { state.getEntity(it)?.get<PlottedComponent>() }
                plotted != null && plotted.turnPlotted < state.turnNumber
            }

            is SourceCastForImpending -> {
                val sourceId = ctx.sourceId
                sourceId != null && state.getEntity(sourceId)?.has<CastForImpendingComponent>() == true
            }

            // The unified "an entity matches a filter" primitive. Dispatches on the entity role:
            // Self / enchanted-or-equipped are dual-mode (resolution + projection); a chosen target
            // or the triggering spell are resolution-only.
            is EntityMatches -> evaluateEntityMatches(state, condition, ctx)

            // CR 701.54e: the source is your Ring-bearer — it carries your Ring-bearer designation
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

            // CR 701.54a: intervening-if for "Whenever the Ring tempts you" payoffs that only
            // fire when the player chose someone other than the source. True iff the controller
            // currently has a Ring-bearer AND that bearer isn't the source. Reads the same
            // controller/projected-controller pair as SourceIsRingBearer so a designation that's
            // been suspended by a control change correctly fails to count as "a bearer".
            is YouChoseOtherCreatureAsRingBearer -> {
                val sourceId = ctx.sourceId
                val controllerId = ctx.controllerId
                if (sourceId == null || controllerId == null) {
                    false
                } else {
                    val bearerId = state.getBattlefield().firstOrNull { id ->
                        val bearer = state.getEntity(id)?.get<RingBearerComponent>() ?: return@firstOrNull false
                        bearer.ownerId == controllerId &&
                            state.projectedState.getController(id) == controllerId
                    }
                    bearerId != null && bearerId != sourceId
                }
            }

            // Aura-controller-aware modified check (CR 700.4) — distinct enough from the
            // generic StatePredicate.IsModified to warrant its own branch.
            is SourceIsModified -> evaluateSourceIsModifiedCtx(state, ctx)

            is EnchantedCreatureHasSubtype -> evaluateEnchantedCreatureHasSubtypeCtx(state, condition, ctx)
            is EnchantedCreatureIsLegendary -> evaluateEnchantedCreatureIsLegendaryCtx(state, ctx)

            // Player-relative trackers (resolve [Player] against the current context).
            is PlayerAttackedWithCreaturesThisTurn -> evaluateAttackedWithCreaturesCtx(state, condition, ctx)
            is PlayerCastSpellsThisTurn -> evaluateCastSpellsThisTurnCtx(state, condition, ctx)
            is PlayerCommittedCrimeThisTurn -> {
                val playerId = resolvePlayer(state, condition.player, ctx)
                playerId != null && playerId in state.playersWhoCommittedCrimeThisTurn
            }
            is PlayerHasCitysBlessing -> evaluateHasCitysBlessingCtx(state, condition, ctx)
            is PermanentTypeEnteredBattlefieldThisTurn ->
                evaluatePermanentTypeEnteredBattlefieldThisTurnCtx(state, condition, ctx)
            is PermanentLeftBattlefieldThisTurn ->
                evaluatePermanentLeftBattlefieldThisTurnCtx(state, condition, ctx)

            // Global facts (no controller/source needed).
            is VoidCondition ->
                state.nonlandPermanentLeftBattlefieldThisTurn || state.spellWarpedThisTurn

            // Existential over all players: some player has at most [threshold] life.
            // Reads each player's LifeTotalComponent from state.turnOrder.
            is APlayerLifeAtMost -> state.turnOrder.any { playerId ->
                val life = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life
                life != null && life <= condition.threshold
            }

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
            is SneakCostWasPaid -> ifResolution { evaluateSneakCostWasPaid(state, it) }
            is BlightWasPaid -> ifResolution { it.wasBlightPaid }
            is ManaSpentToCastIncludes -> ifResolution { evaluateManaSpentToCastIncludes(state, condition, it) }
            is NoManaSpentToCast -> ifResolution { evaluateNoManaSpentToCast(state, it) }
            is SourceChosenModeIs -> {
                // Dual-mode: the chosen mode is stored in the durable cast-choices bag on the
                // source permanent, readable both at resolution (gating triggered abilities) and
                // during projection (gating mode-dependent continuous static abilities, e.g.
                // Frostcliff/Windcrag Sieges' lord and static modes).
                val sourceId = ctx.sourceId
                sourceId != null &&
                    state.getEntity(sourceId)?.chosenModeId() == condition.modeId
            }
            is CastChoiceMade -> {
                // Generic "was this choice made" guard over the durable cast-choices bag; works at
                // both resolution and projection.
                val sourceId = ctx.sourceId
                sourceId != null &&
                    state.getEntity(sourceId)?.get<CastChoicesComponent>()
                        ?.chosen?.containsKey(condition.slot) == true
            }
            is CastChoiceIs -> {
                val sourceId = ctx.sourceId
                sourceId != null &&
                    castChoiceMatches(state.getEntity(sourceId), condition.slot, condition.value)
            }
            is CastTimeFlagSet -> {
                // The "as you cast this spell" capture, frozen onto the spell on the stack at cast
                // (CR 601.2i). Read here at resolution so the answer reflects the cast-time board
                // even after a later change (Steer Clear). The spell entity keeps its id across the
                // hand→stack boundary, so the resolving effect's sourceId locates it.
                val sourceId = ctx.sourceId
                sourceId != null &&
                    state.getEntity(sourceId)
                        ?.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()
                        ?.castTimeFlags?.contains(condition.flag) == true
            }
            is SacrificedPermanentHadSubtype -> ifResolution { evaluateSacrificedPermanentHadSubtype(condition, it) }
            is SacrificedPermanentWasLegendary -> ifResolution { evaluateSacrificedPermanentWasLegendary(it) }
            is YouSacrificedPermanentThisWay -> ifResolution { evaluateYouSacrificedPermanentThisWay(it) }
            is TriggeringEntityWasHistoric -> ifResolution { evaluateTriggeringEntityWasHistoric(state, it) }
            is TriggeringEntityWasCast -> ifResolution { evaluateTriggeringEntityWasCast(state, it) }
            is TriggeringEntityEnteredOrWasCastFromGraveyard ->
                ifResolution { evaluateTriggeringEntityEnteredOrWasCastFromGraveyard(state, it) }
            is TriggeringEntityHadMinusOneMinusOneCounter ->
                ifResolution { (it.triggerMinusOneMinusOneCounterCount ?: 0) > 0 }
            is TriggeringEntityHadCounters ->
                ifResolution { (it.triggerTotalCounterCount ?: 0) > 0 }
            is TriggeringEntityWasNotPutByThisSource ->
                ifResolution { evaluateTriggeringEntityWasNotPutByThisSource(state, it) }
            is TriggeringSpellHasSingleTarget -> ifResolution { evaluateTriggeringSpellHasSingleTarget(state, it) }
            is TargetIsPlayer -> ifResolution { evaluateTargetIsPlayer(condition, it) }
            is TargetMarkedDamageExceedsToughness ->
                ifResolution { evaluateTargetMarkedDamageExceedsToughness(state, condition, it) }
            is TargetSharesMostCommonColor -> ifResolution { evaluateTargetSharesMostCommonColor(state, condition, it) }
            is AnotherPermanentWithSameNameAsTarget ->
                ifResolution { evaluateAnotherPermanentWithSameNameAsTarget(state, condition, it) }
            is IsInPhase -> ifResolution { evaluateIsInPhase(state, condition, it) }
            is YouWereAttackedThisStep -> ifResolution { evaluateYouWereAttackedThisStep(state, it) }
            is IsFirstSpellPaidWithTreasureManaCastThisTurn ->
                ifResolution { evaluateFirstSpellPaidWithTreasureMana(state, it) }
            is SourceAbilityResolvedNTimesThisTurn -> ifResolution { evaluateSourceAbilityResolvedNTimes(state, condition, it) }
            is OpponentSpellOnStack -> ifResolution { evaluateOpponentSpellOnStack(state, it) }
            is CollectionContainsMatch -> ifResolution { evaluateCollectionContainsMatch(state, condition, it) }
            is CreatureDiedThisTurnCondition ->
                ifResolution { CreatureDiedThisTurnConditionEvaluator().evaluate(state, condition, it) }
            is ControlledCreatureDiedThisTurnCondition ->
                ifResolution { CreatureDiedThisTurnConditionEvaluator().evaluateControlled(state, it) }
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

    /**
     * The unified `EntityMatches(entity, filter)` evaluator. Resolves [EntityMatches.entity] and
     * matches it against [EntityMatches.filter], routing to the matching strategy each entity role
     * requires:
     * - [EffectTarget.Self]: live predicate match against the source; dual-mode (resolution +
     *   static-ability projection).
     * - [EffectTarget.EnchantedPermanent] / [EffectTarget.EnchantedCreature] /
     *   [EffectTarget.EquippedCreature]: live match against the source's attachment; dual-mode.
     * - [EffectTarget.ContextTarget]: resolution-only; resolve the chosen target to a game object
     *   (false for a player target) and match live.
     * - [EffectTarget.TriggeringEntity]: resolution-only; match the triggering spell by its static
     *   cast characteristics so the answer survives the spell leaving the stack (CR 603.4).
     *
     * Other entity roles are unsupported: the `CardLinter` rejects them at card load
     * (`UnsupportedEntityMatchesRole` — its supported-role set must extend in lockstep with this
     * dispatch), and the `else` here is the defense-in-depth backstop, not a contract.
     */
    private fun evaluateEntityMatches(
        state: GameState,
        condition: EntityMatches,
        ctx: ConditionEvaluationContext
    ): Boolean = when (val entity = condition.entity) {
        is EffectTarget.Self ->
            evaluateSourceFilterMatch(state, condition.filter, ctx)
        is EffectTarget.EnchantedPermanent,
        is EffectTarget.EnchantedCreature,
        is EffectTarget.EquippedCreature ->
            evaluateAttachmentFilterMatch(state, condition.filter, ctx)
        is EffectTarget.ContextTarget ->
            (ctx as? Resolution)?.let {
                evaluateTargetFilterMatch(state, condition.filter, entity.index, it.effectContext)
            } ?: false
        is EffectTarget.TriggeringEntity ->
            (ctx as? Resolution)?.let {
                evaluateTriggeringSpellFilterMatch(state, condition.filter, it.effectContext)
            } ?: false
        else -> false
    }

    private fun evaluateSourceFilterMatch(
        state: GameState,
        filter: GameObjectFilter,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val sourceId = ctx.sourceId ?: return false
        val projected = ctx.projectedStateFor(state)
        val predicateContext = when (ctx) {
            is Resolution -> PredicateContext.fromEffectContext(ctx.effectContext)
            is Projection -> ctx.controllerId?.let { PredicateContext(controllerId = it) }
                ?: PredicateContext(controllerId = sourceId)
        }
        return PredicateEvaluator().matches(state, projected, sourceId, filter, predicateContext)
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
            is Projection -> controllerId?.let {
                // Pass `sourceId` through so source-reading predicates (e.g.
                // `sharingChosenColorWithSource`) can find the source's
                // `CastChoicesComponent` during static-ability gating.
                PredicateContext(controllerId = it, sourceId = ctx.sourceId)
            } ?: return condition.negate
        }

        val playerIds: List<EntityId> = when (condition.player) {
            is Player.You -> controllerId?.let { listOf(it) } ?: emptyList()
            is Player.EachOpponent -> controllerId?.let { state.getOpponents(it) } ?: emptyList()
            is Player.Each -> state.activePlayers
            is Player.Any -> state.activePlayers
            is Player.Candidate -> listOfNotNull((ctx as? Resolution)?.effectContext?.candidatePlayerId)
            is Player.ChosenOpponent -> listOfNotNull(
                ctx.sourceId?.let { state.getEntity(it)?.chosenOpponent() }
            )
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

    private fun evaluateAttachmentFilterMatch(
        state: GameState,
        filter: GameObjectFilter,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val sourceId = ctx.sourceId ?: return false
        val attached = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
        val permanentId = attached ?: when (ctx) {
            is Resolution -> sourceId  // resolution-mode fallback (granted-ability scope)
            is Projection -> return false
        }
        val projected = ctx.projectedStateFor(state)
        val predicateContext = when (ctx) {
            is Resolution -> PredicateContext.fromEffectContext(ctx.effectContext)
            is Projection -> {
                // "You" (for any controller predicate in [filter]) is the Aura's controller.
                // Fall back to the source's projected controller rather than miscasting the
                // source entity id as a player id; if even that is unknown, we can't evaluate.
                val controller = ctx.controllerId ?: projected.getController(sourceId) ?: return false
                PredicateContext(controllerId = controller)
            }
        }
        return PredicateEvaluator().matches(state, projected, permanentId, filter, predicateContext)
    }

    /**
     * Resolve a [Player] reference against the current evaluation context.
     *
     * - [Player.You]: controller-of-source (resolution: effectContext.controllerId;
     *   projection: source's projected controller).
     * - [Player.AnOpponent]: a non-targeted opponent of the controller. Returns the first opponent
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
            is Player.AnOpponent -> {
                val c = ctx.controllerId ?: return null
                state.getOpponents(c).firstOrNull()
            }
            is Player.TriggeringPlayer -> (ctx as? Resolution)?.effectContext?.triggeringPlayerId
            is Player.Candidate -> (ctx as? Resolution)?.effectContext?.candidatePlayerId
            is Player.ChosenOpponent -> ctx.sourceId?.let { sourceId ->
                state.getEntity(sourceId)?.chosenOpponent()
            }
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
        val evaluator = PredicateEvaluator()
        var matches = 0
        for (record in records) {
            // The zone qualifier is checked independently of the filter: a face-down (morph) spell
            // cast from hand still counts as "cast a spell from your hand" even though matchesFilter
            // bails on face-down characteristics (CR 708.2).
            if (condition.fromZone != null && record.castFromZone != condition.fromZone) continue
            if (condition.filter != GameObjectFilter.Any && !evaluator.matchesFilter(record, condition.filter)) continue
            matches++
            if (matches >= condition.atLeast) return true
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

    private fun evaluatePermanentLeftBattlefieldThisTurnCtx(
        state: GameState,
        condition: PermanentLeftBattlefieldThisTurn,
        ctx: ConditionEvaluationContext
    ): Boolean {
        val playerId = resolvePlayer(state, condition.player, ctx) ?: return false
        val count = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.PermanentLeftBattlefieldThisTurnComponent>()
            ?.count ?: 0
        return count > 0
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
        // For permanents (e.g. an entering creature's own "enters with a counter if cast from
        // graveyard" replacement, or a triggered ability), fall back to the cast-origin marker
        // component stamped on the permanent as it resolved onto the battlefield.
        val sourceId = context.sourceId ?: return false
        val entity = state.getEntity(sourceId) ?: return false
        return when (condition.zone) {
            Zone.HAND -> entity.has<CastFromHandComponent>()
            Zone.GRAVEYARD -> entity.has<CastFromGraveyardComponent>()
            else -> false
        }
    }

    /**
     * "if it wasn't cast or no mana was spent to cast it." The engine stamps a
     * [CastRecordComponent] on a resolving permanent only when the total mana spent to
     * cast it was greater than zero (see StackResolver.resolvePermanentSpell). So the
     * source has no cast record — or a record summing to zero — exactly when no mana was
     * spent: a free / {0} cast, or a permanent put onto the battlefield without being cast.
     * Mana paid for additional costs or cost increases on an otherwise-free cast does land
     * in the record, so such a permanent correctly fails this condition.
     */
    private fun evaluateNoManaSpentToCast(state: GameState, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        val record = state.getEntity(sourceId)?.get<CastRecordComponent>() ?: return true
        return record.whiteSpent + record.blueSpent + record.blackSpent +
            record.redSpent + record.greenSpent + record.colorlessSpent == 0
    }

    private fun evaluateWasKicked(state: GameState, context: EffectContext): Boolean {
        // Check the durable cast-choices bag on the permanent first (for triggered abilities)
        val sourceId = context.sourceId ?: return context.wasKicked
        if (state.getEntity(sourceId)?.wasKickedChoice() == true) return true
        // Fall back to context (for spell resolution, e.g. kicker additional effects)
        return context.wasKicked
    }

    private fun evaluateSneakCostWasPaid(state: GameState, context: EffectContext): Boolean {
        // Durable bag on the resolved permanent first (ETB / ongoing triggered & activated reads).
        val sourceId = context.sourceId ?: return context.wasSneaked
        val flagged = state.getEntity(sourceId)
            ?.get<CastChoicesComponent>()
            ?.chosen
            ?.containsKey(ChoiceSlot.SNEAK) == true
        if (flagged) return true
        // Fall back to the resolution context (a non-permanent spell's own resolving effect,
        // e.g. The Last Ronin's Technique reading "if this spell's sneak cost was paid").
        return context.wasSneaked
    }

    /** Compare the value locked into [slot] on [entity]'s cast-choices bag to [value] as text. */
    private fun castChoiceMatches(
        entity: com.wingedsheep.engine.state.ComponentContainer?,
        slot: ChoiceSlot,
        value: String
    ): Boolean {
        val cv = entity?.get<CastChoicesComponent>()?.chosen?.get(slot) ?: return false
        val actual = when (cv) {
            is ChoiceValue.ColorChoice -> cv.color.name
            is ChoiceValue.TextChoice -> cv.text
            is ChoiceValue.NumberChoice -> cv.amount.toString()
            is ChoiceValue.EntityChoice -> cv.entityId.toString()
            ChoiceValue.Flag -> "true"
        }
        return actual.equals(value, ignoreCase = true)
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

    private fun evaluateSacrificedPermanentWasLegendary(context: EffectContext): Boolean {
        return context.sacrificedPermanents.any { snapshot ->
            "LEGENDARY" in snapshot.supertypes
        }
    }

    private fun evaluateYouSacrificedPermanentThisWay(context: EffectContext): Boolean {
        return context.sacrificedPermanents.any { snapshot ->
            snapshot.controllerId == context.controllerId
        }
    }

    private fun evaluateYouWereAttackedThisStep(state: GameState, context: EffectContext): Boolean {
        return state.entities.any { (_, container) ->
            val attacking = container.get<AttackingComponent>()
            attacking != null && attacking.defenderId == context.controllerId
        }
    }

    private fun evaluateOpponentSpellOnStack(state: GameState, context: EffectContext): Boolean {
        val opponents = state.getOpponents(context.controllerId)
        return state.stack.any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()?.casterId in opponents
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

    /**
     * "if you cast it" referring to the triggering (entering) permanent. Mirrors
     * [evaluateWasCast] but reads the cast-origin markers off the triggering entity rather than
     * the ability's source, so it works for "whenever a creature you control enters, if you cast
     * it" triggers where the source is a separate permanent. Tokens, reanimated, and
     * "put onto the battlefield" permanents lack these markers and are correctly excluded.
     */
    private fun evaluateTriggeringEntityWasCast(state: GameState, context: EffectContext): Boolean {
        val entityId = context.triggeringEntityId ?: return false
        val entity = state.getEntity(entityId) ?: return false
        return entity.has<CastFromHandComponent>() || entity.has<CastFromGraveyardComponent>()
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

    private fun evaluateTargetFilterMatch(
        state: GameState,
        filter: GameObjectFilter,
        targetIndex: Int,
        context: EffectContext
    ): Boolean {
        val target = context.positionalTarget(targetIndex) ?: return false
        val entityId = when (target) {
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> return false
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> target.cardId
        }
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projected = state.projectedState
        return predicateEvaluator.matches(state, projected, entityId, filter, predicateContext)
    }

    /**
     * Evaluate "if a player is dealt damage this way" — true when the context target at the given
     * index is a player target. Used by Sonic Shrieker to gate its discard follow-up on the
     * damaged "any target" having been a player. Permanent/spell/card targets return false.
     */
    private fun evaluateTargetIsPlayer(
        condition: TargetIsPlayer,
        context: EffectContext
    ): Boolean {
        val target = context.positionalTarget(condition.targetIndex) ?: return false
        return target is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player
    }

    /**
     * Evaluate "if excess damage was dealt this way" — true when the target creature's
     * marked damage strictly exceeds its (projected) toughness. Chained after a `DealDamage`
     * step in a composite, so the marked-damage component reflects damage just dealt by
     * the preceding step (Composite doesn't interleave SBA or fire other triggers between
     * its sub-effects, so for the canonical pipeline no other source contributes marked
     * damage in scope).
     *
     * The non-creature and not-on-battlefield branches return false defensively — under
     * `Targets.Creature` + Composite they can't fire, but they keep this condition safe
     * if a future caller wraps it in a longer chain that crosses SBA or re-targets.
     */
    private fun evaluateTargetMarkedDamageExceedsToughness(
        state: GameState,
        condition: TargetMarkedDamageExceedsToughness,
        context: EffectContext
    ): Boolean {
        val target = context.positionalTarget(condition.targetIndex) ?: return false
        val entityId = (target as? com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent)
            ?.entityId ?: return false
        if (entityId !in state.getBattlefield()) return false
        val projected = state.projectedState
        if (!projected.isCreature(entityId)) return false
        val marked = state.getEntity(entityId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
            ?.amount ?: 0
        val toughness = projected.getToughness(entityId) ?: return false
        return marked > toughness
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
        val target = context.positionalTarget(condition.targetIndex) ?: return false
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
        val target = context.positionalTarget(condition.targetIndex) ?: return false
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

    private fun evaluateTriggeringSpellFilterMatch(
        state: GameState,
        filter: GameObjectFilter,
        context: EffectContext
    ): Boolean {
        // Match the triggering spell by its static card characteristics so the check stays correct
        // even after the spell has left the stack. Composes with PlayerCastSpellsThisTurn (via
        // Conditions.YouCastFirstSpellOfTypeThisTurn) to express "first X spell this turn" — this
        // guard is what stops a non-matching cast from satisfying that count-based payoff.
        val triggeringId = context.triggeringEntityId ?: return false
        val entity = state.getEntity(triggeringId) ?: return false
        val card = entity.get<CardComponent>() ?: return false
        val triggeringRecord = CastSpellRecord(
            typeLine = card.typeLine,
            manaValue = card.manaValue,
            colors = card.colors,
            isFaceDown = entity.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()
        )
        return PredicateEvaluator().matchesFilter(triggeringRecord, filter)
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
