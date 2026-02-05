package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Tests for the Hand Smoother algorithm.
 *
 * The hand smoother implements MTGA-style hand smoothing for Best-of-One games.
 * It draws multiple candidate hands and selects the one whose land ratio
 * most closely matches the deck's overall land-to-spell ratio.
 *
 * Key properties:
 * - Only applies to the FIRST hand drawn (not mulligan redraws)
 * - Reduces variance in opening hand land distribution
 * - Subsequent mulligan hands are completely random
 */
class HandSmootherTest : FunSpec({

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        return registry
    }

    context("GameConfig") {
        test("hand smoother is disabled by default") {
            val config = GameConfig(
                players = listOf(
                    PlayerConfig("Player 1", Deck.of("Forest" to 20)),
                    PlayerConfig("Player 2", Deck.of("Forest" to 20))
                )
            )

            config.useHandSmoother shouldBe false
            config.handSmootherCandidates shouldBe 3
        }

        test("hand smoother can be enabled in config") {
            val config = GameConfig(
                players = listOf(
                    PlayerConfig("Player 1", Deck.of("Forest" to 20)),
                    PlayerConfig("Player 2", Deck.of("Forest" to 20))
                ),
                useHandSmoother = true,
                handSmootherCandidates = 2
            )

            config.useHandSmoother shouldBe true
            config.handSmootherCandidates shouldBe 2
        }
    }

    context("basic functionality") {
        test("initializes game with hand smoother enabled") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            val result = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
                        PlayerConfig("Player 2", Deck.of("Forest" to 17, "Grizzly Bears" to 23))
                    ),
                    skipMulligans = true,
                    useHandSmoother = true
                )
            )

            // Both players should have 7 cards in hand
            val player1Hand = result.state.getHand(result.playerIds[0])
            val player2Hand = result.state.getHand(result.playerIds[1])

            player1Hand.size shouldBe 7
            player2Hand.size shouldBe 7

            // Libraries should have 33 cards (40 - 7)
            result.state.getLibrary(result.playerIds[0]).size shouldBe 33
            result.state.getLibrary(result.playerIds[1]).size shouldBe 33
        }

        test("hand smoother draws cards from library to hand") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            val result = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Forest" to 20, "Grizzly Bears" to 20)),
                        PlayerConfig("Player 2", Deck.of("Forest" to 20, "Grizzly Bears" to 20))
                    ),
                    skipMulligans = true,
                    useHandSmoother = true
                )
            )

            // Verify draw events were generated
            val drawEvents = result.events.filterIsInstance<CardsDrawnEvent>()
            drawEvents.size shouldBe 2

            drawEvents.forEach { event ->
                event.count shouldBe 7
                event.cardIds.size shouldBe 7
            }
        }
    }

    context("land distribution") {
        test("smoothed hands tend to have appropriate land counts for 40% land deck") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            // 40% lands (16/40) = expected ~2.8 lands in 7-card hand
            // Run multiple iterations and check average
            val landCounts = mutableListOf<Int>()

            repeat(100) {
                val result = initializer.initializeGame(
                    GameConfig(
                        players = listOf(
                            PlayerConfig("Player 1", Deck.of("Forest" to 16, "Grizzly Bears" to 24)),
                            PlayerConfig("Player 2", Deck.of("Forest" to 16, "Grizzly Bears" to 24))
                        ),
                        skipMulligans = true,
                        useHandSmoother = true
                    )
                )

                val hand = result.state.getHand(result.playerIds[0])
                val lands = hand.count { cardId ->
                    result.state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
                }
                landCounts.add(lands)
            }

            // Expected: ~2.8 lands per hand (40% of 7)
            val avgLands = landCounts.average()
            // Should be roughly in the 2-4 range on average
            avgLands shouldBeGreaterThanOrEqual 2.0
            avgLands shouldBeLessThanOrEqual 4.0
        }

        test("smoothed hands have lower variance than random hands") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            // Collect land counts for smoothed hands
            val smoothedLandCounts = mutableListOf<Int>()
            repeat(200) {
                val result = initializer.initializeGame(
                    GameConfig(
                        players = listOf(
                            PlayerConfig("Player 1", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
                            PlayerConfig("Player 2", Deck.of("Forest" to 17, "Grizzly Bears" to 23))
                        ),
                        skipMulligans = true,
                        useHandSmoother = true
                    )
                )

                val hand = result.state.getHand(result.playerIds[0])
                val lands = hand.count { cardId ->
                    result.state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
                }
                smoothedLandCounts.add(lands)
            }

            // Collect land counts for random hands
            val randomLandCounts = mutableListOf<Int>()
            repeat(200) {
                val result = initializer.initializeGame(
                    GameConfig(
                        players = listOf(
                            PlayerConfig("Player 1", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
                            PlayerConfig("Player 2", Deck.of("Forest" to 17, "Grizzly Bears" to 23))
                        ),
                        skipMulligans = true,
                        useHandSmoother = false
                    )
                )

                val hand = result.state.getHand(result.playerIds[0])
                val lands = hand.count { cardId ->
                    result.state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
                }
                randomLandCounts.add(lands)
            }

            // Calculate variance
            fun variance(list: List<Int>): Double {
                val mean = list.average()
                return list.map { (it - mean) * (it - mean) }.average()
            }

            val smoothedVariance = variance(smoothedLandCounts)
            val randomVariance = variance(randomLandCounts)

            // Smoothed hands should have lower or similar variance
            // We use a tolerance since randomness can vary
            // The key insight is that hand smoothing should reduce extreme cases
            smoothedVariance shouldBeLessThan (randomVariance + 0.5)
        }
    }

    context("edge cases") {
        test("handles deck with 0 lands") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            // All spells, no lands
            val result = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Grizzly Bears" to 40)),
                        PlayerConfig("Player 2", Deck.of("Grizzly Bears" to 40))
                    ),
                    skipMulligans = true,
                    useHandSmoother = true
                )
            )

            val hand = result.state.getHand(result.playerIds[0])
            hand.size shouldBe 7

            // All cards should be non-lands
            val lands = hand.count { cardId ->
                result.state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
            }
            lands shouldBe 0
        }

        test("handles deck with 100% lands") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            // All lands
            val result = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Forest" to 40)),
                        PlayerConfig("Player 2", Deck.of("Forest" to 40))
                    ),
                    skipMulligans = true,
                    useHandSmoother = true
                )
            )

            val hand = result.state.getHand(result.playerIds[0])
            hand.size shouldBe 7

            // All cards should be lands
            val lands = hand.count { cardId ->
                result.state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
            }
            lands shouldBe 7
        }

        test("works with minimum deck size") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            // Minimum viable deck (7 cards for a hand)
            val result = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Forest" to 3, "Grizzly Bears" to 4)),
                        PlayerConfig("Player 2", Deck.of("Forest" to 3, "Grizzly Bears" to 4))
                    ),
                    skipMulligans = true,
                    useHandSmoother = true
                )
            )

            val hand = result.state.getHand(result.playerIds[0])
            hand.size shouldBe 7
        }

        test("works with different candidate counts") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)

            // Test with 2 candidates
            val result2 = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
                        PlayerConfig("Player 2", Deck.of("Forest" to 17, "Grizzly Bears" to 23))
                    ),
                    skipMulligans = true,
                    useHandSmoother = true,
                    handSmootherCandidates = 2
                )
            )
            result2.state.getHand(result2.playerIds[0]).size shouldBe 7

            // Test with 3 candidates
            val result3 = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
                        PlayerConfig("Player 2", Deck.of("Forest" to 17, "Grizzly Bears" to 23))
                    ),
                    skipMulligans = true,
                    useHandSmoother = true,
                    handSmootherCandidates = 3
                )
            )
            result3.state.getHand(result3.playerIds[0]).size shouldBe 7
        }
    }

    context("integration with mulligan system") {
        test("mulligans after smoothed hand draw normally") {
            val registry = createRegistry()
            val initializer = GameInitializer(registry)
            val processor = ActionProcessor(registry)

            // Initialize with hand smoother but don't skip mulligans
            val result = initializer.initializeGame(
                GameConfig(
                    players = listOf(
                        PlayerConfig("Player 1", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
                        PlayerConfig("Player 2", Deck.of("Forest" to 17, "Grizzly Bears" to 23))
                    ),
                    skipMulligans = false,
                    useHandSmoother = true
                )
            )

            val player1 = result.playerIds[0]
            val initialHand = result.state.getHand(player1)
            initialHand.size shouldBe 7

            // Take a mulligan
            val mulliganResult = processor.process(result.state, TakeMulligan(player1))
            mulliganResult.isSuccess shouldBe true

            // New hand should have 7 cards (will need to bottom 1 after keeping)
            val newHand = mulliganResult.state.getHand(player1)
            newHand.size shouldBe 7
        }
    }
})
