package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
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
import io.kotest.matchers.shouldBe

/**
 * An Aura whose dies trigger returns the dying card to the battlefield under its owner's control
 * (Gift of Immortality) undoes its host's death — a gift on our own creature, wasted on an
 * opponent's. Its pipeline spelling (gather the triggering card, move it onto the battlefield)
 * reads as the recursion the single-card return from the graveyard (Unholy Indenture) does.
 *
 * The decision it changes: Gift of Immortality on the opponent's only creature reads as aimed at
 * the wrong side, so the AI holds it for a creature of its own.
 */
class HostDeathReturnAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    private fun aimsOnlyAtTheWrongSide(ours: String?, theirs: String?): Boolean {
        var builder = scenario()
            .withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Plains", 3)
            .withCardInHand(1, "Gift of Immortality")
        if (ours != null) builder = builder.withCardOnBattlefield(1, ours)
        if (theirs != null) builder = builder.withCardOnBattlefield(2, theirs)
        val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
        val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Gift of Immortality"
        }
        return TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry))
    }

    init {
        test("returning the dying host under its owner's control reads as a gift and a recursion") {
            TargetPolarityAnalyzer.forSpell(card("Gift of Immortality")) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(card("Gift of Immortality")).readFully shouldBe true
            CardIntentAnalyzer.analyze(card("Gift of Immortality")).tags shouldContain IntentTag.RECURSION
        }

        test("Gift of Immortality on the opponent's only creature is aimed at the wrong side") {
            withClue("the opponent's Grizzly Bears is the only creature to enchant") {
                aimsOnlyAtTheWrongSide(ours = null, theirs = "Grizzly Bears") shouldBe true
            }
            withClue("our own creature is a fine host") {
                aimsOnlyAtTheWrongSide(ours = "Grizzly Bears", theirs = null) shouldBe false
            }
        }
    }
}
