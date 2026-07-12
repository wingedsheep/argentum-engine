package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Dig Up (VOW #197).
 *
 * {G} Sorcery — Cleave {1}{B}{B}{G}
 * "Search your library for a [basic land] card, [reveal it,] put it into your hand, then shuffle."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. Dig Up has
 * *two* bracket spans — `[basic land]` and `[reveal it,]` — so the printed cast is a basic-land
 * tutor to hand (revealed) while the cleaved cast is an unconditional tutor to hand (no reveal).
 * The two modes differ only in the [effect]'s search filter (and the reveal flag); paying the
 * cleave cost never changes the spell's mana value (CR 202.3b).
 *
 * These tests pin the search filter of each mode: the printed cast may only choose a basic land,
 * while the cleaved cast may choose any card.
 */
class DigUpScenarioTest : ScenarioTestBase() {

    init {
        context("Dig Up — printed cast (brackets present)") {

            test("tutors only a basic land to hand; non-lands are not selectable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dig Up")
                    .withLandsOnBattlefield(1, "Forest", 1) // {G}
                    .withCardInLibrary(1, "Swamp")          // a basic land to find
                    .withCardInLibrary(1, "Grizzly Bears")  // a non-land that must NOT be offered
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val swamp = game.findCardsInLibrary(1, "Swamp").single()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

                val cast = game.castSpell(1, "Dig Up")
                withClue("Casting Dig Up should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Only the basic land is a legal choice for the printed cast") {
                    decision.options shouldContain swamp
                    decision.options shouldNotContain bears
                }

                game.selectCards(listOf(swamp))
                game.resolveStack()

                withClue("The basic land goes to hand") {
                    game.isInHand(1, "Swamp") shouldBe true
                }
            }
        }

        context("Dig Up — cleaved cast (brackets removed)") {

            test("tutors any card to hand, including a non-land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dig Up")
                    .withLandsOnBattlefield(1, "Swamp", 3) // Cleave {1}{B}{B}{G}
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Grizzly Bears") // a non-land, only findable when cleaved
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

                val cast = game.castSpellWithCleave(1, "Dig Up")
                withClue("Casting Dig Up for its cleave cost should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("The cleaved cast may choose a non-land card") {
                    decision.options shouldContain bears
                }

                game.selectCards(listOf(bears))
                game.resolveStack()

                withClue("The chosen non-land card goes to hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
