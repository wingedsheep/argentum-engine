package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.AirElemental
import com.wingedsheep.mtg.sets.definitions.lea.cards.Earthbind
import com.wingedsheep.mtg.sets.definitions.lea.cards.Flight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Earthbind — {R} Aura: "When this Aura enters, if enchanted creature has flying, this Aura deals
 * 2 damage to that creature and this Aura gains 'Enchanted creature loses flying.'"
 *
 * Pins the two things the script can't show: the gained static is projected (Layer 6) onto the
 * host only while Earthbind is on the battlefield, and the intervening-if is rechecked on
 * resolution.
 */
class EarthbindScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Earthbind)
        driver.registerCard(AirElemental)
        driver.registerCard(Flight)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.flies(id: EntityId): Boolean =
        state.projectedState.hasKeyword(id, Keyword.FLYING)

    fun GameTestDriver.castEarthbindOn(creature: EntityId): EntityId {
        val p1 = activePlayer!!
        val aura = putCardInHand(p1, Earthbind.name)
        giveMana(p1, Color.RED, 1)
        castSpell(p1, aura, listOf(creature)).error shouldBe null
        bothPass() // resolve Earthbind; its enters trigger goes on the stack
        return aura
    }

    test("a flier takes 2 damage and loses flying until Earthbind leaves") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val elemental = driver.putCreatureOnBattlefield(p1, AirElemental.name)

        val aura = driver.castEarthbindOn(elemental)
        driver.state.stack.size shouldBe 1
        driver.bothPass() // resolve the trigger

        driver.damageOn(elemental) shouldBe 2
        driver.flies(elemental) shouldBe false

        driver.moveToGraveyard(aura)
        driver.flies(elemental) shouldBe true
    }

    test("a creature without flying is not damaged and the trigger never fires") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser")

        driver.castEarthbindOn(courser)
        driver.state.stack.size shouldBe 0
        driver.damageOn(courser) shouldBe 0
    }

    test("the intervening-if is rechecked: losing flying before resolution means no damage") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser")
        val flight = driver.putCardInHand(p1, Flight.name)
        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpell(p1, flight, listOf(courser)).error shouldBe null
        driver.bothPass()
        driver.flies(courser) shouldBe true

        driver.castEarthbindOn(courser)
        driver.state.stack.size shouldBe 1
        driver.moveToGraveyard(flight) // flying gone with the trigger still on the stack
        driver.bothPass()

        driver.damageOn(courser) shouldBe 0
    }

    test("ruling: a creature that gains flying after Earthbind has flying (timestamp order)") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val elemental = driver.putCreatureOnBattlefield(p1, AirElemental.name)
        driver.castEarthbindOn(elemental)
        driver.bothPass()
        driver.flies(elemental) shouldBe false

        val flight = driver.putCardInHand(p1, Flight.name)
        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpell(p1, flight, listOf(elemental)).error shouldBe null
        driver.bothPass()

        driver.flies(elemental) shouldBe true
    }
})
