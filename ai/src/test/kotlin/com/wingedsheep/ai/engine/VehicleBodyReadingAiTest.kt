package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.VehicleBody
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * An uncrewed Vehicle is a non-creature artifact, so the AI priced Consulate Dreadnought's 7/11
 * body as an unreadable permanent (0.5 on the board, a blank artifact in the draft). [VehicleBody]
 * reads the creature behind its crew: a share of the body, larger when the controller's other
 * creatures can crew it.
 */
class VehicleBodyReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        val intents = IntentCatalog.of(cardRegistry)

        fun dreadnoughtValue(vararg crew: String): Double {
            var builder = scenario().withPlayers().withCardOnBattlefield(1, "Consulate Dreadnought")
            crew.forEach { builder = builder.withCardOnBattlefield(1, it) }
            val game = builder.build()
            val id = game.findPermanent("Consulate Dreadnought")!!
            val component = game.state.getEntity(id)!!.get<CardComponent>()!!
            return BoardPresence.permanentValue(
                game.state, game.state.projectedState, id, component, intents, AiProfile.LIVE.creatureValuation,
            )
        }

        test("printed crew is read off the definition") {
            VehicleBody.printedCrew(card("Consulate Dreadnought")) shouldBe 6
            VehicleBody.printedCrew(card("Grizzly Bears")) shouldBe null
        }

        test("an uncrewed Vehicle is worth more than an unreadable permanent, and more once it can be crewed") {
            val alone = dreadnoughtValue()
            val crewable = dreadnoughtValue("Craw Wurm") // 6/4: enough power for crew 6
            alone shouldBeGreaterThan 0.5
            crewable shouldBeGreaterThan alone
        }

        test("the crewable share stays below the body it becomes") {
            val body = BoardPresence.creatureBodyValue(7, 11, emptySet(), diminishingReturns = true)
            dreadnoughtValue("Craw Wurm") shouldBeLessThan body
        }

        test("the draft heuristic rates the body less its crew, not as a blank artifact") {
            val explained = LimitedCardRater.explain(card("Consulate Dreadnought"))
            explained.heuristic.first { it.part == "Base" }.value shouldBeGreaterThan 1.5
        }

        test("a Vehicle spell on the stack is priced by its body too") {
            val game = scenario().withPlayers().withCardInHand(1, "Consulate Dreadnought").build()
            val id = game.findCardsInHand(1, "Consulate Dreadnought").single()
            val component = game.state.getEntity(id)!!.get<CardComponent>()!!
            val value = BoardPresence.spellValue(game.state.projectedState, component, game.player1Id, intents)!!
            value shouldBeGreaterThan 0.5
        }
    }
}
