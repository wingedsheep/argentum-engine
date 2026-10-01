package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.RejectImperfection
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Reject Imperfection (ONE #67) — {1}{U}{U} Instant.
 *
 * Counter target spell. If that spell's mana value was 3 or less, proliferate.
 */
class RejectImperfectionScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RejectImperfection)
        return driver
    }

    fun plusOnes(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** P1 casts [spellName]; P2 answers with Reject Imperfection while owning a creature with a +1/+1 counter. */
    fun counterAndResolve(spellName: String, color: Color, amount: Int): Pair<GameTestDriver, EntityId> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val courser = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        driver.replaceState(
            driver.state.updateEntity(courser) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
        )

        val spell = driver.putCardInHand(p1, spellName)
        driver.giveMana(p1, color, amount)
        if (spellName == "Lightning Bolt") driver.castSpell(p1, spell, listOf(p2)) else driver.castSpell(p1, spell)
        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(p1)

        val reject = driver.putCardInHand(p2, "Reject Imperfection")
        driver.giveMana(p2, Color.BLUE, 3)
        driver.castSpellWithTargets(p2, reject, listOf(ChosenTarget.Spell(spellOnStack)))
        driver.bothPass()

        var guard = 0
        while (driver.pendingDecision != null && guard++ < 5) {
            driver.submitCardSelection(p2, listOf(courser))
        }
        return driver to courser
    }

    test("countering a spell with mana value 3 or less proliferates") {
        val (driver, courser) = counterAndResolve("Lightning Bolt", Color.RED, 1)
        val p1 = driver.activePlayer!!
        driver.getGraveyardCardNames(p1) shouldContain "Lightning Bolt"
        plusOnes(driver, courser) shouldBe 2
    }

    test("countering a spell with mana value greater than 3 does not proliferate") {
        val (driver, courser) = counterAndResolve("Force of Nature", Color.GREEN, 5)
        val p1 = driver.activePlayer!!
        driver.getGraveyardCardNames(p1) shouldContain "Force of Nature"
        plusOnes(driver, courser) shouldBe 1
    }
})
