package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.WoodlandChampion
import com.wingedsheep.mtg.sets.definitions.mrd.cards.RaiseTheAlarm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Woodland Champion (M20) — {1}{G} 2/2 Creature — Elf Scout
 *
 * "Whenever one or more tokens you control enter, put that many +1/+1 counters on this creature."
 *
 * Exercises "that many" read off the batched-ETB trigger's captured collection: one trigger per
 * batch, sized to the batch, unaffected by the tokens leaving before resolution (2020-08-07 ruling),
 * and scoped to tokens you control.
 */
class WoodlandChampionScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WoodlandChampion, RaiseTheAlarm))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun soldierTokens(driver: GameTestDriver, playerId: EntityId): List<EntityId> =
        driver.getCreatures(playerId).filter { driver.getCardName(it) == "Soldier Token" }

    fun castRaiseTheAlarm(driver: GameTestDriver, playerId: EntityId) {
        driver.giveMana(playerId, Color.WHITE, 2)
        val spell = driver.putCardInHand(playerId, "Raise the Alarm")
        driver.castSpell(playerId, spell).error shouldBe null
        driver.bothPass() // resolve Raise the Alarm
    }

    test("two tokens entering together put two +1/+1 counters on it") {
        val driver = createDriver()
        val me = driver.player1
        val champion = driver.putCreatureOnBattlefield(me, "Woodland Champion")

        castRaiseTheAlarm(driver, me)
        soldierTokens(driver, me).size shouldBe 2
        driver.bothPass() // resolve the single batched trigger

        plusOneCounters(driver, champion) shouldBe 2
        driver.state.stack.isEmpty() shouldBe true
    }

    test("counts tokens that entered even if they left before the trigger resolves") {
        val driver = createDriver()
        val me = driver.player1
        val champion = driver.putCreatureOnBattlefield(me, "Woodland Champion")

        castRaiseTheAlarm(driver, me)
        val tokens = soldierTokens(driver, me)
        tokens.size shouldBe 2
        tokens.forEach { driver.moveToGraveyard(it) }
        driver.bothPass()

        plusOneCounters(driver, champion) shouldBe 2
    }

    test("tokens an opponent controls do not trigger it") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2
        val champion = driver.putCreatureOnBattlefield(me, "Woodland Champion")

        val spell = driver.putCardInHand(opp, "Raise the Alarm")
        driver.passPriority(me)
        driver.giveMana(opp, Color.WHITE, 2)
        driver.castSpell(opp, spell).error shouldBe null
        driver.bothPass()

        soldierTokens(driver, opp).size shouldBe 2
        driver.state.stack.isEmpty() shouldBe true
        plusOneCounters(driver, champion) shouldBe 0
    }
})
