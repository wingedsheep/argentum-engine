package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BackOnTrackScenarioTest : FunSpec({
    val crewThreeVehicle = card("Crew Three Test Vehicle") {
        manaCost = "{3}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.crew(3))
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(crewThreeVehicle)
        initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("returns the target and creates a Pilot token whose contribution pays crew three") {
        val d = driver()
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, "Back on Track")
        d.giveMana(d.player1, Color.BLACK, 5)

        d.submitSuccess(
            CastSpell(
                playerId = d.player1,
                cardId = spell,
                targets = listOf(ChosenTarget.Card(bear, d.player1, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        d.bothPass()

        d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD)).contains(bear) shouldBe true
        val pilot = d.getPermanents(d.player1).single { d.getCardName(it) == "Pilot Token" }
        val vehicle = d.putPermanentOnBattlefield(d.player1, "Crew Three Test Vehicle")
        d.submitSuccess(CrewVehicle(d.player1, vehicle, listOf(pilot)))
        d.isTapped(pilot) shouldBe true
    }
})
