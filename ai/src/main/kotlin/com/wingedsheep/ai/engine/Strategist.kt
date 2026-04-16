package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.advisor.CastContext
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Chooses which [LegalAction] to take when the AI has priority.
 *
 * Two-phase evaluation:
 * 1. **Quick pass**: Score all candidates with 1-ply lookahead.
 * 2. **Deep pass**: If the top candidates are close, re-score them with
 *    multi-ply [Searcher] to break the tie considering opponent responses.
 *
 * Combat decisions are delegated to [CombatAdvisor].
 * Card-specific overrides are handled by [CardAdvisorRegistry].
 */
class Strategist(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val searcher: Searcher = Searcher(simulator, evaluator),
    private val combatAdvisor: CombatAdvisor = CombatAdvisor(simulator, evaluator),
    private val advisorRegistry: CardAdvisorRegistry = CardAdvisorRegistry()
) {
    fun chooseAction(
        state: GameState,
        legalActions: List<LegalAction>,
        playerId: EntityId
    ): LegalAction {
        // Combat declaration steps need the CombatAdvisor to fill in attacker/blocker maps
        // even when there's only one legal action (which is the common case — the enumerator
        // returns a single DeclareAttackers/DeclareBlockers with an empty default map).
        val combatAction = legalActions.find { it.actionType == "DeclareAttackers" || it.actionType == "DeclareBlockers" }
        if (combatAction != null) {
            return handleCombatDeclaration(state, combatAction, playerId)
        }

        if (legalActions.size == 1) return legalActions.first()

        val pass = legalActions.find { it.actionType == "PassPriority" }
        val affordable = preferKickerVariants(
            legalActions.filter { it.affordable && !it.isManaAbility && it.actionType != "PassPriority" }
        )

        if (affordable.isEmpty()) return pass ?: legalActions.first()

        // ── Phase 1: Quick 1-ply scoring of all candidates ──
        val passScore = if (pass != null) {
            evaluate1Ply(state, pass, playerId)
        } else {
            evaluator.evaluate(state, state.projectedState, playerId)
        }

        val scored = affordable.map { action ->
            action to evaluate1Ply(state, action, playerId, passScore)
        }

        // ── Phase 2: Adaptive deep search on close contenders ──
        val depth = searcher.recommendDepth(state, scored, playerId)

        val finalScored = if (depth > 1) {
            deepSearch(state, scored, playerId, depth, passScore)
        } else {
            scored
        }

        // On the opponent's end step, unspent mana is about to be wasted.
        // Reduce the pass threshold so the AI is more willing to use instants
        // rather than letting mana evaporate.
        val adjustedPassScore = if (state.activePlayerId != playerId && state.step == Step.END) {
            passScore - 1.5
        } else {
            passScore
        }

        val best = finalScored.maxByOrNull { it.second }
        return if (best != null && best.second > adjustedPassScore) {
            // Fill in targets on the returned action so the processor can execute it
            val chosen = best.first
            if (chosen.requiresTargets) {
                val resolvedAction = resolveTargetsForSimulation(state, chosen, playerId)
                chosen.copy(action = resolvedAction)
            } else {
                chosen
            }
        } else {
            pass ?: legalActions.first()
        }
    }

    /**
     * Re-score top candidates with multi-ply search.
     * Only the top N contenders get deep search — the rest keep their 1-ply scores.
     */
    private fun deepSearch(
        state: GameState,
        scored: List<Pair<LegalAction, Double>>,
        playerId: EntityId,
        depth: Int,
        passScore: Double
    ): List<Pair<LegalAction, Double>> {
        val sorted = scored.sortedByDescending { it.second }
        val bestScore = sorted.first().second

        // Only deep-search actions within striking distance of the best
        val threshold = (bestScore - passScore).coerceAtLeast(1.0) * 0.5
        val contenders = sorted.takeWhile { it.second >= bestScore - threshold }
            .take(6) // hard cap
        val contenderSet = contenders.map { it.first }.toSet()

        return scored.map { (action, quickScore) ->
            if (action in contenderSet) {
                action to searcher.searchAction(state, action, playerId, depth)
            } else {
                action to quickScore
            }
        }
    }

    private fun handleCombatDeclaration(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId
    ): LegalAction {
        val action = when (legalAction.actionType) {
            "DeclareAttackers" -> combatAdvisor.chooseAttackers(state, legalAction, playerId)
            "DeclareBlockers" -> combatAdvisor.chooseBlockers(state, legalAction, playerId, useSimulation = true)
            else -> legalAction.action
        }
        return legalAction.copy(action = action)
    }

    private fun evaluate1Ply(state: GameState, action: LegalAction, playerId: EntityId, passScore: Double? = null): Double {
        // For targeted spells, fill in heuristic targets before simulating.
        // Without this, the CastSpellHandler rejects the action ("No valid targets")
        // and the spell always scores the same as passing.
        val simulationAction = resolveTargetsForSimulation(state, action, playerId)
        val result = simulator.simulate(state, simulationAction)
        val defaultScore = evaluator.evaluate(result.state, result.state.projectedState, playerId)

        // Check for card-specific advisor override
        val cardName = resolveCardName(state, action) ?: return defaultScore
        val advisor = advisorRegistry.getAdvisor(cardName) ?: return defaultScore
        val context = CastContext(
            state = state,
            projected = state.projectedState,
            playerId = playerId,
            action = action,
            passScore = passScore ?: evaluator.evaluate(state, state.projectedState, playerId),
            defaultScore = defaultScore,
            evaluator = evaluator,
            simulator = simulator
        )
        return advisor.evaluateCast(context) ?: defaultScore
    }

    /**
     * For spells/abilities that require target selection, fill in heuristic
     * targets so the simulation can actually resolve the spell.
     *
     * Multi-target spells: for each requirement, pick the highest-value
     * opponent creature (or lowest-value own creature, depending on context).
     * Single-target spells: pick the best target by creature value.
     *
     * The advisor's [CardAdvisor.respondToDecision] refines target selection
     * during actual gameplay — this is just for scoring purposes.
     */
    private fun resolveTargetsForSimulation(
        state: GameState,
        action: LegalAction,
        playerId: EntityId
    ): com.wingedsheep.engine.core.GameAction {
        if (!action.requiresTargets) return action.action
        val castSpell = action.action as? CastSpell ?: return action.action
        if (castSpell.targets.isNotEmpty()) return action.action

        val projected = state.projectedState

        // Build target info list: prefer targetRequirements (multi-target), fall back to validTargets
        val targetInfos: List<TargetInfo> = action.targetRequirements
            ?: action.validTargets?.let { targets ->
                listOf(TargetInfo(
                    index = 0,
                    description = action.targetDescription ?: "",
                    minTargets = action.minTargets,
                    maxTargets = action.targetCount,
                    validTargets = targets,
                    targetZone = null
                ))
            }
            ?: return action.action

        if (targetInfos.any { it.validTargets.isEmpty() }) return action.action

        // For each requirement, pick the best target using a heuristic
        val chosenTargets = targetInfos.map { info ->
            val best = info.validTargets.maxByOrNull { entityId ->
                val controller = projected.getController(entityId)
                val isOpponent = controller != null && controller != playerId
                val isPlayer = state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>() != null

                if (isPlayer) {
                    // Player target — prefer opponent
                    if (isOpponent) 5.0 else -5.0
                } else if (projected.isCreature(entityId)) {
                    val card = state.getEntity(entityId)?.get<CardComponent>()
                    val value = if (card != null) {
                        BoardPresence.permanentValue(state, projected, entityId, card)
                    } else 0.0
                    // Opponent creatures: higher value = better target for removal
                    // Own creatures: higher value = better target for pump/bite source
                    if (isOpponent) value + 10.0 else -value
                } else {
                    0.0
                }
            } ?: info.validTargets.first()

            // Build the right ChosenTarget variant based on zone
            when (info.targetZone) {
                "GRAVEYARD" -> {
                    val ownerId = state.getEntity(best)
                        ?.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()?.playerId
                        ?: playerId
                    ChosenTarget.Card(best, ownerId, Zone.GRAVEYARD)
                }
                "STACK" -> ChosenTarget.Spell(best)
                else -> {
                    val isPlayer = state.getEntity(best)?.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>() != null
                    if (isPlayer) ChosenTarget.Player(best)
                    else ChosenTarget.Permanent(best)
                }
            }
        }

        return castSpell.copy(targets = chosenTargets)
    }

    /**
     * When both a normal cast and a kicker/offspring variant of the same card are
     * affordable, drop the normal variant. The kicker variant is strictly better —
     * it does everything the normal cast does plus the kicker bonus (e.g., offspring
     * creates an additional token). Keeping both inflates the candidate list and
     * triggers unnecessary deep search (the two variants score close together,
     * tripping the "close call" heuristic).
     */
    private fun preferKickerVariants(actions: List<LegalAction>): List<LegalAction> {
        // Collect cardIds that have an affordable CastWithKicker variant
        val kickedCardIds = mutableSetOf<EntityId>()
        for (action in actions) {
            if (action.actionType == "CastWithKicker") {
                val castSpell = action.action as? CastSpell ?: continue
                kickedCardIds.add(castSpell.cardId)
            }
        }
        if (kickedCardIds.isEmpty()) return actions

        // Remove the normal CastSpell variant for those cards
        return actions.filter { action ->
            if (action.actionType == "CastSpell") {
                val castSpell = action.action as? CastSpell ?: return@filter true
                castSpell.cardId !in kickedCardIds
            } else {
                true
            }
        }
    }

    /** Resolve the card name from a legal action's underlying GameAction. */
    private fun resolveCardName(state: GameState, action: LegalAction): String? {
        val entityId = when (val gameAction = action.action) {
            is CastSpell -> gameAction.cardId
            is ActivateAbility -> gameAction.sourceId
            else -> return null
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.name
    }
}
