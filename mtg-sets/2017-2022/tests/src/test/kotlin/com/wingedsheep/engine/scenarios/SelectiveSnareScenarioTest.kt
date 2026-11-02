package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Selective Snare (GRN #53).
 *
 * Focus: exactly X targets that must share a creature type — two Bears bounce together, a Bear
 * and a Goblin can't be targeted together, and fewer targets than X is rejected.
 */
class SelectiveSnareScenarioTest : ScenarioTestBase() {

    init {
        context("Selective Snare") {

            fun board(): TestGame = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Selective Snare")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun cast(game: TestGame, x: Int, targets: List<EntityId>) =
                game.execute(
                    CastSpell(
                        game.player1Id,
                        game.state.getHand(game.player1Id).first {
                            game.state.getEntity(it)?.get<CardComponent>()?.name == "Selective Snare"
                        },
                        targets.map { ChosenTarget.Permanent(it) },
                        x
                    )
                )

            test("returns X creatures that share a creature type to their owner's hand") {
                val game = board()
                val result = cast(game, 2, game.findPermanents("Grizzly Bears"))
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.findPermanents("Grizzly Bears").size shouldBe 0
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Raging Goblin") shouldBe true
            }

            test("creatures that share no creature type can't be targeted together") {
                val game = board()
                val targets = listOf(game.findPermanents("Grizzly Bears").first(), game.findPermanent("Raging Goblin")!!)
                cast(game, 2, targets).error shouldNotBe null
            }

            test("fewer targets than X is rejected") {
                val game = board()
                cast(game, 2, listOf(game.findPermanents("Grizzly Bears").first())).error shouldNotBe null
            }
        }
    }
}
