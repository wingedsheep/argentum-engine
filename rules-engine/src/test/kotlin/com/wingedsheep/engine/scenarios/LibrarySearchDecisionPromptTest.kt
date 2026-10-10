package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

/**
 * A library search's pick tells the player what they are searching for and where it goes, and
 * says on the decision that it is a search. `Patterns.Library.searchLibrary` derives the prompt and
 * "Selected →" label from its filter and destination; the `search = true` gather marks the pick
 * `librarySearch` with a "Fail to find" decline (CR 701.23b), which the AI reads instead of the
 * label.
 */
class LibrarySearchDecisionPromptTest : ScenarioTestBase() {

    init {
        test("Wood Elves' search names its filter and destination and is marked a search") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Wood Elves")
                .withCardInLibrary(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Wood Elves")
            withClue("casting Wood Elves should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val search = game.state.pendingDecision as? SelectCardsDecision
                ?: error("expected a library search; got ${game.state.pendingDecision}")
            search.prompt shouldStartWith "Search your library for a"
            search.prompt shouldContain "Forest"
            search.prompt shouldEndWith "card to put onto the battlefield"
            search.selectedLabel shouldBe "Put onto the battlefield"
            search.librarySearch shouldBe true
            search.declineLabel shouldBe "Fail to find"
        }

        test("a scry pick is not a library search") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Opt")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Opt")
            withClue("casting Opt should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val scry = game.state.pendingDecision as? SelectCardsDecision
                ?: error("expected a scry pick; got ${game.state.pendingDecision}")
            scry.librarySearch shouldBe false
            scry.declineLabel shouldBe null
        }
    }
}
