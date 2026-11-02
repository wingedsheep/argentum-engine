package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A group effect over "each creature target player controls"
 * ([com.wingedsheep.sdk.scripting.effects.IterationSpace.Group] filtered by the player target) does
 * to that player's side whatever its body does to each creature: Trystan's Command's "creatures
 * target player controls get +3/+3 until end of turn. Untap them" wants us, Ashling's Command's "deals
 * 2 damage to each creature target player controls" wants them.
 *
 * Unread, the player slot fell back to the removal ranking. That ranking also scored every player as
 * ours (a player has no controller to read), so "target player" went to list order and Ashling's
 * Command aimed its 2 damage at our own creatures; with players ranked by side, an unread slot
 * prefers the opponent, which hands Trystan's Command's +3/+3 to their team. The reading fixes both.
 */
class TargetPlayerGroupReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.modePick(card: String, modeIndex: Int): ChosenTarget {
        val action = GameSimulator(cardRegistry).getLegalActions(state, player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            state.getEntity(spell.cardId)?.get<CardComponent>()?.name == card
        }
        val mode = action.modalEnumeration!!.modes.single { it.index == modeIndex }
        return TargetSelection.modeTargetVariants(
            state, action.action as CastSpell, mode, player1Id, intents, perSide = false,
        ).first().single()
    }

    init {
        test("a group over target player's creatures reads as its body does to each of them") {
            withClue("Trystan's Command mode 4: +3/+3 and untap target player's creatures is a gift") {
                intents.spellModeTargetPolarities("Trystan's Command", listOf(3)) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            withClue("Ashling's Command mode 3: 2 damage to each creature target player controls is a curse") {
                intents.spellModeTargetPolarities("Ashling's Command", listOf(2)) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("Sygg's Command mode 2: lifelink to target player's creatures is a gift") {
                intents.spellModeTargetPolarities("Sygg's Command", listOf(1)) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("Ashling's Command burns the opponent's team, not ours") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Ashling's Command")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val pick = game.modePick("Ashling's Command", 2)

            withClue("2 damage to each creature target player controls reads as a curse on the targeted player's side") {
                (pick as ChosenTarget.Player).playerId shouldBe game.player2Id
            }
        }

        test("Trystan's Command pumps our team, not the opponent's") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInHand(1, "Trystan's Command")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            withClue("+3/+3 and untap to each creature target player controls reads as a gift to the targeted player's side") {
                (game.modePick("Trystan's Command", 3) as ChosenTarget.Player).playerId shouldBe game.player1Id
            }
        }
    }
}
