package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for No Escape (WAR #63, reprinted J22 #328) — {2}{U} Instant.
 *
 *   Counter target creature or planeswalker spell. If that spell is countered this way, exile it
 *   instead of putting it into its owner's graveyard.
 *   Scry 1.
 *
 * Exercises Effects.CounterSpellToExile() then Effects.Scry(1), plus the ruling that an
 * uncounterable creature spell is a legal target: it isn't countered, but you still scry 1.
 */
class NoEscapeScenarioTest : ScenarioTestBase() {

    init {
        context("No Escape — counter a creature spell into exile, then scry 1") {

            test("exiles the countered creature spell and scries 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "No Escape")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Plains") // scry 1 fodder
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "No Escape", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Scry 1 presents the single top card") {
                    val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                    scry.options.size shouldBe 1
                }
                game.skipSelection()
                game.keepLibraryOrder()
                game.resolveStack()

                withClue("Grizzly Bears is exiled, not in the graveyard or on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("No Escape itself goes to its owner's graveyard") {
                    game.isInGraveyard(1, "No Escape") shouldBe true
                }
            }

            test("an uncounterable creature spell resolves, but No Escape still scries 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "No Escape")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Plains") // scry 1 fodder
                    .withCardInHand(2, "Vexing Beetle") // "This spell can't be countered."
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Vexing Beetle").error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "No Escape", "Vexing Beetle").error shouldBe null
                game.resolveStack()

                withClue("the counter does nothing, but Scry 1 still happens") {
                    val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                    scry.options.size shouldBe 1
                }
                game.skipSelection()
                game.keepLibraryOrder()
                game.resolveStack()

                withClue("Vexing Beetle resolves onto the battlefield, neither exiled nor graveyarded") {
                    game.isOnBattlefield("Vexing Beetle") shouldBe true
                    game.isInExile(2, "Vexing Beetle") shouldBe false
                    game.isInGraveyard(2, "Vexing Beetle") shouldBe false
                }
            }
        }
    }
}
