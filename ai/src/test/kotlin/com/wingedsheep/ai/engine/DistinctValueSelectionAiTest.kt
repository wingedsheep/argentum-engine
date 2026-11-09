package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * A "keep any number with different mana values" selection (Seasons Past): the server keeps only
 * the first card of each mana value a response names, so the AI's best-first pick must skip a
 * second card of a value it already took and spend that slot on the next distinct one.
 */
class DistinctValueSelectionAiTest : FunSpec({

    val cards = listOf("SOI", "LEA").map(MtgSetCatalog::requireByCode)
        .flatMap { it.cards + it.basicLands }.distinctBy { it.name }

    test("Seasons Past returns three cards of three mana values, not two Craw Wurms") {
        val d = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.player1
        listOf("Craw Wurm", "Craw Wurm", "Hill Giant", "Llanowar Elves").forEach { d.putCardInGraveyard(me, it) }
        val spell = d.putCardInHand(me, "Seasons Past")
        d.giveMana(me, Color.GREEN, 6)
        d.submit(CastSpell(me, spell, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.bothPass()

        val decision = d.state.pendingDecision as SelectCardsDecision
        decision.onePerManaValue shouldBe true
        decision.maxSelections shouldBe 3

        val responder = DecisionResponder(
            GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator(), intents = IntentCatalog.of(d.cardRegistry),
        )
        val picks = (responder.respond(d.state, decision, me) as CardsSelectedResponse).selectedCards
        val manaValues = picks.map { d.state.getEntity(it)!!.get<CardComponent>()!!.manaValue }
        picks shouldHaveSize 3
        manaValues.toSet() shouldBe setOf(6, 4, 1)
    }
})
