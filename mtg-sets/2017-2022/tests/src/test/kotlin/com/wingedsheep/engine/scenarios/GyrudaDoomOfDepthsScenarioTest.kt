package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Gyruda, Doom of Depths (IKO #221).
 *
 *   When Gyruda enters, each player mills four cards. Put a creature card with an even mana value
 *   from among the milled cards onto the battlefield under your control.
 *
 * Pins: every library is milled; only even-mana-value creature cards that were milled by this
 * ability are offered (not odd ones, not lands, not a card already in a graveyard); an opponent's
 * card can be taken and enters under Gyruda's controller; nothing happens when none qualifies.
 */
class GyrudaDoomOfDepthsScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Gyruda", "Opponent")
            .withCardInHand(1, "Gyruda, Doom of Depths")
            .withLandsOnBattlefield(1, "Island", 6)
            // Already in a graveyard before Gyruda enters: even mana value, but never milled.
            .withCardInGraveyard(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("you put an opponent's milled even-mana-value creature onto the battlefield under your control") {
            val game = builder()
                // Exactly four cards each, so the whole library is milled regardless of order.
                .withCardInLibrary(1, "Gray Ogre")      // MV 3 — odd
                .withCardInLibrary(1, "Grizzly Bears")  // MV 2 — even
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Hill Giant")     // MV 4 — even
                .withCardInLibrary(2, "Raging Goblin")  // MV 1 — odd
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .build()
            val preexistingBears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val milledBears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val giant = game.findCardsInLibrary(2, "Hill Giant").single()

            game.castSpell(1, "Gyruda, Doom of Depths").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Gyruda's controller chooses") { decision.playerId shouldBe game.player1Id }
            withClue("only even-mana-value creature cards milled by this ability are offered") {
                decision.options shouldContainExactlyInAnyOrder listOf(milledBears, giant)
            }
            game.selectCards(listOf(giant))
            game.resolveStack()

            withClue("each player milled four cards") {
                game.librarySize(1) shouldBe 0
                game.librarySize(2) shouldBe 0
            }
            val onBattlefield = game.findPermanent("Hill Giant").shouldNotBeNull()
            onBattlefield shouldBe giant
            game.state.projectedState.getController(giant) shouldBe game.player1Id
            game.isInGraveyard(1, "Gray Ogre") shouldBe true
            game.isInGraveyard(2, "Raging Goblin") shouldBe true
            withClue("both Grizzly Bears stay in the graveyard") {
                game.findCardsInGraveyard(1, "Grizzly Bears") shouldContainExactlyInAnyOrder
                    listOf(preexistingBears, milledBears)
            }
        }

        test("nothing is put onto the battlefield when no even-mana-value creature is milled") {
            val game = builder()
                .withCardInLibrary(1, "Gray Ogre")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Raging Goblin")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .build()

            game.castSpell(1, "Gyruda, Doom of Depths").error shouldBe null
            game.resolveStack()

            withClue("no selection is asked for an empty pool") {
                (game.getPendingDecision() is SelectCardsDecision) shouldBe false
            }
            game.librarySize(1) shouldBe 0
            game.librarySize(2) shouldBe 0
            game.findPermanent("Gray Ogre") shouldBe null
            game.findPermanent("Raging Goblin") shouldBe null
            withClue("the pre-existing Grizzly Bears is not eligible") {
                game.findPermanent("Grizzly Bears") shouldBe null
            }
        }
    }
}
