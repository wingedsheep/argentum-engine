package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Transcendent Envoy (THB #40) — {1}{W} 1/2 Enchantment Creature — Griffin.
 *
 * "Flying
 *  Aura spells you cast cost {1} less to cast."
 */
class TranscendentEnvoyScenarioTest : ScenarioTestBase() {

    init {
        context("Transcendent Envoy") {

            test("Aura spells you cast cost {1} less") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Transcendent Envoy")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Pacifism's {1}{W} costs only {W} with the Envoy out") {
                    game.castSpell(1, "Pacifism", bears).error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Pacifism") shouldBe true
            }

            test("without the Envoy the same Aura is unaffordable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Pacifism", bears).error shouldNotBe null
            }

            test("the discount only shaves generic mana, never the coloured pip") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Transcendent Envoy")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Pacifism")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("{W} still has to be paid; a Forest can't pay it") {
                    game.castSpell(1, "Pacifism", bears).error shouldNotBe null
                }
            }

            test("non-Aura spells are not discounted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Transcendent Envoy")
                    .withCardInHand(1, "Glorious Anthem")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .build()

                withClue("Glorious Anthem ({1}{W}{W}) is a non-Aura enchantment and still costs three") {
                    game.castSpell(1, "Glorious Anthem").error shouldNotBe null
                }
            }

            test("an opponent's Aura spells are not discounted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Transcendent Envoy")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Pacifism")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(2)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Pacifism", bears).error shouldNotBe null
            }
        }
    }
}
