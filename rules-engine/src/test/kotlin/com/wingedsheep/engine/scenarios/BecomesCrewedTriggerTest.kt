package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever this Vehicle becomes crewed [for the first time each turn]" — fires when a crew
 * ability of the Vehicle resolves (CR 702.122e), not when it is activated.
 */
class BecomesCrewedTriggerTest : FunSpec({
    val everyTime = card("Every-Crew Test Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        triggeredAbility {
            trigger = Triggers.self.becomesCrewed()
            effect = Effects.GainLife(1)
        }
        keywordAbility(KeywordAbility.crew(1))
    }
    val firstTime = card("First-Crew Test Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        triggeredAbility {
            trigger = Triggers.self.becomesCrewed(firstTimeEachTurn = true)
            effect = Effects.GainLife(1)
        }
        keywordAbility(KeywordAbility.crew(1))
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(everyTime, firstTime))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("triggers when the crew ability resolves, not when it is activated") {
        val d = driver()
        val vehicle = d.putPermanentOnBattlefield(d.player1, "Every-Crew Test Vehicle")
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val life = d.getLifeTotal(d.player1)

        d.submitSuccess(CrewVehicle(d.player1, vehicle, listOf(bear)))
        d.stackSize shouldBe 1 // only the crew ability

        d.bothPass() // crew resolves -> trigger goes on the stack
        d.stackSize shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 1
    }

    test("an unqualified trigger fires on every crew; the first-time rider only once per turn") {
        val d = driver()
        val every = d.putPermanentOnBattlefield(d.player1, "Every-Crew Test Vehicle")
        val first = d.putPermanentOnBattlefield(d.player1, "First-Crew Test Vehicle")
        val bears = (1..4).map { d.putCreatureOnBattlefield(d.player1, "Grizzly Bears") }
        val life = d.getLifeTotal(d.player1)

        for (i in 0..1) {
            d.submitSuccess(CrewVehicle(d.player1, every, listOf(bears[i])))
            d.bothPass()
            d.bothPass()
        }
        d.getLifeTotal(d.player1) shouldBe life + 2

        for (i in 2..3) {
            d.submitSuccess(CrewVehicle(d.player1, first, listOf(bears[i])))
            d.bothPass()
            if (d.stackSize > 0) d.bothPass()
        }
        d.getLifeTotal(d.player1) shouldBe life + 3
    }

    test("the first-time rider resets each turn") {
        val d = driver()
        val first = d.putPermanentOnBattlefield(d.player1, "First-Crew Test Vehicle")
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val life = d.getLifeTotal(d.player1)

        d.submitSuccess(CrewVehicle(d.player1, first, listOf(bear)))
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 1

        d.passPriorityUntil(Step.UPKEEP) // the opponent's turn
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        val bear2 = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.submitSuccess(CrewVehicle(d.player1, first, listOf(bear2)))
        d.bothPass()
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 2
    }

    test("a Vehicle that leaves before its crew ability resolves never becomes crewed") {
        val d = driver()
        val vehicle = d.putPermanentOnBattlefield(d.player1, "Every-Crew Test Vehicle")
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val life = d.getLifeTotal(d.player1)

        d.submitSuccess(CrewVehicle(d.player1, vehicle, listOf(bear)))
        d.moveToGraveyard(vehicle)
        d.bothPass()

        d.stackSize shouldBe 0
        d.getLifeTotal(d.player1) shouldBe life
    }
})
