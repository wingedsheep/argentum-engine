package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Muse Seeker {1}{U} 1/2 Elf Wizard — "Opus — Whenever you cast an instant or sorcery spell,
 * draw a card. Then discard a card unless five or more mana was spent to cast that spell."
 *
 * The discard is the *low* tier: base = draw then discard, replaced by a bare draw when 5+ mana
 * was spent (`insteadIfFiveOrMore`). Exercises both sides of the 5-mana boundary.
 */
class MuseSeekerScenarioTest : ScenarioTestBase() {

    init {
        context("Muse Seeker — Opus draw, discard unless 5+ mana spent") {

            test("a 1-mana spell: draw then discard (net hand size unchanged)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Muse Seeker")
                    .withCardInHand(1, "Lightning Bolt") // {R}, 1 mana
                    .withCardInHand(1, "Forest") // a spare card so the discard is a genuine choice
                    .withCardInLibrary(1, "Island")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                // Hand: Lightning Bolt + a spare Forest (2 cards) so the discard is a real choice.
                game.handSize(1) shouldBe 2

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                // Bolt left hand (1 = Forest). Opus trigger resolves: draw Island (hand = 2), then discard.
                game.resolveStack()

                withClue("1 mana spent → discard decision should be pending") {
                    (game.state.pendingDecision != null) shouldBe true
                }
                val drawn = game.findCardsInHand(1, "Island")
                game.selectCards(drawn).error shouldBe null

                game.resolveStack()
                withClue("Drew Island then discarded it → hand back to just the spare Forest") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("a 5-mana spell: draw only, no discard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Muse Seeker")
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardInLibrary(1, "Island")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.handSize(1) shouldBe 1 // Blaze

                // Blaze X=4 → {4}{R} → 5 mana spent.
                game.castXSpell(1, "Blaze", xValue = 4, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("5 mana spent → draw only, no discard decision") {
                    (game.state.pendingDecision == null) shouldBe true
                }
                withClue("Blaze left hand (0), drew Island (1) with no discard → hand size 1") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
