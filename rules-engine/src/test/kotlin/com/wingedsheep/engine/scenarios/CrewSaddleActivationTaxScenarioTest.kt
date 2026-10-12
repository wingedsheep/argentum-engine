package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Crew and saddle are activated abilities (CR 702.122a, 702.171a), so an activated-ability tax
 * reaches them even though their printed cost has no mana: "{2}, tap creatures with total power N".
 */
class CrewSaddleActivationTaxScenarioTest : FunSpec({
    val taxField = card("Tax Field") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment"
        staticAbility {
            ability = IncreaseActivatedAbilityCost(
                filter = GroupFilter(GameObjectFilter.Any),
                amount = DynamicAmounts.fixed(2),
                excludeManaAbilities = true,
            )
        }
    }
    val vehicle = card("Tax Test Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.crew(1))
    }
    val mount = card("Tax Test Mount") {
        manaCost = "{2}"
        typeLine = "Creature — Mount"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.saddle(1))
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(taxField)
        d.registerCard(vehicle)
        d.registerCard(mount)
        d.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(d.player1, "Tax Field")
        return d
    }

    fun GameTestDriver.crewAction() = legalActions(player1).first { it.action is CrewVehicle }
    fun GameTestDriver.saddleAction() = legalActions(player1).first { it.action is SaddleMount }

    test("a taxed crew is unaffordable and refused without the mana") {
        val d = driver()
        val pilot = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val v = d.putPermanentOnBattlefield(d.player1, "Tax Test Vehicle")

        d.crewAction().affordable shouldBe false
        d.crewAction().manaCostString shouldBe "{2}"
        d.submitExpectFailure(CrewVehicle(d.player1, v, listOf(pilot)))
        d.isTapped(pilot) shouldBe false
    }

    test("a taxed crew pays {2} on top of tapping the crew") {
        val d = driver()
        val pilot = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val v = d.putPermanentOnBattlefield(d.player1, "Tax Test Vehicle")
        val forests = listOf(
            d.putLandOnBattlefield(d.player1, "Forest"),
            d.putLandOnBattlefield(d.player1, "Forest"),
        )

        d.crewAction().affordable shouldBe true
        d.submitSuccess(CrewVehicle(d.player1, v, listOf(pilot)))
        d.isTapped(pilot) shouldBe true
        forests.all { d.isTapped(it) } shouldBe true
    }

    test("a creature tapped to crew can't also be tapped to pay the tax") {
        val d = driver()
        val elves = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
        d.removeSummoningSickness(elves)
        val v = d.putPermanentOnBattlefield(d.player1, "Tax Test Vehicle")
        val forest = d.putLandOnBattlefield(d.player1, "Forest")

        d.submitExpectFailure(CrewVehicle(d.player1, v, listOf(elves)))
        d.isTapped(elves) shouldBe false
        d.isTapped(forest) shouldBe false
    }

    test("saddle is taxed the same way") {
        val d = driver()
        val rider = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val m = d.putCreatureOnBattlefield(d.player1, "Tax Test Mount")

        d.saddleAction().affordable shouldBe false
        d.submitExpectFailure(SaddleMount(d.player1, m, listOf(rider)))

        val forests = listOf(
            d.putLandOnBattlefield(d.player1, "Forest"),
            d.putLandOnBattlefield(d.player1, "Forest"),
        )
        d.saddleAction().affordable shouldBe true
        d.submitSuccess(SaddleMount(d.player1, m, listOf(rider)))
        forests.all { d.isTapped(it) } shouldBe true
    }

    test("an untaxed crew stays free") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(vehicle)
        d.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val pilot = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.putPermanentOnBattlefield(d.player1, "Tax Test Vehicle")

        d.crewAction().affordable shouldBe true
        d.crewAction().manaCostString shouldBe null
    }
})
