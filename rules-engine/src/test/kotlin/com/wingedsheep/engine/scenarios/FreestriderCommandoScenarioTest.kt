package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Freestrider Commando (OTJ #162) — {2}{G} Centaur Mercenary, 3/3, Plot {3}{G}.
 *
 *   "This creature enters with two +1/+1 counters on it if it wasn't cast or no mana was spent
 *   to cast it."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.conditions.NoManaSpentToCast] condition through
 * the real cast → resolve → enters-with-counters path:
 *
 * - Cast normally for {2}{G} → mana was spent → enters as a vanilla 3/3 (no counters).
 * - Cast for free (no mana spent, the plot payoff — here via Weftwalking's free first spell) →
 *   enters as a 5/5 with two +1/+1 counters.
 *
 * The free-cast case also pins the Freestrider Commando ruling that "no mana was spent to cast it"
 * means a total mana payment of zero: any mana spent (including for cost increases on an otherwise
 * free cast) disqualifies it, which is exactly the normal-cast case below.
 */
class FreestriderCommandoScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40, "Island" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("cast normally for {2}{G} (mana spent) — enters as a 3/3 with no counters") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val commando = driver.putCardInHand(player, "Freestrider Commando")
        driver.giveMana(player, Color.GREEN, 3) // {2}{G}

        driver.submit(
            CastSpell(player, commando, paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true
        driver.bothPass()

        val onBattlefield = driver.getCreatures(player).single()
        val counters = driver.state.getEntity(onBattlefield)?.get<CountersComponent>()
        (counters?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        driver.state.projectedState.getPower(onBattlefield) shouldBe 3
        driver.state.projectedState.getToughness(onBattlefield) shouldBe 3
    }

    test("cast for free (no mana spent) — enters as a 5/5 with two +1/+1 counters") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Weftwalking grants the active player's first spell each turn a {0} free-cast. Using it on
        // Freestrider Commando means no mana is spent to cast it — the same state a plotted cast
        // produces — so its enters-with-counters condition is satisfied.
        driver.putPermanentOnBattlefield(player, "Weftwalking")

        val commando = driver.putCardInHand(player, "Freestrider Commando")
        driver.submit(
            CastSpell(player, commando, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true
        driver.bothPass()

        // Weftwalking is an enchantment, so Freestrider Commando is the only creature in play.
        val onBattlefield = driver.getCreatures(player).single()
        val counters = driver.state.getEntity(onBattlefield)?.get<CountersComponent>()
        (counters?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 2
        driver.state.projectedState.getPower(onBattlefield) shouldBe 5
        driver.state.projectedState.getToughness(onBattlefield) shouldBe 5
    }
})
