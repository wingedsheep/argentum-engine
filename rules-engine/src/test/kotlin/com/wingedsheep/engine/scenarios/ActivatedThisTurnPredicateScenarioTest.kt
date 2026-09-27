package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.ActivatedThisTurn` — "a permanent that was activated this turn" (Cut Short). The
 * card's own behaviour lives in `CutShortScenarioTest`; these pin the axis: *any* activation counts
 * (an unrestricted mana ability, crew), not just the restricted abilities whose ids the per-turn
 * tracker already records, and the mark is cleared when the turn ends.
 */
class ActivatedThisTurnPredicateScenarioTest : ScenarioTestBase() {

    private val activated = GameObjectFilter.Any.activatedThisTurn()

    private fun TestGame.wasActivated(entityId: EntityId): Boolean =
        services.predicateEvaluator.matches(
            state,
            state.projectedState,
            entityId,
            activated,
            PredicateContext(controllerId = player1Id),
        )

    init {
        test("an unrestricted mana ability marks its permanent, until the turn ends") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val elves = game.findPermanent("Llanowar Elves").shouldNotBeNull()
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.wasActivated(elves) shouldBe false

            val manaAbility = cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, elves, manaAbility)).error shouldBe null
            game.wasActivated(elves) shouldBe true
            game.wasActivated(bears) shouldBe false

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.wasActivated(elves) shouldBe false
        }

        test("crewing marks the Vehicle, not the crew") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(1, "Careening Mine Cart")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
            val cart = game.findPermanent("Careening Mine Cart").shouldNotBeNull()
            game.execute(CrewVehicle(game.player1Id, cart, listOf(wurm))).error shouldBe null

            game.wasActivated(cart) shouldBe true
            game.wasActivated(wurm) shouldBe false
        }
    }
}
