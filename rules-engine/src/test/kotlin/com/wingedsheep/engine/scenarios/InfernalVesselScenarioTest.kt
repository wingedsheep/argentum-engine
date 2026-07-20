package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.InfernalVessel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Infernal Vessel {2}{B} — Creature — Human Cleric 2/1.
 *
 * "When this creature dies, if it wasn't a Demon, return it to the battlefield under its owner's
 *  control with two +1/+1 counters on it. It's a Demon in addition to its other types."
 *
 * The interesting behaviour is the self-recursion guard. The first death returns the Vessel as a
 * 4/3 Demon; the *second* death must not return it again, and the only thing distinguishing the two
 * is the Demon subtype the card granted itself — which by then exists solely as last-known
 * information on a card sitting in the graveyard. That is what
 * `Conditions.TriggeringEntityHadSubtype` reads.
 *
 * Deaths are staged with real damage (Lightning Bolt, 3 damage) rather than the blunt
 * `moveToGraveyard` test helper, which by design skips dies triggers entirely. Three damage is
 * lethal to both the printed 2/1 and the returned 4/3.
 */
class InfernalVesselScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(InfernalVessel))
        return driver
    }

    /** Bolt [creature] for 3 and let the resulting death trigger (if any) resolve. */
    fun bolt(driver: GameTestDriver, caster: EntityId, creature: EntityId) {
        driver.giveMana(caster, Color.RED, 1)
        val boltCard = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, boltCard, targets = listOf(creature)).error shouldBe null
        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass()
            safety++
        }
    }

    fun vesselOnBattlefield(driver: GameTestDriver, player: EntityId): EntityId? =
        driver.findPermanent(player, "Infernal Vessel")

    test("first death: returns as a 4/3 that is a Demon in addition to Human Cleric") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val vessel = driver.putCreatureOnBattlefield(you, "Infernal Vessel")
        bolt(driver, you, vessel)

        val returned = vesselOnBattlefield(driver, you)
        (returned != null) shouldBe true

        val projected = projector.project(driver.state)
        projected.getPower(returned!!) shouldBe 4      // 2 + two +1/+1 counters
        projected.getToughness(returned) shouldBe 3    // 1 + two +1/+1 counters

        val subtypes = projected.getSubtypes(returned)
        subtypes.contains(Subtype.DEMON.value) shouldBe true
        // "in addition to its other types" — the printed types survive.
        subtypes.contains("Human") shouldBe true
        subtypes.contains("Cleric") shouldBe true

        driver.getGraveyardCardNames(you).contains("Infernal Vessel") shouldBe false
    }

    test("second death: the granted Demon type stops it from returning again") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val vessel = driver.putCreatureOnBattlefield(you, "Infernal Vessel")
        bolt(driver, you, vessel)
        val returned = vesselOnBattlefield(driver, you)!!

        // It is a Demon now, so this death must NOT trigger the return.
        bolt(driver, you, returned)

        vesselOnBattlefield(driver, you) shouldBe null
        driver.getGraveyardCardNames(you).contains("Infernal Vessel") shouldBe true
    }

    test("returns under its OWNER's control even when an opponent controlled it at death") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val thief = driver.getOpponent(owner)

        val vessel = driver.putCreatureOnBattlefield(owner, "Infernal Vessel")
        driver.replaceState(
            driver.state.updateEntity(vessel) { it.with(ControllerComponent(thief)) }
        )
        driver.getController(vessel) shouldBe thief

        bolt(driver, owner, vessel)

        (vesselOnBattlefield(driver, owner) != null) shouldBe true
        vesselOnBattlefield(driver, thief) shouldBe null
    }
})
