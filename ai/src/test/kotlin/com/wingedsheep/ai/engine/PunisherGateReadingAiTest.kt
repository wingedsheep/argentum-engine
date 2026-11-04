package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A gate another player decides ([com.wingedsheep.sdk.scripting.effects.GatedEffect] with a
 * `decisionMaker`, the punisher shape) reads as what **both** of its branches do. Which branch
 * happens is that player's call, so neither branch alone is what the card does — but what they
 * share happens either way.
 *
 * Clash of Elements: "its owner may put it on top of their library … if they didn't, they put it
 * on the bottom". Both branches tuck the target, so the card is removal and the slot is harmful.
 * Unread, nothing stopped the AI from casting it at its own creature when the opponent's only
 * nonland permanent was hexproof.
 */
class PunisherGateReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an opponent's choice between two tucks reads as removal") {
            withClue("Clash of Elements: top or bottom of its owner's library, the owner's pick") {
                TargetPolarityAnalyzer.forSpell(card("Clash of Elements")) shouldBe listOf(TargetPolarity.HARMFUL)
                CardIntentAnalyzer.analyze(card("Clash of Elements")).tags shouldContain IntentTag.REMOVAL
            }
            withClue("Risk Factor: four damage to them or three cards for us share nothing, so neither is read") {
                CardIntentAnalyzer.analyze(card("Risk Factor")).tags shouldNotContain IntentTag.DRAW
            }
        }

        test("Clash of Elements is not cast at our own creature when theirs can't be targeted") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Gladecover Scout")
                .withCardInHand(1, "Clash of Elements")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Clash of Elements"
            }

            withClue("the hexproof Scout leaves only our Bears to target, and either branch tucks it") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry)) shouldBe true
            }
        }
    }
}
