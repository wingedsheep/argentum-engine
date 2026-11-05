package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hour of Glory (HOU #65) — {3}{B} Instant.
 * "Exile target creature. If that creature was a God, its controller reveals their hand and exiles
 * all cards from it with the same name as that creature."
 */
class HourOfGloryScenarioTest : ScenarioTestBase() {

    init {
        context("Hour of Glory") {

            test("exiling a God also exiles same-named cards from its controller's hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hour of Glory")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "The Scarab God")
                    .withCardInHand(2, "The Scarab God")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(1, "The Scarab God")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val god = game.findPermanent("The Scarab God")!!
                game.castSpell(1, "Hour of Glory", god).error shouldBe null
                game.resolveStack()

                withClue("the God and its namesake in its controller's hand are exiled") {
                    game.isOnBattlefield("The Scarab God") shouldBe false
                    game.isInHand(2, "The Scarab God") shouldBe false
                    game.state.getExile(game.player2Id).size shouldBe 2
                }
                withClue("other cards in that hand, and the caster's own hand, are untouched") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "The Scarab God") shouldBe true
                }
            }

            test("exiling a non-God leaves its controller's hand alone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hour of Glory")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Hour of Glory", bears).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                withClue("a non-God target doesn't touch the hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
