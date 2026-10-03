package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.CharitableLevy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Charitable Levy (MH3 #21) — {1}{W} Enchantment.
 *
 * "Noncreature spells cost {1} more to cast.
 *  Whenever a player casts a noncreature spell, put a collection counter on this enchantment. Then
 *  if there are three or more collection counters on it, sacrifice it. If you do, draw a card, then
 *  you may search your library for a Plains card, put it onto the battlefield tapped, then shuffle."
 */
class CharitableLevyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + CharitableLevy)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.collectionCounters(levy: EntityId): Int =
        state.getEntity(levy)?.get<CountersComponent>()?.getCount(CounterType.COLLECTION) ?: 0

    fun GameTestDriver.resolveWholeStack() {
        var guard = 0
        while (stackSize > 0 && pendingDecision == null && guard++ < 20) bothPass()
    }

    /** Cast Test Enchantment ({1}{W} + {1} tax) from [player]'s hand with exactly enough mana. */
    fun GameTestDriver.castTaxedEnchantment(player: EntityId) {
        val enchantment = putCardInHand(player, "Test Enchantment")
        giveMana(player, Color.WHITE, 3)
        val result = castSpell(player, enchantment)
        withClue("taxed enchantment cast should succeed: ${result.error}") { result.error shouldBe null }
    }

    test("noncreature spells cost {1} more") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Charitable Levy")

        val enchantment = driver.putCardInHand(me, "Test Enchantment")
        driver.giveMana(me, Color.WHITE, 2)
        withClue("{1}{W} alone no longer pays for a {1}{W} noncreature spell") {
            driver.submit(
                CastSpell(playerId = me, cardId = enchantment, paymentStrategy = PaymentStrategy.FromPool)
            ).outcome shouldNotBe Outcome.Done
        }

        driver.giveMana(me, Color.WHITE, 1)
        withClue("{2}{W} pays the spell plus the tax") {
            driver.submit(
                CastSpell(playerId = me, cardId = enchantment, paymentStrategy = PaymentStrategy.FromPool)
            ).outcome shouldBe Outcome.Done
        }
    }

    test("creature spells are neither taxed nor counted") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val levy = driver.putPermanentOnBattlefield(me, "Charitable Levy")

        val lions = driver.putCardInHand(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        withClue("{W} alone pays for Savannah Lions") {
            driver.submit(
                CastSpell(playerId = me, cardId = lions, paymentStrategy = PaymentStrategy.FromPool)
            ).outcome shouldBe Outcome.Done
        }
        driver.resolveWholeStack()

        driver.findPermanent(me, "Savannah Lions") shouldNotBe null
        driver.collectionCounters(levy) shouldBe 0
    }

    test("both players' noncreature spells add collection counters") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val levy = driver.putPermanentOnBattlefield(me, "Charitable Levy")

        driver.castTaxedEnchantment(me)
        // Hand priority to the opponent with my enchantment and the Levy trigger on the stack.
        driver.passPriority(me)
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 2) // {R} + {1} tax
        val boltCast = driver.castSpell(opp, bolt, listOf(me))
        withClue("opponent's taxed Bolt should succeed: ${boltCast.error}") { boltCast.error shouldBe null }

        driver.resolveWholeStack()

        withClue("one counter for my spell, one for the opponent's") {
            driver.collectionCounters(levy) shouldBe 2
        }
        driver.findPermanent(me, "Charitable Levy") shouldNotBe null
    }

    test("the third counter sacrifices it, draws a card, and may fetch a tapped Plains") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val levy = driver.putPermanentOnBattlefield(me, "Charitable Levy")

        driver.castTaxedEnchantment(me)
        driver.resolveWholeStack()
        driver.castTaxedEnchantment(me)
        driver.resolveWholeStack()
        driver.collectionCounters(levy) shouldBe 2

        val handBefore = driver.getHandSize(me)
        val landsBefore = driver.getLands(me).size

        driver.castTaxedEnchantment(me)
        // Resolve the Levy trigger (above the enchantment spell) up to the "you may search" prompt.
        driver.bothPass()

        withClue("the Levy was sacrificed and a card was drawn") {
            driver.findPermanent(me, "Charitable Levy") shouldBe null
            driver.getGraveyardCardNames(me) shouldBe listOf("Charitable Levy")
            driver.getHandSize(me) shouldBe handBefore + 1
        }

        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, true)
        val search = driver.pendingDecision
        (search is SelectCardsDecision) shouldBe true
        search as SelectCardsDecision
        driver.submitCardSelection(me, listOf(search.options.first()))

        val lands = driver.getLands(me)
        lands.size shouldBe landsBefore + 1
        // No land was on my battlefield tapped before, so the one tapped land is the fetched Plains.
        val fetched = lands.single { driver.isTapped(it) }
        driver.getCardName(fetched) shouldBe "Plains"
    }

    test("declining the search still draws, and fetches nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Charitable Levy")

        repeat(2) {
            driver.castTaxedEnchantment(me)
            driver.resolveWholeStack()
        }
        val handBefore = driver.getHandSize(me)
        val landsBefore = driver.getLands(me).size

        driver.castTaxedEnchantment(me)
        driver.bothPass()

        (driver.pendingDecision is YesNoDecision) shouldBe true
        driver.submitYesNo(me, false)

        driver.findPermanent(me, "Charitable Levy") shouldBe null
        driver.getHandSize(me) shouldBe handBefore + 1
        driver.getLands(me).size shouldBe landsBefore
    }
})
