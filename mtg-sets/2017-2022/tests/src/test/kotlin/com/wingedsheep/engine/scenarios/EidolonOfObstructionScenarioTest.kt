package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Eidolon of Obstruction (THB #12) — {1}{W} Enchantment Creature — Spirit 2/1.
 *
 * "First strike
 * Loyalty abilities of planeswalkers your opponents control cost {1} more to activate."
 *
 * Ruling: the tax adds a mana component; it doesn't change how much loyalty the ability costs.
 */
class EidolonOfObstructionScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyalty(name: String): Int =
        state.getEntity(findPermanent(name)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    private fun TestGame.canActivate(walker: String, cost: AbilityCost): Boolean {
        val abilityId = cardRegistry.getCard(walker)!!.script.activatedAbilities.first { it.cost == cost }.id
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.abilityId == abilityId && it.isAffordable }
    }

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun board(eidolonOwner: Int, plains: Int, walker: String = "Jace Beleren") = scenario()
        .withPlayers("Player", "Opponent")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .withCardOnBattlefield(eidolonOwner, "Eidolon of Obstruction")
        .withCardOnBattlefield(1, walker)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", plains)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .build()

    init {
        test("an opponent's loyalty abilities can't be activated without the extra {1}") {
            val game = board(eidolonOwner = 2, plains = 0)
            game.canActivate("Jace Beleren", AbilityCost.Loyalty(2)) shouldBe false
        }

        test("the tax is {1} on top of the unchanged loyalty cost") {
            val game = board(eidolonOwner = 2, plains = 1)
            game.canActivate("Jace Beleren", AbilityCost.Loyalty(2)) shouldBe true

            val jace = game.findPermanent("Jace Beleren")!!
            val plusTwo = cardRegistry.getCard("Jace Beleren")!!.script.activatedAbilities
                .first { it.cost == AbilityCost.Loyalty(2) }
            game.execute(ActivateAbility(game.player1Id, jace, plusTwo.id)).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.loyalty("Jace Beleren") shouldBe 5
            game.state.getEntity(game.findPermanent("Plains")!!)!!.has<TappedComponent>() shouldBe true
            game.state.getHand(game.player1Id).size shouldBe 1
        }

        test("your own planeswalkers aren't taxed") {
            val game = board(eidolonOwner = 1, plains = 0)
            game.canActivate("Jace Beleren", AbilityCost.Loyalty(2)) shouldBe true
        }

        test("a taxed [−X] still asks for X and removes exactly X loyalty") {
            val game = board(eidolonOwner = 2, plains = 1, walker = "Chandra Nalaar")
            val chandra = game.findPermanent("Chandra Nalaar")!!
            val minusX = cardRegistry.getCard("Chandra Nalaar")!!.script.activatedAbilities
                .first { it.cost == AbilityCost.LoyaltyX }
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(game.player1Id, chandra, minusX.id, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            decision.maxValue shouldBe 6
            game.chooseNumber(2).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            game.loyalty("Chandra Nalaar") shouldBe 4
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.state.getEntity(game.findPermanent("Plains")!!)!!.has<TappedComponent>() shouldBe true
        }
    }
}
