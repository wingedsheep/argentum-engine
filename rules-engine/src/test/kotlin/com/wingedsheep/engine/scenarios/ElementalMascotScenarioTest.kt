package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Elemental Mascot (Secrets of Strixhaven #185).
 *
 * Elemental Mascot ({1}{U}{R}, 1/4, Elemental Bird):
 *   Flying, vigilance
 *   Opus — Whenever you cast an instant or sorcery spell, this creature gets +1/+0 until end of
 *   turn. If five or more mana was spent to cast that spell, exile the top card of your library.
 *   You may play that card until the end of your next turn.
 *
 * Exercises the Opus base +1/+0 buff and the `alsoIfFiveOrMore` impulse (exile top card, playable
 * until end of next turn).
 */
class ElementalMascotScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Elemental Mascot — Opus +1/+0 and 5+ mana impulse") {

            test("a 1-mana spell applies the +1/+0 buff and exiles nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elemental Mascot") // 1/4
                    .withCardInHand(1, "Lightning Bolt") // {R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mascot = game.findPermanent("Elemental Mascot")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val exileBefore = game.state.getExile(game.state.activePlayerId!!).size

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("1 mana spent → base +1/+0 → 2/4") {
                    projector.getProjectedPower(game.state, mascot) shouldBe 2
                    projector.getProjectedToughness(game.state, mascot) shouldBe 4
                }
                withClue("Under 5 mana spent → no card exiled") {
                    game.state.getExile(game.state.activePlayerId!!).size shouldBe exileBefore
                }
            }

            test("a 5-mana spell applies +1/+0 AND exiles the top card of the library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elemental Mascot") // 1/4
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mascot = game.findPermanent("Elemental Mascot")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val player1 = game.state.activePlayerId!!
                val exileBefore = game.state.getExile(player1).size

                // Blaze X=4 → {4}{R} → 5 mana spent (boundary).
                game.castXSpell(1, "Blaze", xValue = 4, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("5 mana spent → base +1/+0 → 2/4") {
                    projector.getProjectedPower(game.state, mascot) shouldBe 2
                    projector.getProjectedToughness(game.state, mascot) shouldBe 4
                }
                withClue("5+ mana spent → top card of library exiled (impulse)") {
                    game.state.getExile(player1).size shouldBe exileBefore + 1
                }
            }
        }
    }
}
