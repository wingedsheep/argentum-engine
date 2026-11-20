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
 * An Aura whose only host effect is "when enchanted creature dies, its controller draws a card"
 * (Fate Foretold) hands the card to whoever controlled the creature: on an opponent's creature it
 * pays them, on ours it pays us. So the enchant slot is a gift, aimed at our own side — the same
 * reading a death payoff for the Aura's own controller gets (Fruit of the First Tree).
 *
 * The decision it changes: Fate Foretold on the opponent's only creature reads as aimed at the
 * wrong side, so the AI holds it for a creature of its own.
 */
class DeadHostControllerDrawAuraPolarityAiTest : ScenarioTestBase() {

    private fun aimsOnlyAtTheWrongSide(ours: String?, theirs: String?): Boolean {
        var builder = scenario()
            .withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInHand(1, "Fate Foretold")
        if (ours != null) builder = builder.withCardOnBattlefield(1, ours)
        if (theirs != null) builder = builder.withCardOnBattlefield(2, theirs)
        val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
        val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Fate Foretold"
        }
        return TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry))
    }

    init {
        test("a death draw for the dead creature's controller reads as a gift") {
            val card = cardRegistry.requireCard("Fate Foretold")
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.BENEFICIAL)
            val reading = AiReadingAudit.read(card)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("Fate Foretold on the opponent's only creature is aimed at the wrong side") {
            withClue("the opponent's Grizzly Bears is the only creature to enchant") {
                aimsOnlyAtTheWrongSide(ours = null, theirs = "Grizzly Bears") shouldBe true
            }
            withClue("our own creature is a fine host") {
                aimsOnlyAtTheWrongSide(ours = "Grizzly Bears", theirs = null) shouldBe false
            }
        }
    }
}
