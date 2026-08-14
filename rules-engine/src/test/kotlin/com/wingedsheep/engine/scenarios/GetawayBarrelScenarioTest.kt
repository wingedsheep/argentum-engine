package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Getaway Barrel (HOB #98) — "When this artifact is put into a graveyard from the battlefield,
 * reveal the top thirteen cards of your library. Put a random creature card from among them onto
 * the battlefield. Put the rest on the bottom of your library in a random order."
 *
 * The pick is `SelectionMode.Random(1)` narrowed to creatures, so the eligible pool is the creature
 * subset only — the library here holds exactly one creature, which makes the "random" outcome
 * deterministic to assert. Covered: the creature is put onto the battlefield and the rest go back;
 * a creature-less reveal puts nothing onto the battlefield and loses no cards.
 */
class GetawayBarrelScenarioTest : ScenarioTestBase() {

    init {
        context("Getaway Barrel — dies, then a random revealed creature enters") {
            test("the only creature among the revealed cards is put onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Getaway Barrel")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Disenchant", game.findPermanent("Getaway Barrel")!!).error shouldBe null
                game.resolveStack()

                withClue("the Barrel hit the graveyard and its trigger resolved") {
                    game.isInGraveyard(1, "Getaway Barrel") shouldBe true
                }
                withClue("the single creature card among the six revealed entered the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("the other five revealed cards went back to the bottom of the library") {
                    game.librarySize(1) shouldBe 5
                }
            }

            test("no creature among the revealed cards puts nothing onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Getaway Barrel")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Disenchant", game.findPermanent("Getaway Barrel")!!).error shouldBe null
                game.resolveStack()

                withClue("nothing was eligible, so the whole reveal went back to the bottom") {
                    game.librarySize(1) shouldBe 3
                    game.graveyardSize(1) shouldBe 2 // the Barrel and the spent Disenchant
                }
            }
        }
    }
}
