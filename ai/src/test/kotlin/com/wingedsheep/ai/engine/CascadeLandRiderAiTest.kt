package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardAsks
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [com.wingedsheep.sdk.scripting.CascadeMayPutOntoBattlefield] — Averna, the Chaos Bloom's "as you
 * cascade, you may put a land card from among the exiled cards onto the battlefield tapped":
 * read as ramp, rated as half a ramp term (it needs a cascade), asking for the deck's cascade cards,
 * and answered by taking the land — even a lone one, which the generic "choose up to N" declined.
 */
class CascadeLandRiderAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("the rider reads as ramp, earns a heuristic term and asks for cascade cards") {
            val averna = card("Averna, the Chaos Bloom")
            CardIntentAnalyzer.analyze(averna).tags shouldContain IntentTag.RAMP

            val terms = LimitedCardRater.explain(averna).heuristic
            withClue("Averna terms: $terms") {
                terms.single { it.part == "Static ability 1" }.value shouldBe 0.2
            }

            val asks = CardAsks.of(averna).map { it.filter }
            asks shouldContain GameObjectFilter.Any.withKeyword(Keyword.CASCADE)
        }

        test("the AI puts the exiled land onto the battlefield") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Quandrix, the Proof")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Averna, the Chaos Bloom")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Grizzly Bears")
                .build()
            game.castSpell(1, "Quandrix, the Proof").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val responder = DecisionResponder(GameSimulator(cardRegistry), AIPlayer.defaultEvaluator())
            val answer = responder.respond(game.state, decision, game.player1Id)
                .shouldBeInstanceOf<CardsSelectedResponse>()

            answer.selectedCards.map { game.state.getEntity(it)?.get<CardComponent>()?.name } shouldBe listOf("Mountain")
            LimitedCardRater.explain(card("Averna, the Chaos Bloom")).heuristicRating shouldBeGreaterThan 3.0
        }
    }
}
