package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Yawgmoth Demon (ATQ #21).
 *
 * {4}{B}{B} Creature — Phyrexian Demon, 6/6, flying, first strike.
 * "At the beginning of your upkeep, you may sacrifice an artifact. If you don't, tap this
 *  creature and it deals 2 damage to you."
 *
 * Modeled as the literal "you may [action]. If you don't, [consequence]" — an optional triggered
 * ability (no target) whose body is "sacrifice an artifact" and whose `elseEffect` is the upkeep
 * tax. The upkeep trigger only fires on a real step transition into UPKEEP, so each scenario starts
 * on the opponent's turn (precombat main) and passes around to player 1's upkeep. The flow exercises
 * the no-target may/else engine path: with no artifact the sacrifice is impossible, so the may
 * prompt is skipped and the tax applies automatically; with an artifact the controller is asked
 * yes/no — yes sacrifices one (no tax), no declines (tax applies).
 */
class YawgmothDemonScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun atPlayer1Upkeep(artifacts: List<String>): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Yawgmoth Demon", summoningSickness = false)
            .withLifeTotal(1, 20)
            // Start on the opponent's turn so we transition into player 1's upkeep.
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (artifact in artifacts) builder = builder.withCardOnBattlefield(1, artifact)
        // Library fuel so neither player decks out during step advances.
        repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        context("Yawgmoth Demon upkeep tax") {

            test("with no artifact the may is infeasible: no prompt, the tax applies (tap + 2 to controller)") {
                val game = atPlayer1Upkeep(artifacts = emptyList())
                val demonId = game.findPermanent("Yawgmoth Demon")!!

                withClue("No artifact to sacrifice → the may is skipped, no decision offered") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("The tax taps Yawgmoth Demon") {
                    isTapped(game, demonId) shouldBe true
                }
                withClue("The tax deals 2 damage to its controller (20 → 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("choosing to sacrifice the only artifact avoids the tax (auto-sacrificed)") {
                val game = atPlayer1Upkeep(artifacts = listOf("Ornithopter"))
                val demonId = game.findPermanent("Yawgmoth Demon")!!

                withClue("With an artifact, the ability asks the may question") {
                    game.hasPendingDecision() shouldBe true
                }
                // Say yes — with a single artifact it is sacrificed without a further "which?" prompt.
                game.answerYesNo(true)
                game.resolveStack()

                withClue("The sacrificed artifact should be gone from the battlefield") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                }
                withClue("Sacrificing spares Yawgmoth Demon — it is NOT tapped") {
                    isTapped(game, demonId) shouldBe false
                }
                withClue("Sacrificing spares the controller — no life loss (stays at 20)") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("with multiple artifacts, saying yes lets the controller choose which to sacrifice") {
                val game = atPlayer1Upkeep(artifacts = listOf("Ornithopter", "Su-Chi"))
                val demonId = game.findPermanent("Yawgmoth Demon")!!

                withClue("The ability asks the may question") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                // Now choose which single artifact to sacrifice.
                val ornithopterId = game.findPermanent("Ornithopter")!!
                game.selectCards(listOf(ornithopterId))
                game.resolveStack()

                withClue("Only the chosen artifact is sacrificed") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                    game.isOnBattlefield("Su-Chi") shouldBe true
                }
                withClue("Sacrificing spares Yawgmoth Demon — it is NOT tapped") {
                    isTapped(game, demonId) shouldBe false
                }
                withClue("Sacrificing spares the controller — no life loss (stays at 20)") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("declining the may with an artifact available triggers the tax") {
                val game = atPlayer1Upkeep(artifacts = listOf("Ornithopter"))
                val demonId = game.findPermanent("Yawgmoth Demon")!!

                withClue("With an artifact, the ability asks the may question") {
                    game.hasPendingDecision() shouldBe true
                }
                // Say no — decline the optional sacrifice.
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Declining keeps the artifact on the battlefield") {
                    game.isOnBattlefield("Ornithopter") shouldBe true
                }
                withClue("Declining the sacrifice taps Yawgmoth Demon") {
                    isTapped(game, demonId) shouldBe true
                }
                withClue("Declining the sacrifice deals 2 damage to its controller (20 → 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }
        }
    }
}
