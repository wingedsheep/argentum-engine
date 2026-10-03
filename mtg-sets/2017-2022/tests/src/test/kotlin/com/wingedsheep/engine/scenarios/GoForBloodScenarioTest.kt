package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Go for Blood (IKO) — target creature you control fights target creature you
 * don't control; cycling {1}.
 */
class GoForBloodScenarioTest : ScenarioTestBase() {

    private fun TestGame.castGoForBlood(first: EntityId, second: EntityId) = execute(
        CastSpell(
            player1Id,
            findCardsInHand(1, "Go for Blood").first(),
            listOf(ChosenTarget.Permanent(first), ChosenTarget.Permanent(second))
        )
    )

    private fun setup(): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Go for Blood")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
            .withCardOnBattlefield(1, "Wall of Wood", summoningSickness = false)
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
        repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    init {
        context("Go for Blood") {
            test("your creature fights the opponent's creature") {
                val game = setup()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castGoForBlood(giant, bears).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (3/3) deals 3 to the 2/2 Bears, which dies") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("Bears deal 2 back, which the 3/3 Giant survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("the second target must be a creature you don't control") {
                val game = setup()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Wood").shouldNotBeNull()

                withClue("your own Wall of Wood is not a legal second target") {
                    game.castGoForBlood(giant, wall).error shouldNotBe null
                }
            }

            test("cycling for {1} discards it and draws a card") {
                val game = setup()
                val handBefore = game.handSize(1)

                game.cycleCard(1, "Go for Blood").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Go for Blood") shouldBe true
                game.handSize(1) shouldBe handBefore
            }
        }
    }
}
