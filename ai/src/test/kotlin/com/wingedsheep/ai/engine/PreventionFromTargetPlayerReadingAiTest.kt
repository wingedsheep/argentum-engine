package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A prevention shield over the sources a *target player* controls — "Prevent all combat damage
 * that would be dealt this turn by creatures target opponent controls" (Encircling Fissure) —
 * muzzles that player's side, so the player slot reads [TargetPolarity.HARMFUL].
 *
 * Unread, the slot was an unknown in the reading audit: the shield names the player only through
 * its source filter, never as the effect's own target.
 */
class PreventionFromTargetPlayerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a shield over the creatures a target opponent controls reads harmful to that opponent") {
            withClue("Encircling Fissure: the targeted opponent's creatures deal no combat damage") {
                intents.spellTargetPolarities("Encircling Fissure") shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Encircling Fissure aimed at the opponent is not vetoed as aiming at the wrong side") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInHand(1, "Encircling Fissure")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Encircling Fissure"
            }

            withClue("muzzling the opponent's creatures is what the spell is for") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe false
            }
        }
    }
}
