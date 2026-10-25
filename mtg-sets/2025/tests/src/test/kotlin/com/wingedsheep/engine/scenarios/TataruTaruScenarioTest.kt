package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tataru Taru (FIC #30).
 *
 *   When Tataru Taru enters, you draw a card and target opponent may draw a card.
 *   Scions' Secretary — Whenever an opponent draws a card, if it isn't that player's turn, create a
 *   tapped Treasure token. This ability triggers only once each turn.
 *
 * Pinned here: the opponent (not Tataru's controller) answers the "may draw"; an off-turn opponent
 * draw makes one tapped Treasure, and only one per turn; an opponent's draw on their own turn
 * doesn't trigger.
 */
class TataruTaruScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.targetOpponentIfAsked() {
        if (getPendingDecision() is ChooseTargetsDecision) selectTargets(listOf(player2Id))
        resolveStack()
    }

    init {
        context("Tataru Taru") {

            test("enters: you draw, the opponent chooses to draw, and their off-turn draw makes a tapped Treasure") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Tataru Taru")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tataru Taru").error shouldBe null
                game.resolveStack()
                game.targetOpponentIfAsked()

                val ask = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                withClue("the targeted opponent decides whether to draw") {
                    ask.playerId shouldBe game.player2Id
                }
                game.answerYesNo(true)
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.handSize(2) shouldBe 1

                val treasures = game.findPermanents("Treasure")
                withClue("one Treasure for the opponent's off-turn draw") { treasures.size shouldBe 1 }
                withClue("the Treasure enters tapped") {
                    game.state.getEntity(treasures.single())!!.has<TappedComponent>() shouldBe true
                }
            }

            test("an opponent who declines to draw gives no Treasure") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Tataru Taru")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tataru Taru").error shouldBe null
                game.resolveStack()
                game.targetOpponentIfAsked()
                game.answerYesNo(false)
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.handSize(2) shouldBe 0
                game.findPermanents("Treasure").size shouldBe 0
            }

            test("the Treasure ability triggers only once each turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Tataru Taru")
                    .withCardInHand(1, "Bargain")
                    .withCardInHand(1, "Bargain")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Bargain", 2).error shouldBe null
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Bargain", 2).error shouldBe null
                game.resolveStack()

                game.handSize(2) shouldBe 2
                game.findPermanents("Treasure").size shouldBe 1
            }

            test("an opponent drawing on their own turn doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Tataru Taru")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                game.handSize(2) shouldBe 1
                game.findPermanents("Treasure").size shouldBe 0
            }
        }
    }
}
