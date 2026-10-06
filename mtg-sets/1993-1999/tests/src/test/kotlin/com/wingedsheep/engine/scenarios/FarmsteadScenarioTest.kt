package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Farmstead
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Farmstead (Limited Edition Alpha).
 *
 * Enchant land. Enchanted land has "At the beginning of your upkeep, you may pay {W}{W}. If you do,
 * you gain 1 life."
 *
 * The granted trigger belongs to the land, so "your upkeep" and "you" are the *land's* controller,
 * not the Aura's. The Aura goes on an opponent's land to tell the two apart.
 */
class FarmsteadScenarioTest : FunSpec({

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Farmstead)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val landOwner = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val land = driver.putPermanentOnBattlefield(landOwner, "Plains")
        val aura = driver.putPermanentOnBattlefield(me, "Farmstead")
        attach(driver, aura, land)
        return Triple(driver, me, landOwner)
    }

    fun settle(driver: GameTestDriver, payer: EntityId, pay: Boolean) {
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            if (driver.pendingDecision == null) driver.bothPass() else driver.submitYesNo(payer, pay)
        }
    }

    test("the enchanted land's controller may pay {W}{W} on their upkeep to gain 1 life") {
        val (driver, me, landOwner) = setup()

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe landOwner
        withClue("the granted trigger fires on the land controller's upkeep") {
            driver.state.stack.isNotEmpty() shouldBe true
        }
        driver.giveMana(landOwner, Color.WHITE, 2)
        settle(driver, landOwner, pay = true)

        driver.getLifeTotal(landOwner) shouldBe 21
        driver.getLifeTotal(me) shouldBe 20
    }

    test("declining gains nothing") {
        val (driver, _, landOwner) = setup()

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.giveMana(landOwner, Color.WHITE, 2)
        settle(driver, landOwner, pay = false)

        driver.getLifeTotal(landOwner) shouldBe 20
    }
})
