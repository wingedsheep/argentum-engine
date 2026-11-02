package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * First Contact (TRK #199) — {1}{G} Instant.
 *
 *   "Mill four cards. You may put a permanent card from among them into your hand. If this spell is
 *    the first spell you've cast this game, you gain 2 life."
 *
 * "First spell" is judged as of the cast, so a spell cast after it — even one cast in response that
 * resolves first — doesn't take the life away, while a spell cast earlier does.
 */
class FirstContactScenarioTest : ScenarioTestBase() {

    private fun newGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "First Contact")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Sol Ring")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("as the first spell of the game it mills four, returns a permanent card and gains 2 life") {
            val game = newGame()
            game.castSpell(1, "First Contact").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision as? SelectCardsDecision
            withClue("the optional pick pauses for a selection") { decision shouldNotBe null }
            withClue("four cards were milled") { game.graveyardSize(1) shouldBe 4 }
            game.selectCards(game.findCardsInGraveyard(1, "Grizzly Bears"))

            withClue("the chosen permanent card goes to hand") { game.isInHand(1, "Grizzly Bears") shouldBe true }
            withClue("the instant Lightning Bolt stays milled") { game.isInGraveyard(1, "Lightning Bolt") shouldBe true }
            withClue("first spell this game: gain 2 life") { game.getLifeTotal(1) shouldBe 22 }
        }

        test("cast after another spell, it gains no life") {
            val game = newGame()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            game.castSpell(1, "First Contact").error shouldBe null
            game.resolveStack()
            game.skipSelection()

            withClue("four milled cards, the Bolt and First Contact itself are in the graveyard") { game.graveyardSize(1) shouldBe 6 }
            withClue("not the first spell cast this game: no life") { game.getLifeTotal(1) shouldBe 20 }
        }

        test("a spell cast in response doesn't stop it from being the first spell") {
            val game = newGame()
            game.castSpell(1, "First Contact").error shouldBe null
            // Holding priority, the Bolt goes on top and resolves first.
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            withClue("the Bolt resolved first") { game.getLifeTotal(2) shouldBe 17 }

            game.skipSelection()
            withClue("First Contact was still the first spell cast this game") { game.getLifeTotal(1) shouldBe 22 }
        }
    }
}
