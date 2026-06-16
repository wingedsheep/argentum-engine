package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pursue the Past {R}{W} Sorcery — "You gain 2 life. You may discard a card. If you do, draw two
 * cards. Flashback {2}{R}{W}."
 *
 * Covers the unconditional life gain, the optional loot (discard a card -> draw two), declining the
 * loot, and re-casting from the graveyard via flashback.
 */
class PursueThePastScenarioTest : ScenarioTestBase() {

    init {
        context("Pursue the Past — gain 2, optional discard-to-draw-two, flashback") {

            test("gains 2 life, discards a card, then draws two") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pursue the Past")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Counterspell")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Pursue the Past").error shouldBe null
                game.resolveStack()

                val fodder = game.findCardsInHand(1, "Grizzly Bears").single()
                // "You may discard a card" -> yes.
                game.answerYesNo(true)
                // The discard prompts for which card; with a single eligible card the engine may
                // auto-resolve, so only submit a selection when a decision is pending.
                if (game.state.pendingDecision != null) {
                    game.selectCards(listOf(fodder))
                }
                game.resolveStack()

                withClue("Gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("Grizzly Bears was discarded") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                // -1 cast Pursue, -1 discarded Grizzly Bears, +2 drawn = net 0 from start.
                withClue("Hand size: -1 cast, -1 discard, +2 draw = net 0") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("declining the discard gains 2 life but draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pursue the Past")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Counterspell")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pursue the Past").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Still gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("No card was discarded") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
                withClue("No cards drawn — Grizzly Bears still in hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("can be flashed back from the graveyard for {2}{R}{W}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Pursue the Past")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Counterspell")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellFromGraveyard(1, "Pursue the Past").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Gained 2 life from the flashback cast") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("Flashback exiles Pursue the Past, so it is no longer in the graveyard") {
                    game.isInGraveyard(1, "Pursue the Past") shouldBe false
                }
            }
        }
    }
}
