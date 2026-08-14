package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Bloodmad Vampire (Shadows over Innistrad #146) — {2}{R} Creature — Vampire Berserker 4/1.
 *
 * "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
 * "Madness {1}{R}"
 *
 * The growth trigger is ordinary; what's worth pinning is that madness (CR 702.35) on a
 * *permanent* card still resolves as a normal creature spell — the card goes on the stack and the
 * creature enters, it just cost {1}{R}.
 */
class BloodmadVampireScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    /** Discard [cardId] as Tormenting Voice's additional cost. */
    fun discardAsCost(driver: GameTestDriver, player: EntityId, cardId: EntityId) {
        val voice = driver.putCardInHand(player, "Tormenting Voice")
        driver.giveMana(player, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(cardId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    test("it grows when it deals combat damage to a player") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val vampire = driver.putCreatureOnBattlefield(player, "Bloodmad Vampire")
        driver.removeSummoningSickness(vampire)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(vampire), opponent)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        var guard = 0
        while (guard++ < 30 && driver.state.pendingDecision != null) driver.autoResolveDecision()
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 16
        // 4/1 base plus the +1/+1 counter its own trigger put on it.
        driver.state.projectedState.getPower(vampire) shouldBe 5
        driver.state.projectedState.getToughness(vampire) shouldBe 2
    }

    test("discarded, it is exiled and can be cast for its madness cost of {1}{R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vampire = driver.putCardInHand(player, "Bloodmad Vampire")
        discardAsCost(driver, player, vampire)

        driver.getExile(player) shouldContain vampire
        driver.getGraveyard(player).shouldNotContain(vampire)

        driver.giveMana(player, Color.RED, 2)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(player, "Bloodmad Vampire").shouldNotBeNull()
        driver.getExile(player).shouldNotContain(vampire)
    }
})
