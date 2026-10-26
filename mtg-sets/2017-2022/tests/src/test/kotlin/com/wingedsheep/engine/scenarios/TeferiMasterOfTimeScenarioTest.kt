package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Teferi, Master of Time: his loyalty abilities work at instant speed on any player's turn, still
 * one per turn — so once on an opponent's turn and once again on his controller's own.
 */
class TeferiMasterOfTimeScenarioTest : ScenarioTestBase() {
    private val name = "Teferi, Master of Time"

    private fun TestGame.loyalty(index: Int, target: EntityId? = null) = ActivateAbility(
        player1Id,
        findPermanent(name)!!,
        cardRegistry.getCard(name)!!.script.activatedAbilities[index].id,
        targets = listOfNotNull(target).map { ChosenTarget.Permanent(it) },
    )

    private fun TestGame.offered(index: Int) =
        getLegalActions(1).any { (it.action as? ActivateAbility)?.abilityId == loyalty(index).abilityId }

    private fun TestGame.teferiLoyalty() =
        state.getEntity(findPermanent(name)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    private fun TestGame.setTeferiLoyalty(n: Int) {
        val id = findPermanent(name)!!
        state = state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, n)) }
    }

    /** Bob's combat — not a main phase, and not Alice's turn — with Alice holding priority. */
    private fun onBobsTurn(): TestGame = scenario().withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, name)
        .withCardInHand(1, "Island")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains")
        .withActivePlayer(2).withPriorityPlayer(1)
        .inPhase(Phase.BEGINNING, Step.UPKEEP)
        .build()

    init {
        test("+1 loots at instant speed during an opponent's upkeep") {
            val game = onBobsTurn()
            game.offered(0) shouldBe true
            game.execute(game.loyalty(0)).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldNotBe null
            game.selectCards(listOf(game.findCardsInHand(1, "Island").first()))
            game.handSize(1) shouldBe 1
            game.teferiLoyalty() shouldBe 4
        }

        test("still only one loyalty ability per turn, but another on the next player's turn") {
            val game = onBobsTurn()
            game.execute(game.loyalty(0)).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(game.findCardsInHand(1, "Island").first()))
            game.state = game.state.copy(priorityPlayerId = game.player1Id)
            game.offered(0) shouldBe false
            game.execute(game.loyalty(0)).error shouldNotBe null

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state = game.state.copy(priorityPlayerId = game.player1Id)
            game.offered(0) shouldBe true
            game.execute(game.loyalty(0)).error shouldBe null
        }

        test("-3 phases out a creature you don't control until its controller's next turn") {
            val game = onBobsTurn()
            game.setTeferiLoyalty(5)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(game.loyalty(1, bears)).error shouldBe null
            game.resolveStack()
            game.state.getEntity(bears)!!.has<PhasedOutComponent>() shouldBe true
            game.teferiLoyalty() shouldBe 2

            // Alice's turn passes; the Bears phase back in as Bob's next turn begins.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.getEntity(bears)!!.has<PhasedOutComponent>() shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.getEntity(bears)!!.has<PhasedOutComponent>() shouldBe false
        }

        test("-3 can't target a creature you control") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.setTeferiLoyalty(5)
            game.execute(game.loyalty(1, game.findPermanent("Grizzly Bears")!!)).error shouldNotBe null
        }

        test("-10 gives you two extra turns after this one") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, name)
                .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1).withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.setTeferiLoyalty(10)
            game.execute(game.loyalty(2)).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, name) shouldBe true

            repeat(2) {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
            }
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
        }
    }
}
