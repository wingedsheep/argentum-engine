package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.CitadelSiege
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Citadel Siege (FRF) — {2}{W}{W} Enchantment. "As this enchantment enters, choose Khans or Dragons.
 * • Khans — At the beginning of combat on your turn, put two +1/+1 counters on target creature you
 *   control.
 * • Dragons — At the beginning of combat on each opponent's turn, tap target creature that player
 *   controls."
 *
 * The Dragons trigger is the interesting half: it fires on the opponent's turn and may only target a
 * creature the active opponent controls. Each mode is also checked off-turn / under the other choice,
 * so a swapped gate or a wrong trigger player fails.
 */
class CitadelSiegeScenarioTest : FunSpec({

    data class Board(
        val d: GameTestDriver,
        val you: EntityId,
        val opponent: EntityId,
        val yourA: EntityId,
        val yourB: EntityId,
        val theirA: EntityId,
        val theirB: EntityId,
    )

    /** The Siege's controller is the starting active player; each side has two Centaur Coursers. */
    fun setUp(mode: String): Board {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + CitadelSiege)
            initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        }
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val siege = d.putPermanentOnBattlefield(you, "Citadel Siege")
        d.addComponent(siege, CastChoicesComponent(chosen = mapOf(ChoiceSlot.MODE to ChoiceValue.TextChoice(mode))))
        return Board(
            d, you, opponent,
            yourA = d.putCreatureOnBattlefield(you, "Centaur Courser"),
            yourB = d.putCreatureOnBattlefield(you, "Centaur Courser"),
            theirA = d.putCreatureOnBattlefield(opponent, "Centaur Courser"),
            theirB = d.putCreatureOnBattlefield(opponent, "Centaur Courser"),
        )
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("Dragons: at the beginning of combat on an opponent's turn, tap target creature that player controls") {
        val b = setUp("dragons")
        val d = b.d
        d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = b.opponent)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe b.you
        decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(b.theirA, b.theirB)

        d.submitTargetSelection(b.you, listOf(b.theirA)).error shouldBe null
        d.drainStack()

        d.isTapped(b.theirA) shouldBe true
        d.isTapped(b.theirB) shouldBe false
        d.isTapped(b.yourA) shouldBe false
    }

    test("Dragons: nothing triggers at the beginning of combat on your own turn") {
        val b = setUp("dragons")
        val d = b.d
        d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = b.you)

        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
        listOf(b.yourA, b.yourB, b.theirA, b.theirB).forEach { d.isTapped(it) shouldBe false }
    }

    test("Khans: at the beginning of combat on your turn, put two +1/+1 counters on target creature you control") {
        val b = setUp("khans")
        val d = b.d
        d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = b.you)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(b.yourA, b.yourB)

        d.submitTargetSelection(b.you, listOf(b.yourA)).error shouldBe null
        d.drainStack()

        d.plusCounters(b.yourA) shouldBe 2
        d.plusCounters(b.yourB) shouldBe 0
    }

    test("Khans: no tap trigger on the opponent's turn") {
        val b = setUp("khans")
        val d = b.d
        // Your own begin-combat Khans trigger happens on the way; answer it, then move on.
        d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = b.you)
        d.submitTargetSelection(b.you, listOf(b.yourA)).error shouldBe null
        d.drainStack()

        d.passPriorityUntil(Step.BEGIN_COMBAT, activePlayer = b.opponent)
        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
        d.isTapped(b.theirA) shouldBe false
        d.isTapped(b.theirB) shouldBe false
    }
})
