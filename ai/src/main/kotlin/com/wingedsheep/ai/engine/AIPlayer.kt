package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.CardAdvisorModule
import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.evaluation.*
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.hidden.Determinizer
import com.wingedsheep.ai.engine.hidden.OpponentModel
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.engine.rollout.CandidateEvaluator
import com.wingedsheep.ai.engine.rollout.FastDecisionResponder
import com.wingedsheep.ai.engine.rollout.PlayoutEngine
import com.wingedsheep.ai.engine.rollout.PlayoutPolicy
import com.wingedsheep.ai.engine.rollout.RolloutCandidateEvaluator
import com.wingedsheep.ai.engine.rollout.StaticCandidateEvaluator
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.MeaningfulActionFilter
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * A complete AI player that can play a full game of Magic: The Gathering.
 *
 * Compose from the three layers:
 * - [GameSimulator] — "what happens if I do X?"
 * - [BoardEvaluator] — "how good is this state for me?"
 * - [Strategist] — "which action should I take?"
 * - [DecisionResponder] — "how do I answer this decision?"
 *
 * Usage:
 * ```
 * val ai = AIPlayer.create(cardRegistry, playerId)
 * val action = ai.chooseAction(state)
 * val result = processor.process(state, action)
 * // if paused:
 * val response = ai.respondToDecision(result.state, result.pendingDecision!!)
 * ```
 */
class AIPlayer(
    val playerId: EntityId,
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val strategist: Strategist,
    private val responder: DecisionResponder,
    /**
     * Phase 4a. Skip enumeration entirely on priority windows that
     * [MeaningfulActionFilter.canAutoPassWithoutEnumerating] can decide from the state alone.
     */
    private val useMeaningfulFilter: Boolean = false,
) {
    /**
     * Choose the best action from the current legal actions.
     * Returns the [GameAction] to submit to the [ActionProcessor].
     *
     * The fast path matters more than it looks. Phase 0 measured **76.2% of priority windows
     * offering zero candidates** while still paying ~0.40 ms to enumerate and find that out —
     * ~148 ms per game of pure waste, and a cost a rollout would pay again on every one of the
     * ~20-30 windows it crosses.
     */
    fun chooseAction(state: GameState): GameAction {
        if (useMeaningfulFilter && MeaningfulActionFilter.canAutoPassWithoutEnumerating(state, playerId)) {
            return PassPriority(playerId)
        }
        val legalActions = simulator.getLegalActions(state, playerId)
        return chooseFrom(state, legalActions).action
    }

    /**
     * Choose the best [LegalAction] from the given list.
     */
    fun chooseFrom(state: GameState, legalActions: List<LegalAction>): LegalAction {
        return strategist.chooseAction(state, legalActions, playerId)
    }

    /**
     * Respond to a pending decision.
     */
    fun respondToDecision(state: GameState, decision: PendingDecision): DecisionResponse {
        return responder.respond(state, decision, playerId)
    }

    /**
     * Play a full turn step: choose action, submit it, handle any decisions,
     * repeat until priority passes or the game ends.
     *
     * Returns the final state after all AI actions this priority window.
     */
    fun playPriorityWindow(state: GameState, processor: ActionProcessor): GameState {
        var current = state
        var iterations = 0
        val maxIterations = 100

        while (!current.gameOver && iterations < maxIterations) {
            // Handle pending decisions first
            val decision = current.pendingDecision
            if (decision != null) {
                if (decision.playerId != playerId) break // not our decision
                val response = respondToDecision(current, decision)
                val result = processor.process(current, SubmitDecision(playerId, response)).result
                if (result.error != null) break
                current = result.state
                iterations++
                continue
            }

            // Check if we have priority
            if (current.priorityPlayerId != playerId) break

            // Choose and execute an action
            val action = chooseAction(current)
            val result = processor.process(current, action).result
            if (result.error != null) {
                // Action was illegal — submit a safe fallback action
                val fallback = when {
                    current.step == Step.DECLARE_ATTACKERS && current.isActiveTurnFor(playerId) -> {
                        // Include mandatory attackers to avoid rejection
                        val legalActions = simulator.getLegalActions(current, playerId)
                        val attackAction = legalActions.find { it.actionType == "DeclareAttackers" }
                        val mandatory = attackAction?.mandatoryAttackers ?: emptyList()
                        // The enumerator already worked out who may legally be attacked (CR 802.2a
                        // — opponents plus their planeswalkers, minus anyone this creature can't
                        // attack). Guessing "the other player in turn order" instead is wrong the
                        // moment there is more than one, and a teammate is never a legal defender.
                        val opponentId = attackAction?.validAttackTargets?.firstOrNull()
                        val attackerMap = if (mandatory.isNotEmpty() && opponentId != null) {
                            mandatory.associateWith { opponentId }
                        } else emptyMap()
                        DeclareAttackers(playerId, attackerMap)
                    }
                    current.step == Step.DECLARE_BLOCKERS && !current.isActiveTurnFor(playerId) -> {
                        // Include mandatory blockers to avoid rejection
                        val legalActions = simulator.getLegalActions(current, playerId)
                        val blockerAction = legalActions.find { it.actionType == "DeclareBlockers" }
                        val mandatory = blockerAction?.mandatoryBlockerAssignments ?: emptyMap()
                        val blockerMap = mandatory.mapValues { (_, targets) ->
                            if (targets.isNotEmpty()) listOf(targets.first()) else emptyList()
                        }
                        DeclareBlockers(playerId, blockerMap)
                    }
                    else -> PassPriority(playerId)
                }
                val fallbackResult = processor.process(current, fallback).result
                if (fallbackResult.error != null) break
                current = fallbackResult.state
                iterations++
                break
            }
            current = result.state
            iterations++

            // If we chose to pass priority, stop
            if (action is PassPriority) break
        }

        return current
    }

    companion object {
        /**
         * Create an AI player with the default evaluation weights.
         *
         * @param advisorModules Per-set card advisor modules. Each module registers
         *   [CardAdvisor]s that override generic AI behavior for specific cards.
         */
        fun create(
            cardRegistry: CardRegistry,
            playerId: EntityId,
            advisorModules: List<CardAdvisorModule> = emptyList()
        ): AIPlayer = create(cardRegistry, playerId, AiProfile.CURRENT.copy(advisorModules = advisorModules))

        /**
         * Create an AI player from a named [AiProfile] — the single constructor everything else
         * delegates to. The profile is what an arena report identifies an agent by, so two runs
         * quoting the same profile id are comparing the same thing.
         */
        fun create(
            cardRegistry: CardRegistry,
            playerId: EntityId,
            profile: AiProfile,
            opponentModels: Map<EntityId, OpponentModel> = emptyMap(),
            /**
             * Local testing mode: where the [Strategist]'s per-candidate scores are published
             * instead of being discarded. Null (the default) is production — no recording.
             */
            insightSink: AiInsightSink? = null,
        ): AIPlayer {
            val advisorRegistry = CardAdvisorRegistry()
            profile.advisorModules.forEach { it.register(advisorRegistry) }

            // Phase 6. `IntentCatalog.NONE` answers nothing, and every consumer falls back to its
            // pre-Phase-6 behaviour — so a profile that doesn't opt in is untouched.
            val intents = if (profile.useCardIntent) IntentCatalog.of(cardRegistry) else IntentCatalog.NONE

            val simulator = GameSimulator(
                cardRegistry,
                resolveThroughCombatDamage = profile.resolveThroughCombatDamage,
            )
            // Raw Phase 9 vectors contain CardIntent-derived facts even when the surrounding
            // legacy-based arena profile deliberately keeps Phase 6 timing/targeting behavior off.
            // Give only the evaluator the full catalog so the fitted training schema and runtime
            // schema agree without smuggling other AI improvements into the A/B.
            val evaluationIntents = if (EvalWeights.isRawProfile(profile.evalWeightsId)) {
                IntentCatalog.of(cardRegistry)
            } else {
                intents
            }
            val evaluator = EvalWeights.resolveEvaluator(
                profile.evalWeightsId,
                evaluationIntents,
                landDropIsNotCardLoss = profile.landDropIsNotCardLoss,
                sequenceLandsByUsableMana = profile.sequenceLandsByUsableMana,
                discountedRaceClock = profile.discountedRaceClock,
                creatureValuation = profile.creatureValuation,
                priceLandsInHandAsMana = profile.priceLandsInHandAsMana,
            )
            val combatAdvisor = CombatAdvisor(
                simulator, evaluator, cardRegistry, advisorRegistry,
                priceCrackBackAsLife = profile.priceCrackBackAsLife,
                lifeWeight = EvalWeights.resolve(profile.evalWeightsId).life,
            )
            val responder = DecisionResponder(
                simulator, evaluator,
                advisorRegistry = advisorRegistry,
                budgetPolicy = profile.budgetPolicy,
                intents = intents,
            )

            // Wire up the decision resolver so simulations can resolve non-trivial
            // decisions (modal spells, gift choices, etc.) instead of returning
            // NeedsDecision with an unresolved board state.
            simulator.decisionResolver = { state, decision ->
                responder.respond(state, decision, decision.playerId)
            }

            return AIPlayer(
                playerId = playerId,
                simulator = simulator,
                evaluator = evaluator,
                strategist = Strategist(
                    simulator, evaluator,
                    combatAdvisor = combatAdvisor,
                    advisorRegistry = advisorRegistry,
                    useMeaningfulFilter = profile.useMeaningfulFilter,
                    budgetPolicy = profile.budgetPolicy,
                    intents = intents,
                    combatTricksWaitForBlocks = profile.combatTricksWaitForBlocks,
                    holdRemovalForBetterTargets = profile.holdRemovalForBetterTargets,
                    holdCountersForBetterSpells = profile.holdCountersForBetterSpells,
                    cashCantripsInTheEndStep = profile.cashCantripsInTheEndStep,
                    holdFlashPermanentsForAmbush = profile.holdFlashPermanentsForAmbush,
                    holdExpiringGrantsForCombat = profile.holdExpiringGrantsForCombat,
                    // Same seam as `CombatAdvisor`'s `lifeWeight`: a raw Phase 9 profile resolves
                    // to the compiled fallback here, which is the right answer for a policy that
                    // only needs to know what a point of board value trades against.
                    boardPresenceWeight = EvalWeights.resolve(profile.evalWeightsId).boardPresence,
                    candidateEvaluator = candidateEvaluatorFor(
                        cardRegistry, profile, evaluator, advisorRegistry, intents,
                        EvalWeights.winProbabilityScale(profile.evalWeightsId),
                    ),
                    stateSampler = if (profile.determinizeHiddenInformation) {
                        val determinizer = Determinizer(cardRegistry)
                        val sampler: (GameState, EntityId) -> GameState = { state, viewer ->
                            determinizer.sampleForSearch(state, viewer, opponentModels)
                        }
                        sampler
                    } else {
                        null
                    },
                    insightSink = insightSink,
                ),
                responder = responder,
                useMeaningfulFilter = profile.useMeaningfulFilter,
            )
        }

        /**
         * Phase 7. The rollout stack, or the pre-Phase-7 static leaf when the profile doesn't ask
         * for one.
         *
         * The [PlayoutEngine] gets its **own** processor, enumerator, `GameSimulator` and
         * `CombatAdvisor` rather than sharing the Strategist's. `GameSimulator.isResolving` is a
         * mutable recursion guard and `decisionResolver` is a mutable `var`, so a playout running
         * inside a simulation on a shared instance would corrupt the guard of the very simulation
         * that spawned it. The playout's `CombatAdvisor` is only ever asked for its heuristic seed
         * (`useSimulation = false`), so its simulator is never actually driven — it exists so the
         * blocking heuristic has one implementation rather than two.
         */
        private fun candidateEvaluatorFor(
            cardRegistry: CardRegistry,
            profile: AiProfile,
            evaluator: BoardEvaluator,
            advisorRegistry: CardAdvisorRegistry,
            intents: IntentCatalog,
            winProbabilityScale: Double,
        ): CandidateEvaluator {
            val settings = profile.rollouts ?: return StaticCandidateEvaluator(evaluator)
            val playoutCombat = CombatAdvisor(
                GameSimulator(cardRegistry), evaluator, cardRegistry, advisorRegistry
            )
            val engine = PlayoutEngine(
                cardRegistry = cardRegistry,
                evaluator = evaluator,
                policy = PlayoutPolicy(playoutCombat, intents, settings, cardRegistry),
                decisions = FastDecisionResponder(intents),
                settings = settings,
                winProbabilityScale = winProbabilityScale,
            )
            return RolloutCandidateEvaluator(
                playouts = engine,
                staticEvaluator = evaluator,
                settings = settings,
                winProbabilityScale = winProbabilityScale,
            )
        }

        /**
         * Default evaluator: weighted combination of strategic dimensions.
         *
         * The weights, and why each is what it is, live on [EvaluationWeights.DEFAULT].
         */
        fun defaultEvaluator(): BoardEvaluator = EvaluationWeights.DEFAULT.toEvaluator()
    }
}
