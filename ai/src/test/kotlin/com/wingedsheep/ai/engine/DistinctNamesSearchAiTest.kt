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
 * A search whose payoff later counts the found cards by different names (Signal the Clans: "if you
 * reveal three cards with different names, …"): the decision itself carries no name restriction,
 * so best-first picking took both Llanowar Elves and switched the payoff off. The responder reads the
 * pending `DistinctNamesInCollections` check and keeps its picks one per name.
 */
class DistinctNamesSearchAiTest : FunSpec({

    val cards = listOf("GTC", "LEA").map(MtgSetCatalog::requireByCode)
        .flatMap { it.cards + it.basicLands }.distinctBy { it.name }

    test("Signal the Clans finds three differently named creatures, not two Llanowar Elves") {
        val d = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.player1
        listOf("Llanowar Elves", "Llanowar Elves", "Grizzly Bears", "Grizzly Bears", "Serra Angel").forEach { d.putCardOnTopOfLibrary(me, it) }
        val spell = d.putCardInHand(me, "Signal the Clans")
        d.giveMana(me, Color.RED, 1)
        d.giveMana(me, Color.GREEN, 1)
        d.submit(CastSpell(me, spell, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.bothPass()

        val decision = d.state.pendingDecision as SelectCardsDecision
        decision.onePerCardName shouldBe false
        decision.maxSelections shouldBe 3

        val responder = DecisionResponder(
            GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator(), intents = IntentCatalog.of(d.cardRegistry),
        )
        val picks = (responder.respond(d.state, decision, me) as CardsSelectedResponse).selectedCards
        val names = picks.map { d.state.getEntity(it)!!.get<CardComponent>()!!.name }
        picks shouldHaveSize 3
        names.toSet() shouldBe setOf("Llanowar Elves", "Grizzly Bears", "Serra Angel")
    }
})
