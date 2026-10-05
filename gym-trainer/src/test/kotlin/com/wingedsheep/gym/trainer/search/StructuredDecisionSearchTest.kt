package com.wingedsheep.gym.trainer.search

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.MoveCollectionOrderContinuation
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.trainer.spi.ActionFeaturizer
import com.wingedsheep.gym.trainer.spi.EvaluationResult
import com.wingedsheep.gym.trainer.spi.Evaluator
import com.wingedsheep.gym.trainer.spi.PolicyHead
import com.wingedsheep.gym.trainer.spi.SlotEncoding
import com.wingedsheep.gym.trainer.spi.StateFeaturizer
import com.wingedsheep.gym.trainer.spi.StructuredDecisionExpander
import com.wingedsheep.gym.trainer.spi.StructuredDecisionExpansion
import com.wingedsheep.gym.trainer.spi.StructuredDecisionResolver
import com.wingedsheep.gym.trainer.spi.TrainerContext
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val ORDERING_HEAD = "ordering"

/**
 * A spell whose target is chosen *while it resolves* — a pipeline `selectTarget` — so casting it
 * leaves the game paused on a [ChooseTargetsDecision] for the search to expand. No printed card
 * should choose a target this late (CR 601.2c), which is why this is a test fixture.
 */
private val resolutionTimeExile = card("Resolution-Time Exile") {
    manaCost = "{5}"
    typeLine = "Instant"
    spell {
        effect = Effects.Pipeline {
            val chosen = selectTarget(
                TargetObject(filter = TargetFilter(GameObjectFilter.Creature), id = "target creature")
            )
            run(Effects.Exile(chosen.asTarget))
        }
    }
}

/** Lexicographic-by-input-index permutations — the same enumeration the expander produces. */
private fun <T> permutationsOf(items: List<T>): List<List<T>> =
    if (items.isEmpty()) listOf(emptyList())
    else items.indices.flatMap { index ->
        permutationsOf(items.filterIndexed { other, _ -> other != index }).map { listOf(items[index]) + it }
    }

class StructuredDecisionSearchTest : FunSpec({

    val stateFeaturizer = object : StateFeaturizer<Unit> {
        override fun featurize(ctx: TrainerContext) = Unit
    }
    val sharedSlotFeaturizer = object : ActionFeaturizer {
        override val heads: List<PolicyHead> = listOf(PolicyHead("shared", 1))

        override fun slot(action: GameAction, ctx: TrainerContext): SlotEncoding =
            SlotEncoding("shared", 0)
    }
    val uniformEvaluator = Evaluator<Unit> { _, _, _ ->
        EvaluationResult(priors = mapOf("shared" to floatArrayOf(1f)), value = 0f)
    }

    fun newDriver(deckCard: String): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + PredefinedTokens.allTokens + resolutionTimeExile)
        initMirrorMatch(
            deck = Deck.of(deckCard to 40),
            skipMulligans = true,
            startingPlayer = 0
        )
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun environment(driver: GameTestDriver): GameEnvironment =
        GameEnvironment.create(driver.cardRegistry).also {
            it.restore(driver.state, listOf(driver.player1, driver.player2))
        }

    test("MCTS exposes exact target responses as independent edges and executes each branch") {
        val driver = newDriver("Mountain")
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val ownCreature = driver.putCreatureOnBattlefield(caster, "Raging Goblin")
        val opposingCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        repeat(5) { driver.putLandOnBattlefield(caster, "Mountain") }
        val spell = driver.putCardInHand(caster, "Resolution-Time Exile")

        driver.castSpell(caster, spell).error shouldBe null
        while (driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()

        val pending = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        pending.legalTargets.values.flatten().toSet() shouldBe setOf(ownCreature, opposingCreature)
        val rootState = driver.state
        val env = environment(driver)

        val fallback = AlphaZeroSearch(
            env = env,
            featurizer = stateFeaturizer,
            actionFeaturizer = sharedSlotFeaturizer,
            evaluator = uniformEvaluator,
            structuredResolver = StructuredDecisionResolver { _, decision ->
                TargetsResponse(decision.id, mapOf(0 to listOf(ownCreature)))
            },
            structuredExpander = StructuredDecisionExpander { _, _ ->
                StructuredDecisionExpansion.Unsupported
            },
            dirichletAlpha = null
        ).run(simulations = 1)
        fallback.root.edges.size shouldBe 1
        (fallback.root.edges.single().action as SubmitDecision).response shouldBe
            TargetsResponse(pending.id, mapOf(0 to listOf(ownCreature)))

        val result = AlphaZeroSearch(
            env = env,
            featurizer = stateFeaturizer,
            actionFeaturizer = sharedSlotFeaturizer,
            evaluator = uniformEvaluator,
            structuredResolver = StructuredDecisionResolver { _, decision ->
                error("exact expansion unexpectedly fell back for ${decision::class.simpleName}")
            },
            dirichletAlpha = null
        ).run(simulations = 2)

        result.root.edges.size shouldBe 2
        result.visits.toList().shouldContainExactly(1, 1)
        result.root.edges.forEach { edge ->
            edge.slot shouldBe SlotEncoding("shared", 0)
            edge.prior.shouldBeExactly(0.5f)
            edge.child shouldNotBe null
        }

        val responsesByTarget = result.root.edges.associate { edge ->
            val response = (edge.action as SubmitDecision).response
                .shouldBeInstanceOf<TargetsResponse>()
            DecisionValidators.validate(pending, response, rootState) shouldBe null
            response.selectedTargets.getValue(0).single() to edge
        }
        responsesByTarget.keys shouldBe setOf(ownCreature, opposingCreature)

        for ((chosenTarget, edge) in responsesByTarget) {
            val branch = GameEnvironment.create(driver.cardRegistry).also {
                it.restore(rootState, listOf(driver.player1, driver.player2))
            }
            branch.step(edge.action)

            branch.lastRejection shouldBe null
            branch.lastStepEvents.filterIsInstance<ZoneChangeEvent>()
                .any { it.entityId == chosenTarget } shouldBe true
            branch.state.getBattlefield().contains(chosenTarget) shouldBe false
            branch.state.getBattlefield().contains(
                if (chosenTarget == ownCreature) opposingCreature else ownCreature
            ) shouldBe true
        }
        responsesByTarget.getValue(ownCreature).child!!.state shouldNotBe
            responsesByTarget.getValue(opposingCreature).child!!.state
    }

    test("default MCTS exposes every small library ordering and selects the best-evaluated one") {
        val driver = newDriver("Mountain")
        val player = driver.activePlayer!!
        val cards = driver.state.getLibrary(player).take(3)
        val orders = permutationsOf(cards)
        orders.size shouldBe 6
        val favoured = orders[4]

        val rootState = driver.state.suspendForDecision(
            question = { id -> ReorderLibraryDecision(
                id = id,
                playerId = player,
                prompt = "Put them back in any order",
                context = DecisionContext(),
                cards = cards,
                cardInfo = emptyMap(),
            ) },
            answer = MoveCollectionOrderContinuation(
                playerId = player,
                sourceId = null,
                sourceName = "Search ordering probe",
                cards = cards,
                destinationZone = Zone.LIBRARY,
                destinationPlayerId = player,
            )
        ).state
        val decision = rootState.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        val env = GameEnvironment.create(driver.cardRegistry).also {
            it.restore(rootState, listOf(driver.player1, driver.player2))
        }

        // One head per ordering slot, so the evaluator can prefer a single permutation. A shared
        // featurizer would collapse all six onto one slot and make "best evaluated" untestable —
        // exactly the gap that a two-object ordering hides.
        val orderingFeaturizer = object : ActionFeaturizer {
            override val heads: List<PolicyHead> =
                listOf(PolicyHead(ORDERING_HEAD, orders.size), PolicyHead("shared", 1))

            override fun slot(action: GameAction, ctx: TrainerContext): SlotEncoding {
                val response = (action as? SubmitDecision)?.response
                val index = (response as? OrderedResponse)?.let { orders.indexOf(it.orderedObjects) } ?: -1
                return if (index >= 0) SlotEncoding(ORDERING_HEAD, index) else SlotEncoding("shared", 0)
            }
        }
        val favouringEvaluator = Evaluator<Unit> { _, _, _ ->
            val orderingPriors = FloatArray(orders.size) { if (it == orders.indexOf(favoured)) 10f else 1f }
            EvaluationResult(
                priors = mapOf(ORDERING_HEAD to orderingPriors, "shared" to floatArrayOf(1f)),
                value = 0f,
            )
        }

        val result = AlphaZeroSearch(
            env = env,
            featurizer = stateFeaturizer,
            actionFeaturizer = orderingFeaturizer,
            evaluator = favouringEvaluator,
            structuredResolver = StructuredDecisionResolver { _, pending ->
                error("exact expansion unexpectedly fell back for ${pending::class.simpleName}")
            },
            dirichletAlpha = null,
        ).run(simulations = 12)

        result.root.edges.size shouldBe 6
        val responses = result.root.edges.map { edge ->
            (edge.action as SubmitDecision).response.shouldBeInstanceOf<OrderedResponse>()
        }
        responses.map { it.orderedObjects }.toSet() shouldBe orders.toSet()
        result.root.edges.map { it.slot }.toSet().size shouldBe 6
        result.visits.sum() shouldBe 12

        val bestEdge = result.bestEdge.shouldNotBeNull()
        (bestEdge.action as SubmitDecision).response
            .shouldBeInstanceOf<OrderedResponse>().orderedObjects shouldBe favoured
        val others = result.root.edges.filter { it !== bestEdge }
        others.forEach { bestEdge.visits shouldBeGreaterThan it.visits }

        // Executed rather than read off the tree: a 10:1 prior leaves the weakest orders unvisited,
        // so the tree holds fewer children than edges. Every edge must still be a distinct, legal,
        // engine-accepted branch.
        val branchStates = result.root.edges.zip(responses).map { (edge, response) ->
            val branch = GameEnvironment.create(driver.cardRegistry).also {
                it.restore(rootState, listOf(driver.player1, driver.player2))
            }
            branch.step(edge.action)

            branch.lastRejection shouldBe null
            branch.state.getLibrary(player).take(3) shouldBe response.orderedObjects
            branch.state
        }
        branchStates.toSet().size shouldBe 6
    }

    test("existing yes-no folding remains a complete two-edge decision") {
        val driver = newDriver("Grizzly Bears")
        val player = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(player, "Forest")
        val train = driver.putCardInHand(player, "Subway Train")
        driver.giveColorlessMana(player, 2)
        driver.giveMana(player, Color.GREEN, 1)

        driver.castSpell(player, train).error shouldBe null
        driver.bothPass()
        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val result = AlphaZeroSearch(
            env = environment(driver),
            featurizer = stateFeaturizer,
            actionFeaturizer = sharedSlotFeaturizer,
            evaluator = uniformEvaluator,
            structuredResolver = StructuredDecisionResolver { _, decision ->
                when (decision) {
                    is SelectManaSourcesDecision ->
                        ManaSourcesSelectedResponse(decision.id, autoPay = true)
                    is SelectCardsDecision ->
                        CardsSelectedResponse(decision.id, decision.options.take(decision.minSelections))
                    else -> error("unexpected fallback for ${decision::class.simpleName}")
                }
            },
            dirichletAlpha = null
        ).run(simulations = 2)

        result.root.edges.size shouldBe 2
        result.root.edges.map { (it.action as SubmitDecision).response::class }
            .all { it == com.wingedsheep.engine.core.YesNoResponse::class } shouldBe true
    }

    test("unsupported multi-card decision keeps one resolver-selected edge") {
        val driver = newDriver("Island")
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Thirst for Identity")
        driver.giveColorlessMana(player, 2)
        driver.giveMana(player, Color.BLUE, 1)

        driver.castSpell(player, spell).error shouldBe null
        while (driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()

        val pending = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pending.maxSelections shouldBe 2
        val selected = pending.options.take(2)

        val wrongIdError = shouldThrow<IllegalStateException> {
            AlphaZeroSearch(
                env = environment(driver),
                featurizer = stateFeaturizer,
                actionFeaturizer = sharedSlotFeaturizer,
                evaluator = uniformEvaluator,
                structuredExpander = StructuredDecisionExpander { _, _ ->
                    StructuredDecisionExpansion.Complete(
                        listOf(CardsSelectedResponse("wrong-decision", selected))
                    )
                },
                dirichletAlpha = null
            ).run(simulations = 1)
        }
        wrongIdError.message.orEmpty() shouldContain
            "Structured decision expander returned response for wrong-decision"

        val invalidError = shouldThrow<IllegalStateException> {
            AlphaZeroSearch(
                env = environment(driver),
                featurizer = stateFeaturizer,
                actionFeaturizer = sharedSlotFeaturizer,
                evaluator = uniformEvaluator,
                structuredExpander = StructuredDecisionExpander { _, _ ->
                    StructuredDecisionExpansion.Complete(
                        listOf(CardsSelectedResponse(pending.id, emptyList()))
                    )
                },
                dirichletAlpha = null
            ).run(simulations = 1)
        }
        invalidError.message.orEmpty() shouldContain "Structured decision expander returned an illegal"

        var emptyResolverCalls = 0
        val emptyError = shouldThrow<IllegalStateException> {
            AlphaZeroSearch(
                env = environment(driver),
                featurizer = stateFeaturizer,
                actionFeaturizer = sharedSlotFeaturizer,
                evaluator = uniformEvaluator,
                structuredResolver = StructuredDecisionResolver { _, _ ->
                    emptyResolverCalls += 1
                    CardsSelectedResponse(pending.id, selected)
                },
                structuredExpander = StructuredDecisionExpander { _, _ ->
                    StructuredDecisionExpansion.Complete(emptyList())
                },
                dirichletAlpha = null
            ).run(simulations = 1)
        }
        emptyError.message.orEmpty() shouldContain "Complete response expansion"
        emptyResolverCalls shouldBe 0

        val duplicateError = shouldThrow<IllegalStateException> {
            AlphaZeroSearch(
                env = environment(driver),
                featurizer = stateFeaturizer,
                actionFeaturizer = sharedSlotFeaturizer,
                evaluator = uniformEvaluator,
                structuredExpander = StructuredDecisionExpander { _, _ ->
                    StructuredDecisionExpansion.Complete(
                        listOf(
                            CardsSelectedResponse(pending.id, selected),
                            CardsSelectedResponse(pending.id, selected)
                        )
                    )
                },
                dirichletAlpha = null
            ).run(simulations = 1)
        }
        duplicateError.message.orEmpty() shouldContain
            "Structured expander returned duplicate CardsSelectedResponse"

        var resolverCalls = 0
        val result = AlphaZeroSearch(
            env = environment(driver),
            featurizer = stateFeaturizer,
            actionFeaturizer = sharedSlotFeaturizer,
            evaluator = uniformEvaluator,
            structuredResolver = StructuredDecisionResolver { _, decision ->
                resolverCalls += 1
                CardsSelectedResponse(decision.id, selected)
            },
            dirichletAlpha = null
        ).run(simulations = 1)

        resolverCalls shouldBeGreaterThan 0
        result.root.edges.size shouldBe 1
        val response: DecisionResponse = (result.root.edges.single().action as SubmitDecision).response
        response shouldBe CardsSelectedResponse(pending.id, selected)
        result.root.edges.single().child shouldNotBe null
    }
})
