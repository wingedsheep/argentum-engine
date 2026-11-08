package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A per-category edict ([com.wingedsheep.sdk.scripting.effects.ChooseOnePerCategoryEffect] with the
 * SACRIFICE purpose — Perilous Predicament's "sacrifices an artifact creature and a nonartifact
 * creature") asks the AI victim for its picks "to sacrifice", so it gives up the least valuable
 * creature of each category. With the step's old hardcoded "to keep" prompt the same choice would
 * have been answered as a keep and handed over the best one.
 *
 * The card reads as removal, not a sweeper: the per-category picks are a picked few, the way a
 * selection's chosen side is — while Consuming Tide's "the rest", the group minus the picks, stays
 * a mass bounce.
 */
class PerCategoryEdictAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("a per-category edict reads as removal, not a sweeper") {
            val tags = CardIntentAnalyzer.analyze(card("Perilous Predicament")).tags
            tags shouldContain IntentTag.REMOVAL
            tags shouldNotContain IntentTag.SWEEPER
            withClue("the rest of a keep-one-per-category pick is still the group") {
                CardIntentAnalyzer.analyze(card("Consuming Tide")).tags shouldContain IntentTag.SWEEPER
            }
        }

        test("the AI sacrifices its least valuable artifact creature and nonartifact creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Perilous Predicament")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Juggernaut")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Perilous Predicament").error shouldBe null
            game.resolveStack()

            val responder = DecisionResponder(GameSimulator(cardRegistry), AIPlayer.defaultEvaluator())
            val picked = mutableListOf<String>()
            repeat(2) {
                val decision = game.getPendingDecision() as SelectCardsDecision
                decision.playerId shouldBe game.player2Id
                val answer = responder.respond(game.state, decision, game.player2Id) as CardsSelectedResponse
                answer.selectedCards.forEach { id ->
                    picked += game.state.getEntity(id)!!.get<CardComponent>()!!.name
                }
                game.selectCards(answer.selectedCards).error shouldBe null
                game.resolveStack()
            }

            withClue("the cheap artifact creature and the cheap nonartifact creature go") {
                picked.toSet() shouldBe setOf("Ornithopter", "Grizzly Bears")
            }
            game.findPermanents("Juggernaut").size shouldBe 1
            game.findPermanents("Serra Angel").size shouldBe 1
        }
    }
}
