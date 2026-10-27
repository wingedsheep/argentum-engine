package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.zen.cards.BladeOfTheBloodchief
import com.wingedsheep.mtg.sets.definitions.zen.cards.VampireNighthawk
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Blade of the Bloodchief (ZEN #196) — {1} Artifact — Equipment.
 *
 * Whenever a creature dies, put a +1/+1 counter on equipped creature. If equipped creature is a
 * Vampire, put two +1/+1 counters on it instead.
 * Equip {1}
 *
 * Tests:
 *  1. A non-Vampire host gets one counter when any creature (an opponent's) dies.
 *  2. A Vampire host gets two counters instead.
 *  3. Ruling: the Blade leaves the battlefield with the trigger on the stack — the counters still
 *     go to the creature it was attached to when it left, and that last-known host being a
 *     Vampire still makes it two (the "is a Vampire" check reads the same last-known host).
 */
class BladeOfTheBloodchiefScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BladeOfTheBloodchief, VampireNighthawk, Shatter))
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.putBladeAttached(playerId: EntityId, hostId: EntityId): EntityId {
        val bladeId = putPermanentOnBattlefield(playerId, "Blade of the Bloodchief")
        var newState = state.updateEntity(bladeId) { c -> c.with(AttachedToComponent(hostId)) }
        val existing = newState.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(hostId) { c -> c.with(AttachmentsComponent(existing + bladeId)) }
        replaceState(newState)
        return bladeId
    }

    // Player 1 may not be active at game start (random turn order) — advance until it is.
    fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    fun GameTestDriver.killWithDoomBlade(victim: EntityId) {
        val doomBlade = putCardInHand(player1, "Doom Blade")
        giveMana(player1, Color.BLACK, 2)
        castSpell(player1, doomBlade, targets = listOf(victim)).outcome shouldBe Outcome.Done
        bothPass() // resolve Doom Blade -> victim dies, the Blade's trigger goes on the stack
        state.getBattlefield().contains(victim) shouldBe false
    }

    test("a non-Vampire equipped creature gets one +1/+1 counter when a creature dies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.putBladeAttached(driver.player1, bear)
        val victim = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.advanceToPlayer1Main()
        driver.killWithDoomBlade(victim)
        driver.bothPass() // resolve the Blade's trigger

        plusOneCounters(driver, bear) shouldBe 1
    }

    test("a Vampire equipped creature gets two +1/+1 counters instead") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val nighthawk = driver.putCreatureOnBattlefield(driver.player1, "Vampire Nighthawk")
        driver.putBladeAttached(driver.player1, nighthawk)
        val victim = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.advanceToPlayer1Main()
        driver.killWithDoomBlade(victim)
        driver.bothPass() // resolve the Blade's trigger

        plusOneCounters(driver, nighthawk) shouldBe 2
    }

    test("Blade destroyed in response: its last-known Vampire host still gets two counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val nighthawk = driver.putCreatureOnBattlefield(driver.player1, "Vampire Nighthawk")
        val blade = driver.putBladeAttached(driver.player1, nighthawk)
        val victim = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.advanceToPlayer1Main()
        driver.killWithDoomBlade(victim)

        // With the trigger on the stack, Shatter the Blade.
        val shatter = driver.putCardInHand(driver.player1, "Shatter")
        driver.giveMana(driver.player1, Color.RED, 2)
        driver.castSpell(driver.player1, shatter, targets = listOf(blade)).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve Shatter -> the Blade leaves the battlefield
        driver.state.getBattlefield().contains(blade) shouldBe false
        driver.bothPass() // resolve the Blade's trigger

        plusOneCounters(driver, nighthawk) shouldBe 2
    }
})
