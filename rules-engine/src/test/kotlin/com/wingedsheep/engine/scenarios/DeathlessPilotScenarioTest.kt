package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeathlessPilotScenarioTest : FunSpec({
    val crewFourVehicle = card("Crew Four Test Vehicle") {
        manaCost = "{4}"
        typeLine = "Artifact — Vehicle"
        power = 4
        toughness = 4
        keywordAbility(KeywordAbility.crew(4))
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(crewFourVehicle)
        initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("its projected power plus two pays crew") {
        val d = driver()
        val pilot = d.putCreatureOnBattlefield(d.player1, "Deathless Pilot")
        val vehicle = d.putPermanentOnBattlefield(d.player1, "Crew Four Test Vehicle")

        d.submitSuccess(CrewVehicle(d.player1, vehicle, listOf(pilot)))
        d.isTapped(pilot) shouldBe true
    }
})
