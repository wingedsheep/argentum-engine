package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.GlorifierOfSuffering
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Glorifier of Suffering (LCI #15) — {2}{W} Creature — Vampire Soldier 3/2.
 *
 * "When this creature enters, you may sacrifice another creature or artifact. When you do,
 *  put a +1/+1 counter on each of up to two target creatures."
 *
 * Proves three scenarios:
 *  1. Happy path: sacrifice another creature → put +1/+1 counter on two target creatures.
 *  2. Decline: player says "no" to the sacrifice → no counters are placed.
 *  3. No valid sacrifice target (no other creature or artifact) → no decision is presented,
 *     no counters placed.
 */
class GlorifierOfSufferingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GlorifierOfSuffering))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counterCount(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun castGlorifier(driver: GameTestDriver, playerId: EntityId): EntityId {
        val card = driver.putCardInHand(playerId, "Glorifier of Suffering")
        driver.giveMana(playerId, Color.WHITE, 1)
        driver.giveColorlessMana(playerId, 2)
        driver.castSpell(playerId, card).isSuccess shouldBe true
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // trigger goes on the stack; let it resolve
        return card
    }

    test("sacrificing another creature puts a +1/+1 counter on up to two target creatures") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val target1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val target2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        castGlorifier(driver, me)

        // Reflexive trigger: "You may sacrifice another creature or artifact." — accept.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)

        // Choose which creature/artifact to sacrifice.
        driver.submitTargetSelection(me, listOf(fodder))

        // "When you do" — reflexive trigger prompts for up to two target creatures.
        driver.submitTargetSelection(me, listOf(target1, target2))
        driver.bothPass() // resolve reflexive trigger

        // Fodder was sacrificed; the two targets stay on the battlefield.
        driver.getGraveyard(me).contains(fodder) shouldBe true
        driver.state.getBattlefield(me).contains(target1) shouldBe true
        driver.state.getBattlefield(me).contains(target2) shouldBe true

        // Both targets received a +1/+1 counter.
        counterCount(driver, target1) shouldBe 1
        counterCount(driver, target2) shouldBe 1
    }

    test("sacrificing places counter on only one target when player chooses one") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val target1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val target2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        castGlorifier(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)

        driver.submitTargetSelection(me, listOf(fodder))

        // Choose only one of the two available targets.
        driver.submitTargetSelection(me, listOf(target1))
        driver.bothPass()

        counterCount(driver, target1) shouldBe 1
        counterCount(driver, target2) shouldBe 0
    }

    test("declining the optional sacrifice places no counters") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val target1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val target2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        castGlorifier(driver, me)

        // Player declines to sacrifice.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        // No counters should be placed on either creature.
        counterCount(driver, target1) shouldBe 0
        counterCount(driver, target2) shouldBe 0
    }

    test("no decision presented when no other creature or artifact to sacrifice") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // No other creature or artifact on the battlefield; Glorifier itself can't be the fodder.
        castGlorifier(driver, me)

        // The optional sacrifice is infeasible — no prompt should be raised.
        driver.pendingDecision shouldBe null
    }
})
