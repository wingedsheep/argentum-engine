package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.RunadiBehemothCaller
import com.wingedsheep.mtg.sets.definitions.lea.cards.CrawWurm
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.lgn.cards.EnormousBaloth
import com.wingedsheep.mtg.sets.definitions.m14.cards.RumblingBaloth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Runadi, Behemoth Caller — {2}{G} Legendary Creature — Cat Shaman, 1/3 (J22).
 *
 *   Whenever you cast a creature spell with mana value 5 or greater, that creature enters with X
 *   additional +1/+1 counters on it, where X is its mana value minus 4.
 *   Creatures you control with three or more +1/+1 counters on them have haste.
 *   {T}: Add {G}.
 */
class RunadiBehemothCallerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(RunadiBehemothCaller, CrawWurm, EnormousBaloth, RumblingBaloth, GrizzlyBears)
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    fun GameTestDriver.castGreen(me: EntityId, name: String, manaValue: Int): EntityId {
        val spell = putCardInHand(me, name)
        giveMana(me, Color.GREEN, manaValue)
        castSpell(me, spell)
        resolveAll()
        return findPermanent(me, name)!!
    }

    fun GameTestDriver.hasHaste(id: EntityId): Boolean = state.projectedState.hasKeyword(id, Keyword.HASTE)

    test("a mana value 7 creature enters with three counters and has haste") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Runadi, Behemoth Caller")

        val baloth = driver.castGreen(me, "Enormous Baloth", 7)
        driver.plusOneCounters(baloth) shouldBe 3
        driver.state.projectedState.getPower(baloth) shouldBe 10
        driver.hasHaste(baloth) shouldBe true
    }

    test("a mana value 6 creature enters with two counters and no haste") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Runadi, Behemoth Caller")

        val wurm = driver.castGreen(me, "Craw Wurm", 6)
        driver.plusOneCounters(wurm) shouldBe 2
        driver.hasHaste(wurm) shouldBe false
    }

    test("a mana value 4 creature spell does not trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Runadi, Behemoth Caller")

        val baloth = driver.castGreen(me, "Rumbling Baloth", 4)
        driver.plusOneCounters(baloth) shouldBe 0
    }

    test("haste needs three or more +1/+1 counters, on a creature you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Runadi, Behemoth Caller")
        val two = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val three = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.addComponent(two, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        driver.addComponent(three, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
        driver.addComponent(other, CountersComponent(mapOf(CounterType.TIME to 3)))
        driver.addComponent(theirs, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))

        driver.hasHaste(two) shouldBe false
        driver.hasHaste(three) shouldBe true
        driver.hasHaste(other) shouldBe false
        driver.hasHaste(theirs) shouldBe false
    }
})
