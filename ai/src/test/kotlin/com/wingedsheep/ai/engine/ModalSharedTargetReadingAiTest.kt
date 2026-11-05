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
 * "Target creature gains your choice of double strike or lifelink" — a
 * [com.wingedsheep.sdk.scripting.effects.ModalEffect] whose modes declare no targets of their own
 * and all act on a target the ability declared (Manifold Mouse, Practiced Offense, Rattleback
 * Apothecary, Ballroom Brawlers). Whichever mode is chosen happens to that one slot, so the slot
 * reads as the modes agree, as a controller-chosen `ChooseActionEffect` already does.
 *
 * Unread, Practiced Offense's creature slot fell back to the two-sided ranking and could hand
 * double strike to the opponent's creature.
 */
class ModalSharedTargetReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.permanent(playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("a choice of keywords over one shared target reads as a gift") {
            for (card in listOf("Manifold Mouse", "Rattleback Apothecary")) {
                val trigger = cardRegistry.requireCard(card).script.triggeredAbilities.first { it.targetRequirement != null }
                withClue("$card: either mode grants a keyword to the same creature") {
                    intents.effectTargetPolarities(trigger.effect, listOf(trigger.targetRequirement!!), card) shouldBe
                        listOf(TargetPolarity.BENEFICIAL)
                }
            }
        }

        test("Practiced Offense gives double strike to our creature, not the opponent's") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInHand(1, "Practiced Offense")
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(2, "Serra Angel")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? CastSpell)?.let { c -> game.state.getEntity(c.cardId)?.get<CardComponent>()?.name } == "Practiced Offense" }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell

            withClue("double strike or lifelink is a gift: the creature slot goes on our angel (targets ${filled.targets})") {
                filled.targets.filterIsInstance<ChosenTarget.Permanent>().map { it.entityId } shouldBe
                    listOf(game.permanent(game.player1Id, "Serra Angel"))
            }
        }
    }
}
