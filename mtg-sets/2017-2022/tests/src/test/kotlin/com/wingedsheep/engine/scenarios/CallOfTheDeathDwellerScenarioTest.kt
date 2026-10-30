package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.CallOfTheDeathDweller
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Call of the Death-Dweller — {2}{B} Sorcery
 * Return up to two target creature cards with total mana value 3 or less from your graveyard to
 * the battlefield. Put a deathtouch counter on either of them. Then put a menace counter on either
 * of them.
 *
 * Covers the two resolution-time "either of them" choices over the creatures that actually returned.
 */
class CallOfTheDeathDwellerScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CallOfTheDeathDweller))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.BLACK, 1)
        return driver to me
    }

    fun gy(card: EntityId, owner: EntityId) = ChosenTarget.Card(card, owner, Zone.GRAVEYARD)

    fun GameTestDriver.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("two returned creatures can split the counters") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Call of the Death-Dweller")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")    // MV 2
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")   // MV 1

        driver.castSpellWithTargets(me, spell, listOf(gy(bears, me), gy(lions, me))).error shouldBe null
        driver.bothPass()

        driver.findPermanent(me, "Grizzly Bears") shouldBe bears
        driver.findPermanent(me, "Savannah Lions") shouldBe lions

        (driver.pendingDecision is SelectCardsDecision) shouldBe true
        driver.submitCardSelection(me, listOf(bears)).error shouldBe null
        (driver.pendingDecision is SelectCardsDecision) shouldBe true
        driver.submitCardSelection(me, listOf(lions)).error shouldBe null

        driver.counters(bears, CounterType.DEATHTOUCH) shouldBe 1
        driver.counters(bears, CounterType.MENACE) shouldBe 0
        driver.counters(lions, CounterType.DEATHTOUCH) shouldBe 0
        driver.counters(lions, CounterType.MENACE) shouldBe 1
    }

    test("both counters can go on the same returned creature") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Call of the Death-Dweller")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")

        driver.castSpellWithTargets(me, spell, listOf(gy(bears, me), gy(lions, me))).error shouldBe null
        driver.bothPass()
        driver.submitCardSelection(me, listOf(lions)).error shouldBe null
        driver.submitCardSelection(me, listOf(lions)).error shouldBe null

        driver.counters(lions, CounterType.DEATHTOUCH) shouldBe 1
        driver.counters(lions, CounterType.MENACE) shouldBe 1
        driver.counters(bears, CounterType.DEATHTOUCH) shouldBe 0
        driver.counters(bears, CounterType.MENACE) shouldBe 0
    }

    test("a single returned creature takes both counters") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Call of the Death-Dweller")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")

        driver.castSpellWithTargets(me, spell, listOf(gy(bears, me))).error shouldBe null
        driver.bothPass()

        driver.findPermanent(me, "Grizzly Bears") shouldBe bears
        driver.counters(bears, CounterType.DEATHTOUCH) shouldBe 1
        driver.counters(bears, CounterType.MENACE) shouldBe 1
    }

    test("targets over total mana value 3 can't be chosen") {
        val (driver, me) = setup()
        val spell = driver.putCardInHand(me, "Call of the Death-Dweller")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")    // MV 2
        val giant = driver.putCardInGraveyard(me, "Hill Giant")       // MV 4

        driver.castSpellWithTargets(me, spell, listOf(gy(bears, me), gy(giant, me))).error shouldNotBe null
    }
})
