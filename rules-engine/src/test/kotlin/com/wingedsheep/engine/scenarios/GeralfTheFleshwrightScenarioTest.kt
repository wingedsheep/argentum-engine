package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.GeralfTheFleshwright
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Geralf, the Fleshwright — {2}{U} 2/3 Legendary Creature — Human Warlock
 *
 * 1) "Whenever you cast a spell during your turn other than your first spell that turn, create a
 *    2/2 blue and black Zombie Rogue creature token."
 * 2) "Whenever a Zombie you control enters, put a +1/+1 counter on it for each other Zombie that
 *    entered the battlefield under your control this turn."
 *
 * The two abilities feed each other: each 2nd+ spell makes a Zombie token, whose entry then
 * triggers the counter ability seeing the Zombies that entered earlier this turn. This exercises
 * the new `DynamicAmount.SubtypeEnteredUnderControlThisTurn` (turn-history count with
 * excludeTriggeringEntity).
 */
class GeralfTheFleshwrightScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GeralfTheFleshwright)
        return driver
    }

    fun zombieTokens(driver: GameTestDriver, me: EntityId): List<EntityId> =
        driver.getCreatures(me).filter { driver.getCardName(it) == "Zombie Rogue Token" }

    fun counters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast a Lightning Bolt at the opponent (a spell) and resolve the whole stack (spell + triggers). */
    fun castBolt(driver: GameTestDriver, me: EntityId, opp: EntityId) {
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        // Resolve the whole stack (the spell, then Geralf's token trigger, then that token's enters
        // trigger), but stop once the stack is empty so we stay in the precombat main phase.
        var guard = 0
        while (driver.stackSize > 0 && guard++ < 20) {
            driver.bothPass()
        }
    }

    test("first spell of the turn makes no token; second spell makes one Zombie Rogue") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Geralf, the Fleshwright")

        // First spell — no token.
        castBolt(driver, me, opp)
        zombieTokens(driver, me).size shouldBe 0

        // Second spell — one token (its entry triggers ability 2 with 0 other Zombies → 0 counters).
        castBolt(driver, me, opp)
        val tokens = zombieTokens(driver, me)
        tokens.size shouldBe 1
        counters(driver, tokens.single()) shouldBe 0
    }

    test("each later Zombie token gets +1/+1 for each other Zombie that entered this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Geralf, the Fleshwright")

        // Spell 1: no token.
        castBolt(driver, me, opp)
        // Spell 2: token A enters, 0 other Zombies this turn → 0 counters.
        castBolt(driver, me, opp)
        // Spell 3: token B enters, 1 other Zombie (A) this turn → 1 counter.
        castBolt(driver, me, opp)
        // Spell 4: token C enters, 2 other Zombies (A, B) this turn → 2 counters.
        castBolt(driver, me, opp)

        val tokens = zombieTokens(driver, me)
        tokens.size shouldBe 3
        // Counters across the three tokens are {0, 1, 2} in creation order.
        tokens.map { counters(driver, it) }.sorted() shouldBe listOf(0, 1, 2)
    }
})
