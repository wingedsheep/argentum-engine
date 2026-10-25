package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Emet-Selch of the Third Seat (FIC #81).
 *
 *   Spells you cast from your graveyard cost {2} less to cast.
 *   Whenever one or more opponents lose life, you may cast target instant or sorcery card from
 *   your graveyard. If that spell would be put into your graveyard, exile it instead. Do this only
 *   once each turn.
 *
 * Pins the resolution-time graveyard cast paying the discounted cost (Divination for {U}), the
 * exile replacement, and the "do this only once each turn" rider being spent by the cast.
 */
class EmetSelchOfTheThirdSeatScenarioTest : ScenarioTestBase() {

    private fun emetGame(bolts: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Emet-Selch of the Third Seat")
        .withCardInGraveyard(1, "Divination")
        .withCardsInHand(1, "Lightning Bolt", bolts)
        .withLandsOnBattlefield(1, "Mountain", bolts)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.boltOpponentAndTargetDivination() {
        castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
        resolveStack()
        val divination = findCardsInGraveyard(1, "Divination").single()
        if (hasPendingDecision()) selectTargets(listOf(divination))
        resolveStack()
    }

    init {
        context("Emet-Selch of the Third Seat") {

            test("an opponent losing life lets you cast a graveyard sorcery for {2} less, then exiles it") {
                val game = emetGame(bolts = 1)
                game.boltOpponentAndTargetDivination()

                game.answerYesNo(true)
                game.resolveStack()

                withClue("Bolt hit the opponent") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("Divination ({2}{U}) was cast with a single Island and drew two") {
                    game.handSize(1) shouldBe 2
                }
                withClue("Divination was exiled instead of going back to the graveyard") {
                    game.isInExile(1, "Divination") shouldBe true
                    game.findCardsInGraveyard(1, "Divination").size shouldBe 0
                }
            }

            test("once the cast has happened this turn, a later life loss doesn't trigger it again") {
                val game = emetGame(bolts = 2)
                game.boltOpponentAndTargetDivination()
                game.answerYesNo(true)
                game.resolveStack()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("Second Bolt resolved") {
                    game.getLifeTotal(2) shouldBe 14
                }
                withClue("No second trigger: the first Bolt stays in the graveyard, nothing on the stack") {
                    game.state.stack.size shouldBe 0
                    game.hasPendingDecision() shouldBe false
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 2
                }
            }

            test("declining the cast spends nothing — the next life loss offers it again") {
                val game = emetGame(bolts = 2)
                game.boltOpponentAndTargetDivination()
                game.answerYesNo(false)
                game.resolveStack()

                game.findCardsInGraveyard(1, "Divination").size shouldBe 1

                game.boltOpponentAndTargetDivination()
                withClue("The trigger fired again after the declined first instance") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
