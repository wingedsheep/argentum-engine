package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reverse the Sands (CHK #41) — "Redistribute any number of players' life totals."
 *
 * Rulings: the caster picks who gets which total on resolution; totals can't be split — at 5 and 15
 * the only outcomes are "leave them" or "swap them".
 */
class ReverseTheSandsScenarioTest : ScenarioTestBase() {

    private fun setup(myLife: Int, theirLife: Int, vararg extra: String): TestGame {
        val builder = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Reverse the Sands")
            .withLandsOnBattlefield(1, "Plains", 8)
            .withLifeTotal(1, myLife)
            .withLifeTotal(2, theirLife)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        extra.forEach { builder.withCardOnBattlefield(2, it) }
        return builder.build()
    }

    private fun TestGame.castAndResolve() {
        castSpell(1, "Reverse the Sands").error shouldBe null
        resolveStack()
    }

    private fun TestGame.choose(option: String) {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe player1Id
        execute(SubmitDecision(player1Id, OptionChosenResponse(decision.id, decision.options.indexOf(option))))
            .error shouldBe null
    }

    init {
        context("Reverse the Sands") {

            test("swaps a low total for the opponent's high one") {
                val game = setup(myLife = 5, theirLife = 15)
                game.castAndResolve()

                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("only the two whole totals are offered for Alice") {
                    decision.prompt shouldBe "Choose the life total Alice gets"
                    decision.options shouldBe listOf("15 life (Bob's)", "5 life (unchanged)")
                }
                game.choose("15 life (Bob's)")

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(1) shouldBe 15
                game.getLifeTotal(2) shouldBe 5
                game.isInGraveyard(1, "Reverse the Sands") shouldBe true
            }

            test("the caster may leave the totals as they are") {
                val game = setup(myLife = 5, theirLife = 15)
                game.castAndResolve()
                game.choose("5 life (unchanged)")

                game.getLifeTotal(1) shouldBe 5
                game.getLifeTotal(2) shouldBe 15
            }

            test("with equal totals it resolves without a prompt") {
                val game = setup(myLife = 20, theirLife = 20)
                game.castAndResolve()

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("under Sulfuric Vortex nobody can be handed a higher total, so nothing changes") {
                val game = setup(myLife = 5, theirLife = 15, "Sulfuric Vortex")
                game.findPermanent("Sulfuric Vortex") shouldNotBe null
                game.castAndResolve()

                withClue("Alice can't gain life, so her only legal total is her own — no question") {
                    game.state.pendingDecision shouldBe null
                }
                game.getLifeTotal(1) shouldBe 5
                game.getLifeTotal(2) shouldBe 15
            }
        }
    }
}
