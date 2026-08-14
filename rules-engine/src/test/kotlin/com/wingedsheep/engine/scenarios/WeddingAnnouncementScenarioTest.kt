package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wedding Announcement // Wedding Festivity (VOW #45).
 *
 *   Front — Wedding Announcement — At the beginning of your end step, put an invitation counter on
 *           it. If you attacked with two or more creatures this turn, draw a card; otherwise create
 *           a 1/1 white Human. Then if it has three or more invitation counters, transform it.
 *   Back  — Wedding Festivity — Creatures you control get +1/+1.
 *
 * Exercises the end-step counter accrual with the "otherwise create a Human token" branch (no attack
 * this turn), the transform once three invitation counters have accumulated, and the back face's
 * +1/+1 anthem.
 */
class WeddingAnnouncementScenarioTest : ScenarioTestBase() {

    init {
        context("Wedding Announcement") {

            test("accrues invitation counters over three of your end steps, then transforms and anthems") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Announcement")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                // Plenty of library fuel so neither player decks out over several turns.
                repeat(20) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(20) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val announcement = game.findPermanent("Wedding Announcement")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Cycle through THREE of the controller's end steps. No attacks → the "otherwise"
                // branch makes a 1/1 white Human token each time, and the third counter transforms it.
                repeat(3) { iteration ->
                    game.passUntilPhase(Phase.ENDING, Step.END)
                    game.resolveStack()
                    if (iteration < 2) {
                        // Advance past this end step into the opponent's turn and back to ours.
                        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // opponent upkeep (no accrual)
                        game.passUntilPhase(Phase.ENDING, Step.END)       // opponent end step (no accrual)
                        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // our next upkeep
                    }
                }

                withClue("did not attack any turn → each end step made a Human token (3 total)") {
                    game.findPermanents("Human Token").size shouldBe 3
                }
                withClue("after three invitation counters it transformed to Wedding Festivity") {
                    game.state.getEntity(announcement)!!.get<CardComponent>()!!.name shouldBe "Wedding Festivity"
                }
                withClue("Wedding Festivity gives creatures you control +1/+1 (Bears 2/2 -> 3/3)") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }
        }
    }
}
