package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Wandering Emperor: flashed in on an opponent's turn, she may use a loyalty ability at
 * instant speed that turn — still only one — and goes back to sorcery timing on later turns.
 */
class TheWanderingEmperorScenarioTest : ScenarioTestBase() {
    private val name = "The Wandering Emperor"

    private fun TestGame.loyalty(index: Int, target: EntityId? = null) = ActivateAbility(
        player1Id,
        findPermanent(name)!!,
        cardRegistry.getCard(name)!!.script.activatedAbilities[index].id,
        targets = listOfNotNull(target).map { ChosenTarget.Permanent(it) },
    )

    private fun TestGame.offered(index: Int) =
        getLegalActions(1).any { (it.action as? ActivateAbility)?.abilityId == loyalty(index).abilityId }

    private fun TestGame.givePriorityTo1() {
        state = state.copy(priorityPlayerId = player1Id)
    }

    /** Bob's postcombat main, his Grizzly Bears tapped; Alice flashes the Emperor in off four Plains. */
    private fun flashedIn(): TestGame {
        val game = scenario().withPlayers("Alice", "Bob")
            .withCardInHand(1, name)
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
            .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
            .withActivePlayer(2).withPriorityPlayer(1)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()
        game.castSpell(1, name).error shouldBe null
        game.resolveStack()
        game.isOnBattlefield(name) shouldBe true
        game.givePriorityTo1()
        return game
    }

    init {
        test("flashed in on an opponent's turn, her -2 exiles a tapped creature at instant speed") {
            val game = flashedIn()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.offered(2) shouldBe true
            game.execute(game.loyalty(2, bears)).error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 22
        }

        test("only one loyalty ability the turn she enters") {
            val game = flashedIn()
            game.execute(game.loyalty(1)).error shouldBe null
            game.resolveStack()
            game.state.getBattlefield(game.player1Id).count { game.state.projectedState.isCreature(it) } shouldBe 1
            game.givePriorityTo1()
            game.offered(0) shouldBe false
            game.execute(game.loyalty(0)).error shouldNotBe null
        }

        test("on a later opponent's turn she is back to sorcery timing") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(2).withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.offered(1) shouldBe false
            game.execute(game.loyalty(1)).error shouldNotBe null
        }

        test("+1 puts a counter on up to one target creature and gives it first strike") {
            val game = flashedIn()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(game.loyalty(0, bears)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
        }

        test("+1 with no target still adds loyalty") {
            val game = flashedIn()
            game.execute(game.loyalty(0)).error shouldBe null
            game.resolveStack()
            game.state.getEntity(game.findPermanent(name)!!)!!
                .get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
        }
    }
}
