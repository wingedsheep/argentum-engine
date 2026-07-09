package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.ExplorersCache
import com.wingedsheep.mtg.sets.definitions.lci.cards.FanaticalOffering
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression test for last-known-information (LKI) plumbing on **sacrifice-as-an-additional-cost**.
 *
 * A permanent sacrificed to pay a cast cost must emit a `ZoneChangeEvent` carrying the same LKI
 * snapshot (CR 603.10 / 608.2h) that destruction and sacrifice-*effects* produce, so dies/leaves
 * triggers filtering on the dying permanent's counters / power-toughness / keywords / token-ness
 * still fire. Previously the four `CastSpellHandler` cost-sacrifice paths hand-built a
 * `ZoneChangeEvent(lastKnown = null)`, silently dropping every LKI-dependent trigger; they now
 * route through `ZoneTransitionService.moveToZone`, mirroring `SacrificeExecutor`.
 *
 * Vehicle: Explorer's Cache — "Whenever a creature you control with a +1/+1 counter on it dies, put
 * a +1/+1 counter on this artifact." — with the counter-bearing creature sacrificed as Fanatical
 * Offering's additional cost. (The committed [ExplorersCacheScenarioTest] already covers the same
 * trigger via *destruction*.)
 */
class SacrificeAsCostDeathTriggerTest : FunSpec({

    fun plusOne(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("LKI counter filter fires when the creature is sacrificed as an additional cost") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(PredefinedTokens.allTokens)
        driver.registerCard(ExplorersCache)
        driver.registerCard(FanaticalOffering)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!

        // Explorer's Cache on the battlefield with two +1/+1 counters (its ETB state).
        val cache = driver.putPermanentOnBattlefield(me, "Explorer's Cache")
        driver.addComponent(cache, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))

        // A Grizzly Bears with a +1/+1 counter — the creature we will sacrifice as the cost.
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.addComponent(bear, CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))

        withClue("Cache starts with two counters") { plusOne(driver, cache) shouldBe 2 }
        withClue("Bear has a +1/+1 counter") { plusOne(driver, bear) shouldBe 1 }

        val spell = driver.putCardInHand(me, "Fanatical Offering")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLACK, 1)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass() // resolve Fanatical Offering
        driver.bothPass() // resolve the Cache's dies trigger

        withClue("Bear was sacrificed") { driver.findPermanent(me, "Grizzly Bears") shouldBe null }
        withClue("Cache gains a +1/+1 counter because a counter-bearing creature died") {
            plusOne(driver, cache) shouldBe 3
        }
    }
})
