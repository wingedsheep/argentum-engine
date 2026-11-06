package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.kld.cards.AetherbornMarauder
import com.wingedsheep.mtg.sets.definitions.ktk.cards.HardenedScales
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aetherborn Marauder (KLD #71) — {3}{B} 2/2 Flying, lifelink.
 *
 * "When this creature enters, move any number of +1/+1 counters from other permanents you control
 * onto this creature." Rulings: counters may come from any number of your other permanents, you
 * choose as the ability resolves, and zero is allowed. An opponent's permanents are never offered.
 * The counters gathered from several permanents land as one move, so Hardened Scales adds one.
 */
class AetherbornMarauderScenarioTest : FunSpec({

    fun GameTestDriver.addCounters(entityId: EntityId, count: Int) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        })
    }

    fun GameTestDriver.plusOnes(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castMarauder(): EntityId {
        val active = activePlayer!!
        val spell = putCardInHand(active, "Aetherborn Marauder")
        giveMana(active, Color.BLACK, 4)
        castSpell(active, spell).error shouldBe null
        bothPass() // creature resolves, enters trigger goes on the stack
        bothPass() // trigger resolves
        return findPermanent(active, "Aetherborn Marauder")!!
    }

    test("gathers chosen +1/+1 counters from several of your permanents, never an opponent's") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AetherbornMarauder, HardenedScales))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val courser = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(active, "Savannah Lions")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.addCounters(courser, 2)
        driver.addCounters(lions, 1)
        driver.addCounters(theirs, 3)

        val marauder = driver.castMarauder()

        // One prompt per permanent of yours that carries +1/+1 counters; take one from each.
        val asked = (1..2).map {
            val decision = driver.pendingDecision
            decision.shouldBeInstanceOf<ChooseNumberDecision>()
            decision.minValue shouldBe 0
            driver.submitDecision(active, NumberChosenResponse(decision.id, 1)).error shouldBe null
            decision.context.sourceId
        }
        driver.pendingDecision shouldBe null

        asked.toSet() shouldBe setOf(courser, lions)
        driver.plusOnes(marauder) shouldBe 2
        driver.plusOnes(courser) shouldBe 1
        driver.plusOnes(lions) shouldBe 0
        driver.plusOnes(theirs) shouldBe 3
        driver.state.projectedState.getPower(marauder) shouldBe 4
        driver.state.projectedState.getToughness(marauder) shouldBe 4
    }

    test("with no +1/+1 counters among your other permanents the trigger asks nothing") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AetherbornMarauder, HardenedScales))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        driver.putCreatureOnBattlefield(active, "Centaur Courser")

        val marauder = driver.castMarauder()

        driver.pendingDecision shouldBe null
        driver.plusOnes(marauder) shouldBe 0
    }

    test("counters gathered from several permanents land as one move: Hardened Scales adds one, not one per source") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AetherbornMarauder, HardenedScales))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(active, "Hardened Scales")
        val courser = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(active, "Savannah Lions")
        driver.addCounters(courser, 1)
        driver.addCounters(lions, 1)

        val marauder = driver.castMarauder()
        repeat(2) {
            val decision = driver.pendingDecision
            decision.shouldBeInstanceOf<ChooseNumberDecision>()
            driver.submitDecision(active, NumberChosenResponse(decision.id, 1)).error shouldBe null
        }
        driver.pendingDecision shouldBe null

        driver.plusOnes(courser) shouldBe 0
        driver.plusOnes(lions) shouldBe 0
        driver.plusOnes(marauder) shouldBe 3
    }
})
