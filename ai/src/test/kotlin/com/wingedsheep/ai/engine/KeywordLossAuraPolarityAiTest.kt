package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * An Aura whose static takes a keyword away from what it enchants
 * ([com.wingedsheep.sdk.scripting.RemoveKeywordStatic] — Grounded's "enchanted creature loses
 * flying") is the keyword grant turned around: losing a keyword that helps its holder is a curse,
 * so the enchant slot names the opponent's flier. Unread, the slot fell back to the removal ranking.
 */
class KeywordLossAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an Aura that strips flying from its host is a curse on the host") {
            TargetPolarityAnalyzer.forSpell(card("Grounded")) shouldBe listOf(TargetPolarity.HARMFUL)
            withClue("and, like Bonds of Mortality's one-shot strip, it is for winning the fight") {
                CardIntentAnalyzer.analyze(card("Grounded")).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
            }
        }

        test("Grounded enchants the opponent's flier, not our own") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(4)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Grounded")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val grounded = game.state.getZone(game.player1Id, Zone.HAND)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grounded" }
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? CastSpell)?.cardId == grounded }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as CastSpell

            val host = (filled.targets.single() as ChosenTarget.Permanent).entityId
            withClue("losing flying only hurts the creature that had it, so the host is theirs") {
                game.state.projectedState.getController(host) shouldBe game.player2Id
            }
        }
    }
}
