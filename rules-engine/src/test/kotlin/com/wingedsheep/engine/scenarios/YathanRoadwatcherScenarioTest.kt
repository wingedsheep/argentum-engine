package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Yathan Roadwatcher. */
class YathanRoadwatcherScenarioTest : ScenarioTestBase() {

    private val rainveilManaAbilityId =
        cardRegistry.getCard("Rainveil Rejuvenator")!!.activatedAbilities.first().id
    private val unrootedAbilityId =
        cardRegistry.getCard("Unrooted Ancestor")!!.activatedAbilities.first().id

    init {
        context("Yathan Roadwatcher") {
            test("ETB (cast) mills four and reanimates a creature with mana value 3 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Yathan Roadwatcher")
                    .withCardInGraveyard(1, "Glory Seeker") // MV 2 — legal reanimation target
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Yathan Roadwatcher")
                withClue("Casting Yathan Roadwatcher should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // The mill is non-optional; reflexive return then asks for a target.
                withClue("Should prompt for a reanimation target after milling") {
                    game.hasPendingDecision() shouldBe true
                }
                withClue("Four cards should have been milled") {
                    game.librarySize(1) shouldBe 0
                }
                val glorySeeker = game.findCardsInGraveyard(1, "Glory Seeker").first()
                game.selectTargets(listOf(glorySeeker))
                game.resolveStack()

                withClue("Glory Seeker should be returned to the battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                    game.findCardsInGraveyard(1, "Glory Seeker").size shouldBe 0
                }
            }
        }
    }
}
