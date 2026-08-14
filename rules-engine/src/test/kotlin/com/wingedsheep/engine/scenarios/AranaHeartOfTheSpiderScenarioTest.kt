package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.AranaHeartOfTheSpider
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Araña, Heart of the Spider (SPM #123) — {1}{R}{W} Legendary Creature — Spider Human Hero 3/3.
 *
 * "Whenever you attack, put a +1/+1 counter on target attacking creature.
 *  Whenever a modified creature you control deals combat damage to a player, exile the top card of
 *  your library. You may play that card this turn. (Equipment, Auras you control, and counters are
 *  modifications.)"
 *
 * Proven end-to-end:
 *  1. Attacking fires the first trigger and puts one +1/+1 counter on a chosen attacking creature.
 *  2. When a MODIFIED creature you control (a Centaur Courser bearing a +1/+1 counter) connects with
 *     the opponent, the second trigger exiles the top card of your library and grants may-play
 *     permission for it.
 *  3. When only an UNMODIFIED creature connects with the opponent (the modified attacker being
 *     blocked, so its combat damage goes to a creature rather than a player), no exile happens —
 *     the IsModified source filter gates the trigger.
 */
class AranaHeartOfTheSpiderScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AranaHeartOfTheSpider)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.addPlusCounter(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        })
    }

    /** Resolve any pending combat-damage confirmation, then drain the stack (resolving triggers). */
    fun GameTestDriver.resolveCombatAndTriggers() {
        if (state.pendingDecision != null) confirmCombatDamage()
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: attacking puts a +1/+1 counter on a target attacking creature
    // ─────────────────────────────────────────────────────────────────────────
    test("attacking puts a +1/+1 counter on a target attacking creature") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val arana = driver.putCreatureOnBattlefield(me, "Araña, Heart of the Spider")
        val bear = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(arana)
        driver.removeSummoningSickness(bear)

        driver.plusOneCounters(bear) shouldBe 0

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(arana, bear), opp)

        // The "whenever you attack" trigger fires and asks for its target attacking creature.
        driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass() // resolve the attack trigger

        withClue("The chosen attacking creature gains one +1/+1 counter") {
            driver.plusOneCounters(bear) shouldBe 1
        }
        withClue("The unchosen attacker is untouched") {
            driver.plusOneCounters(arana) shouldBe 0
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: a modified creature connecting exiles the top card to play this turn
    // ─────────────────────────────────────────────────────────────────────────
    test("a modified creature dealing combat damage to a player exiles the top card to play this turn") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Araña provides the ability; a Centaur Courser with a +1/+1 counter is the modified attacker.
        driver.putCreatureOnBattlefield(me, "Araña, Heart of the Spider")
        val attacker = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.addPlusCounter(attacker, 1)
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val topCard = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val exileBefore = driver.getExile(me).size

        driver.declareAttackers(me, listOf(attacker), opp)

        // Resolve the "whenever you attack" trigger (only legal target is the lone attacker).
        driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(me, listOf(attacker))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opp)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.resolveCombatAndTriggers()

        withClue("The modified attacker connected with the opponent") {
            driver.getLifeTotal(opp) shouldBe 20 - 5 // 3/3 + two +1/+1 counters (seeded + attack trigger)
        }
        withClue("The top card of the library was exiled") {
            (topCard in driver.getExile(me)) shouldBe true
            (topCard in driver.state.getLibrary(me)) shouldBe false
            driver.getExile(me).size shouldBe exileBefore + 1
        }
        withClue("A may-play permission was granted for the exiled card") {
            driver.state.mayPlayPermissions.any { topCard in it.cardIds } shouldBe true
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: an unmodified creature connecting does NOT exile
    // ─────────────────────────────────────────────────────────────────────────
    test("an unmodified creature dealing combat damage to a player does not exile the top card") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val arana = driver.putCreatureOnBattlefield(me, "Araña, Heart of the Spider")
        val bear = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3, unmodified
        driver.removeSummoningSickness(arana)
        driver.removeSummoningSickness(bear)
        // Opponent blocker to soak Araña so its (modified) combat damage lands on a creature, not a player.
        val blocker = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val topCard = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val exileBefore = driver.getExile(me).size

        driver.declareAttackers(me, listOf(arana, bear), opp)

        // Direct the attack-trigger counter onto Araña, making IT the modified creature.
        driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(me, listOf(arana))
        driver.bothPass()
        driver.plusOneCounters(arana) shouldBe 1

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opp, mapOf(blocker to listOf(arana)))
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.resolveCombatAndTriggers()

        withClue("Only the unmodified Centaur Courser connected (3 damage); Araña was blocked") {
            driver.getLifeTotal(opp) shouldBe 20 - 3
        }
        withClue("No card was exiled — the connecting creature was unmodified") {
            driver.getExile(me).size shouldBe exileBefore
            (topCard in driver.state.getLibrary(me)) shouldBe true
        }
        withClue("No may-play permission was granted") {
            driver.state.mayPlayPermissions.any { topCard in it.cardIds } shouldBe false
        }
    }
})
