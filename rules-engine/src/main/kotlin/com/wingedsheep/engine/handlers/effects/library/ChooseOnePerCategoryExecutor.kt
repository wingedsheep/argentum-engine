package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ChooseOnePerCategoryContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChooseOnePerCategoryEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChooseOnePerCategoryEffect] — "…chooses a permanent they control of each permanent
 * type…" (Liliana, Dreadhorde General's −9; the same step backs Cataclysm and Divine Reckoning).
 *
 * A pure *choosing* step: it collects one pick per category from each controller represented in the
 * source collection and publishes them all to `storeAs`. Nothing moves zones — the caller composes
 * `exclude(pool, kept)` plus a move step to decide what happens to the rest, which is what lets the
 * same choice drive "sacrifices the rest" (Liliana) and "returns the rest to their hands"
 * (Consuming Tide).
 *
 * Two rules drive the structure:
 *
 * - **One choice per category, not one restricted selection.** The oracle wording is a sequence of
 *   picks, which is what allows a single permanent to be the pick for several categories — an
 *   artifact creature spared as both the artifact and the creature. A one-shot selection with
 *   `SelectionRestriction.OnePerCardType` would let it claim only one of its types.
 * - **All choices before anything happens (CR 101.4).** Choosers are asked in APNAP order, each
 *   knowing the picks made before them, and the collection is published only once the last pick is
 *   in. The in-progress tally rides on [ChooseOnePerCategoryContinuation] across the pauses.
 *
 * A category the chooser controls nothing of is skipped and a category with a single candidate
 * resolves itself, so a board with one permanent per type asks nothing at all.
 */
class ChooseOnePerCategoryExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ChooseOnePerCategoryEffect> {

    override val effectType: KClass<ChooseOnePerCategoryEffect> = ChooseOnePerCategoryEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ChooseOnePerCategoryEffect,
        context: EffectContext
    ): EffectResult {
        val collections = context.pipeline.storedCollections
        val pool = collections[effect.from]
            ?: return EffectResult.error(
                state, "No collection named '${effect.from}' in storedCollections"
            )

        return collectPicks(
            state = state,
            effect = effect,
            storedCollections = collections,
            pendingPlayers = choosersInApnapOrder(state, pool),
            startCategory = 0,
            picks = emptyList(),
            sourceId = context.sourceId
        )
    }

    /**
     * Walk [pendingPlayers] (the first resuming at [startCategory], the rest from the top), asking
     * for one pick per satisfiable category. Pauses on the first category that needs a real
     * decision; once every pick is in, publishes them under `effect.storeAs`.
     *
     * Shared with `SacrificeAndPayContinuationResumer` so a resumed pick re-enters the same loop.
     */
    fun collectPicks(
        state: GameState,
        effect: ChooseOnePerCategoryEffect,
        storedCollections: Map<String, List<EntityId>>,
        pendingPlayers: List<EntityId>,
        startCategory: Int,
        picks: List<EntityId>,
        sourceId: EntityId?
    ): EffectResult {
        val pool = storedCollections[effect.from].orEmpty()
        val accumulated = picks.toMutableList()

        for ((offset, playerId) in pendingPlayers.withIndex()) {
            val firstCategory = if (offset == 0) startCategory else 0
            for (categoryIndex in firstCategory until effect.categories.size) {
                val candidates = candidatesFor(state, pool, playerId, effect, categoryIndex, sourceId)
                if (candidates.isEmpty()) continue
                // Every candidate is already being kept for an earlier category — the pick is
                // forced, so don't ask.
                if (candidates.all { it in accumulated }) continue

                if (candidates.size == 1) {
                    accumulated += candidates
                    continue
                }

                return pauseForChoice(
                    state = state,
                    effect = effect,
                    storedCollections = storedCollections,
                    playerId = playerId,
                    candidates = candidates,
                    categoryIndex = categoryIndex,
                    pendingPlayers = pendingPlayers.drop(offset),
                    picks = accumulated,
                    sourceId = sourceId
                )
            }
        }

        return EffectResult.success(state)
            .copy(updatedCollections = mapOf(effect.storeAs to accumulated.distinct()))
    }

    /**
     * The distinct controllers of [pool] in APNAP order (CR 101.4). Deriving the choosers from the
     * collection rather than a `Player` parameter keeps the scoping where it belongs — in the
     * `gather` that built the pool — so the same step serves "each player", "each opponent" and a
     * single target player.
     */
    private fun choosersInApnapOrder(state: GameState, pool: List<EntityId>): List<EntityId> {
        val controllers = pool.mapNotNull { controllerOf(state, it) }.toHashSet()
        return state.apnapOrder.filter { it in controllers }
    }

    /**
     * The members of [pool] that [playerId] controls and that match the category at
     * [categoryIndex]. The category filters are controller-agnostic (`Artifact`, `Creature`, …) —
     * the control scoping comes from the pool split, so each chooser only ever sees their own
     * permanents.
     */
    private fun candidatesFor(
        state: GameState,
        pool: List<EntityId>,
        playerId: EntityId,
        effect: ChooseOnePerCategoryEffect,
        categoryIndex: Int,
        sourceId: EntityId?
    ): List<EntityId> {
        val category = effect.categories[categoryIndex]
        val projected = state.projectedState
        val predicateContext = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return pool.filter { id ->
            controllerOf(state, id) == playerId &&
                predicateEvaluator.matches(state, projected, id, category, predicateContext)
        }
    }

    /** Projected control first, so control-changing effects decide who chooses. */
    private fun controllerOf(state: GameState, entityId: EntityId): EntityId? =
        state.projectedState.getController(entityId)
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

    private fun pauseForChoice(
        state: GameState,
        effect: ChooseOnePerCategoryEffect,
        storedCollections: Map<String, List<EntityId>>,
        playerId: EntityId,
        candidates: List<EntityId>,
        categoryIndex: Int,
        pendingPlayers: List<EntityId>,
        picks: List<EntityId>,
        sourceId: EntityId?
    ): EffectResult {
        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val noun = effect.categories[categoryIndex].description
        val article = if (noun.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) {
            "an"
        } else {
            "a"
        }

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = "Choose $article $noun to keep",
            options = candidates,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            // On-battlefield selection: the chooser is picking among permanents already in play,
            // where counters, auras and duplicates matter (see the UX rules in AGENTS.md).
            useTargetingUI = true
        )

        val continuation = ChooseOnePerCategoryContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            effect = effect,
            sourceId = sourceId,
            sourceName = sourceName,
            storedCollections = storedCollections,
            pendingPlayers = pendingPlayers,
            categoryIndex = categoryIndex,
            picks = picks
        )

        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }
}
