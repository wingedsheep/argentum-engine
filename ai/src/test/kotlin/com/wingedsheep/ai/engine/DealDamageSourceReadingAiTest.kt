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
 * The damage *source* slot of a one-sided "bite" ([com.wingedsheep.sdk.scripting.effects.DealDamageEffect]
 * with a targeted `damageSource`): "target creature you control deals damage equal to its power to
 * target creature you don't control". Unlike a fight's fighter, the biter takes nothing back — it
 * lends its power and nothing else — so the slot reads [TargetPolarity.BENEFICIAL]: the best
 * creature is the one to bite with.
 *
 * Unread, the slot fell back to the removal ranking, which orders our own creatures worst first:
 * Rabid Bite bit with a 1/1 Llanowar Elves and left the opponent's Hill Giant alive.
 */
class DealDamageSourceReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.onBattlefield(playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("a bite's source reads as ours to lend; its victim as harmful") {
            withClue("Rabid Bite: target creature you control deals damage to target creature you don't control") {
                intents.spellTargetPolarities("Rabid Bite") shouldBe listOf(TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
            }
        }

        test("Rabid Bite bites with Craw Wurm, not Llanowar Elves") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Rabid Bite")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { action ->
                    val spell = action.action as? CastSpell ?: return@first false
                    game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Rabid Bite"
                }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val targets = filled.targets.map { (it as ChosenTarget.Permanent).entityId }

            withClue("bite source reads as a creature we lend: Craw Wurm's 6 kills Hill Giant, Elves' 1 does not") {
                targets shouldBe listOf(
                    game.onBattlefield(game.player1Id, "Craw Wurm"),
                    game.onBattlefield(game.player2Id, "Hill Giant"),
                )
            }
        }
    }
}
