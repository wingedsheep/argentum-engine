package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Anointed Peacekeeper (DMU #2): the activated-ability tax reaches a named Vehicle's crew, whose
 * printed cost has no mana, and the spell tax spares its controller's own spells.
 */
class AnointedPeacekeeperScenarioTest : ScenarioTestBase() {

    private fun TestGame.castAndName(name: String) {
        castSpell(1, "Anointed Peacekeeper")
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
        val decision = getPendingDecision() as ChooseOptionDecision
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    init {
        test("naming a Vehicle makes its crew cost {2} more") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Anointed Peacekeeper")
                .withCardOnBattlefield(1, "Sky Skiff")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castAndName("Sky Skiff")
            val skiff = game.findPermanent("Sky Skiff")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val crew = game.getLegalActions(1).first { it.action is CrewVehicle }
            withClue("all three Plains paid for Peacekeeper, so the {2} tax can't be paid") {
                crew.manaCostString shouldBe "{2}"
                crew.isAffordable shouldBe false
            }
            game.execute(CrewVehicle(game.player1Id, skiff, listOf(bears))).error shouldBe
                "Not enough mana to pay the crew cost ({2})"
        }

        test("a taxed crew is paid with two spare lands") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Anointed Peacekeeper")
                .withCardOnBattlefield(1, "Sky Skiff")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castAndName("Sky Skiff")
            val skiff = game.findPermanent("Sky Skiff")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(CrewVehicle(game.player1Id, skiff, listOf(bears))).error shouldBe null
            withClue("every Plains is tapped: three for Peacekeeper, two for the crew tax") {
                game.findAllPermanents("Plains").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
            }
        }

        test("its controller's own spells with the chosen name aren't taxed") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Anointed Peacekeeper")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castAndName("Grizzly Bears")

            val bears = game.getLegalActions(1).first {
                (it.action as? CastSpell)?.cardId == game.findCardsInHand(1, "Grizzly Bears").first()
            }
            bears.isAffordable shouldBe true
        }
    }
}
