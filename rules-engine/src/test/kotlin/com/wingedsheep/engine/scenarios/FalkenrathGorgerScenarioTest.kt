package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Falkenrath Gorger (Shadows over Innistrad #155) — {R} Creature — Vampire Berserker 2/1.
 *
 * "Each Vampire creature card you own that isn't on the battlefield has madness. The madness cost
 * is equal to its mana cost."
 *
 * The card is the only user of granted madness, so this file is also the coverage for the grant
 * itself: that it reaches the discard replacement (exile instead of graveyard), that the cast offer
 * is priced at the discarded card's *own* mana cost, that it is scoped to Vampires, and that a
 * printed madness cost still wins when a card has both.
 */
class FalkenrathGorgerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    /** Discard [cardId] as Tormenting Voice's additional cost. */
    fun discardAsCost(driver: GameTestDriver, player: EntityId, cardId: EntityId) {
        val voice = driver.putCardInHand(player, "Tormenting Voice")
        driver.giveMana(player, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(cardId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    test("a discarded Vampire creature card is exiled and castable for its own mana cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Falkenrath Gorger")
        val nighthawk = driver.putCardInHand(player, "Vampire Nighthawk")
        discardAsCost(driver, player, nighthawk)

        driver.getExile(player) shouldContain nighthawk
        driver.getGraveyard(player).shouldNotContain(nighthawk)

        // Vampire Nighthawk's mana cost is {1}{B}{B} — exactly what the granted madness charges.
        driver.giveMana(player, Color.BLACK, 3)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Vampire Nighthawk").shouldNotBeNull()
        driver.getExile(player).shouldNotContain(nighthawk)
    }

    test("without the Gorger on the battlefield the same discard goes to the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nighthawk = driver.putCardInHand(player, "Vampire Nighthawk")
        discardAsCost(driver, player, nighthawk)

        driver.getGraveyard(player) shouldContain nighthawk
        driver.getExile(player).shouldNotContain(nighthawk)
    }

    test("the grant is scoped to Vampires — a discarded Bear still hits the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Falkenrath Gorger")
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        discardAsCost(driver, player, bears)

        driver.getGraveyard(player) shouldContain bears
        driver.getExile(player).shouldNotContain(bears)
    }

    test("a discarded Gorger gets no madness from itself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gorger = driver.putCardInHand(player, "Falkenrath Gorger")
        discardAsCost(driver, player, gorger)

        driver.getGraveyard(player) shouldContain gorger
        driver.getExile(player).shouldNotContain(gorger)
    }

    test("printed madness wins over the grant — Bloodmad Vampire still costs {1}{R}, not {2}{R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Falkenrath Gorger")
        val vampire = driver.putCardInHand(player, "Bloodmad Vampire")
        discardAsCost(driver, player, vampire)

        driver.getExile(player) shouldContain vampire

        // Exactly two red — enough for the printed madness cost {1}{R} and one short of the
        // {2}{R} the grant would have charged.
        driver.giveMana(player, Color.RED, 2)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Bloodmad Vampire").shouldNotBeNull()
        driver.getExile(player).shouldNotContain(vampire)
    }
})
