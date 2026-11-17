package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An Aura whose only ability is "when enchanted creature dies, you gain X life and draw X cards"
 * (Fruit of the First Tree) pays its controller off its host's death. On our own creature that
 * death is ours to time; an opponent keeps their enchanted creature alive, so the enchant slot is
 * a gift aimed at our side. Beside a curse it is only a consolation: Gremlin Infestation, which
 * burns its host's controller and makes a Gremlin when the artifact dies, stays a curse; and beside
 * any static (Public Enemy's forced attacks) the death payoff isn't the whole card, so it decides nothing.
 *
 * The decision it changes: Fruit of the First Tree on the opponent's only creature reads as aimed
 * at the wrong side, so the AI holds it for a creature of its own.
 */
class HostDeathPayoffAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    private fun aimsOnlyAtTheWrongSide(ours: String?, theirs: String?): Boolean {
        var builder = scenario()
            .withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardInHand(1, "Fruit of the First Tree")
        if (ours != null) builder = builder.withCardOnBattlefield(1, ours)
        if (theirs != null) builder = builder.withCardOnBattlefield(2, theirs)
        val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
        val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Fruit of the First Tree"
        }
        return TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry))
    }

    init {
        test("a death payoff for the Aura's controller reads as a gift, and beside a curse stays a consolation") {
            TargetPolarityAnalyzer.forSpell(card("Fruit of the First Tree")) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(card("Fruit of the First Tree")).readFully shouldBe true
            withClue("a Gremlin when the burned artifact dies is a consolation beside the curse, not a gift") {
                TargetPolarityAnalyzer.forSpell(card("Gremlin Infestation")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("beside a static no reader handles, the death draw doesn't make Public Enemy a gift") {
                TargetPolarityAnalyzer.forSpell(card("Public Enemy")) shouldBe listOf(TargetPolarity.UNKNOWN)
            }
        }

        test("Fruit of the First Tree on the opponent's only creature is aimed at the wrong side") {
            withClue("the opponent's Grizzly Bears is the only creature to enchant") {
                aimsOnlyAtTheWrongSide(ours = null, theirs = "Grizzly Bears") shouldBe true
            }
            withClue("our own creature is a fine host") {
                aimsOnlyAtTheWrongSide(ours = "Grizzly Bears", theirs = null) shouldBe false
            }
        }
    }
}
