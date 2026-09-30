package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.InvertPolarity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Invert Polarity (MH3 #190) — "Choose target spell, then flip a coin. If you win the flip, gain
 * control of that spell and you may choose new targets for it. If you lose the flip, counter that
 * spell."
 *
 * The flip is read, not seeded: each test replays fresh games until it sees the branch it covers.
 */
class InvertPolarityScenarioTest : FunSpec({

    class Game(val d: GameTestDriver, val p1: EntityId, val p2: EntityId, val bolt: EntityId, val won: Boolean)

    /** P2 bolts P1; P1 answers with Invert Polarity on the Bolt, which then resolves up to its flip. */
    fun invertBolt(): Game {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(InvertPolarity))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val bolt = d.putCardInHand(p2, "Lightning Bolt")
        val invert = d.putCardInHand(p1, "Invert Polarity")
        d.giveMana(p2, Color.RED, 1)
        d.giveMana(p1, Color.BLUE, 2)
        d.giveMana(p1, Color.RED, 1)

        d.passPriority(p1)
        d.submit(
            CastSpell(p2, bolt, targets = listOf(ChosenTarget.Player(p1)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        val boltOnStack = d.getTopOfStack()!!
        d.passPriority(p2)
        d.submit(
            CastSpell(p1, invert, targets = listOf(ChosenTarget.Spell(boltOnStack)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done

        val won = d.bothPass().events.filterIsInstance<CoinFlipEvent>().single().won
        return Game(d, p1, p2, boltOnStack, won)
    }

    fun playUntil(won: Boolean): Game =
        generateSequence { invertBolt() }.take(60).firstOrNull { it.won == won }
            ?: error("never saw a ${if (won) "won" else "lost"} flip in 60 games")

    test("win the flip: take the Bolt and point it at its caster") {
        val g = playUntil(won = true)
        val d = g.d

        withClue("the Bolt is now controlled by Invert Polarity's caster") {
            d.state.getEntity(g.bolt)!!.get<SpellOnStackComponent>()!!.casterId shouldBe g.p1
        }
        // "You may choose new targets for it": redirect the Bolt's single target to P2.
        d.submitCardSelection(g.p1, listOf(g.p2)).error shouldBe null

        d.bothPass() // the stolen Bolt resolves
        d.getLifeTotal(g.p2) shouldBe 17
        d.getLifeTotal(g.p1) shouldBe 20
        withClue("a stolen card still goes to its owner's graveyard") {
            (g.bolt in d.getGraveyard(g.p2)) shouldBe true
        }
    }

    test("win the flip and keep the targets: the stolen Bolt still hits the original target") {
        val g = playUntil(won = true)
        val d = g.d

        d.submitCardSelection(g.p1, listOf(g.p1)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(g.p1) shouldBe 17
        d.getLifeTotal(g.p2) shouldBe 20
    }

    test("lose the flip: the Bolt is countered") {
        val g = playUntil(won = false)
        val d = g.d

        d.stackSize shouldBe 0
        (g.bolt in d.getGraveyard(g.p2)) shouldBe true
        d.getLifeTotal(g.p1) shouldBe 20
        d.getLifeTotal(g.p2) shouldBe 20
    }
})
