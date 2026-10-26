package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.Withercrown
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

/**
 * Scenario tests for Withercrown.
 *
 * "Base power 0" sets only power in layer 7b: the creature keeps its printed toughness, and a
 * +1/+1 counter still applies on top. The granted upkeep trigger belongs to the enchanted
 * creature's controller, and its escape is sacrificing *that* creature — not any other one.
 */
class WithercrownScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Withercrown)
        return driver
    }

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    fun settle(driver: GameTestDriver, payer: EntityId, pay: Boolean) {
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val decision = driver.pendingDecision) {
                null -> driver.bothPass()
                is SelectCardsDecision -> driver.submitCardSelection(
                    payer,
                    if (pay) decision.options.take(decision.maxSelections) else emptyList()
                )
                else -> driver.submitYesNo(payer, pay)
            }
        }
    }

    test("enchanted creature has base power 0 and keeps its toughness; counters apply on top") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val courser = driver.putPermanentOnBattlefield(victim, "Centaur Courser")
        val crown = driver.putPermanentOnBattlefield(me, "Withercrown")
        attach(driver, crown, courser)

        withClue("Centaur Courser is a 0/3 under Withercrown") {
            driver.state.projectedState.getPower(courser) shouldBe 0
            driver.state.projectedState.getToughness(courser) shouldBe 3
        }

        driver.addComponent(courser, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        withClue("a +1/+1 counter applies after the base is set (layer 7c)") {
            driver.state.projectedState.getPower(courser) shouldBe 1
            driver.state.projectedState.getToughness(courser) shouldBe 4
        }
    }

    test("declining costs the enchanted creature's controller 1 life on their upkeep only") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val courser = driver.putPermanentOnBattlefield(victim, "Centaur Courser")
        val crown = driver.putPermanentOnBattlefield(me, "Withercrown")
        attach(driver, crown, courser)

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim
        settle(driver, victim, pay = false)

        withClue("the creature's controller lost 1, the Aura's controller nothing") {
            driver.getLifeTotal(victim) shouldBe 19
            driver.getLifeTotal(me) shouldBe 20
        }
        withClue("declining keeps the creature") {
            driver.getGraveyard(victim) shouldBe emptyList()
        }

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
        settle(driver, me, pay = false)
        withClue("nothing fires on the Aura controller's own upkeep") {
            driver.getLifeTotal(victim) shouldBe 19
            driver.getLifeTotal(me) shouldBe 20
        }
    }

    test("the trigger offers sacrificing only the enchanted creature, and paying saves the life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val courser = driver.putPermanentOnBattlefield(victim, "Centaur Courser")
        val spare = driver.putPermanentOnBattlefield(victim, "Centaur Courser")
        val crown = driver.putPermanentOnBattlefield(me, "Withercrown")
        attach(driver, crown, courser)

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim

        var guard = 0
        while (guard++ < 16 && driver.pendingDecision == null && driver.state.stack.isNotEmpty()) {
            driver.bothPass()
        }
        val decision = driver.pendingDecision
        withClue("the trigger asks, and only the enchanted creature is a candidate") {
            (decision is SelectCardsDecision) shouldBe true
            (decision as SelectCardsDecision).options shouldBe listOf(courser)
            decision.prompt shouldStartWith "Sacrifice this creature or "
        }

        settle(driver, victim, pay = true)

        withClue("paying sacrifices the enchanted creature and costs no life") {
            driver.getLifeTotal(victim) shouldBe 20
            (courser in driver.getGraveyard(victim)) shouldBe true
            (spare in driver.getGraveyard(victim)) shouldBe false
        }
    }
})
