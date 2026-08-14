package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Obyra's Attendants // Desperate Parry. */
class ObyrasAttendantsScenarioTest : ScenarioTestBase() {

    init {
        context("Obyra's Attendants // Desperate Parry — the Adventure shrinks power only") {
            test("Desperate Parry gives -4/-0 and never kills the creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Obyra's Attendants")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                withClue("Centaur Courser starts as a 3/3") {
                    game.state.projectedState.getPower(courser) shouldBe 3
                    game.state.projectedState.getToughness(courser) shouldBe 3
                }

                // faceIndex = 0 is the Adventure face; the creature face casts with faceIndex = null.
                val cardId = game.findCardsInHand(1, "Obyra's Attendants").first()
                game.execute(
                    CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Permanent(courser)), faceIndex = 0)
                ).isSuccess shouldBe true
                game.resolveStack()

                withClue("-4/-0 — power drops by four, toughness is untouched") {
                    game.state.projectedState.getPower(courser) shouldBe -1
                    game.state.projectedState.getToughness(courser) shouldBe 3
                }
                withClue("a 3/3 hit by -4/-0 still has toughness 3, so it survives") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
                withClue("resolving the Adventure exiles the card so it can be cast as a creature later") {
                    game.isInExile(1, "Obyra's Attendants") shouldBe true
                }
            }

            test("the creature face is a 3/4 flier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Obyra's Attendants")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Obyra's Attendants")
                game.resolveStack()

                val attendants = game.findPermanent("Obyra's Attendants")!!
                game.state.projectedState.getPower(attendants) shouldBe 3
                game.state.projectedState.getToughness(attendants) shouldBe 4
                withClue("Flying") {
                    game.state.projectedState.hasKeyword(attendants, com.wingedsheep.sdk.core.Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
