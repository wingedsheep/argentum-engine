package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jou.cards.BloodcrazedHoplite
import com.wingedsheep.mtg.sets.definitions.ktk.cards.DragonscaleBoon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bloodcrazed Hoplite — "Heroic — Whenever you cast a spell that targets this creature, put a
 * +1/+1 counter on it. Whenever a +1/+1 counter is put on this creature, remove a +1/+1 counter
 * from target creature an opponent controls."
 *
 * Proves the self-scoped per-counter trigger: Dragonscale Boon's heroic counter triggers it once,
 * and the Boon's two simultaneous counters trigger it twice more (the ruling), so three counters
 * come off the opponent's creature.
 */
class BloodcrazedHopliteScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BloodcrazedHoplite, DragonscaleBoon))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun plusCounters(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("heroic counter plus two simultaneous counters trigger it three times") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val hoplite = d.putCreatureOnBattlefield(me, "Bloodcrazed Hoplite")
        val victim = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.addComponent(victim, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))

        d.giveMana(me, Color.GREEN, 4)
        val boon = d.putCardInHand(me, "Dragonscale Boon")
        d.castSpellWithTargets(me, boon, listOf(ChosenTarget.Permanent(hoplite))).error shouldBe null

        var guard = 0
        while ((d.state.stack.isNotEmpty() || d.pendingDecision != null) && guard++ < 40) {
            val decision = d.pendingDecision
            if (decision is ChooseTargetsDecision) {
                d.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(victim)))).error shouldBe null
            } else {
                d.bothPass()
            }
        }

        withClue("Hoplite ends with the heroic counter and the Boon's two") { plusCounters(d, hoplite) shouldBe 3 }
        withClue("one removal per counter placed on the Hoplite") { plusCounters(d, victim) shouldBe 1 }
    }
})
