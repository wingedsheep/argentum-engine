package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Plumb the Forbidden (STX #81) — "As an additional cost to cast this spell, you may sacrifice one
 * or more creatures. When you do, copy this spell for each creature sacrificed this way. You draw a
 * card and lose 1 life."
 *
 * The copy count is read off the spell's own cost payment by its cast trigger; copies are put on the
 * stack, not cast, so they don't copy themselves again (CR 707.10).
 */
class PlumbTheForbiddenScenarioTest : ScenarioTestBase() {

    private fun TestGame.castPlumb(sacrificed: List<EntityId>?) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Plumb the Forbidden").single(),
            additionalCostPayment = sacrificed?.let { AdditionalCostPayment(variableCostPermanents = it) },
        )
    )

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Plumb the Forbidden")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Llanowar Elves")
        .apply { repeat(6) { withCardInLibrary(1, "Swamp") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Plumb the Forbidden") {

            test("sacrificing two creatures copies it twice: three draws, three life lost") {
                val game = board()
                val sacrificed = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!)

                game.castPlumb(sacrificed).error shouldBe null
                withClue("the creatures are sacrificed as the spell is cast") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                withClue("the spell plus its 'when you do' cast trigger") {
                    game.state.stack.size shouldBe 2
                }
                game.resolveStack()

                game.handSize(1) shouldBe 3
                game.getLifeTotal(1) shouldBe 17
            }

            test("sacrificing nothing puts no trigger on the stack and resolves once") {
                val game = board()

                game.castPlumb(null).error shouldBe null
                game.state.stack.size shouldBe 1
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.getLifeTotal(1) shouldBe 19
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
        }
    }
}
