package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.ClackbridgeTroll
import com.wingedsheep.mtg.sets.definitions.soi.cards.SinProdder
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * An offer only our opponents answer (`AnyPlayerMayPayEffect` scoped to `Player.EachOpponent`) is
 * read by both branches, since either is ours: Sin Prodder is card draw (the revealed card to our
 * hand) and damage to players (the acceptor takes its mana value). Tapping the source itself is the
 * price of the deal, so Clackbridge Troll is no tapper. The opponent answering the free choice
 * weighs the branches by simulation: it never takes lethal, and bins a land for free.
 */
class OpponentOfferAiTest : FunSpec({

    test("Sin Prodder reads as card draw and damage to players, with nothing unread") {
        CardIntentAnalyzer.analyze(SinProdder).tags shouldContainAll listOf(IntentTag.DRAW, IntentTag.DAMAGE_TO_PLAYERS)
        AiReadingAudit.audit(SinProdder).shouldBeEmpty()
    }

    test("the Troll tapping itself when an opponent sacrifices is not a tapper") {
        val tags = CardIntentAnalyzer.analyze(ClackbridgeTroll).tags
        tags shouldContain IntentTag.DRAW
        tags shouldNotContain IntentTag.TAPPER
    }

    data class Setup(val driver: GameTestDriver, val opp: EntityId)

    fun offered(topCard: String, opponentLife: Int): Setup {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + SinProdder)
            initMirrorMatch(Deck.of("Mountain" to 30))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Sin Prodder")
        d.passPriorityUntil(Step.UPKEEP, activePlayer = me)
        d.putCardOnTopOfLibrary(me, topCard)
        d.setLifeTotal(opp, opponentLife)
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opp
        return Setup(d, opp)
    }

    fun answer(d: GameTestDriver, player: EntityId): Boolean {
        val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        val simulator = GameSimulator(d.cardRegistry)
        val responder = DecisionResponder(simulator, AIPlayer.defaultEvaluator())
        simulator.decisionResolver = { state, next -> responder.respond(state, next, next.playerId) }
        return responder.respond(d.state, decision, player).shouldBeInstanceOf<YesNoResponse>().choice
    }

    test("the opponent never accepts damage that would kill them") {
        val (d, opp) = offered("Centaur Courser", opponentLife = 2) // mana value 3
        answer(d, opp) shouldBe false
    }

    test("the opponent bins a revealed land, which costs them nothing") {
        val (d, opp) = offered("Forest", opponentLife = 20)
        answer(d, opp) shouldBe true
    }
})
