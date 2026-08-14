package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CrewSaddleCharacteristic
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage for contribution variants not used by the first three Aetherdrift cards.
 */
class CrewSaddleContributionScenarioTest : FunSpec({
    val toughnessPilot = card("Toughness Pilot") {
        manaCost = "{2}"
        typeLine = "Creature — Pilot"
        power = 1
        toughness = 4
        staticAbility {
            ability = CrewSaddleContribution(
                characteristic = CrewSaddleCharacteristic.TOUGHNESS
            )
        }
    }
    val crewFourVehicle = card("Contribution Test Vehicle") {
        manaCost = "{4}"
        typeLine = "Artifact — Vehicle"
        power = 4
        toughness = 4
        keywordAbility(KeywordAbility.crew(4))
    }

    test("a toughness contribution replaces rather than merely floors at power") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(toughnessPilot)
        d.registerCard(crewFourVehicle)
        d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val pilot = d.putCreatureOnBattlefield(d.player1, "Toughness Pilot")
        val vehicle = d.putPermanentOnBattlefield(d.player1, "Contribution Test Vehicle")

        d.submitSuccess(CrewVehicle(d.player1, vehicle, listOf(pilot)))
        d.isTapped(pilot) shouldBe true
    }
})
