package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Angel of the Ruins (C21 #11) — {5}{W}{W} Artifact Creature — Angel, 5/7.
 *
 *   Flying
 *   When this creature enters, exile up to two target artifacts and/or enchantments.
 *   Plainscycling {2}
 */
class AngelOfTheRuinsScenarioTest : ScenarioTestBase() {

    init {
        context("Angel of the Ruins ETB") {

            test("exiles an artifact and an enchantment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angel of the Ruins")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ring = game.findPermanent("Sol Ring")!!
                val anthem = game.findPermanent("Glorious Anthem")!!

                game.castSpell(1, "Angel of the Ruins").error shouldBe null
                game.resolveStack()

                game.selectTargets(listOf(ring, anthem)).error shouldBe null
                game.resolveStack()

                withClue("Sol Ring exiled") { game.isInExile(2, "Sol Ring") shouldBe true }
                withClue("Glorious Anthem exiled") { game.isInExile(2, "Glorious Anthem") shouldBe true }
                withClue("Angel remains") { game.isOnBattlefield("Angel of the Ruins") shouldBe true }
                withClue("Grizzly Bears untouched") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
            }

            test("a creature that is neither artifact nor enchantment is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angel of the Ruins")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Angel of the Ruins").error shouldBe null
                game.resolveStack()

                game.selectTargets(listOf(bears)).error shouldNotBe null
            }

            test("declining targets exiles nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angel of the Ruins")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Angel of the Ruins").error shouldBe null
                game.resolveStack()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("Sol Ring stays") { game.isOnBattlefield("Sol Ring") shouldBe true }
            }
        }
    }
}
