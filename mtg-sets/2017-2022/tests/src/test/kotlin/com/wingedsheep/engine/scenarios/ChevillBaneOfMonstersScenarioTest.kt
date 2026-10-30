package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.ChevillBaneOfMonsters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Chevill, Bane of Monsters (IKO) —
 * At the beginning of your upkeep, if your opponents control no permanents with bounty counters on
 * them, put a bounty counter on target creature or planeswalker an opponent controls.
 * Whenever a permanent an opponent controls with a bounty counter on it dies, you gain 3 life and
 * draw a card.
 *
 * Covers the intervening "if" (no new bounty while one is already out) and the dies trigger reading
 * the dead permanent's last-known counters.
 */
class ChevillBaneOfMonstersScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + ChevillBaneOfMonsters)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bounty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.BOUNTY) ?: 0

    fun GameTestDriver.seedBounty(id: EntityId) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.BOUNTY, 1))
        })
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (guard++ < 10 && state.stack.isNotEmpty()) bothPass()
    }

    /** Walk to [me]'s next upkeep and resolve whatever triggers there, aiming any target at [aim]. */
    fun GameTestDriver.runMyNextUpkeep(me: EntityId, aim: EntityId) {
        passPriorityUntil(Step.END, getOpponent(me))
        passPriorityUntil(Step.UPKEEP, me)
        if (pendingDecision is ChooseTargetsDecision) {
            submitTargetSelection(me, listOf(aim)).error shouldBe null
        }
        resolveStack()
    }

    test("upkeep puts a bounty counter on an opponent's creature when none is marked") {
        val d = setup()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Chevill, Bane of Monsters")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(me), "Hill Giant")

        d.runMyNextUpkeep(me, theirs)

        d.bounty(theirs) shouldBe 1
    }

    test("no new bounty counter while an opponent's permanent already has one") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Chevill, Bane of Monsters")
        val marked = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val unmarked = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.seedBounty(marked)

        d.runMyNextUpkeep(me, unmarked)

        d.bounty(marked) shouldBe 1
        d.bounty(unmarked) shouldBe 0
    }

    test("a bountied opposing creature dying gains 3 life and draws a card") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Chevill, Bane of Monsters")
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.seedBounty(theirs)
        val life = d.getLifeTotal(me)

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        val handBefore = d.getHandSize(me)
        d.submit(
            CastSpell(me, bolt, targets = listOf(ChosenTarget.Permanent(theirs)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        d.resolveStack()

        d.getGraveyard(opp) shouldContain theirs
        d.getLifeTotal(me) shouldBe life + 3
        // Bolt left the hand, the trigger drew one back.
        d.getHandSize(me) shouldBe handBefore
    }

    test("an unmarked opposing creature dying does nothing") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Chevill, Bane of Monsters")
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val life = d.getLifeTotal(me)

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        val handBefore = d.getHandSize(me)
        d.submit(
            CastSpell(me, bolt, targets = listOf(ChosenTarget.Permanent(theirs)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        d.resolveStack()

        d.getGraveyard(opp) shouldContain theirs
        d.getLifeTotal(me) shouldBe life
        d.getHandSize(me) shouldBe handBefore - 1
    }
})
