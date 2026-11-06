package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.Decommission
import com.wingedsheep.mtg.sets.definitions.aer.cards.GreenwheelLiberator
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Greenwheel Liberator (AER) — revolt as an entry replacement: it enters with two +1/+1
 * counters only if a permanent its controller controlled left the battlefield this turn.
 * Decommission destroying an enchantment is the way a permanent leaves.
 */
class GreenwheelLiberatorScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + Decommission + GreenwheelLiberator)
        it.initMirrorMatch(Deck.of("Plains" to 20, "Forest" to 20), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(3) { _ -> it.putLandOnBattlefield(it.player1, "Plains") }
        repeat(3) { _ -> it.putLandOnBattlefield(it.player1, "Forest") }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castLiberator(): EntityId {
        val card = putCardInHand(player1, "Greenwheel Liberator")
        castSpell(player1, card).error shouldBe null
        bothPass()
        return findPermanent(player1, "Greenwheel Liberator")!!
    }

    test("enters with no counters when nothing of yours left the battlefield") {
        val d = driver()
        val liberator = d.castLiberator()
        d.plusOneCounters(liberator) shouldBe 0
        d.state.projectedState.getPower(liberator) shouldBe 2
        d.state.projectedState.getToughness(liberator) shouldBe 1
    }

    test("an opponent's permanent leaving does not turn on revolt") {
        val d = driver()
        val theirs = d.putPermanentOnBattlefield(d.player2, "Test Enchantment")
        val spell = d.putCardInHand(d.player1, "Decommission")
        d.castSpell(d.player1, spell, listOf(theirs)).error shouldBe null
        d.bothPass()

        val liberator = d.castLiberator()
        withClue("only the opponent's enchantment left") { d.plusOneCounters(liberator) shouldBe 0 }
    }

    test("enters with two +1/+1 counters after a permanent of yours left this turn") {
        val d = driver()
        val mine = d.putPermanentOnBattlefield(d.player1, "Test Enchantment")
        val spell = d.putCardInHand(d.player1, "Decommission")
        d.castSpell(d.player1, spell, listOf(mine)).error shouldBe null
        d.bothPass()

        val liberator = d.castLiberator()
        d.plusOneCounters(liberator) shouldBe 2
        d.state.projectedState.getPower(liberator) shouldBe 4
        d.state.projectedState.getToughness(liberator) shouldBe 3
    }
})
