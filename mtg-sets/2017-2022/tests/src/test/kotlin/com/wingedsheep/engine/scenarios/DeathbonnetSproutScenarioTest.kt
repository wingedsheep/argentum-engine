package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deathbonnet Sprout // Deathbonnet Hulk (MID).
 *
 * Pins that the Sprout counts its graveyard *after* the upkeep mill (the milled card can be the
 * third creature), and that the Hulk's untargeted exile grows it only when the exiled card is a
 * creature card, from any graveyard.
 */
class DeathbonnetSproutScenarioTest : ScenarioTestBase() {

    /** Player 1 controls [permanent]; the game sits at the opponent's end step, one pass from our upkeep. */
    private fun builder(permanent: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, permanent)
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)

    private fun TestGame.toMyUpkeep() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    private fun TestGame.self(): EntityId = state.getBattlefield().single {
        state.getEntity(it)?.get<CardComponent>()?.name in setOf("Deathbonnet Sprout", "Deathbonnet Hulk")
    }

    private fun TestGame.name(): String = state.getEntity(self())!!.get<CardComponent>()!!.name

    private fun TestGame.plusOnes(): Int =
        state.getEntity(self())?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Deathbonnet Sprout") {
            test("the milled card counts toward three creature cards, so it transforms") {
                val game = builder("Deathbonnet Sprout")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Hill Giant")
                    .build()

                game.toMyUpkeep()

                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.name() shouldBe "Deathbonnet Hulk"
            }

            test("milling a noncreature card leaves it at two creature cards — no transform") {
                val game = builder("Deathbonnet Sprout")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Forest")
                    .build()

                game.toMyUpkeep()

                game.isInGraveyard(1, "Forest") shouldBe true
                game.name() shouldBe "Deathbonnet Sprout"
            }
        }

        context("Deathbonnet Hulk") {
            test("exiling a creature card from an opponent's graveyard puts a +1/+1 counter on it") {
                val game = builder("Deathbonnet Hulk")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Forest")
                    .build()

                game.toMyUpkeep()
                withClue("the exile is a choice, not a target") { game.state.pendingDecision shouldNotBe null }
                game.selectCards(game.findCardsInGraveyard(2, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.plusOnes() shouldBe 1
            }

            test("exiling a noncreature card adds no counter") {
                val game = builder("Deathbonnet Hulk")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Forest")
                    .build()

                game.toMyUpkeep()
                game.selectCards(game.findCardsInGraveyard(2, "Forest")).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Forest") shouldBe true
                game.plusOnes() shouldBe 0
            }

            test("it may exile nothing") {
                val game = builder("Deathbonnet Hulk")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Forest")
                    .build()

                game.toMyUpkeep()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.plusOnes() shouldBe 0
            }
        }
    }
}
