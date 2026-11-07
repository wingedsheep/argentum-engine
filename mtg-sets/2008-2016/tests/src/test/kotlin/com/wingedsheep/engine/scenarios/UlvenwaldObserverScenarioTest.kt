package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ulvenwald Observer (EMN #176) — {4}{G}{G} Creature — Treefolk, 6/6.
 *
 *   Whenever a creature you control with toughness 4 or greater dies, draw a card.
 *
 * The toughness check reads the creature's last-known toughness: a 2/2 pumped to 5/5 that dies
 * counts, an unpumped 2/2 doesn't.
 */
class UlvenwaldObserverScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Ulvenwald Observer")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Giant Growth")
        .withCardInHand(1, "Doom Blade")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInLibrary(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("a creature pumped to toughness 4 or greater dying draws a card (last-known toughness)") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getToughness(bears) shouldBe 5

            game.castSpell(1, "Doom Blade", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            withClue("the Bears died as a 5/5, so the Observer drew a card") {
                game.isInHand(1, "Hill Giant") shouldBe true
            }
        }

        test("a creature with toughness less than 4 dying draws nothing") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Doom Blade", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            withClue("a 2/2 dying doesn't trigger the Observer") {
                game.isInHand(1, "Hill Giant") shouldBe false
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
