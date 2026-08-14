package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.DancingFromDarkToDawn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dancing from Dark to Dawn — {3}{G}{G} Enchantment (HOB #123).
 *
 * "Whenever you cast a creature spell, put X +1/+1 counters on target creature you control, where
 *  X is that spell's mana value.
 *  Landfall — Whenever a land you control enters, create a 2/2 green Bear creature token."
 *
 * X is read off the *triggering spell*, and the trigger resolves above the creature spell — so the
 * creature being cast is still on the stack and can't be the target.
 */
class DancingFromDarkToDawnScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DancingFromDarkToDawn))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Drain the stack, pointing any target prompt at [preferredTarget] (null: none expected). */
    fun GameTestDriver.settle(preferredTarget: EntityId? = null) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 40) {
            val pending = state.pendingDecision
            when {
                pending is ChooseTargetsDecision && preferredTarget != null ->
                    submitTargetSelection(pending.playerId, listOf(preferredTarget))
                pending != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("casting a creature spell puts counters equal to its mana value on a creature you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putPermanentOnBattlefield(me, "Dancing from Dark to Dawn")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        // Centaur Courser is {2}{G} — mana value 3.
        driver.giveMana(me, Color.GREEN, 3)
        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.castSpell(me, courser).error shouldBe null
        driver.settle(lions)

        withClue("three counters — Centaur Courser's mana value") {
            driver.plusOneCounters(lions) shouldBe 3
        }
        withClue("the creature spell itself still resolved onto the battlefield") {
            (driver.findPermanent(me, "Centaur Courser") != null) shouldBe true
        }
        withClue("and it did not receive the counters — it was on the stack when the trigger resolved") {
            driver.plusOneCounters(driver.findPermanent(me, "Centaur Courser")!!) shouldBe 0
        }
    }

    test("landfall creates a 2/2 green Bear token") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putPermanentOnBattlefield(me, "Dancing from Dark to Dawn")

        val forest = driver.putCardInHand(me, "Forest")
        driver.playLand(me, forest).isSuccess shouldBe true
        driver.settle()

        val bears = driver.getPermanents(me).filter { driver.getCardName(it) == "Bear Token" }
        withClue("exactly one Bear token") {
            bears.size shouldBe 1
        }
        withClue("it is a 2/2") {
            driver.state.projectedState.getPower(bears.single()) shouldBe 2
            driver.state.projectedState.getToughness(bears.single()) shouldBe 2
        }
    }
})
