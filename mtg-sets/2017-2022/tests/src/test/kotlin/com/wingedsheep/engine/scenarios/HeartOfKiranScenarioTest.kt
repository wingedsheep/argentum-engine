package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Heart of Kiran (AER #153): a 4/4 flying, vigilance Vehicle with crew 3 whose crew cost can be
 * paid instead by removing a loyalty counter from a planeswalker you control — even one that has
 * already activated a loyalty ability this turn (the card's 2017-02-09 ruling).
 */
class HeartOfKiranScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun TestGame.isCreature(id: EntityId): Boolean = state.projectedState.isCreature(id)

    init {
        test("removing a loyalty counter from a planeswalker crews it without tapping a creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Heart of Kiran")
                .withCardOnBattlefield(1, "Tezzeret the Schemer")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val heart = game.findPermanent("Heart of Kiran")!!
            val tezzeret = game.findPermanent("Tezzeret the Schemer")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val alternative = game.getLegalActions(1).single {
                (it.action as? CrewVehicle)?.alternativeCostFrom == tezzeret
            }
            alternative.description shouldBe "Crew Heart of Kiran — remove a loyalty counter from Tezzeret the Schemer"

            game.execute(alternative.action).error shouldBe null
            withClue("the loyalty counter is the crew cost, paid on activation") {
                game.loyalty(tezzeret) shouldBe 4
            }
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            game.isCreature(heart) shouldBe false

            game.resolveStack()
            game.isCreature(heart) shouldBe true
            val projected = game.state.projectedState
            projected.getPower(heart) shouldBe 4
            projected.getToughness(heart) shouldBe 4
            projected.hasKeyword(heart, Keyword.FLYING) shouldBe true
            projected.hasKeyword(heart, Keyword.VIGILANCE) shouldBe true
        }

        test("the planeswalker can pay it after activating a loyalty ability this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Heart of Kiran")
                .withCardOnBattlefield(1, "Tezzeret the Schemer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val heart = game.findPermanent("Heart of Kiran")!!
            val tezzeret = game.findPermanent("Tezzeret the Schemer")!!
            val plusOne = cardRegistry.getCard("Tezzeret the Schemer")!!.script.activatedAbilities[0]

            game.execute(ActivateAbility(game.player1Id, tezzeret, plusOne.id)).error shouldBe null
            game.resolveStack()
            game.loyalty(tezzeret) shouldBe 6

            game.execute(CrewVehicle(game.player1Id, heart, emptyList(), alternativeCostFrom = tezzeret)).error shouldBe null
            game.resolveStack()
            game.loyalty(tezzeret) shouldBe 5
            game.isCreature(heart) shouldBe true
        }

        test("crew 3 can still be paid by tapping creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Heart of Kiran")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val heart = game.findPermanent("Heart of Kiran")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!

            withClue("with no planeswalker, only the tap-creatures crew is offered") {
                game.getLegalActions(1).filter { (it.action as? CrewVehicle)?.vehicleId == heart }.size shouldBe 1
            }
            game.execute(CrewVehicle(game.player1Id, heart, listOf(bears))).error shouldBe
                "Total power (2) is less than crew requirement (3)"
            game.execute(CrewVehicle(game.player1Id, heart, listOf(bears, lions))).error shouldBe null
            game.resolveStack()
            game.isCreature(heart) shouldBe true
        }
    }
}
