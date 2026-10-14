package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Veronica, Dissident Scribe — "Whenever you discard one or more nonland cards for the first time
 * each turn, create a Junk token."
 *
 * "For the first time each turn" is turn history, not a once-per-turn cap: the turn's first nonland
 * discard closes the window even when Veronica wasn't on the battlefield for it, and a land discard
 * leaves it open.
 */
class VeronicaDissidentScribeScenarioTest : ScenarioTestBase() {

    private val veronica = "Veronica, Dissident Scribe"

    init {
        test("the first nonland discard makes a Junk; a second one that turn doesn't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, veronica)
                .withCardInHand(1, "Chitin Gravestalker")
                .withCardInHand(1, "Chitin Gravestalker")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cycle("Chitin Gravestalker")
            withClue("First nonland discard this turn → one Junk") { game.junk() shouldBe 1 }

            game.cycle("Chitin Gravestalker")
            withClue("Second nonland discard the same turn → no more Junk") { game.junk() shouldBe 1 }
        }

        test("a land discard doesn't close the window") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, veronica)
                .withCardInHand(1, "Forgotten Cave")
                .withCardInHand(1, "Chitin Gravestalker")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cycle("Forgotten Cave")
            withClue("A land discard isn't a nonland discard → no Junk") { game.junk() shouldBe 0 }

            game.cycle("Chitin Gravestalker")
            withClue("The turn's first nonland discard comes after the land → Junk") { game.junk() shouldBe 1 }
        }

        test("a nonland discard before Veronica entered closes the window") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, veronica)
                .withCardInHand(1, "Chitin Gravestalker")
                .withCardInHand(1, "Chitin Gravestalker")
                .withLandsOnBattlefield(1, "Mountain", 7)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cycle("Chitin Gravestalker")
            val cast = game.castSpell(1, veronica)
            withClue("Veronica cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            withClue("Veronica resolved onto the battlefield") { (game.findPermanent(veronica) != null) shouldBe true }

            game.cycle("Chitin Gravestalker")
            withClue("Not the first nonland discard this turn — a oncePerTurn cap would wrongly fire") {
                game.junk() shouldBe 0
            }
        }

        test("discarding two nonland cards at once makes a single Junk") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, veronica)
                .withCardInHand(1, "Faithless Looting")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .stocked()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Faithless Looting")
            withClue("Faithless Looting cast should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(2))
            }
            game.resolveStack()

            withClue("Both drawn Grizzly Bears were discarded") { game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0 }
            withClue("One discard event of two nonland cards → one Junk") { game.junk() shouldBe 1 }
        }
    }

    private fun TestGame.cycle(cardName: String) {
        val result = cycleCard(1, cardName)
        withClue("Cycling $cardName should succeed: ${result.error}") { result.error shouldBe null }
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
    }

    private fun TestGame.junk(): Int = findPermanents("Junk").size

    /** Library stocked with nonland cards so the draws never deck anyone. */
    private fun ScenarioBuilder.stocked(): ScenarioBuilder = apply {
        repeat(6) {
            withCardInLibrary(1, "Grizzly Bears")
            withCardInLibrary(2, "Grizzly Bears")
        }
    }
}
