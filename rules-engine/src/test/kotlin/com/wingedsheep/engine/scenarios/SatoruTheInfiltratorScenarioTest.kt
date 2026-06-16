package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Satoru, the Infiltrator (OTJ #230, {U}{B} 2/3 Menace).
 *
 *   Whenever Satoru and/or one or more other nontoken creatures you control enter, if none of them
 *   were cast or no mana was spent to cast them, draw a card.
 *
 * Exercises the new batch-level `Conditions.NoManaSpentToCastEntered` gate over a
 * `Triggers.OneOrMorePermanentsEnter` trigger: a creature put onto the battlefield (no mana spent)
 * draws; a creature hard-cast for mana does not.
 */
class SatoruTheInfiltratorScenarioTest : ScenarioTestBase() {

    init {
        context("Satoru, the Infiltrator") {

            test("a creature entering with no mana spent draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Satoru, the Infiltrator")
                    .withCardInGraveyard(1, "Grizzly Bears")          // creature to reanimate
                    .withCardInHand(1, "Breath of Life")              // {3}{W} reanimation (no mana spent on the creature)
                    .withCardInLibrary(1, "Mountain")                 // something to draw
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size

                // Reanimate Grizzly Bears — it's put onto the battlefield, not cast.
                game.castSpellTargetingGraveyardCard(1, "Breath of Life", 1, "Grizzly Bears")
                game.resolveStack()

                withClue("Grizzly Bears was reanimated") {
                    (game.findPermanent("Grizzly Bears") != null) shouldBe true
                }
                // Hand: -Breath of Life (cast) + drew 1 from Satoru = net same as before the cast,
                // but the key check is the Satoru draw fired. Library should have shrunk by one.
                withClue("Satoru drew a card for the no-mana-spent entry") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore // -1 cast +1 drawn
                }
            }

            test("a creature hard-cast for mana does not draw") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Satoru, the Infiltrator")
                    .withCardInHand(1, "Grizzly Bears")    // {1}{G} — cast for mana
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.state.getHand(game.player1Id).size

                game.castSpell(1, "Grizzly Bears")
                game.resolveStack()

                withClue("Grizzly Bears entered") {
                    (game.findPermanent("Grizzly Bears") != null) shouldBe true
                }
                withClue("No Satoru draw: hand dropped by one (the cast Bears), no extra card drawn") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore - 1
                }
            }
        }
    }
}
