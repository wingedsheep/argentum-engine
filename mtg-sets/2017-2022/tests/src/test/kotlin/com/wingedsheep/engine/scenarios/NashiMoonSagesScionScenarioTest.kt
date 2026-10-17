package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.NashiMoonSagesScion
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Nashi, Moon Sage's Scion — "Whenever Nashi deals combat damage to a player, exile the top card
 * of each player's library. Until end of turn, you may play one of those cards. If you cast a
 * spell this way, pay life equal to its mana value rather than paying its mana cost."
 */
class NashiMoonSagesScionScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + NashiMoonSagesScion)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Nashi connects unblocked; the trigger resolves and the game moves to the postcombat main. */
    fun GameTestDriver.connectWithNashi() {
        val nashi = putCreatureOnBattlefield(player1, "Nashi, Moon Sage's Scion").also(::removeSummoningSickness)
        val opponent = getOpponent(player1)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, mapOf(nashi to opponent)).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareBlockers(opponent, emptyMap()).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
        state.stack.isEmpty() shouldBe true
    }

    fun GameTestDriver.castAction(cardId: EntityId) =
        legalActions(player1).singleOrNull { (it.action as? CastSpell)?.cardId == cardId }

    test("exiles the top card of each library; casting one spell costs life equal to its mana value and ends the permission") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val myBears = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val theirGiant = d.putCardOnTopOfLibrary(opponent, "Hill Giant")

        d.connectWithNashi()

        d.getExile(d.player1).contains(myBears) shouldBe true
        d.getExile(opponent).contains(theirGiant) shouldBe true
        d.castAction(myBears)?.additionalCostInfo?.description shouldBe "Pay 2 life"
        val giantAction = d.castAction(theirGiant)!!
        giantAction.affordable shouldBe true
        giantAction.manaCostString shouldBe "{0}"
        giantAction.additionalCostInfo?.description shouldBe "Pay 4 life"

        val lifeBefore = d.getLifeTotal(d.player1)
        d.submit(
            CastSpell(playerId = d.player1, cardId = theirGiant, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        d.bothPass()

        d.getLifeTotal(d.player1) shouldBe lifeBefore - 4
        d.findPermanent(d.player1, "Hill Giant") shouldBe theirGiant
        d.castAction(myBears) shouldBe null
    }

    test("playing a land from the pile uses the one play") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val myForest = d.putCardOnTopOfLibrary(d.player1, "Forest")
        val theirGiant = d.putCardOnTopOfLibrary(opponent, "Hill Giant")

        d.connectWithNashi()

        d.submit(PlayLand(d.player1, myForest)).error shouldBe null
        d.findPermanent(d.player1, "Forest") shouldBe myForest
        d.castAction(theirGiant) shouldBe null
    }

    test("the permission ends at end of turn") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val theirGiant = d.putCardOnTopOfLibrary(opponent, "Hill Giant")

        d.connectWithNashi()
        (d.castAction(theirGiant) != null) shouldBe true

        d.passPriorityUntil(Step.END, maxPasses = 300)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        d.state.activePlayerId shouldBe opponent
        d.castAction(theirGiant) shouldBe null
    }
})
