package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bred for the Hunt (DGM #59) — {1}{G}{U} Enchantment.
 *
 *   Whenever a creature you control with a +1/+1 counter on it deals combat damage to a player,
 *   you may draw a card.
 */
class BredForTheHuntScenarioTest : ScenarioTestBase() {

    private fun seedCounter(game: TestGame, id: EntityId) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        }
    }

    private fun attackUnblocked(game: TestGame) {
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        game.resolveStack()
    }

    init {
        test("a creature with a +1/+1 counter connecting lets you draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bred for the Hunt")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            seedCounter(game, game.findPermanent("Grizzly Bears")!!)

            attackUnblocked(game)
            withClue("the \"you may\" prompt is offered") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.handSize(1) shouldBe 1
        }

        test("a creature without a +1/+1 counter doesn't trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bred for the Hunt")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            attackUnblocked(game)

            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe 0
        }
    }
}
