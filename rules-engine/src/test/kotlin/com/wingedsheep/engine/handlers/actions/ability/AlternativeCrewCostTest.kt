package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `AlternativeCrewCost` — "You may remove a loyalty counter from a planeswalker you control rather
 * than pay this Vehicle's crew cost" (Heart of Kiran). The crew ability is still activated (CR
 * 702.122a), but the counter is paid in place of tapping creatures: one crew action per permanent
 * that can pay it, none for an opponent's permanent, none once the Vehicle has lost its abilities,
 * and no creature is recorded as having crewed it.
 */
class AlternativeCrewCostTest : ScenarioTestBase() {

    private val vehicleSuppressor = card("Test Vehicle Suppressor") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Vehicles lose all abilities."
        staticAbility {
            ability = LoseAllAbilities(GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.VEHICLE)))
        }
    }

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun TestGame.setLoyalty(id: EntityId, amount: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount)) }
    }

    private fun TestGame.alternativeCrews(vehicle: EntityId): List<EntityId?> =
        getLegalActions(1).mapNotNull { it.action as? CrewVehicle }
            .filter { it.vehicleId == vehicle && it.alternativeCostFrom != null }
            .map { it.alternativeCostFrom }

    init {
        cardRegistry.register(vehicleSuppressor)

        test("each planeswalker you control is its own crew action; an opponent's is not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Heart of Kiran")
                .withCardOnBattlefield(1, "Tezzeret the Schemer")
                .withCardOnBattlefield(1, "Ajani Unyielding")
                .withCardOnBattlefield(2, "Tezzeret, Master of Metal")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val heart = game.findPermanent("Heart of Kiran")!!
            val mine = listOf(game.findPermanent("Tezzeret the Schemer")!!, game.findPermanent("Ajani Unyielding")!!)
            val theirs = game.findPermanent("Tezzeret, Master of Metal")!!

            game.alternativeCrews(heart) shouldContainExactlyInAnyOrder mine
            game.execute(CrewVehicle(game.player1Id, heart, emptyList(), alternativeCostFrom = theirs)).error shouldBe
                "That permanent can't pay this Vehicle's alternative crew cost"
            game.loyalty(theirs) shouldBe 5
        }

        test("the alternative cost can't be combined with tapping creatures") {
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

            game.execute(CrewVehicle(game.player1Id, heart, listOf(bears), alternativeCostFrom = tezzeret)).error shouldBe
                "An alternative crew cost is paid instead of tapping creatures"
        }

        test("no creature crews it, and a planeswalker paying its last loyalty counter dies") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Heart of Kiran")
                .withCardOnBattlefield(1, "Tezzeret the Schemer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val heart = game.findPermanent("Heart of Kiran")!!
            val tezzeret = game.findPermanent("Tezzeret the Schemer")!!
            game.setLoyalty(tezzeret, 1)

            game.execute(CrewVehicle(game.player1Id, heart, emptyList(), alternativeCostFrom = tezzeret)).error shouldBe null
            game.resolveStack()

            withClue("a 0-loyalty planeswalker goes to the graveyard as a state-based action") {
                game.findPermanent("Tezzeret the Schemer") shouldBe null
            }
            game.state.projectedState.isCreature(heart) shouldBe true
            val crew = game.state.getEntity(heart)?.get<CrewSaddleContributorsComponent>()
            crew shouldNotBe null
            crew!!.creatureIds.shouldBeEmpty()
            crew.crewActivations shouldBe 1
        }

        test("a Vehicle that has lost all abilities has no alternative crew cost") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Heart of Kiran")
                .withCardOnBattlefield(1, "Tezzeret the Schemer")
                .withCardOnBattlefield(2, "Test Vehicle Suppressor")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val heart = game.findPermanent("Heart of Kiran")!!
            val tezzeret = game.findPermanent("Tezzeret the Schemer")!!

            game.alternativeCrews(heart).shouldBeEmpty()
            game.execute(CrewVehicle(game.player1Id, heart, emptyList(), alternativeCostFrom = tezzeret)).error shouldNotBe null
            game.loyalty(tezzeret) shouldBe 5
        }
    }
}
