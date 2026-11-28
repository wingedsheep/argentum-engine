package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Brain Weevil (ISD #91) — {3}{B} 1/1 Insect.
 *
 *   Intimidate
 *   Sacrifice this creature: Target player discards two cards. Activate only as a sorcery.
 */
class BrainWeevilScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = cardRegistry.getCard("Brain Weevil")!!.script.activatedAbilities.single().id

        test("sacrificing it makes the target player discard two cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Brain Weevil")
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Forest")
                .withCardInHand(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val weevil = game.findPermanent("Brain Weevil")!!
            val result = game.execute(
                ActivateAbility(game.player1Id, weevil, abilityId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.isInGraveyard(1, "Brain Weevil") shouldBe true

            game.resolveStack()
            val decision = game.getPendingDecision()
            if (decision is SelectCardsDecision) {
                game.selectCards(decision.options.take(decision.minSelections)).error shouldBe null
                game.resolveStack()
            }

            game.handSize(2) shouldBe 1
            game.graveyardSize(2) shouldBe 2
        }

        test("it can only be activated at sorcery speed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Brain Weevil")
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            val weevil = game.findPermanent("Brain Weevil")!!
            game.execute(
                ActivateAbility(game.player1Id, weevil, abilityId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldNotBe null
            game.isOnBattlefield("Brain Weevil") shouldBe true
            game.handSize(2) shouldBe 2
        }
    }
}
