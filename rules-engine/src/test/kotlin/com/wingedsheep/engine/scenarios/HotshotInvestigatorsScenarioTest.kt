package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hotshot Investigators (MKM #60) — {5}{U} 4/4 Creature — Vedalken Detective.
 *
 * "When this creature enters, return up to one other target creature to its owner's hand.
 *  If you controlled it, investigate."
 *
 * The interesting part is the rider: the control check has to be made *before* the bounce (a card
 * in hand has no controller), and declining the "up to one" must investigate nothing. All three
 * branches are covered here. The Investigators are cast rather than placed, since only a real
 * zone change fires the enters trigger.
 */
class HotshotInvestigatorsScenarioTest : ScenarioTestBase() {

    init {
        context("Hotshot Investigators — bounce, and investigate only for your own creature") {

            test("bouncing your own creature returns it to hand and investigates") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Hotshot Investigators")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .build()

                game.castSpell(1, "Hotshot Investigators").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Returned to its owner's hand") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("You controlled it, so the rider investigates") {
                    game.isOnBattlefield("Clue") shouldBe true
                }
            }

            test("bouncing an opponent's creature returns it but investigates nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Hotshot Investigators")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .build()

                game.castSpell(1, "Hotshot Investigators").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Still bounced — the bounce itself is unconditional") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
                withClue("You did not control it, so no Clue") {
                    game.isOnBattlefield("Clue") shouldBe false
                }
            }

            test("declining the optional target bounces nothing and investigates nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Hotshot Investigators")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .build()

                game.castSpell(1, "Hotshot Investigators").error shouldBe null
                game.resolveStack()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("Nothing bounced") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("\"Up to one\" declined — nothing returned, so nothing to investigate off") {
                    game.isOnBattlefield("Clue") shouldBe false
                }
            }
        }
    }
}
