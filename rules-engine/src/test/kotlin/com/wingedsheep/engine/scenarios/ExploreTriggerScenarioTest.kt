package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.CenoteScout
import com.wingedsheep.mtg.sets.definitions.lci.cards.MerfolkCaveDiver
import com.wingedsheep.mtg.sets.definitions.lci.cards.NicanzilCurrentConductor
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Exercises the explore trigger feature (CR 701.44) — [com.wingedsheep.sdk.dsl.Triggers.creatureExplores]
 * / `WheneverCreatureYouControlExplores[Land|Nonland]` — via two LCI observers:
 *
 *  - **Merfolk Cave-Diver** — "Whenever a creature you control explores, this creature gets +1/+0
 *    and can't be blocked this turn." (ANY reveal.)
 *  - **Nicanzil, Current Conductor** — a land-card explore lets you put a land from hand tapped;
 *    a nonland-card explore puts a +1/+1 counter on Nicanzil. (LAND vs NONLAND split.)
 *
 * A separate [CenoteScout] ("When this creature enters, it explores") is the exploring creature the
 * observers watch. Seeding the top of the library controls the reveal outcome. Covers the happy
 * paths, the land/nonland split, and the empty-library edge (CR 701.44b — the permanent still
 * explores, so ANY fires but LAND/NONLAND do not).
 */
class ExploreTriggerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CenoteScout)
        driver.registerCard(MerfolkCaveDiver)
        driver.registerCard(NicanzilCurrentConductor)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast Cenote Scout from hand so its ETB explore fires with the observer already in play. */
    fun castCenoteScout(driver: GameTestDriver, controller: EntityId) {
        val scout = driver.putCardInHand(controller, "Cenote Scout")
        driver.giveMana(controller, Color.GREEN, 1)
        driver.castSpell(controller, scout)
    }

    // -------------------------------------------------------------------------
    // Merfolk Cave-Diver — ANY explore fires the pump + evasion
    // -------------------------------------------------------------------------
    test("a creature you control exploring pumps Merfolk Cave-Diver and makes it unblockable") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30))
        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caveDiver = driver.putCreatureOnBattlefield(controller, "Merfolk Cave-Diver")
        // Land on top → explore takes the land-to-hand branch (no graveyard decision).
        driver.putCardOnTopOfLibrary(controller, "Forest")

        castCenoteScout(driver, controller)
        driver.bothPass() // Cenote Scout resolves, enters → ETB explore trigger on stack
        driver.bothPass() // explore resolves (Forest → hand) → Cave-Diver trigger on stack
        driver.bothPass() // Cave-Diver trigger resolves → +1/+0 and can't be blocked

        driver.state.projectedState.getPower(caveDiver) shouldBe 3
        driver.state.projectedState.getToughness(caveDiver) shouldBe 4
        driver.state.projectedState.hasKeyword(caveDiver, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
    }

    // -------------------------------------------------------------------------
    // Nicanzil — LAND explore fires the land-drop, not the counter
    // -------------------------------------------------------------------------
    test("Nicanzil: exploring a land card lets you put a land from hand tapped (no counter)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30))
        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nicanzil = driver.putCreatureOnBattlefield(controller, "Nicanzil, Current Conductor")
        // Land on top → LAND explore; the revealed Forest goes to hand and becomes the land we drop.
        driver.putCardOnTopOfLibrary(controller, "Forest")

        castCenoteScout(driver, controller)
        driver.bothPass() // Cenote Scout enters → ETB explore trigger on stack
        driver.bothPass() // explore resolves (Forest → hand) → Nicanzil LAND trigger on stack
        driver.bothPass() // Nicanzil LAND trigger resolves → putFromHand pauses for the land choice

        // The land trigger fired: a "put a land from hand" selection is pending.
        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        val forestInHand = driver.findCardInHand(controller, "Forest")
        forestInHand.shouldNotBeNull()
        driver.submitDecision(controller, CardsSelectedResponse(decision.id, listOf(forestInHand)))

        // The chosen land entered tapped; the nonland trigger never fired (Nicanzil has no counter).
        val forestOnBattlefield = driver.findPermanent(controller, "Forest")
        forestOnBattlefield.shouldNotBeNull()
        driver.isTapped(forestOnBattlefield) shouldBe true
        plusOneCounters(driver, nicanzil) shouldBe 0
    }

    // -------------------------------------------------------------------------
    // Nicanzil — NONLAND explore fires the counter, not the land-drop
    // -------------------------------------------------------------------------
    test("Nicanzil: exploring a nonland card puts a +1/+1 counter on Nicanzil (no land-drop)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30))
        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nicanzil = driver.putCreatureOnBattlefield(controller, "Nicanzil, Current Conductor")
        // Nonland on top → NONLAND explore: +1/+1 on the explorer, then a graveyard decision.
        driver.putCardOnTopOfLibrary(controller, "Grizzly Bears")

        castCenoteScout(driver, controller)
        driver.bothPass() // Cenote Scout enters → ETB explore trigger on stack
        driver.bothPass() // explore reveals nonland → +1/+1 on Cenote Scout → pauses for graveyard choice

        // Resolve the explore's own "top or graveyard" YesNo decision (No = graveyard). Only once
        // this continuation completes does the explored event fire (CR 701.44b), queuing Nicanzil's
        // NONLAND trigger.
        driver.pendingDecision.shouldNotBeNull()
        driver.submitYesNo(controller, false)
        driver.bothPass() // Nicanzil NONLAND trigger resolves → +1/+1 counter on Nicanzil

        plusOneCounters(driver, nicanzil) shouldBe 1
        // No pending land-drop selection — the LAND trigger never fired.
        driver.pendingDecision shouldBe null
    }

    // -------------------------------------------------------------------------
    // Empty library — CR 701.44b: the permanent still explores
    // -------------------------------------------------------------------------
    test("empty-library explore still fires ANY (Cave-Diver) but not LAND/NONLAND (Nicanzil)") {
        val driver = createDriver()
        // A 7-card deck empties the library after the opening hand; turn-1 draw step is skipped.
        driver.initMirrorMatch(deck = Deck.of("Plains" to 7))
        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caveDiver = driver.putCreatureOnBattlefield(controller, "Merfolk Cave-Diver")
        val nicanzil = driver.putCreatureOnBattlefield(controller, "Nicanzil, Current Conductor")
        driver.state.getLibrary(controller).isEmpty() shouldBe true

        castCenoteScout(driver, controller)
        driver.bothPass() // Cenote Scout enters → ETB explore trigger on stack
        driver.bothPass() // explore resolves (empty library, no reveal) → Cave-Diver ANY trigger on stack
        driver.bothPass() // Cave-Diver trigger resolves

        // ANY fired for Cave-Diver; neither of Nicanzil's reveal-typed triggers fired.
        driver.state.projectedState.getPower(caveDiver) shouldBe 3
        driver.state.projectedState.hasKeyword(caveDiver, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        plusOneCounters(driver, nicanzil) shouldBe 0
        driver.pendingDecision shouldBe null
    }
})
