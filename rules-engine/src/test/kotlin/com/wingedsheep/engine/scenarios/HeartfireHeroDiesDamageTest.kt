package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.HeartfireHero
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Heartfire Hero {R} — Mouse Soldier 1/1.
 *   Valiant — +1/+1 counter when first targeted each turn.
 *   When this creature dies, it deals damage equal to its power to each opponent.
 *
 * The dies trigger must use the creature's LAST KNOWN power (power at the moment
 * it left the battlefield), not the printed base power. In particular, if the Hero
 * has two +1/+1 counters when it dies, its power-at-death is 3 and the opponent
 * should take 3 damage.
 *
 * Bug this guards against: after zone change to graveyard, the projected state no
 * longer tracks the creature, so a naive lookup falls back to `baseStats.power`
 * (printed 1). The engine must plumb `lastKnownPower` from the ZoneChangeEvent
 * through the trigger context into the effect context.
 */
class HeartfireHeroDiesDamageTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HeartfireHero))
        return driver
    }

    fun addPlusOnePlusOne(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        }
        driver.replaceState(newState)
    }

    test("dies trigger deals damage equal to projected power including +1/+1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        val opponentLifeBefore = driver.getLifeTotal(opponent)

        // Active player controls a 1/1 Heartfire Hero with two +1/+1 counters → 3/3.
        // The Hero has 3 toughness and Lightning Bolt deals 3 → lethal.
        val hero = driver.putCreatureOnBattlefield(activePlayer, "Heartfire Hero")
        addPlusOnePlusOne(driver, hero, 2)

        // Opponent casts Lightning Bolt on the Hero at instant speed during active
        // player's main phase. Opponent-controlled spell → no Valiant trigger on the
        // hero (Valiant only triggers on "a spell or ability YOU control").
        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")

        driver.passPriority(activePlayer)
        driver.castSpellWithTargets(
            opponent, bolt, listOf(ChosenTarget.Permanent(hero))
        ).error shouldBe null

        // Drain the stack (bolt + dies trigger).
        var safety = 0
        while (driver.stackSize > 0 && driver.state.pendingDecision == null && safety < 20) {
            driver.bothPass()
            safety++
        }

        // Hero died as a 3/3 → each opponent (the bolt-caster here) loses 3 life.
        driver.getLifeTotal(opponent) shouldBe (opponentLifeBefore - 3)
    }
})
