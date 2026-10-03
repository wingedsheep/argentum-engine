package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pearl-Ear, Imperial Advisor (MH3 #39).
 *
 * "Enchantment spells you cast have affinity for Auras." and
 * "Whenever you cast an Aura spell that targets a modified permanent you control, draw a card."
 */
class PearlEarImperialAdvisorScenarioTest : ScenarioTestBase() {

    init {
        context("Pearl-Ear, Imperial Advisor") {

            test("affinity for Auras: Pacifism costs {W} with one Aura; targeting a modified creature draws") {
                // Pacifism is {1}{W}; with one Aura controlled it costs {W}. Only one Plains is available.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pearl-Ear, Imperial Advisor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                val result = game.castSpell(1, "Pacifism", bears)
                withClue("affinity for Auras makes Pacifism castable off a single Plains") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Pacifism left hand, Pearl-Ear's trigger drew one card") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("an Aura targeting an unmodified creature you control does not draw") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pearl-Ear, Imperial Advisor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Holy Strength")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Holy Strength", bears).error shouldBe null
                game.resolveStack()

                withClue("no draw: the Bears weren't modified when the Aura was cast") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }

            test("an opponent's Aura doesn't count for affinity") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pearl-Ear, Imperial Advisor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("one Plains can't pay {1}{W}: the opponent's Aura gives no affinity") {
                    game.castSpell(1, "Pacifism", bears).error shouldNotBe null
                }
            }

            test("an opponent's Aura doesn't make your creature modified") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pearl-Ear, Imperial Advisor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Pacifism", bears).error shouldBe null
                game.resolveStack()

                withClue("no draw: an Aura controlled by another player doesn't modify the Bears") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }

            test("a creature with a counter is modified") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pearl-Ear, Imperial Advisor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Holy Strength")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(
                        com.wingedsheep.engine.state.components.battlefield.CountersComponent(
                            mapOf(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE to 1)
                        )
                    )
                }
                val handBefore = game.handSize(1)

                game.castSpell(1, "Holy Strength", bears).error shouldBe null
                game.resolveStack()

                withClue("Holy Strength left hand, the trigger replaced it with a draw") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
