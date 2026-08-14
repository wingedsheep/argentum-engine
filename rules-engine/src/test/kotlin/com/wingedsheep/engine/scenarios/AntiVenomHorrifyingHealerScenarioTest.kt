package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Anti-Venom, Horrifying Healer (SPM) — a 5/5 with two abilities:
 *  - "When Anti-Venom enters, if he was cast, return target creature card from your graveyard to
 *    the battlefield." (an intervening-"if" `Conditions.WasCast` ETB reanimation), and
 *  - "If damage would be dealt to Anti-Venom, prevent that damage and put that many +1/+1 counters
 *    on him." — the `RecipientFilter.Self` `ReplaceDamageWithCounters` wired on both creature-damage
 *    paths: `DamageUtils.applyDamage` (noncombat) and `CombatDamageManager.applyDamageToCreature`
 *    (combat).
 */
class AntiVenomHorrifyingHealerScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun GameTestDriver.bolt(player: EntityId, target: ChosenTarget) {
        giveMana(player, Color.RED, 1)
        val b = putCardInHand(player, "Lightning Bolt")
        castSpellWithTargets(player, b, listOf(target))
        bothPass()
        resolveStack(this)
    }

    test("noncombat damage to Anti-Venom is prevented and put on him as +1/+1 counters") {
        val (driver, you, opponent) = newGame()
        val av = driver.putCreatureOnBattlefield(you, "Anti-Venom, Horrifying Healer") // 5/5

        // You have priority in your main phase; bolt your own Anti-Venom (damage to him is replaced
        // regardless of the source).
        driver.bolt(you, ChosenTarget.Permanent(av)) // 3 damage

        // Damage replaced entirely — none marked, three +1/+1 counters, still on the battlefield.
        (driver.state.getEntity(av)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (driver.state.getEntity(av)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 3
        driver.state.getBattlefield().contains(av) shouldBe true
    }

    test("combat damage to Anti-Venom is prevented and put on him as +1/+1 counters") {
        val (driver, you, opponent) = newGame()
        val av = driver.putCreatureOnBattlefield(you, "Anti-Venom, Horrifying Healer") // 5/5
        driver.removeSummoningSickness(av)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3 blocker

        // Anti-Venom attacks; the 3/3 blocks. The blocker's 3 combat damage hits Anti-Venom via
        // CombatDamageManager.applyDamageToCreature — the distinct path from the Lightning Bolt case.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(av), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(courser to listOf(av)))
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // The 3 combat damage is replaced entirely — none marked, three +1/+1 counters on him.
        (driver.state.getEntity(av)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        (driver.state.getEntity(av)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 3
        driver.state.getBattlefield().contains(av) shouldBe true
        // Anti-Venom still dealt its own 5 — the 3/3 blocker is destroyed.
        driver.state.getBattlefield().contains(courser) shouldBe false
    }

    test("cast: the intervening-if ETB returns a creature card from your graveyard to the battlefield") {
        val (driver, you, _) = newGame()
        driver.putCardInGraveyard(you, "Centaur Courser")
        val av = driver.putCardInHand(you, "Anti-Venom, Horrifying Healer")
        repeat(5) { driver.putLandOnBattlefield(you, "Plains") } // {W}{W}{W}{W}{W}

        driver.submit(
            CastSpell(playerId = you, cardId = av, paymentStrategy = PaymentStrategy.AutoPay),
        ).isSuccess shouldBe true
        driver.bothPass() // resolve Anti-Venom; the was-cast ETB trigger goes on the stack, wants a target

        val courser = driver.getGraveyard(you).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Centaur Courser"
        }
        driver.pendingDecision.shouldNotBeNull()
        driver.submitTargetSelection(you, listOf(courser))
        resolveStack(driver)

        driver.findPermanent(you, "Centaur Courser").shouldNotBeNull()
    }

    test("not cast: an Anti-Venom put onto the battlefield does not trigger the reanimation") {
        val (driver, you, _) = newGame()
        driver.putCardInGraveyard(you, "Centaur Courser")

        // Enters without being cast → Conditions.WasCast is false → no ETB trigger, no target request.
        driver.putCreatureOnBattlefield(you, "Anti-Venom, Horrifying Healer")

        driver.pendingDecision.shouldBeNull()
        driver.findPermanent(you, "Centaur Courser").shouldBeNull()
    }
})
