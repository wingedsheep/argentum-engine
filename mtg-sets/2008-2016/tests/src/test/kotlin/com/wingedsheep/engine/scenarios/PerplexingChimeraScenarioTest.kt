package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.PerplexingChimera
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Perplexing Chimera (BNG #48) — "Whenever an opponent casts a spell, you may exchange control of
 * this creature and that spell. If you do, you may choose new targets for the spell."
 */
class PerplexingChimeraScenarioTest : FunSpec({

    class Game(val d: GameTestDriver, val p1: EntityId, val p2: EntityId, val chimera: EntityId)

    fun setup(startingPlayer: Int = 0): Game {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(PerplexingChimera))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = startingPlayer)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = d.player1
        val p2 = d.player2
        val chimera = d.putCreatureOnBattlefield(p1, "Perplexing Chimera")
        return Game(d, p1, p2, chimera)
    }

    /** P2 casts [cardName] ([mana] of [color], [targets]); Chimera's trigger goes on the stack above it. */
    fun Game.opponentCasts(cardName: String, mana: Int, color: Color = Color.RED, targets: List<ChosenTarget> = emptyList()): EntityId {
        val card = d.putCardInHand(p2, cardName)
        d.giveMana(p2, color, mana)
        if (d.priorityPlayer == p1) d.passPriority(p1)
        d.submit(CastSpell(p2, card, targets = targets, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldBe Outcome.Done
        d.stackSize shouldBe 2
        return card
    }

    fun Game.controllerOf(id: EntityId) = d.state.projectedState.getController(id)

    test("exchange and redirect: P1 takes the Bolt aimed at them and points it at its caster") {
        val g = setup()
        val d = g.d
        val bolt = g.opponentCasts("Lightning Bolt", 1, targets = listOf(ChosenTarget.Player(g.p1)))

        d.bothPass() // the Chimera trigger resolves
        d.submitYesNo(g.p1, true).error.shouldBeNull()

        withClue("the spell is now P1's and the Chimera is P2's") {
            d.state.getEntity(bolt)!!.get<SpellOnStackComponent>()!!.casterId shouldBe g.p1
            g.controllerOf(g.chimera) shouldBe g.p2
        }
        d.submitCardSelection(g.p1, listOf(g.p2)).error.shouldBeNull()

        d.bothPass() // the Bolt resolves under P1
        d.getLifeTotal(g.p2) shouldBe 17
        d.getLifeTotal(g.p1) shouldBe 20
        withClue("the card still goes to its owner's graveyard") { (bolt in d.getGraveyard(g.p2)) shouldBe true }
        withClue("the exchange is indefinite: the Chimera stays with P2 after the spell is gone") {
            g.controllerOf(g.chimera) shouldBe g.p2
        }
    }

    test("decline: nothing changes hands and the spell resolves as cast") {
        val g = setup()
        val d = g.d
        val bolt = g.opponentCasts("Lightning Bolt", 1, targets = listOf(ChosenTarget.Player(g.p1)))

        d.bothPass()
        d.submitYesNo(g.p1, false).error.shouldBeNull()

        d.state.getEntity(bolt)!!.get<SpellOnStackComponent>()!!.casterId shouldBe g.p2
        g.controllerOf(g.chimera) shouldBe g.p1
        d.bothPass()
        d.getLifeTotal(g.p1) shouldBe 17
    }

    test("a stolen permanent spell enters under the Chimera's former controller") {
        val g = setup(startingPlayer = 1) // a creature spell needs P2's own main phase
        val d = g.d
        val bears = g.opponentCasts("Grizzly Bears", 2, Color.GREEN)

        d.bothPass()
        d.submitYesNo(g.p1, true).error.shouldBeNull()
        withClue("a spell without targets offers no retarget") { d.pendingDecision.shouldBeNull() }

        d.bothPass() // the Bears resolve
        withClue("the Bears entered under P1") { g.controllerOf(bears) shouldBe g.p1 }
        g.controllerOf(g.chimera) shouldBe g.p2
    }

    test("the Chimera gone before its trigger resolves: no exchange and no retarget") {
        val g = setup()
        val d = g.d
        val bolt = g.opponentCasts("Lightning Bolt", 1, targets = listOf(ChosenTarget.Player(g.p1)))

        // P1 answers the trigger by bolting their own Chimera (their own spell doesn't trigger it).
        val ownBolt = d.putCardInHand(g.p1, "Lightning Bolt")
        d.giveMana(g.p1, Color.RED, 1)
        d.passPriority(g.p2)
        d.submit(
            CastSpell(g.p1, ownBolt, targets = listOf(ChosenTarget.Permanent(g.chimera)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        d.bothPass() // P1's Bolt kills the Chimera
        (g.chimera in d.getGraveyard(g.p1)) shouldBe true

        d.bothPass() // the trigger resolves
        d.pendingDecision?.let { d.submitYesNo(g.p1, true).error.shouldBeNull() }
        withClue("CR 701.12a: half an exchange is no exchange, so the spell stays P2's and is not retargeted") {
            d.pendingDecision.shouldBeNull()
            d.state.getEntity(bolt).shouldNotBeNull().get<SpellOnStackComponent>()!!.casterId shouldBe g.p2
        }
        d.bothPass()
        d.getLifeTotal(g.p1) shouldBe 17
    }
})
