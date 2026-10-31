package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario coverage for Into the Story (ELD #50).
 *
 * {5}{U}{U} Instant
 * "This spell costs {3} less to cast if an opponent has seven or more cards in their graveyard.
 *  Draw four cards."
 *
 * The gate reads the greatest graveyard among the caster's *opponents* (rebinding "your graveyard"
 * per opponent), so the caster's own graveyard never counts, and the reduction is a flat {3}
 * however full the opponent's graveyard is.
 */
class IntoTheStoryScenarioTest : ScenarioTestBase() {

    private val costCalculator by lazy { CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator) }

    init {
        fun board(opponentGraveyard: Int, ownGraveyard: Int = 0, islands: Int = 7) = run {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Into the Story")
                .withLandsOnBattlefield(1, "Island", islands)
            repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
            repeat(opponentGraveyard) { builder = builder.withCardInGraveyard(2, "Grizzly Bears") }
            repeat(ownGraveyard) { builder = builder.withCardInGraveyard(1, "Grizzly Bears") }
            builder.inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        }

        fun TestGame.genericCost(): Int = costCalculator.calculateEffectiveCost(
            state,
            cardRegistry.requireCard("Into the Story"),
            player1Id
        ).genericAmount

        context("Into the Story") {

            test("costs full {5}{U}{U} while the opponent has six cards in their graveyard") {
                board(opponentGraveyard = 6).genericCost() shouldBe 5
            }

            test("costs {3} less once the opponent has seven cards in their graveyard") {
                board(opponentGraveyard = 7).genericCost() shouldBe 2
            }

            test("the reduction stays {3} with fourteen cards in the opponent's graveyard") {
                board(opponentGraveyard = 14).genericCost() shouldBe 2
            }

            test("the caster's own graveyard does not count") {
                board(opponentGraveyard = 0, ownGraveyard = 10).genericCost() shouldBe 5
            }

            test("cast for {2}{U}{U} off four Islands and draw four cards") {
                val game = board(opponentGraveyard = 7, islands = 4)
                val handBefore = game.handSize(1)

                game.castSpell(1, "Into the Story").error shouldBe null
                game.resolveStack()

                withClue("Into the Story left hand, four cards came in: net +3") {
                    game.handSize(1) shouldBe handBefore + 3
                }
            }

            test("four Islands are not enough without the reduction") {
                val game = board(opponentGraveyard = 6, islands = 4)
                game.castSpell(1, "Into the Story").error shouldNotBe null
            }
        }
    }
}
