package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rna.cards.GallopingLizrog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Galloping Lizrog (RNA #175) — {3}{G}{U} 3/3 Trample.
 * "When this creature enters, you may remove any number of +1/+1 counters from among creatures you
 * control. If you do, put twice that many +1/+1 counters on this creature."
 *
 * The removal is one distribution over your creatures carrying +1/+1 counters, each capped at its
 * own count, any total including none; the Lizrog then gets twice what actually came off.
 */
class GallopingLizrogScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(GallopingLizrog))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        return d
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.setCounters(id: EntityId, kind: CounterType, n: Int) =
        addComponent(id, CountersComponent(mapOf(kind to n)))

    /** Casts the Lizrog and resolves it, leaving its enters trigger resolving (paused or done). */
    fun GameTestDriver.castLizrog(player: EntityId): EntityId {
        val card = putCardInHand(player, "Galloping Lizrog")
        giveMana(player, Color.GREEN, 4)
        giveMana(player, Color.BLUE, 1)
        castSpell(player, card).outcome shouldBe Outcome.Done
        bothPass() // Lizrog resolves; its trigger goes on the stack
        bothPass() // the trigger resolves
        return findPermanent(player, "Galloping Lizrog")!!
    }

    test("removes the chosen counters from among your creatures and puts twice that many on itself") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(you, "Centaur Courser")
        val bare = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val charged = d.putCreatureOnBattlefield(you, "Centaur Courser")
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.setCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)
        d.setCounters(courser, CounterType.PLUS_ONE_PLUS_ONE, 3)
        d.setCounters(charged, CounterType.CHARGE, 2)
        d.setCounters(theirs, CounterType.PLUS_ONE_PLUS_ONE, 4)

        d.castLizrog(you)

        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.playerId shouldBe you
        // Only your creatures with +1/+1 counters: not the bare one, the charge-counter one, or the opponent's.
        decision.targets shouldContainExactlyInAnyOrder listOf(bears, courser)
        decision.maxPerTarget shouldBe mapOf(bears to 2, courser to 3)
        decision.allowPartial shouldBe true

        d.submitDecision(you, DistributionResponse(decision.id, mapOf(bears to 2, courser to 1)))

        val lizrog = d.findPermanent(you, "Galloping Lizrog")!!
        d.plusCounters(bears) shouldBe 0
        d.plusCounters(courser) shouldBe 2
        d.plusCounters(lizrog) shouldBe 6
        d.plusCounters(theirs) shouldBe 4
        d.plusCounters(bare) shouldBe 0
    }

    test("its own +1/+1 counters count among creatures you control") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.setCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

        val card = d.putCardInHand(you, "Galloping Lizrog")
        d.giveMana(you, Color.GREEN, 4)
        d.giveMana(you, Color.BLUE, 1)
        d.castSpell(you, card).outcome shouldBe Outcome.Done
        d.bothPass() // Lizrog resolves; its trigger goes on the stack
        val lizrog = d.findPermanent(you, "Galloping Lizrog")!!
        d.setCounters(lizrog, CounterType.PLUS_ONE_PLUS_ONE, 2) // e.g. placed in response
        d.bothPass() // the trigger resolves

        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.targets shouldContainExactlyInAnyOrder listOf(bears, lizrog)
        d.submitDecision(you, DistributionResponse(decision.id, mapOf(lizrog to 2, bears to 1)))

        // 3 removed (2 of them its own), 6 put back: 2 - 2 + 6.
        d.plusCounters(lizrog) shouldBe 6
        d.plusCounters(bears) shouldBe 0
    }

    test("removing none puts no counters on it") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.setCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)

        d.castLizrog(you)
        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        d.submitDecision(you, DistributionResponse(decision.id, emptyMap()))

        d.plusCounters(bears) shouldBe 2
        d.plusCounters(d.findPermanent(you, "Galloping Lizrog")!!) shouldBe 0
    }

    test("with no +1/+1 counters among your creatures nothing is asked and nothing is placed") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.setCounters(theirs, CounterType.PLUS_ONE_PLUS_ONE, 3)

        val lizrog = d.castLizrog(you)

        d.pendingDecision shouldBe null
        d.plusCounters(lizrog) shouldBe 0
        d.plusCounters(theirs) shouldBe 3
    }
})
