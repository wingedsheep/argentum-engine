package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.BewitchingLeechcraft
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bewitching Leechcraft (LTR #41).
 *
 * Proves the granted untap-step replacement
 * ([com.wingedsheep.sdk.core.AbilityFlag.REMOVE_COUNTER_TO_UNTAP]):
 *  - The ETB trigger taps the enchanted creature.
 *  - On the enchanted creature's controller's next untap step, if it has a +1/+1
 *    counter, one is removed and the creature untaps.
 *  - With no +1/+1 counter, the creature stays tapped (does not untap).
 */
class BewitchingLeechcraftScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BewitchingLeechcraft))
        return driver
    }

    fun addPlusOneCounters(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        }
        driver.replaceState(newState)
    }

    fun plusOneCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // Cast Bewitching Leechcraft on the active player's own creature and resolve
    // the spell plus its ETB tap trigger. Returns the creature entity.
    fun enchantOwnCreature(driver: GameTestDriver): Pair<EntityId, EntityId> {
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val aura = driver.putCardInHand(p1, "Bewitching Leechcraft")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.castSpellWithTargets(p1, aura, listOf(ChosenTarget.Permanent(creature)))
            .isSuccess shouldBe true

        // Resolve the Aura, then its ETB "tap enchanted creature" trigger.
        driver.bothPass() // Aura resolves, ETB trigger goes on the stack
        driver.bothPass() // ETB trigger resolves

        return p1 to creature
    }

    // Drive from the active player's main phase all the way back to their next
    // untap step (one full turn cycle), passing through the untap step.
    fun advanceToNextUntapStep(driver: GameTestDriver, activePlayer: EntityId) {
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.END)
        driver.passPriority(activePlayer)
        driver.passPriority(opponent)
        // Opponent's turn.
        driver.passPriorityUntil(Step.END)
        driver.passPriority(opponent)
        driver.passPriority(activePlayer)
        // Back to active player's turn — untap step already happened.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("ETB taps the enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Grizzly Bears" to 20))

        val (_, creature) = enchantOwnCreature(driver)

        driver.isTapped(creature) shouldBe true
    }

    test("with a +1/+1 counter, the creature untaps and one counter is removed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Grizzly Bears" to 20))

        val (p1, creature) = enchantOwnCreature(driver)
        addPlusOneCounters(driver, creature, 2)

        driver.isTapped(creature) shouldBe true
        plusOneCounters(driver, creature) shouldBe 2

        advanceToNextUntapStep(driver, p1)

        // Untapped, and exactly one +1/+1 counter was removed.
        driver.isTapped(creature) shouldBe false
        plusOneCounters(driver, creature) shouldBe 1
    }

    test("with no +1/+1 counters, the creature stays tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Grizzly Bears" to 20))

        val (p1, creature) = enchantOwnCreature(driver)

        driver.isTapped(creature) shouldBe true
        plusOneCounters(driver, creature) shouldBe 0

        advanceToNextUntapStep(driver, p1)

        // Did not untap; nothing to remove.
        driver.isTapped(creature) shouldBe true
        plusOneCounters(driver, creature) shouldBe 0
    }
})
