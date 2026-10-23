package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Keen-Eared Sentry (AFR #22): "Each opponent can't venture into the dungeon more than once each
 * turn." An opponent's second venture of the turn simply doesn't happen; the Sentry's controller
 * ventures freely. The ventures come from two Dungeon Maps.
 */
class KeenEaredSentryScenarioTest : ScenarioTestBase() {

    private fun ventureWithMap(game: TestGame, map: EntityId) {
        val venture = cardRegistry.getCard("Dungeon Map")!!.activatedAbilities[1].id
        game.execute(ActivateAbility(game.player1Id, map, venture)).error shouldBe null
        game.resolveStack()
    }

    private fun board(sentryController: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(sentryController, "Keen-Eared Sentry")
        .withCardOnBattlefield(1, "Dungeon Map")
        .withCardOnBattlefield(1, "Dungeon Map")
        .withLandsOnBattlefield(1, "Swamp", 6)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun firstVenture(game: TestGame): EntityId {
        val (first, second) = game.findAllPermanents("Dungeon Map")
        ventureWithMap(game, first)
        val decision = game.getPendingDecision() as ChooseOptionDecision
        game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Dungeon of the Mad Mage")))
            .error shouldBe null
        game.resolveStack()
        DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
        return second
    }

    init {
        test("an opponent's second venture of the turn doesn't happen") {
            val game = board(sentryController = 2)
            val second = firstVenture(game)

            ventureWithMap(game, second)

            withClue("The marker stays in Yawning Portal and nothing triggers") {
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
                game.state.stack.size shouldBe 0
                game.getLifeTotal(1) shouldBe 21
            }
        }

        test("the Sentry's controller can venture more than once") {
            val game = board(sentryController = 1)
            val second = firstVenture(game)

            ventureWithMap(game, second)

            withClue("Yawning Portal → Dungeon Level") {
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 1
            }
        }
    }
}
