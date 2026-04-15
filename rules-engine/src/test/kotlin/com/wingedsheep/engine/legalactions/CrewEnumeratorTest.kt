package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.EnumerationFixtures
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.Weatherlight
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.CrewEnumerator] using Weatherlight (Crew 3) as the
 * test Vehicle.
 */
class CrewEnumeratorTest : FunSpec({

    fun driverWithVehicleAndCreatures(creatures: List<String>): com.wingedsheep.engine.legalactions.support.EnumerationTestDriver {
        val driver = com.wingedsheep.engine.legalactions.support.EnumerationTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(Weatherlight))
        driver.game.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Weatherlight" to 5, "Grizzly Bears" to 5),
            skipMulligans = true
        )
        driver.game.passPriorityUntil(com.wingedsheep.sdk.core.Step.PRECOMBAT_MAIN)

        // Place Weatherlight on P1's battlefield.
        var state = driver.game.state
        val weatherlightId = state.getHand(driver.player1).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Weatherlight"
        } ?: run {
            // Not in opening hand — synthesize a battlefield Weatherlight via library.
            state.getLibrary(driver.player1).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Weatherlight"
            }
        }
        val fromZone = if (state.getHand(driver.player1).contains(weatherlightId))
            ZoneKey(driver.player1, Zone.HAND)
        else ZoneKey(driver.player1, Zone.LIBRARY)
        state = state.moveToZone(weatherlightId, fromZone, ZoneKey(driver.player1, Zone.BATTLEFIELD))

        // Move requested creatures from library/hand onto P1's battlefield.
        for (creatureName in creatures) {
            val creatureId = state.getHand(driver.player1).firstOrNull { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == creatureName
            } ?: state.getLibrary(driver.player1).firstOrNull { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == creatureName
            } ?: error("could not find $creatureName in P1's hand or library")
            val src = if (state.getHand(driver.player1).contains(creatureId))
                ZoneKey(driver.player1, Zone.HAND)
            else ZoneKey(driver.player1, Zone.LIBRARY)
            state = state.moveToZone(creatureId, src, ZoneKey(driver.player1, Zone.BATTLEFIELD))
        }
        driver.game.replaceState(state)
        return driver
    }

    test("a Vehicle on the battlefield surfaces a CrewVehicle action") {
        val driver = driverWithVehicleAndCreatures(creatures = listOf("Grizzly Bears"))

        val crewActions = driver.enumerateFor(driver.player1).filter { it.actionType == "CrewVehicle" }

        crewActions shouldHaveSize 1
        val crew = crewActions.single()
        crew.hasCrew shouldBe true
        crew.crewPower shouldBe 3
    }

    test("Grizzly Bears (power 2) cannot pay Crew 3 alone — unaffordable") {
        val driver = driverWithVehicleAndCreatures(creatures = listOf("Grizzly Bears"))

        val crew = driver.enumerateFor(driver.player1)
            .single { it.actionType == "CrewVehicle" }

        crew.affordable shouldBe false
        crew.crewCreatures!!.map { it.power }.sum() shouldBe 2  // only Bears' 2
    }

    test("two Grizzly Bears (combined power 4) can afford Crew 3 — affordable") {
        val driver = driverWithVehicleAndCreatures(
            creatures = listOf("Grizzly Bears", "Grizzly Bears")
        )

        val crew = driver.enumerateFor(driver.player1)
            .single { it.actionType == "CrewVehicle" }

        crew.affordable shouldBe true
        crew.crewCreatures!! shouldHaveSize 2
    }

    test("the Vehicle itself does not appear in its own crew creature list") {
        val driver = driverWithVehicleAndCreatures(creatures = listOf("Grizzly Bears"))

        val crew = driver.enumerateFor(driver.player1)
            .single { it.actionType == "CrewVehicle" }

        crew.crewCreatures!!.map { it.name } shouldBe listOf("Grizzly Bears")
    }

    test("tapped creatures are excluded from the crew list") {
        val driver = driverWithVehicleAndCreatures(
            creatures = listOf("Grizzly Bears", "Grizzly Bears")
        )

        // Tap one of the Bears.
        val firstBearsId = driver.game.state.getBattlefield(driver.player1)
            .first { id -> driver.game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears" }
        val tapped = driver.game.state.getEntity(firstBearsId)!!.with(TappedComponent)
        driver.game.replaceState(driver.game.state.withEntity(firstBearsId, tapped))

        val crew = driver.enumerateFor(driver.player1)
            .single { it.actionType == "CrewVehicle" }

        crew.crewCreatures!! shouldHaveSize 1  // the untapped one only
    }

    test("a battlefield with no Vehicles produces no CrewVehicle actions") {
        val driver = EnumerationFixtures.allForestsMainPhase()
        val forestId = driver.game.state.getHand(driver.player1).first()
        driver.game.playLand(driver.player1, forestId)

        val crewActions = driver.enumerateFor(driver.player1).filter { it.actionType == "CrewVehicle" }

        crewActions.shouldBeEmpty()
    }
})
