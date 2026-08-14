package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ons.cards.Aurification
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Aurification (ONS #6) — regression guard for the damage-to-you observer path.
 *
 * "Whenever a creature deals damage to you, put a gold counter on it." The creature restriction now
 * rides on the trigger's own `sourceFilter` rather than being hardcoded in the detector, so this
 * pins both directions: a creature's combat damage still lands a gold counter, and a burn spell —
 * which the shared detector now lets through to source-blind triggers — must not.
 */
class AurificationScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Aurification))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        return Triple(driver, active, driver.getOpponent(active))
    }

    /** Drain the stack: the spell resolves, then the damage trigger it caused resolves too. */
    fun GameTestDriver.resolveAll(max: Int = 10) {
        var i = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && i++ < max) bothPass()
    }

    fun GameTestDriver.goldCountersOn(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.GOLD) ?: 0

    test("a creature's combat damage to you lands a gold counter and makes it a defender") {
        // The Aurification controller is the non-active player; the active player attacks them.
        val (driver, attacker, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Aurification")
        val lions = driver.putCreatureOnBattlefield(attacker, "Savannah Lions")
        driver.removeSummoningSickness(lions)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(lions), you)
        driver.passPriorityUntil(Step.END_COMBAT)

        driver.goldCountersOn(lions) shouldBe 1
        driver.state.projectedState.hasKeyword(lions, Keyword.DEFENDER) shouldBe true
    }

    test("a burn spell is not a creature — no gold counter") {
        val (driver, opponent, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Aurification")

        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()

        driver.getLifeTotal(you) shouldBe 17
        driver.goldCountersOn(bolt) shouldBe 0
    }
})
