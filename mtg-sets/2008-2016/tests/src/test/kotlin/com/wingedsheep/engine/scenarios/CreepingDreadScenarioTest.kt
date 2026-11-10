package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Creeping Dread (SOI #104).
 *
 *   At the beginning of your upkeep, each player discards a card. Each opponent who discarded a
 *   card that shares a card type with the card you discarded loses 3 life. (Players reveal the
 *   discarded cards simultaneously.)
 *
 * What the script has to get right, and the snapshot can't show: every player chooses before any
 * card moves (the controller's pick is still in hand while the opponent chooses), the comparison
 * reads the controller's own discard out of the shared aggregate, and an empty-handed controller
 * makes nobody lose life.
 */
class CreepingDreadScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    /** Pass from player 2's end step into player 1's upkeep; the trigger resolves until it asks. */
    private fun TestGame.toDreadUpkeep() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    /** Answer the pending discard choice for [player] by picking the card named [name]. */
    private fun TestGame.discardNamed(player: Int, name: String) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe if (player == 1) player1Id else player2Id
        selectCards(listOf(decision.options.first { nameOf(it) == name }))
    }

    init {
        context("Creeping Dread") {

            fun board(yourHand: List<String>, theirHand: List<String>): TestGame {
                var builder = scenario()
                    .withPlayers("Dread", "Opponent")
                    .withCardOnBattlefield(1, "Creeping Dread")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                yourHand.forEach { builder = builder.withCardInHand(1, it) }
                theirHand.forEach { builder = builder.withCardInHand(2, it) }
                return builder.build()
            }

            test("both choose before anything is discarded; a shared card type costs the opponent 3 life") {
                val game = board(listOf("Grizzly Bears", "Swamp"), listOf("Grizzly Bears", "Forest"))
                game.toDreadUpkeep()

                game.discardNamed(1, "Grizzly Bears")
                withClue("the controller's pick stays hidden in hand until every player has chosen") {
                    game.state.getHand(game.player1Id).map { game.nameOf(it) } shouldContain "Grizzly Bears"
                    game.graveyardSize(1) shouldBe 0
                }
                game.discardNamed(2, "Grizzly Bears")
                game.resolveStack()

                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 1
                withClue("creature shares creature: the opponent loses 3, the controller nothing") {
                    game.getLifeTotal(2) shouldBe 17
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("an opponent who discarded a card of a different type loses no life") {
                val game = board(listOf("Grizzly Bears", "Swamp"), listOf("Grizzly Bears", "Forest"))
                game.toDreadUpkeep()

                game.discardNamed(1, "Grizzly Bears")
                game.discardNamed(2, "Forest")
                game.resolveStack()

                game.findCardsInGraveyard(2, "Forest").size shouldBe 1
                game.getLifeTotal(2) shouldBe 20
            }

            test("if you discarded nothing, the opponent still discards but loses no life") {
                val game = board(emptyList(), listOf("Grizzly Bears", "Forest"))
                game.toDreadUpkeep()

                game.discardNamed(2, "Grizzly Bears")
                game.resolveStack()

                game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 1
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
