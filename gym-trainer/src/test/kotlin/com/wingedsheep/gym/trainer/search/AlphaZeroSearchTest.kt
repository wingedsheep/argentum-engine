package com.wingedsheep.gym.trainer.search

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.trainer.defaults.DynamicSlotActionFeaturizer
import com.wingedsheep.gym.trainer.defaults.HeuristicEvaluator
import com.wingedsheep.gym.trainer.defaults.StructuralFeatures
import com.wingedsheep.gym.trainer.defaults.StructuralStateFeaturizer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [AlphaZeroSearch] — exercises expansion, PUCT selection,
 * visit accounting, and the Dirichlet noise path.
 */
class AlphaZeroSearchTest : FunSpec({

    fun setupRoot(): GameEnvironment {
        val reg = CardRegistry().apply {
            register(PortalSet.cards)
            register(PortalSet.basicLands)
        }
        val env = GameEnvironment.create(reg)
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", Deck.of("Mountain" to 17, "Raging Goblin" to 3)),
                    PlayerConfig("Bob", Deck.of("Mountain" to 17, "Raging Goblin" to 3))
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return env
    }

    test("run expands the root and sums visits to exactly `simulations`") {
        val env = setupRoot()
        val search = AlphaZeroSearch<StructuralFeatures>(
            env = env,
            featurizer = StructuralStateFeaturizer(),
            actionFeaturizer = DynamicSlotActionFeaturizer(headSize = 128),
            evaluator = HeuristicEvaluator(),
            dirichletAlpha = null
        )
        val result = search.run(simulations = 8)

        result.root.edges.shouldNotBeEmpty()
        result.root.visits shouldBeGreaterThan 0
        // Every simulation adds one visit to the root plus one to every
        // descendant on its path; sum of edge visits == simulations.
        result.visits.sum() shouldBe 8
    }

    test("forced play expands normal land choices without a structured resolver") {
        val env = setupRoot()
        val player = env.state.activePlayerId!!
        val land = env.state.getHand(player).first { id ->
            env.state.getEntity(id)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name == "Mountain"
        }
        val services = com.wingedsheep.engine.core.EngineServices(env.cardRegistry)
        val forced = services.effectExecutorRegistry.execute(env.state,
            com.wingedsheep.sdk.dsl.Effects.ForcePlay("chosen"),
            com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = player,
                pipeline = com.wingedsheep.engine.handlers.PipelineState(storedCollections = mapOf("chosen" to listOf(land)))))
        (forced.pendingDecision is com.wingedsheep.engine.core.PlayCardDecision).shouldBeTrue()
        env.restore(forced.state, env.playerIds)
        val search = AlphaZeroSearch<StructuralFeatures>(env = env,
            featurizer = StructuralStateFeaturizer(),
            actionFeaturizer = DynamicSlotActionFeaturizer(headSize = 128),
            evaluator = HeuristicEvaluator(), dirichletAlpha = null)
        val result = search.run(simulations = 2)
        result.root.edges.shouldNotBeEmpty()
        result.root.edges.all { edge ->
            val submit = edge.action as? com.wingedsheep.engine.core.SubmitDecision
            val play = submit?.response as? com.wingedsheep.engine.core.PlayCardResponse
            (play?.action as? com.wingedsheep.engine.core.PlayLand)?.cardId == land
        }.shouldBeTrue()
    }

    test("bestEdge is the most-visited edge") {
        val env = setupRoot()
        val search = AlphaZeroSearch<StructuralFeatures>(
            env = env,
            featurizer = StructuralStateFeaturizer(),
            actionFeaturizer = DynamicSlotActionFeaturizer(headSize = 128),
            evaluator = HeuristicEvaluator(),
            dirichletAlpha = null
        )
        val result = search.run(simulations = 12)
        val best = result.bestEdge
        (best != null && result.root.edges.all { it.visits <= best.visits }).shouldBeTrue()
    }

    test("enabling Dirichlet noise is accepted and does not break search") {
        // Sanity test only — verifying the exact prior perturbation needs a
        // root state with multiple legal actions, and the opening priority
        // step often has just one (Pass). The self-play integration test
        // exercises the noise path on real states.
        val env = setupRoot()
        val result = AlphaZeroSearch<StructuralFeatures>(
            env = env,
            featurizer = StructuralStateFeaturizer(),
            actionFeaturizer = DynamicSlotActionFeaturizer(headSize = 128),
            evaluator = HeuristicEvaluator(),
            dirichletAlpha = 0.3,
            dirichletWeight = 0.25
        ).run(simulations = 4)
        result.visits.sum() shouldBe 4
    }
})
