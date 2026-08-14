package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.PincerSpider
import com.wingedsheep.mtg.sets.definitions.spm.cards.SpinneretAndSpiderling
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spinneret and Spiderling (SPM #94) — {R} Legendary Creature — Spider Human Hero 1/2.
 *
 * "Whenever you attack with two or more Spiders, put a +1/+1 counter on Spinneret and Spiderling.
 *  Whenever Spinneret and Spiderling deals 4 or more damage, exile the top card of your library.
 *  Until the end of your next turn, you may play that card."
 *
 * Proven end-to-end:
 *  1. Attacking with two Spiders (Spinneret itself + a Pincer Spider) fires the first trigger and
 *     puts one +1/+1 counter on Spinneret and Spiderling.
 *  2. Attacking with only one Spider (Spinneret alone) does NOT fire it — the trigger needs two or
 *     more Spider attackers.
 *  3. When Spinneret and Spiderling deals 4 damage (pumped to 4 power by counters) to the opponent,
 *     the second trigger exiles the top card of the library and grants may-play permission for it.
 */
class SpinneretAndSpiderlingScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SpinneretAndSpiderling)
        driver.registerCard(PincerSpider)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Attack unblocked with [attackers] and resolve combat damage plus any resulting triggers. */
    fun GameTestDriver.swingUnblocked(player: EntityId, opponent: EntityId, attackers: List<EntityId>) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player, attackers, opponent)
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        passPriorityUntil(Step.COMBAT_DAMAGE)
        if (pendingDecision is CombatResolutionDecision) confirmCombatDamage()
        var guard = 0
        while (guard++ < 30) {
            val decision = pendingDecision
            when {
                decision is CombatResolutionDecision -> confirmCombatDamage()
                decision is OrderObjectsDecision -> submitOrderedResponse(decision.playerId, decision.objects)
                decision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: attacking with two Spiders puts a +1/+1 counter on Spinneret and Spiderling
    // ─────────────────────────────────────────────────────────────────────────
    test("attacking with two or more Spiders puts a +1/+1 counter on Spinneret and Spiderling") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val spinneret = driver.putCreatureOnBattlefield(attacker, "Spinneret and Spiderling")
        val pincer = driver.putCreatureOnBattlefield(attacker, "Pincer Spider")
        driver.removeSummoningSickness(spinneret)
        driver.removeSummoningSickness(pincer)

        driver.plusOneCounters(spinneret) shouldBe 0

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(spinneret, pincer), defender)
        driver.bothPass()

        withClue("Two Spider attackers fire the trigger for one +1/+1 counter") {
            driver.plusOneCounters(spinneret) shouldBe 1
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: attacking with a single Spider does not fire the trigger
    // ─────────────────────────────────────────────────────────────────────────
    test("attacking with only one Spider does not put a counter on Spinneret and Spiderling") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val spinneret = driver.putCreatureOnBattlefield(attacker, "Spinneret and Spiderling")
        driver.removeSummoningSickness(spinneret)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(spinneret), defender)
        driver.bothPass()

        withClue("A lone Spider attacker is below the two-Spider threshold") {
            driver.plusOneCounters(spinneret) shouldBe 0
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: dealing 4+ damage exiles the top card and grants permission to play it
    // ─────────────────────────────────────────────────────────────────────────
    test("dealing four or more damage exiles the top card of the library and lets you play it") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val spinneret = driver.putCreatureOnBattlefield(attacker, "Spinneret and Spiderling")
        driver.removeSummoningSickness(spinneret)
        // Pump to 4 power with three +1/+1 counters so its combat damage crosses the "4 or more" gate.
        driver.addComponent(spinneret, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Seed a known card on top of the library so we can track its exile.
        val topCard = driver.putCardOnTopOfLibrary(attacker, "Grizzly Bears")
        val exileBefore = driver.state.getExile(attacker).size

        driver.swingUnblocked(attacker, defender, listOf(spinneret))

        withClue("Spinneret dealt 4 combat damage to the opponent") {
            driver.getLifeTotal(defender) shouldBe 16
        }
        withClue("The top card of the library was exiled") {
            (topCard in driver.state.getExile(attacker)) shouldBe true
            (topCard in driver.state.getLibrary(attacker)) shouldBe false
            driver.state.getExile(attacker).size shouldBe exileBefore + 1
        }
        withClue("A may-play permission was granted for the exiled card") {
            driver.state.mayPlayPermissions.any { topCard in it.cardIds } shouldBe true
        }
    }
})
