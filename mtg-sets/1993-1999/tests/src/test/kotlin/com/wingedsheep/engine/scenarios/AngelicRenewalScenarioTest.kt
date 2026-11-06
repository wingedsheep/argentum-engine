package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Angelic Renewal (WTH #4) — {1}{W} Enchantment.
 *
 *   Whenever a creature is put into your graveyard from the battlefield, you may sacrifice this
 *   enchantment. If you do, return that card to the battlefield.
 *
 * The trigger is an ownership test ("your graveyard"), the sacrifice is the *cost* of the payoff, and the
 * return reads the triggering card out of the graveyard after that cost was paid. A token that died has
 * ceased to exist, so sacrificing for it returns nothing; with several creatures dying at once the
 * enchantment can be sacrificed only once, so only one comes back.
 */
class AngelicRenewalScenarioTest : ScenarioTestBase() {

    private fun TestGame.boltBears() {
        val bears = findPermanent("Grizzly Bears") ?: error("Grizzly Bears not on the battlefield")
        castSpell(1, "Lightning Bolt", bears).error shouldBe null
        resolveStack()
    }

    init {
        context("Angelic Renewal") {
            test("sacrificing it returns the creature that died to the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angelic Renewal")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.boltBears()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("the creature is back on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
                withClue("the enchantment was the price") {
                    game.isOnBattlefield("Angelic Renewal") shouldBe false
                    game.isInGraveyard(1, "Angelic Renewal") shouldBe true
                }
            }

            test("declining leaves the creature in the graveyard and keeps the enchantment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angelic Renewal")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.boltBears()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Angelic Renewal") shouldBe true
            }

            test("a creature an opponent owns dying does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angelic Renewal")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.boltBears()

                withClue("it isn't put into *your* graveyard, so there is nothing to answer") {
                    game.hasPendingDecision() shouldBe false
                }
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Angelic Renewal") shouldBe true
            }

            test("a token that died is gone for good: you may sacrifice, but nothing returns") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angelic Renewal")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.boltBears()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Angelic Renewal") shouldBe false
            }

            test("with two creatures dying at once it can be sacrificed only once, so only one returns") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angelic Renewal")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Wrath of God")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wrath of God").error shouldBe null
                game.resolveStack()
                var answered = 0
                while (game.hasPendingDecision() && answered++ < 4) {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                    game.answerYesNo(true).error shouldBe null
                    game.resolveStack()
                }

                withClue("exactly one of the two Bears came back") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 1
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                }
                game.isInGraveyard(1, "Angelic Renewal") shouldBe true
            }
        }
    }
}
