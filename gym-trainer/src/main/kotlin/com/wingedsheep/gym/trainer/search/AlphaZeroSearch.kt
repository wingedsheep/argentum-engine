package com.wingedsheep.gym.trainer.search

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.BudgetModalDecision
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
import com.wingedsheep.engine.core.GameAction
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
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.BatchYesNoResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.trainer.defaults.ExactStructuredDecisionExpander
import com.wingedsheep.gym.trainer.spi.ActionFeaturizer
import com.wingedsheep.gym.trainer.spi.Evaluator
import com.wingedsheep.gym.trainer.spi.StateFeaturizer
import com.wingedsheep.gym.trainer.spi.StructuredDecisionExpander
import com.wingedsheep.gym.trainer.spi.StructuredDecisionExpansion
import com.wingedsheep.gym.trainer.spi.StructuredDecisionResolver
import com.wingedsheep.gym.trainer.spi.TrainerContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random
import java.util.Random as JavaRandom

/**
 * AlphaZero-style MCTS over a [GameEnvironment].
 *
 * ## Algorithm (one simulation)
 *
 * 1. **Select** — starting at the root, repeatedly pick the child edge that
 *    maximises `PUCT = Q + c_puct * prior * sqrt(parent.visits) / (1 + edge.visits)`.
 *    Q is stored from the *root player's* perspective; the sign is flipped
 *    when the node's acting player is not the root player so the opponent
 *    picks moves that minimise root value.
 * 2. **Expand** — at a leaf (not yet terminal), enumerate outgoing edges
 *    from the engine:
 *      - At a priority state → every [LegalAction] becomes an edge.
 *      - At a simple pending decision (YesNo, ChooseNumber, single-mode
 *        ChooseMode, ChooseColor, ChooseOption, single-select SelectCards)
 *        → every folded [DecisionResponse] becomes an edge.
 *      - At a structured pending decision → [StructuredDecisionExpander]
 *        may expose a complete response set as independent edges. Unsupported
 *        families retain a single policy-selected [StructuredDecisionResolver]
 *        fallback edge.
 *    Then call the [Evaluator] to get priors and value.
 * 3. **Simulate (value)** — at terminal, use the game outcome; at an
 *    expanded leaf, use the evaluator's value (from the acting player's
 *    perspective, flipped into the root-player frame).
 * 4. **Backprop** — push the leaf value up every node/edge on the path,
 *    updating visits and mean value.
 *
 * ## Thread-safety
 *
 * An instance owns a single working [GameEnvironment] and is **not**
 * thread-safe. For parallel search, create one instance per worker thread
 * (envs fork in O(1) — [GameEnvironment.fork]).
 *
 * @param env the root env — its current state is the root of the search
 * @param featurizer state → feature representation fed to [evaluator]
 * @param actionFeaturizer action → `(head, slot)` policy index
 * @param evaluator priors + value provider; typically a remote NN
 * @param structuredResolver called on complex engine decisions the
 *        [structuredExpander] does not support; this is a policy fallback
 * @param cPuct exploration constant; `1.0` is a sensible default
 * @param dirichletAlpha optional Dirichlet noise alpha applied to root
 *        priors only; `null` disables noise
 * @param dirichletWeight `(1-w) * prior + w * dirichlet` mix weight
 * @param rng seed for noise sampling
 * @param structuredExpander policy-free source of complete structured
 *        response sets; defaults to exact required/optional single targets and unique orderings
 *        or library reorders through four objects (24 responses). That width is a search-budget
 *        ceiling, not an engine legality rule — pass
 *        `ExactStructuredDecisionExpander(maxOrderingResponses = n)` to trade ordering breadth
 *        against the per-move simulation count. Larger or duplicate shapes are Unsupported,
 *        never partial.
 */
class AlphaZeroSearch<T>(
    private val env: GameEnvironment,
    private val featurizer: StateFeaturizer<T>,
    private val actionFeaturizer: ActionFeaturizer,
    private val evaluator: Evaluator<T>,
    private val structuredResolver: StructuredDecisionResolver = RandomStructuredResolver(),
    private val cPuct: Double = 1.0,
    private val dirichletAlpha: Double? = null,
    private val dirichletWeight: Double = 0.25,
    private val rng: Random = Random.Default,
    private val structuredExpander: StructuredDecisionExpander = ExactStructuredDecisionExpander.Default
) {
    private val workingEnv: GameEnvironment = env.fork()
    private val javaRng: JavaRandom = JavaRandom(rng.nextLong())

    /**
     * Run [simulations] rollouts from the current env state, returning the
     * visit-count distribution at the root plus the MCTS-estimated root
     * value.
     */
    fun run(simulations: Int): MctsSearchResult {
        require(simulations > 0) { "simulations must be positive" }

        val rootState = env.state
        val rootPlayer = env.agentToAct
            ?: error("No agent to act at root — game is over or not reset")

        val root = buildNode(rootState, env.playerIds, env.agentToAct, env.pendingDecision, env.isTerminal, env.winnerId, rootPlayer)
        check(!root.terminal) { "Root is terminal; nothing to search" }

        expand(root, rootPlayer, addRootNoise = dirichletAlpha != null)

        repeat(simulations) { simulate(root, rootPlayer) }
        return MctsSearchResult(root, rootPlayer)
    }

    // =========================================================================
    // Selection
    // =========================================================================

    private fun simulate(root: MctsNode, rootPlayer: EntityId) {
        val nodePath = ArrayList<MctsNode>(16)
        val edgePath = ArrayList<MctsEdge>(16)
        var node = root
        nodePath += node

        while (!node.isLeaf && !node.terminal) {
            val edge = selectEdge(node, rootPlayer)
            edgePath += edge
            if (edge.child == null) edge.child = createChild(node, edge, rootPlayer)
            node = edge.child!!
            nodePath += node
        }

        val leafValue: Float = if (node.terminal) {
            terminalValue(node.winnerId, rootPlayer)
        } else {
            expand(node, rootPlayer, addRootNoise = false)
        }

        for (n in nodePath) {
            n.visits += 1
            n.valueSum += leafValue
        }
        for (e in edgePath) {
            e.visits += 1
            e.meanValue += (leafValue - e.meanValue) / e.visits
        }
    }

    private fun selectEdge(node: MctsNode, rootPlayer: EntityId): MctsEdge {
        val sqrtN = sqrt(node.visits.coerceAtLeast(1).toDouble())
        val sign = if (node.agentToAct == rootPlayer) +1.0 else -1.0

        var best: MctsEdge? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (e in node.edges) {
            val q = sign * e.meanValue
            val u = cPuct * e.prior * sqrtN / (1.0 + e.visits)
            val score = q + u
            if (score > bestScore) { bestScore = score; best = e }
        }
        return best ?: error("No edges to select from at node")
    }

    // =========================================================================
    // Expansion
    // =========================================================================

    private fun expand(node: MctsNode, rootPlayer: EntityId, addRootNoise: Boolean): Float {
        val acting = node.agentToAct
            ?: error("Expanding a terminal node — should be caught earlier")

        val edges = enumerateEdges(node, acting)
        check(edges.isNotEmpty()) {
            val source = node.pendingDecision?.let { it::class.simpleName } ?: "priority state"
            "No search edges for $source"
        }
        node.edges = edges

        val ctx = TrainerContext(node.state, acting, node.pendingDecision)
        val features = featurizer.featurize(ctx)
        val legalSlots = edges.map { it.slot }
        val result = evaluator.evaluate(features, legalSlots, ctx)

        // Pull per-edge priors out of the head arrays and normalise over legal slots.
        val raw = FloatArray(edges.size)
        var sum = 0f
        for ((i, edge) in edges.withIndex()) {
            val headArr = result.priors[edge.slot.head]
            val p = if (headArr != null && edge.slot.slot in headArr.indices) headArr[edge.slot.slot] else 0f
            val safe = if (p.isFinite() && p > 0f) p else 0f
            raw[i] = safe
            sum += safe
        }
        if (sum <= 1e-8f) {
            // Uniform fallback — evaluator produced nothing usable.
            val u = 1f / edges.size.coerceAtLeast(1)
            for (i in edges.indices) edges[i].prior = u
        } else {
            for (i in edges.indices) edges[i].prior = raw[i] / sum
        }

        if (addRootNoise && dirichletAlpha != null && edges.size > 1) {
            applyDirichletNoise(edges, dirichletAlpha, dirichletWeight)
        }

        // Evaluator returns value from the acting player's perspective.
        // Flip into root-player frame.
        return if (acting == rootPlayer) result.value else -result.value
    }

    private fun enumerateEdges(node: MctsNode, acting: EntityId): List<MctsEdge> {
        val pending = node.pendingDecision
        val ctx = TrainerContext(node.state, acting, pending)

        if (pending == null || pending is com.wingedsheep.engine.core.PlayCardDecision) {
            workingEnv.restore(node.state, node.playerIds, 0)
            val legal = workingEnv.legalActions()
            return legal.filter { pending == null || (it.affordable && !it.hasUnfillableTargetRequirement) }.map { la ->
                val action = if (pending is com.wingedsheep.engine.core.PlayCardDecision)
                    SubmitDecision(pending.playerId, com.wingedsheep.engine.core.PlayCardResponse(pending.id, la.action))
                    else la.action
                MctsEdge(
                    action = action,
                    legalAction = la,
                    slot = actionFeaturizer.slot(la.action, ctx)
                )
            }
        }

        val foldedResponses = foldSimpleDecision(pending)
        if (foldedResponses != null) {
            return foldedResponses.map { response -> responseEdge(pending, response, ctx) }
        }

        return when (val expansion = structuredExpander.expand(node.state, pending)) {
            is StructuredDecisionExpansion.Complete -> {
                val edges = completeStructuredEdges(node.state, pending, expansion.responses, ctx)
                check(edges.isNotEmpty()) {
                    "Complete response expansion for ${pending::class.simpleName} was empty " +
                        "at a non-terminal search node"
                }
                edges
            }

            StructuredDecisionExpansion.Unsupported -> {
                val response = structuredResolver.resolve(node.state, pending)
                listOf(
                    validatedResponseEdge(
                        state = node.state,
                        decision = pending,
                        response = response,
                        ctx = ctx,
                        source = "Structured decision resolver"
                    )
                )
            }
        }
    }

    private fun completeStructuredEdges(
        state: GameState,
        decision: PendingDecision,
        responses: List<DecisionResponse>,
        ctx: TrainerContext
    ): List<MctsEdge> {
        val seen = mutableSetOf<DecisionResponse>()
        return responses.map { response ->
            check(seen.add(response)) {
                "Structured expander returned duplicate ${response::class.simpleName}: $response"
            }
            validatedResponseEdge(
                state = state,
                decision = decision,
                response = response,
                ctx = ctx,
                source = "Structured decision expander"
            )
        }
    }

    private fun validatedResponseEdge(
        state: GameState,
        decision: PendingDecision,
        response: DecisionResponse,
        ctx: TrainerContext,
        source: String
    ): MctsEdge {
        check(response.decisionId == decision.id) {
            "$source returned response for ${response.decisionId}, expected ${decision.id}"
        }
        val validationError = DecisionValidators.validate(decision, response, state)
        check(validationError == null) {
            "$source returned an illegal ${response::class.simpleName}: $validationError"
        }
        return responseEdge(decision, response, ctx)
    }

    private fun responseEdge(
        decision: PendingDecision,
        response: DecisionResponse,
        ctx: TrainerContext
    ): MctsEdge {
        val submit = SubmitDecision(decision.playerId, response)
        return MctsEdge(
            action = submit,
            legalAction = null,
            slot = actionFeaturizer.slot(submit, ctx)
        )
    }

    /**
     * Returns the list of concrete [DecisionResponse]s when the pending
     * decision folds cleanly into a discrete action space; returns `null`
     * when the structured expander or resolver owns it.
     */
    private fun foldSimpleDecision(d: PendingDecision): List<DecisionResponse>? = when (d) {
        is YesNoDecision -> listOf(YesNoResponse(d.id, true), YesNoResponse(d.id, false))
        // The batched may-question folds to two whole-run actions; per-instance peel-off isn't an
        // MCTS action (the search treats the run as one decision, matching the AI heuristic).
        is BatchYesNoDecision -> listOf(
            BatchYesNoResponse(d.id, choice = true, applyToAll = true),
            BatchYesNoResponse(d.id, choice = false, applyToAll = true),
        )
        is ChooseNumberDecision -> (d.minValue..d.maxValue).map { NumberChosenResponse(d.id, it) }
        is ChooseColorDecision -> d.availableColors.map { ColorChosenResponse(d.id, it) }
        is ChooseOptionDecision -> d.options.indices.map { OptionChosenResponse(d.id, it) }
        is ChooseModeDecision ->
            if (d.minModes == 1 && d.maxModes == 1)
                d.modes.filter { it.available }.map { ModesChosenResponse(d.id, listOf(it.index)) }
            else null
        is SelectCardsDecision ->
            if (d.minSelections == 1 && d.maxSelections == 1 && !d.ordered)
                d.options.map { CardsSelectedResponse(d.id, listOf(it)) }
            else null
        is com.wingedsheep.engine.core.PlayCardDecision,
        is ChooseTargetsDecision,
        is ChooseReplacementDecision,
        is DistributeDecision,
        is OrderObjectsDecision,
        is SplitPilesDecision,
        is SearchLibraryDecision,
        is ReorderLibraryDecision,
        is AssignDamageDecision,
        is CombatResolutionDecision,
        is SelectManaSourcesDecision,
        is BudgetModalDecision -> null
    }

    private fun createChild(parent: MctsNode, edge: MctsEdge, rootPlayer: EntityId): MctsNode {
        workingEnv.restore(parent.state, parent.playerIds, 0)
        workingEnv.step(edge.action)
        if (edge.action is SubmitDecision) {
            check(workingEnv.lastRejection == null) {
                "Search decision edge was rejected by the engine: ${workingEnv.lastRejection}"
            }
        }
        return buildNode(
            workingEnv.state,
            workingEnv.playerIds,
            workingEnv.agentToAct,
            workingEnv.pendingDecision,
            workingEnv.isTerminal,
            workingEnv.winnerId,
            rootPlayer
        )
    }

    private fun buildNode(
        state: GameState,
        playerIds: List<EntityId>,
        agentToAct: EntityId?,
        pending: PendingDecision?,
        terminal: Boolean,
        winnerId: EntityId?,
        @Suppress("UNUSED_PARAMETER") rootPlayer: EntityId
    ): MctsNode = MctsNode(
        state = state,
        playerIds = playerIds,
        agentToAct = agentToAct,
        pendingDecision = pending,
        terminal = terminal,
        terminalValue = 0f,
        winnerId = winnerId
    )

    // =========================================================================
    // Noise
    // =========================================================================

    private fun applyDirichletNoise(edges: List<MctsEdge>, alpha: Double, weight: Double) {
        val noise = sampleDirichlet(edges.size, alpha)
        for ((i, edge) in edges.withIndex()) {
            edge.prior = ((1.0 - weight) * edge.prior + weight * noise[i]).toFloat()
        }
    }

    private fun sampleDirichlet(n: Int, alpha: Double): DoubleArray {
        val out = DoubleArray(n)
        var sum = 0.0
        for (i in 0 until n) {
            out[i] = sampleGamma(alpha)
            sum += out[i]
        }
        val denom = if (sum > 0) sum else 1.0
        for (i in 0 until n) out[i] /= denom
        return out
    }

    /** Marsaglia-Tsang gamma sampler; boost path for alpha < 1. */
    private fun sampleGamma(alpha: Double): Double {
        if (alpha < 1.0) {
            val g = sampleGamma(alpha + 1.0)
            val u = javaRng.nextDouble()
            return g * Math.pow(u, 1.0 / alpha)
        }
        val d = alpha - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        while (true) {
            var x: Double
            var v: Double
            do {
                x = javaRng.nextGaussian()
                v = 1.0 + c * x
            } while (v <= 0)
            v = v * v * v
            val u = javaRng.nextDouble()
            val x2 = x * x
            if (u < 1.0 - 0.0331 * x2 * x2) return d * v
            if (ln(u) < 0.5 * x2 + d * (1.0 - v + ln(v))) return d * v
        }
    }

    // =========================================================================
    // Terminal values
    // =========================================================================

    private fun terminalValue(winnerId: EntityId?, rootPlayer: EntityId): Float = when (winnerId) {
        rootPlayer -> 1f
        null -> 0f
        else -> -1f
    }
}

/** Summary of a finished MCTS call — the root + visit distribution over its edges. */
class MctsSearchResult(
    val root: MctsNode,
    val rootPlayer: EntityId
) {
    /** Visit count per edge, in the same order as `root.edges`. */
    val visits: IntArray
        get() = IntArray(root.edges.size) { root.edges[it].visits }

    /** The single most-visited edge (argmax of [visits]); ties broken by first-seen. */
    val bestEdge: MctsEdge? get() = root.edges.maxByOrNull { it.visits }

    /** MCTS-estimated value at the root, from [rootPlayer]'s perspective. */
    val rootValue: Float get() = root.meanValue.toFloat()
}

/**
 * Default [StructuredDecisionResolver] — picks a random minimum-cardinality
 * response for the target, card-selection, and mode families it supports.
 * This is a fallback policy, not uniform enumeration of the legal response
 * space. Replace it with a domain policy when unsupported decisions can occur.
 *
 * Minimum cardinality on its own does not satisfy the state-dependent restrictions the engine's
 * validator enforces — "with different names", "one per card type", a total-mana-value cap. Rather
 * than re-implement constraint solving here, the resolver draws up to [attempts] random candidates
 * and returns the first the engine accepts. When none is accepted it returns the last draw, so the
 * caller fails loudly against the authoritative validator instead of quietly submitting an illegal
 * response.
 *
 * @param attempts how many candidates to draw before giving up; each draw beyond the first costs one
 *        [DecisionValidators] call
 */
class RandomStructuredResolver(
    private val rng: Random = Random.Default,
    private val attempts: Int = 16
) : StructuredDecisionResolver {

    init {
        require(attempts >= 1) { "attempts must be positive" }
    }

    override fun resolve(state: GameState, decision: PendingDecision): DecisionResponse {
        var candidate = draw(decision)
        repeat(attempts - 1) {
            if (DecisionValidators.validate(decision, candidate, state) == null) return candidate
            candidate = draw(decision)
        }
        return candidate
    }

    /**
     * One unvalidated minimum-cardinality candidate. Minimal coverage — handles the common
     * structured decisions. Unknown decisions fall through to an exception the caller can see.
     */
    private fun draw(decision: PendingDecision): DecisionResponse = when (decision) {
        is ChooseTargetsDecision -> {
            val picked = decision.targetRequirements.associate { req ->
                val legal = decision.legalTargets[req.index].orEmpty()
                // `take` clamps on its own; a requirement asking for more targets than are legal is
                // unsatisfiable, and an under-count that fails validation says so louder than a
                // silently shrunk selection would.
                req.index to legal.shuffled(rng).take(req.minTargets)
            }
            com.wingedsheep.engine.core.TargetsResponse(decision.id, picked)
        }
        is SelectCardsDecision -> {
            val n = decision.minSelections.coerceAtLeast(1)
                .coerceAtMost(minOf(decision.maxSelections, decision.options.size))
            val picked = decision.options.shuffled(rng).take(n)
            CardsSelectedResponse(decision.id, picked)
        }
        is ChooseModeDecision -> {
            val available = decision.modes.filter { it.available }
            val n = decision.minModes.coerceAtLeast(1)
                .coerceAtMost(minOf(decision.maxModes, available.size))
            val picked = available.shuffled(rng).take(n).map { it.index }
            ModesChosenResponse(decision.id, picked)
        }
        else -> throw UnsupportedOperationException(
            "RandomStructuredResolver does not yet handle ${decision::class.simpleName}; " +
                "provide a custom StructuredDecisionResolver"
        )
    }
}
