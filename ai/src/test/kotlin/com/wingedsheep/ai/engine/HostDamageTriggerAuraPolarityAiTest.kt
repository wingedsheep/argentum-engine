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
 * An Aura's "whenever enchanted creature deals / is dealt damage" trigger names the host's
 * controller off the damage event — Ragged Veins' "its controller loses that much life" — so it is
 * a curse on the host; Spirit Link's "you gain that much life" off its host's damage is a gift to
 * a creature of ours.
 *
 * The decision it changes: with only our own creature to enchant, casting Ragged Veins would drain
 * ourselves, so the AI reads the cast as aimed only at the wrong side — and Spirit Link the reverse.
 */
class HostDamageTriggerAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    private fun aimsOnlyAtTheWrongSide(aura: String, land: String, ours: String?, theirs: String?): Boolean {
        var builder = scenario()
            .withPlayers()
            .withActivePlayer(1)
            .withLandsOnBattlefield(1, land, 2)
            .withCardInHand(1, aura)
        if (ours != null) builder = builder.withCardOnBattlefield(1, ours)
        if (theirs != null) builder = builder.withCardOnBattlefield(2, theirs)
        val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
        val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == aura
        }
        return TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry))
    }

    init {
        test("draining the host's controller reads as a curse, gaining off its damage as a gift") {
            TargetPolarityAnalyzer.forSpell(card("Ragged Veins")) shouldBe listOf(TargetPolarity.HARMFUL)
            AiReadingAudit.read(card("Ragged Veins")).readFully shouldBe true
            TargetPolarityAnalyzer.forSpell(card("Visions of Brutality")) shouldBe listOf(TargetPolarity.HARMFUL)
            TargetPolarityAnalyzer.forSpell(card("Spirit Link")) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(card("Spirit Link")).readFully shouldBe true
        }

        test("Ragged Veins on our only creature is aimed at the wrong side") {
            withClue("our Grizzly Bears is the only creature to enchant") {
                aimsOnlyAtTheWrongSide("Ragged Veins", "Swamp", ours = "Grizzly Bears", theirs = null) shouldBe true
            }
            withClue("the opponent's creature is a fine host") {
                aimsOnlyAtTheWrongSide("Ragged Veins", "Swamp", ours = null, theirs = "Grizzly Bears") shouldBe false
            }
        }

        test("Spirit Link on the opponent's only creature is aimed at the wrong side") {
            aimsOnlyAtTheWrongSide("Spirit Link", "Plains", ours = null, theirs = "Grizzly Bears") shouldBe true
        }
    }
}
