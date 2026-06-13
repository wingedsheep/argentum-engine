package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Call of the Ring — upkeep "the Ring tempts you" + "Whenever you choose a creature as your
 * Ring-bearer, you may pay 2 life. If you do, draw a card." Exercises the new
 * `Triggers.WheneverYouChooseRingBearer` (RingTemptedEvent with requireBearerChosen): it fires
 * only when a creature is actually chosen.
 */
class CallOfTheRingScenarioTest : ScenarioTestBase() {

    init {
        test("choosing a Ring-bearer lets you pay 2 life to draw") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Call of the Ring")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // P1's upkeep → "the Ring tempts you"
            game.resolveStack()

            // The temptation pauses to choose a Ring-bearer; the only creature is Grizzly Bears.
            val ringDec = game.getPendingDecision()
            ringDec.shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(ringDec.options.first()))
            game.resolveStack()

            // The chosen-bearer trigger asks whether to pay 2 life and draw.
            game.answerYesNo(true)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 18
            game.state.getHand(game.player1Id).size shouldBe 1
            game.state.getEntity(game.player1Id)?.get<TheRingComponent>()?.temptCount shouldBe 1
        }

        test("upkeep temptation with no creatures does not trigger the draw") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Call of the Ring")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            // No creatures → no Ring-bearer chosen → the second ability never triggers.
            game.getPendingDecision() shouldBe null
            game.getLifeTotal(1) shouldBe 20
            game.state.getHand(game.player1Id).size shouldBe 0
            game.state.getEntity(game.player1Id)?.get<TheRingComponent>()?.temptCount shouldBe 1
        }
    }
}
