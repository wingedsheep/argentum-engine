package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Taurean Mauler — "{2}{R} 2/2 Changeling. Whenever an opponent casts a spell, you may put a
 * +1/+1 counter on this creature."
 *
 * The opponent must cast on a window they control, so these tests hand the opponent priority
 * (instant-speed) before they cast a harmless {0} instant. The scenario harness takes the
 * beneficial optional "may", so a successful trigger shows up as the +1/+1 counter being added.
 */
class TaureanMaulerScenarioTest : FunSpec({

    // A harmless {0} instant the opponent can cast to fire "whenever an opponent casts a spell".
    val zap = card("Zap") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(zap))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 8) d.bothPass()
    }

    test("opponent casts a spell: a +1/+1 counter is added (2/2 -> 3/3)") {
        val d = newDriver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        val mauler = d.putCreatureOnBattlefield(p1, "Taurean Mauler")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriority(p1) // active passes; opponent gets priority to cast the instant

        val zapId = d.putCardInHand(p2, "Zap")
        d.castSpell(p2, zapId).error shouldBe null
        resolveStack(d)

        val projected = d.state.projectedState
        projected.getPower(mauler) shouldBe 3
        projected.getToughness(mauler) shouldBe 3
    }

    test("a second opponent spell grows it again (3/3 -> 4/4)") {
        val d = newDriver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        val mauler = d.putCreatureOnBattlefield(p1, "Taurean Mauler")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriority(p1)

        d.castSpell(p2, d.putCardInHand(p2, "Zap")).error shouldBe null
        resolveStack(d)
        d.castSpell(p2, d.putCardInHand(p2, "Zap")).error shouldBe null
        resolveStack(d)

        val projected = d.state.projectedState
        projected.getPower(mauler) shouldBe 4
        projected.getToughness(mauler) shouldBe 4
    }

    test("your own spell does not trigger it (stays 2/2)") {
        val d = newDriver()
        val p1 = d.activePlayer!!

        val mauler = d.putCreatureOnBattlefield(p1, "Taurean Mauler")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castSpell(p1, d.putCardInHand(p1, "Zap")).error shouldBe null
        resolveStack(d)

        val projected = d.state.projectedState
        projected.getPower(mauler) shouldBe 2
        projected.getToughness(mauler) shouldBe 2
    }
})
