package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Can't crew Vehicles" ([AbilityFlag.CANT_CREW]): the creature can't be tapped to pay a crew cost
 * (CR 702.122a; Intercessor's Arrest ruling 2022-02-18), in both the legal-action enumeration and the
 * crew handler. Saddling a Mount is a different cost and stays open.
 */
class CantCrewScenarioTest : FunSpec({
    val groundedPilot = card("Grounded Pilot") {
        manaCost = "{2}"
        typeLine = "Creature — Pilot"
        power = 3
        toughness = 3
        staticAbility {
            ability = GrantKeyword(AbilityFlag.CANT_CREW.name, GroupFilter.source())
        }
    }
    val crewTwoVehicle = card("Crew Two Test Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.crew(2))
    }
    val saddleTwoMount = card("Saddle Two Test Mount") {
        manaCost = "{2}"
        typeLine = "Creature — Mount"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.saddle(2))
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(groundedPilot, crewTwoVehicle, saddleTwoMount))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a creature that can't crew is not offered as a crew candidate") {
        val game = driver()
        val pilot = game.putCreatureOnBattlefield(game.player1, "Grounded Pilot")
        val bears = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        game.putPermanentOnBattlefield(game.player1, "Crew Two Test Vehicle")

        val crew = game.legalActions(game.player1).single { it.actionType == "CrewVehicle" }
        val candidates = crew.tapForPowerCreatures!!.map { it.entityId }
        candidates shouldContain bears
        candidates shouldNotContain pilot
    }

    test("crewing with a creature that can't crew is rejected and taps nothing") {
        val game = driver()
        val pilot = game.putCreatureOnBattlefield(game.player1, "Grounded Pilot")
        val vehicle = game.putPermanentOnBattlefield(game.player1, "Crew Two Test Vehicle")

        game.submit(CrewVehicle(game.player1, vehicle, listOf(pilot))).error shouldNotBe null
        game.isTapped(pilot) shouldBe false
        game.state.projectedState.isCreature(vehicle) shouldBe false
    }

    test("the only creature able to pay the cost can't crew, so the crew is unaffordable") {
        val game = driver()
        game.putCreatureOnBattlefield(game.player1, "Grounded Pilot")
        game.putPermanentOnBattlefield(game.player1, "Crew Two Test Vehicle")

        val crew = game.legalActions(game.player1).single { it.actionType == "CrewVehicle" }
        crew.affordable shouldBe false
    }

    test("other creatures still crew normally") {
        val game = driver()
        game.putCreatureOnBattlefield(game.player1, "Grounded Pilot")
        val bears = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        val vehicle = game.putPermanentOnBattlefield(game.player1, "Crew Two Test Vehicle")

        game.submitSuccess(CrewVehicle(game.player1, vehicle, listOf(bears)))
        game.isTapped(bears) shouldBe true
    }

    test("a creature that can't crew can still saddle a Mount") {
        val game = driver()
        val pilot = game.putCreatureOnBattlefield(game.player1, "Grounded Pilot")
        val mount = game.putCreatureOnBattlefield(game.player1, "Saddle Two Test Mount")

        game.submitSuccess(SaddleMount(game.player1, mount, listOf(pilot)))
        game.isTapped(pilot) shouldBe true
    }
})
