package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario coverage for Rust Elemental (MRD #234).
 *
 * {4} Artifact Creature — Elemental 4/4
 * "Flying
 *  At the beginning of your upkeep, sacrifice another artifact. If you can't, tap this creature
 *  and you lose 4 life."
 *
 * The claims pinned down here are the ones the "if you can't" clause turns on:
 *  - with another artifact around, the sacrifice happens and no life is lost;
 *  - Rust Elemental never counts as its own fodder, even though it *is* an artifact — a lone
 *    Rust Elemental takes the penalty instead of eating itself;
 *  - with several artifacts out the controller chooses which one goes;
 *  - the penalty branch fires on the controller's upkeep only, not on the opponent's.
 */
class RustElementalScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.isTapped(id: EntityId): Boolean =
            state.getEntity(id)?.has<TappedComponent>() == true

        /** Board with Rust Elemental on player 1's side, parked at the opponent's end step. */
        fun board(vararg otherArtifacts: String): TestGame {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withLifeTotal(1, 20)
                .withCardOnBattlefield(1, "Rust Elemental")
            otherArtifacts.forEach { builder = builder.withCardOnBattlefield(1, it) }
            return builder
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()
        }

        context("Rust Elemental") {

            test("sacrifices the only other artifact and pays nothing else") {
                val game = board("Bonesplitter")
                val rust = game.findPermanent("Rust Elemental")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("exactly one legal choice, so it is sacrificed without a prompt") {
                    game.findPermanent("Bonesplitter") shouldBe null
                    game.findCardsInGraveyard(1, "Bonesplitter").size shouldBe 1
                }
                withClue("the sacrifice succeeded, so the penalty branch never runs") {
                    game.getLifeTotal(1) shouldBe 20
                    game.isTapped(rust) shouldBe false
                    game.findPermanent("Rust Elemental") shouldNotBe null
                }
            }

            test("alone on the battlefield it taps itself and drains 4 — it is not its own fodder") {
                val game = board()
                val rust = game.findPermanent("Rust Elemental")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Rust Elemental is an artifact, but 'another' excludes it") {
                    game.findPermanent("Rust Elemental") shouldNotBe null
                    game.isTapped(rust) shouldBe true
                    game.getLifeTotal(1) shouldBe 16
                }
            }

            test("with two other artifacts the controller picks which one to sacrifice") {
                val game = board("Bonesplitter", "Leonin Scimitar")
                val rust = game.findPermanent("Rust Elemental")!!
                val scimitar = game.findPermanent("Leonin Scimitar")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("both other artifacts are offered; Rust Elemental itself is not") {
                    decision.options shouldContain scimitar
                    decision.options shouldContain game.findPermanent("Bonesplitter")!!
                    decision.options.contains(rust) shouldBe false
                }

                game.selectCards(listOf(scimitar))

                withClue("the chosen artifact dies and the penalty branch is skipped") {
                    game.findPermanent("Leonin Scimitar") shouldBe null
                    game.findPermanent("Bonesplitter") shouldNotBe null
                    game.getLifeTotal(1) shouldBe 20
                    game.isTapped(rust) shouldBe false
                }
            }

            test("does nothing on the opponent's upkeep") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withLifeTotal(1, 20)
                    .withCardOnBattlefield(1, "Rust Elemental")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                val rust = game.findPermanent("Rust Elemental")!!

                // Passing from player 1's end step lands in player 2's upkeep.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("'your upkeep' is the controller's upkeep only") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.isTapped(rust) shouldBe false
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
