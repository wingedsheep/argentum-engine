package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.Weatherlight
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Handler-level tests for [CrewVehicleHandler] covering the validation branches
 * and the execution path (tap crew creatures, put the "Vehicle becomes a
 * creature" activated ability on the stack).
 *
 * Rules reference (CR 702.122):
 *   - Crew N means "tap any number of other untapped creatures you control with
 *     total power N or greater: this permanent becomes an artifact creature
 *     until end of turn."
 *   - The crew cost doesn't use the tap symbol, so summoning sickness does NOT
 *     prevent a creature from being tapped to crew a Vehicle (702.122c-ish, as
 *     already noted in the handler).
 *   - A Vehicle can't crew itself (the crewing creatures must be "other").
 */
class CrewVehicleHandlerTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(Weatherlight))
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("validates: rejects the action when the player does not have priority") {
        val driver = setup()
        val opponent = driver.getOpponent(driver.activePlayer!!)
        val vehicle = driver.putPermanentOnBattlefield(opponent, "Weatherlight")
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // opponent doesn't have priority here — active player does
        val result = driver.submit(CrewVehicle(opponent, vehicle, listOf(bears)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("priority")
    }

    test("validates: rejects when the vehicle entity is not on the battlefield") {
        val driver = setup()
        val player = driver.activePlayer!!
        // Put Weatherlight in hand rather than on the battlefield.
        val vehicle = driver.putCardInHand(player, "Weatherlight")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(bears)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("not on the battlefield")
    }

    test("validates: rejects when the player does not control the vehicle") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val vehicle = driver.putPermanentOnBattlefield(opponent, "Weatherlight")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(bears)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("don't control this vehicle")
    }

    test("validates: rejects when the permanent has no Crew ability") {
        val driver = setup()
        val player = driver.activePlayer!!
        // Savannah Lions is a vanilla 1/1 creature — definitely no Crew.
        val notAVehicle = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val result = driver.submit(CrewVehicle(player, notAVehicle, listOf(bears)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("doesn't have crew")
    }

    test("validates: rejects with an empty crew creature list") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight")

        val result = driver.submit(CrewVehicle(player, vehicle, emptyList()))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("at least one creature")
    }

    test("validates: rejects when the vehicle tries to crew itself (CR 702.122)") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight")

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(vehicle)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("cannot crew itself")
    }

    test("validates: rejects when a crew creature is not a creature at all") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight")
        val forest = driver.putLandOnBattlefield(player, "Forest")

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(forest)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("Not a creature")
    }

    test("validates: rejects when a crew creature is already tapped") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight")
        val force = driver.putCreatureOnBattlefield(player, "Force of Nature") // 5/5
        driver.tapPermanent(force)

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(force)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("already tapped")
    }

    test("validates: rejects when the player does not control a crew creature") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight")
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(theirCreature)))

        result.isSuccess shouldBe false
        result.error.shouldNotBeNull().shouldContain("don't control creature")
    }

    test("validates: rejects when total power is less than the crew requirement") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight") // Crew 3
        // Grizzly Bears is 2/2 — alone, cannot pay Crew 3.
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(bears)))

        result.isSuccess shouldBe false
        val err = result.error.shouldNotBeNull()
        err.shouldContain("Total power")
        err.shouldContain("less than crew requirement")
    }

    test("executes: taps all crew creatures and puts the crew activation on the stack") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight") // Crew 3
        // Two 2/2 Bears can pay Crew 3 (combined power 4).
        val bearsA = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val bearsB = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val before = driver.stackSize
        val result = driver.submit(CrewVehicle(player, vehicle, listOf(bearsA, bearsB)))

        result.isSuccess shouldBe true
        driver.isTapped(bearsA) shouldBe true
        driver.isTapped(bearsB) shouldBe true
        // The Vehicle itself is NOT tapped by crewing — only the crew creatures.
        driver.isTapped(vehicle) shouldBe false
        // One activated ability was pushed onto the stack.
        driver.stackSize shouldBe (before + 1)
        val topOfStack = driver.getTopOfStack().shouldNotBeNull()
        driver.state.getEntity(topOfStack)?.get<ActivatedAbilityOnStackComponent>()
            .shouldNotBeNull()
            .sourceName shouldBe "Weatherlight"
        // Priority returns to the activating player (per the handler contract).
        driver.state.priorityPlayerId shouldBe player
    }

    test("executes: summoning sickness does not prevent crewing (CR 702.122)") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight") // Crew 3
        // Force of Nature: 5/5 — plenty of power, but putCreatureOnBattlefield
        // leaves it with SummoningSicknessComponent attached.
        val force = driver.putCreatureOnBattlefield(player, "Force of Nature")

        val result = driver.submit(CrewVehicle(player, vehicle, listOf(force)))

        result.isSuccess shouldBe true
        driver.isTapped(force) shouldBe true
    }

    test("executes: Weatherlight becomes an artifact creature after the crew ability resolves") {
        val driver = setup()
        val player = driver.activePlayer!!
        val vehicle = driver.putPermanentOnBattlefield(player, "Weatherlight") // base 4/5
        val force = driver.putCreatureOnBattlefield(player, "Force of Nature") // 5/5, pays Crew 3

        // Sanity check: Weatherlight is an artifact, not a creature, before crewing.
        driver.state.projectedState.isCreature(vehicle) shouldBe false

        driver.submit(CrewVehicle(player, vehicle, listOf(force))).isSuccess shouldBe true
        // Resolve the crew activation currently on the stack.
        var guard = 0
        while (driver.stackSize > 0 && guard < 10) {
            driver.bothPass()
            guard++
        }

        // After resolution, the Vehicle is a creature with its printed P/T for
        // the rest of the turn.
        val projected = driver.state.projectedState
        projected.isCreature(vehicle) shouldBe true
        projected.getPower(vehicle) shouldBe 4
        projected.getToughness(vehicle) shouldBe 5
    }
})
