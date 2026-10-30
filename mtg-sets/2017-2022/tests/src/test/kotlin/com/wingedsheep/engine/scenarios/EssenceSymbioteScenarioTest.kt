package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Essence Symbiote (IKO #149) — {1}{G} Creature — Beast 2/2.
 *
 *   Whenever a creature you control mutates, put a +1/+1 counter on that creature and you gain 2 life.
 *
 * Porcuparrot (no mutate trigger of its own) mutates under a creature, so the merged permanent keeps
 * the host's 2/2 and the counter is easy to read. Covers another creature mutating, the Symbiote
 * itself being mutated onto, an opponent's creature mutating (no trigger) and a normal cast (no mutate).
 */
class EssenceSymbioteScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.mutateUnder(playerNumber: Int, host: EntityId) {
        val offer = getLegalActions(playerNumber).single {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE
        }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(host)))).error shouldBe null
        resolveStack()
        answerYesNo(false)
        resolveStack()
    }

    private fun board(activePlayer: Int = 1, porcuparrotOwner: Int = 1) = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(1, "Essence Symbiote")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(porcuparrotOwner, "Porcuparrot")
        .withLandsOnBattlefield(porcuparrotOwner, "Mountain", 4)
        .withActivePlayer(activePlayer)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.bearsOf(playerId: EntityId) = findPermanents("Grizzly Bears").single {
        state.projectedState.getController(it) == playerId
    }

    init {
        context("Essence Symbiote") {

            test("another creature you control mutating gets a +1/+1 counter and you gain 2 life") {
                val game = board()
                val bears = game.bearsOf(game.player1Id)
                game.mutateUnder(1, bears)

                withClue("the merged Bears permanent gets the counter") {
                    game.plusOneCounters(bears) shouldBe 1
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
                withClue("the Symbiote itself gets nothing") {
                    game.plusOneCounters(game.findPermanent("Essence Symbiote")!!) shouldBe 0
                }
                game.getLifeTotal(1) shouldBe 22
            }

            test("mutating onto the Symbiote itself triggers it") {
                val game = board()
                val symbiote = game.findPermanent("Essence Symbiote")!!
                game.mutateUnder(1, symbiote)

                game.plusOneCounters(symbiote) shouldBe 1
                game.state.projectedState.getPower(symbiote) shouldBe 3
                game.getLifeTotal(1) shouldBe 22
            }

            test("an opponent's creature mutating doesn't trigger it") {
                val game = board(activePlayer = 2, porcuparrotOwner = 2)
                val theirBears = game.bearsOf(game.player2Id)
                game.mutateUnder(2, theirBears)

                withClue("the opponent's mutate resolved") {
                    game.state.projectedState.getPower(theirBears) shouldBe 2
                    game.isOnBattlefield("Porcuparrot") shouldBe false
                }
                game.plusOneCounters(theirBears) shouldBe 0
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("a creature cast normally doesn't mutate, so nothing triggers") {
                val game = board()
                game.castSpell(1, "Porcuparrot").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Porcuparrot") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
