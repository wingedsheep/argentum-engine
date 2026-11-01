package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * What copying does ([com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect]) to a
 * slot its filter keeps on our side: "create a token that's a copy of target creature you control"
 * gives *us* a second one of that creature, so the slot is [TargetPolarity.BENEFICIAL] — the best
 * creature is the one to copy.
 *
 * Unread, the slot fell back to the removal ranking, which orders our own creatures worst first:
 * the cheap target pick had Electroduplicate copy a 1/1 Llanowar Elves rather than the Craw Wurm
 * whose hasty copy is lethal.
 */
class CreateTokenCopyOfTargetReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.ours(name: String): EntityId =
        state.getBattlefield(player1Id).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("a copy of a target you control reads as a gift; one of any creature stays two-sided") {
            withClue("Electroduplicate: copy of target creature you control is ours to pick") {
                intents.spellTargetPolarities("Electroduplicate") shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            withClue("Heat Shimmer: copy of target creature — either side's creature may be the best copy") {
                intents.spellTargetPolarities("Heat Shimmer") shouldBe listOf(TargetPolarity.UNKNOWN)
            }
        }

        test("Electroduplicate copies Craw Wurm for lethal, not Llanowar Elves") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLifeTotal(2, 6)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInHand(1, "Electroduplicate")
                .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = true)
                .withCardOnBattlefield(1, "Llanowar Elves")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { action ->
                    val spell = action.action as? CastSpell ?: return@first false
                    game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Electroduplicate"
                }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell

            withClue("copy reads as a gift to the copied creature's controller: the hasty 6/4 copy swings for exactly 6") {
                (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe game.ours("Craw Wurm")
            }
        }
    }
}
