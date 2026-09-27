package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Corruption of Towashi — ETB incubate 4; "Whenever a permanent you control transforms or a
 * permanent you control enters transformed, you may draw a card. Do this only once each turn."
 *
 * The once-each-turn limit is on the draw: a declined draw leaves the ability live (ruling
 * 2023-04-14).
 */
class CorruptionOfTowashiScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateTransform(name: String) {
        val permanent = findPermanent(name)!!
        val abilityId = cardRegistry.getCard(name)!!.activatedAbilities.first().id
        execute(ActivateAbility(playerId = player1Id, sourceId = permanent, abilityId = abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Corruption of Towashi")
        .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
        .withLandsOnBattlefield(1, "Island", 9)
        .withLandsOnBattlefield(1, "Mountain", 12)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Corruption of Towashi") {

            test("a transform offers a draw, and only one draw is allowed each turn") {
                val game = board().build()
                game.castSpell(1, "Corruption of Towashi")
                game.resolveStack()

                val handBefore = game.state.getHand(game.player1Id).size
                game.activateTransform("Incubator")
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()
                game.state.getHand(game.player1Id).size shouldBe handBefore + 1

                game.activateTransform("Smoldering Werewolf")
                withClue("already drew this turn — the ability doesn't trigger again") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                    game.state.getHand(game.player1Id).size shouldBe handBefore + 1
                }
            }

            test("declining the draw leaves the ability live for the next transform") {
                val game = board().build()
                game.castSpell(1, "Corruption of Towashi")
                game.resolveStack()

                val handBefore = game.state.getHand(game.player1Id).size
                game.activateTransform("Incubator")
                game.answerYesNo(false)
                game.resolveStack()
                game.state.getHand(game.player1Id).size shouldBe handBefore

                game.activateTransform("Smoldering Werewolf")
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()
                game.state.getHand(game.player1Id).size shouldBe handBefore + 1
            }
        }
    }
}
