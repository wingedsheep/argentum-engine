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
 * An Aura that damages its host's *controller* — "this Aura deals 2 damage to enchanted artifact's
 * controller" (Gremlin Infestation), a pipeline that gathers the enchanted permanent and aims at
 * `ControllerOfPipelineTarget` — is a curse on whoever's permanent it enchants, so the enchant
 * slot reads [TargetPolarity.HARMFUL]. The damage is to a player: a clock, not removal.
 *
 * The decision it changes: with no artifact of the opponent's to enchant, Gremlin Infestation
 * could only go on our own artifact, burning ourselves every end step; the AI no longer casts it.
 */
class HostControllerDamageAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    init {
        test("damage to the enchanted permanent's controller curses the host, and is no removal") {
            TargetPolarityAnalyzer.forSpell(card("Gremlin Infestation")) shouldBe listOf(TargetPolarity.HARMFUL)
            val tags = CardIntentAnalyzer.analyze(card("Gremlin Infestation")).tags
            tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
            withClue("two damage to a player answers no creature") { tags shouldNotContain IntentTag.REMOVAL }
        }

        test("Gremlin Infestation is not cast on our own artifact") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Gremlin Infestation")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Gremlin Infestation"
            }

            withClue("our Ornithopter is the only artifact to enchant") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry)) shouldBe true
            }
        }
    }
}
