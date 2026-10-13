package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ExtinguishTheLight
import com.wingedsheep.mtg.sets.definitions.j22.cards.PiratedCopy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Extinguish the Light (DMU #94) — {2}{B}{B} Instant.
 *   "Destroy target creature or planeswalker. If its mana value was 3 or less, you gain 3 life."
 *
 * "Was" is last-known information: the mana value is the one the permanent had on the
 * battlefield, not the card's printed value once it is in the graveyard.
 */
class ExtinguishTheLightScenarioTest : ScenarioTestBase() {

    init {
        context("Extinguish the Light") {

            fun buildGame(victim: String) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Extinguish the Light")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, victim)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("destroying a creature with mana value 3 or less gains 3 life") {
                val game = buildGame("Grizzly Bears")
                val bears = game.findPermanent("Grizzly Bears")!!
                val before = game.getLifeTotal(1)

                game.castSpell(1, "Extinguish the Light", bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("Mana value 2 is 3 or less — gain 3") {
                    game.getLifeTotal(1) shouldBe before + 3
                }
            }

            test("destroying a creature with mana value 4 gains no life") {
                val game = buildGame("Hill Giant")
                val giant = game.findPermanent("Hill Giant")!!
                val before = game.getLifeTotal(1)

                game.castSpell(1, "Extinguish the Light", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                withClue("Mana value 4 is above the threshold") {
                    game.getLifeTotal(1) shouldBe before
                }
            }

            test("reads the mana value the permanent had on the battlefield — a destroyed copy of a 2-drop gains life") {
                val d = GameTestDriver()
                d.registerCards(TestCards.all + listOf(PiratedCopy, ExtinguishTheLight))
                d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
                d.passPriorityUntil(Step.PRECOMBAT_MAIN)

                // Player 1's Pirated Copy ({4}{U}, mana value 5 as a card) enters as a copy of Grizzly Bears.
                val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
                val copy = d.putCardInHand(d.player1, "Pirated Copy")
                d.giveMana(d.player1, Color.BLUE, 5)
                d.castSpell(d.player1, copy).error shouldBe null
                d.bothPass()
                d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                d.submitCardSelection(d.player1, listOf(bears)).error shouldBe null

                // Player 2 destroys the copy at instant speed.
                d.passPriority(d.player1)
                val before = d.getLifeTotal(d.player2)
                val extinguish = d.putCardInHand(d.player2, "Extinguish the Light")
                d.giveMana(d.player2, Color.BLACK, 4)
                d.castSpell(d.player2, extinguish, listOf(copy)).error shouldBe null
                d.bothPass()

                withClue("The copy is in its owner's graveyard") {
                    d.getGraveyard(d.player1).contains(copy) shouldBe true
                }
                withClue("It had mana value 2 on the battlefield, so its caster gains 3") {
                    d.getLifeTotal(d.player2) shouldBe before + 3
                }
            }
        }
    }
}
