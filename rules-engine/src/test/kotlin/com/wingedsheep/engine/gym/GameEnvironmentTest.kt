package com.wingedsheep.engine.gym

import com.wingedsheep.engine.ai.buildHeuristicSealedDeck
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.bloomburrow.BloomburrowSet
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class GameEnvironmentTest : FunSpec({

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(PortalSet.allCards)
        registry.register(BloomburrowSet.allCards)
        return registry
    }

    fun simpleDeck() = Deck.of("Mountain" to 17, "Raging Goblin" to 3)

    /** Build a sealed deck from a random Bloomburrow pool (6 "boosters" of 15 cards). */
    fun sealedDeck(): Deck {
        val pool = BloomburrowSet.allCards.shuffled().take(90)
        val deckMap = buildHeuristicSealedDeck(pool)
        val cards = deckMap.flatMap { (name, count) -> List(count) { name } }
        return Deck(cards)
    }

    test("create returns a usable environment") {
        val env = GameEnvironment.create(createRegistry())
        env.playerIds shouldBe emptyList()
        env.isTerminal.shouldBeFalse()
    }

    test("reset initializes a game with two players") {
        val env = GameEnvironment.create(createRegistry())
        val result = env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        env.playerIds.size shouldBe 2
        env.isTerminal.shouldBeFalse()
        env.turnNumber shouldBe 1
        result.terminated.shouldBeFalse()
        result.agentToAct.shouldNotBeNull()
    }

    test("legalActions returns non-empty list when player has priority") {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        val actions = env.legalActions()
        actions.shouldNotBeEmpty()
        actions.any { it.action is PassPriority }.shouldBeTrue()
    }

    test("step advances the game state") {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        val initialStep = env.stepCount
        val actions = env.legalActions()
        val result = env.step(actions.first().action)

        env.stepCount shouldBe initialStep + 1
        result.state.shouldNotBeNull()
    }

    test("fork creates an independent copy") {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        val forked = env.fork()

        forked.state shouldBe env.state
        forked.playerIds shouldBe env.playerIds
        forked.stepCount shouldBe env.stepCount

        val originalState = env.state
        val actions = forked.legalActions()
        if (actions.isNotEmpty()) {
            forked.step(actions.first().action)
            env.state shouldBe originalState
        }
    }

    test("evaluate returns finite scores for non-terminal states") {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        val score = env.evaluate(env.playerIds[0])
        score.isFinite().shouldBeTrue()
    }

    test("terminalRewards returns zeros during game") {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        val rewards = env.terminalRewards()
        rewards.size shouldBe 2
        rewards.values.all { it == 0.0 }.shouldBeTrue()
    }

    test("playGame with sealed Bloomburrow decks runs to completion") {
        val env = GameEnvironment.create(createRegistry())
        val config = GameConfig(
            players = listOf(
                PlayerConfig("Alice", sealedDeck()),
                PlayerConfig("Bob", sealedDeck())
            ),
            skipMulligans = true,
            startingPlayerIndex = 0
        )

        val result = env.playGame(config)

        (result.terminated || env.stepCount >= 2000).shouldBeTrue()
        env.stepCount shouldBeGreaterThan 0
    }

    test("playGame with random agents and sealed decks completes") {
        val env = GameEnvironment.create(createRegistry())
        val config = GameConfig(
            players = listOf(
                PlayerConfig("Alice", sealedDeck()),
                PlayerConfig("Bob", sealedDeck())
            ),
            skipMulligans = true,
            startingPlayerIndex = 0
        )

        env.reset(config)
        val agents = env.playerIds.associateWith<EntityId, ActionSelector> { RandomActionSelector() }

        val result = env.playGame(config, agents)
        (result.terminated || env.stepCount >= 2000).shouldBeTrue()
    }

    test("MCTS-style fork and rollout pattern works") {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        val actions = env.legalActions()
        if (actions.size >= 2) {
            val scores = actions.take(3).map { action ->
                val child = env.fork()
                child.step(action.action)
                child.evaluate(env.playerIds[0])
            }

            scores.all { it.isFinite() }.shouldBeTrue()
        }
    }

    test("multiple forks from same state are independent") {
        val env = GameEnvironment.create(createRegistry())
        env.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", simpleDeck()),
                    PlayerConfig("Bob", simpleDeck())
                ),
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )

        val fork1 = env.fork()
        val fork2 = env.fork()

        val actions = fork1.legalActions()
        if (actions.isNotEmpty()) {
            fork1.step(actions.first().action)
            fork2.state shouldBe env.state
        }
    }

    test("reset can be called multiple times") {
        val env = GameEnvironment.create(createRegistry())
        val config = GameConfig(
            players = listOf(
                PlayerConfig("Alice", simpleDeck()),
                PlayerConfig("Bob", simpleDeck())
            ),
            skipMulligans = true,
            startingPlayerIndex = 0
        )

        env.reset(config)
        env.reset(config)
        env.playerIds.size shouldBe 2
        env.stepCount shouldBe 0
        env.isTerminal.shouldBeFalse()
    }
})
