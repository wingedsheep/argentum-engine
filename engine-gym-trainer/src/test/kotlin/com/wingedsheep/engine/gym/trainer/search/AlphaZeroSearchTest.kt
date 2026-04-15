package com.wingedsheep.engine.gym.trainer.search

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.gym.GameEnvironment
import com.wingedsheep.engine.gym.trainer.defaults.DynamicSlotActionFeaturizer
import com.wingedsheep.engine.gym.trainer.defaults.HeuristicEvaluator
import com.wingedsheep.engine.gym.trainer.defaults.StructuralFeatures
import com.wingedsheep.engine.gym.trainer.defaults.StructuralStateFeaturizer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
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
        val reg = CardRegistry().apply { register(PortalSet.allCards) }
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
