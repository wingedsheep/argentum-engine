package com.wingedsheep.ai.engine

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
 * An edict — "target player sacrifices a creature" ([com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect]),
 * or a pipeline that takes a target player's permanents to a graveyard or exile (Break Under
 * Pressure's "sacrifices a creature or planeswalker with the greatest mana value") — costs that
 * player a permanent, so the player slot reads [TargetPolarity.HARMFUL].
 *
 * Unread, nothing stopped the AI from casting Diabolic Edict at itself when the opponent was
 * hexproof: the wrong-side guard only fires on a slot it can read.
 */
class ForceSacrificeReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an edict's player slot reads as harmful") {
            withClue("Diabolic Edict: target player sacrifices a creature") {
                TargetPolarityAnalyzer.forSpell(card("Diabolic Edict")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("Break Under Pressure: target opponent sacrifices their greatest-mana-value creature or planeswalker") {
                TargetPolarityAnalyzer.forSpell(card("Break Under Pressure")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("Liliana of the Veil: −2 makes target player sacrifice a creature, −6 a pile of their permanents") {
                val (_, minusTwo, minusSix) = card("Liliana of the Veil").script.activatedAbilities
                TargetPolarityAnalyzer.forRequirements(minusTwo.effect, minusTwo.targetRequirements) shouldBe listOf(TargetPolarity.HARMFUL)
                TargetPolarityAnalyzer.forRequirements(minusSix.effect, minusSix.targetRequirements) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Diabolic Edict is not cast at ourselves when the opponent can't be targeted") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .withCardInHand(1, "Diabolic Edict")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Diabolic Edict"
            }

            withClue("with the opponent hexproof the only target is ourselves, and an edict on ourselves costs us the Bears") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry)) shouldBe true
            }
        }
    }
}
