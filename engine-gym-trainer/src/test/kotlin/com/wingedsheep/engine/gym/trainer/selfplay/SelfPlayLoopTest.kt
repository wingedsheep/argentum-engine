package com.wingedsheep.engine.gym.trainer.selfplay

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.gym.GameEnvironment
import com.wingedsheep.engine.gym.trainer.defaults.DynamicSlotActionFeaturizer
import com.wingedsheep.engine.gym.trainer.defaults.HeuristicEvaluator
import com.wingedsheep.engine.gym.trainer.defaults.JsonlSelfPlaySink
import com.wingedsheep.engine.gym.trainer.defaults.StructuralFeatures
import com.wingedsheep.engine.gym.trainer.defaults.StructuralStateFeaturizer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines

/**
 * End-to-end smoke test: a full self-play game with defaults, asserting
 * the game completes, JSONL rows are written, and terminal outcome labels
 * are back-patched correctly.
 */
class SelfPlayLoopTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply { register(PortalSet.allCards) }
    fun miniDeck() = Deck.of("Mountain" to 17, "Raging Goblin" to 3)

    fun config() = GameConfig(
        players = listOf(
            PlayerConfig("Alice", miniDeck()),
            PlayerConfig("Bob", miniDeck())
        ),
        skipMulligans = true,
        startingPlayerIndex = 0
    )

    test("self-play with defaults runs a full game and writes a JSONL row per move") {
        val tmpDir = createTempDirectory("mz-trainer-test-")
        val outPath = tmpDir.resolve("selfplay.jsonl")

        val sink = JsonlSelfPlaySink(
            path = outPath,
            featureSerializer = StructuralFeatures.serializer(),
            append = false
        )

        val reg = registry()
        val loop = SelfPlayLoop(
            envFactory = { GameEnvironment.create(reg) },
            featurizer = StructuralStateFeaturizer(),
            actionFeaturizer = DynamicSlotActionFeaturizer(headSize = 256),
            evaluator = HeuristicEvaluator(),
            sink = sink,
            simulationsPerMove = 4,  // tiny — we're testing plumbing, not play strength
            dirichletAlpha = null,   // skip noise to keep the test deterministic-ish
            temperature = 0.0,       // argmax from move 0
            maxSteps = 300
        )

        val outcome = loop.playGame(config(), gameId = "test-1")
        sink.close()

        outcome.stepCount shouldBeGreaterThan 0
        (outcome.winner != null || outcome.truncated).shouldBeTrue()

        val lines = outPath.readLines()
        lines.size shouldBeGreaterThan 0
        // Every line should have the outcome label back-patched.
        lines.forEach { it.contains("\"outcome\":").shouldBeTrue() }
    }
})
