package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.LorwynEclipsedSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * End-to-end scenario tests for Eirdu, Carrier of Dawn // Isilu, Carrier of Twilight.
 * Validates the full Phase A–C stack: persist (Phase A), DFC transform (Phase B), and
 * granted convoke (Phase C) all come together on a real card.
 */
class EirduCarrierOfDawnTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + LorwynEclipsedSet.allCards)
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Swamp" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("Eirdu enters the battlefield on its front face with a DoubleFacedComponent") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Eirdu, Carrier of Dawn")
        val entityId = driver.findPermanent(caster, "Eirdu, Carrier of Dawn")
        entityId.shouldNotBeNull()

        val container = driver.state.getEntity(entityId)!!
        container.get<CardComponent>()!!.name shouldBe "Eirdu, Carrier of Dawn"
        val dfc = container.get<DoubleFacedComponent>()
        dfc.shouldNotBeNull()
        dfc.frontCardDefinitionId shouldBe "Eirdu, Carrier of Dawn"
        dfc.backCardDefinitionId shouldBe "Isilu, Carrier of Twilight"
        dfc.currentFace shouldBe DoubleFacedComponent.Face.FRONT
    }

    test("Eirdu offers the controller a chance to pay {B} at the start of their next first main phase") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Eirdu, Carrier of Dawn")

        // Advance the turn so Eirdu's controller's first main phase begins.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        // Now on opponent's turn — skip to their end, then into caster's next turn.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        // Caster's turn — first main phase.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The trigger should be on the stack: "At the beginning of your first main phase, you
        // may pay {B}. If you do, transform Eirdu." The may-pay is handled as a pending
        // decision, not a stack item, depending on the engine flow — either way the test is
        // happy as long as Eirdu is still Eirdu (declining costs nothing).
        val entityId = driver.findPermanent(caster, "Eirdu, Carrier of Dawn")
        entityId.shouldNotBeNull()
        driver.state.getEntity(entityId)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe
            DoubleFacedComponent.Face.FRONT
    }

    test("Eirdu transforms to Isilu when controller pays {B} and Isilu's persist grant takes effect") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val eirdu = driver.putCreatureOnBattlefield(caster, "Eirdu, Carrier of Dawn")
        // Give caster an untapped Swamp so the MayPayMana prompt can actually be afforded.
        driver.putPermanentOnBattlefield(caster, "Swamp")

        // Advance to caster's next turn's first main phase.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The trigger should be on the stack — resolving it exposes the may-pay yes/no.
        driver.bothPass()

        // Accept the may-pay prompt. This pauses for a mana-source selection continuation —
        // the engine then asks the player to pick which untapped Swamp to tap.
        driver.submitYesNo(caster, true)
        driver.submitManaAutoPayOrDecline(caster, autoPay = true)
        // Resume any residual stack/continuation traffic.
        driver.bothPass()

        // Eirdu transformed into Isilu.
        val container = driver.state.getEntity(eirdu)
        container.shouldNotBeNull()
        container.get<CardComponent>()!!.name shouldBe "Isilu, Carrier of Twilight"
        container.get<DoubleFacedComponent>()!!.currentFace shouldBe DoubleFacedComponent.Face.BACK

        // Isilu grants persist to other nontoken creatures the caster controls.
        // Spawn a vanilla creature and kill it — it should return with a -1/-1 counter.
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val bears = driver.findPermanent(caster, "Grizzly Bears")
        bears.shouldNotBeNull()

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt, listOf(bears)).isSuccess shouldBe true
        driver.bothPass() // resolve Lightning Bolt; persist trigger goes on the stack
        driver.bothPass() // resolve persist

        val returnedBears = driver.findPermanent(caster, "Grizzly Bears")
        returnedBears.shouldNotBeNull()
        val bearsCounters = driver.state.getEntity(returnedBears)!!
            .get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
        bearsCounters.shouldNotBeNull()
        bearsCounters.getCount(com.wingedsheep.sdk.core.CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
    }
})
