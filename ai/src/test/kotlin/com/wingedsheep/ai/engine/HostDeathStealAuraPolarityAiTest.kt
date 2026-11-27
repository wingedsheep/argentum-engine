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
 * An Aura whose dies trigger returns the dying card to the battlefield under the Aura
 * controller's own control (Unhallowed Pact) serves them on either side: on our creature it
 * undoes the death, on the opponent's it steals the card. Its enchant slot reads FLEXIBLE.
 *
 * The decision it changes: the AI no longer treats Unhallowed Pact as unread — it will cast it on
 * the opponent's only creature as readily as on its own.
 */
class HostDeathStealAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    private fun aimsOnlyAtTheWrongSide(ours: String?, theirs: String?): Boolean {
        var builder = scenario()
            .withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInHand(1, "Unhallowed Pact")
        if (ours != null) builder = builder.withCardOnBattlefield(1, ours)
        if (theirs != null) builder = builder.withCardOnBattlefield(2, theirs)
        val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
        val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Unhallowed Pact"
        }
        return TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry))
    }

    init {
        test("returning the dying host under your control reads as useful on either side") {
            TargetPolarityAnalyzer.forSpell(card("Unhallowed Pact")) shouldBe listOf(TargetPolarity.FLEXIBLE)
            AiReadingAudit.read(card("Unhallowed Pact")).readFully shouldBe true
        }

        test("Unhallowed Pact is never aimed at the wrong side") {
            withClue("the opponent's Grizzly Bears is stolen when it dies") {
                aimsOnlyAtTheWrongSide(ours = null, theirs = "Grizzly Bears") shouldBe false
            }
            withClue("our own Grizzly Bears comes back when it dies") {
                aimsOnlyAtTheWrongSide(ours = "Grizzly Bears", theirs = null) shouldBe false
            }
        }

        test("a return under its owner's control still reads as a gift only") {
            TargetPolarityAnalyzer.forSpell(card("Gift of Immortality")) shouldBe listOf(TargetPolarity.BENEFICIAL)
        }
    }
}
