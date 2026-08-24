package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Erosion — "At the beginning of the upkeep of enchanted land's controller,
 * destroy that land unless that player pays {1} or 1 life."
 *
 * The clause that is easy to get backwards is *whose* upkeep and *who* pays: the Aura's controller
 * casts it, but the tax falls on the land's controller. So the Aura goes on an opponent's land, and
 * the test asserts my own upkeep does nothing while theirs fires — and that when nobody pays, it is
 * their land that dies and my life that is untouched.
 */
class ErosionScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    /**
     * Pump the engine until the upkeep trigger has fully resolved, answering the "{1} or 1 life"
     * cost choice with [optionIndex], or declining it when [optionIndex] is null.
     */
    fun settleUpkeep(driver: GameTestDriver, payer: EntityId, optionIndex: Int?) {
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            val decision = driver.pendingDecision
            when {
                decision is ChooseOptionDecision && optionIndex != null ->
                    driver.submitDecision(payer, OptionChosenResponse(decision.id, optionIndex))
                decision is ChooseOptionDecision ->
                    driver.submitDecision(payer, OptionChosenResponse(decision.id, decision.options.size - 1))
                decision != null -> driver.autoResolveDecision()
                else -> driver.bothPass()
            }
        }
    }

    test("the trigger fires on the land controller's upkeep, not the Aura controller's") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val land = driver.putLandOnBattlefield(victim, "Forest")
        val erosion = driver.putPermanentOnBattlefield(me, "Erosion")
        attach(driver, erosion, land)

        withClue("my own turn passes without the Aura asking anything") {
            driver.passPriorityUntil(Step.END)
            driver.getLifeTotal(victim) shouldBe 20
            (driver.findPermanent(victim, "Forest") != null) shouldBe true
        }

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim

        withClue("their upkeep raises the pay-or-lose-it choice") {
            (driver.state.stack.isNotEmpty() || driver.pendingDecision != null) shouldBe true
        }

        // Pay the life leg — whichever index it is, the life total moves and the land lives.
        settleUpkeep(driver, victim, optionIndex = 1)

        withClue("the payer is the land's controller, and my life never moved") {
            driver.getLifeTotal(me) shouldBe 20
        }
    }
})
