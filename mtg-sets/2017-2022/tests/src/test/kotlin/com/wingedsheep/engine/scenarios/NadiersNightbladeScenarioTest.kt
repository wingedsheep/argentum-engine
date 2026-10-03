package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nadier's Nightblade (CMR #136) — "Whenever a token you control leaves the battlefield, each
 * opponent loses 1 life and you gain 1 life."
 *
 * The trigger is a controller-scoped, token-only, destination-less leaves-the-battlefield. Each
 * test pins one of those axes: a token dying drains, a bounced token drains too (any destination),
 * a nontoken creature does nothing, and an opponent's token does nothing.
 */
class NadiersNightbladeScenarioTest : ScenarioTestBase() {

    init {
        context("Nadier's Nightblade") {

            test("a token you control dying drains each opponent for 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nadier's Nightblade")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("the token died, so the opponent lost 1 and I gained 1") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("a token you control bounced to hand also triggers — any destination counts") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nadier's Nightblade")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Unsummon", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("leaving to hand is still leaving the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("a nontoken creature you control dying does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nadier's Nightblade")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("a card is not a token") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("an opponent's token leaving does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nadier's Nightblade")
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("only tokens you control count") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
