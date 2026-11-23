package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bioshift (GTC #214) — "Move any number of +1/+1 counters from target creature onto another
 * target creature with the same controller."
 *
 * The pair must share a controller when chosen, the number is chosen on resolution (capped at the
 * source's +1/+1 counters), and — per the ruling — nothing moves if the two creatures are
 * controlled by different players by the time Bioshift resolves.
 */
class BioshiftScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.stamp(id: EntityId, counters: Map<CounterType, Int>) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent(counters)) }
    }

    private fun TestGame.castBioshift(source: EntityId, destination: EntityId) =
        execute(
            CastSpell(
                player1Id,
                state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Bioshift" },
                listOf(ChosenTarget.Permanent(source), ChosenTarget.Permanent(destination))
            )
        )

    init {
        context("Bioshift") {

            fun board(): Triple<TestGame, EntityId, EntityId> {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bioshift")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.stamp(bears, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3))
                return Triple(game, bears, giant)
            }

            test("moves the chosen number of +1/+1 counters onto the other creature") {
                val (game, bears, giant) = board()

                game.castBioshift(bears, giant).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseNumberDecision>()
                withClue("capped at the +1/+1 counters on the source") { decision.maxValue shouldBe 3 }
                game.chooseNumber(2).error shouldBe null
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                game.plusOnes(bears) shouldBe 1
                game.plusOnes(giant) shouldBe 2
            }

            test("can't target two creatures with different controllers") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bioshift")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castBioshift(bears, giant).error shouldNotBe null
            }

            test("moves nothing if the creatures have different controllers on resolution") {
                val (game, bears, giant) = board()

                game.castBioshift(bears, giant).error shouldBe null
                // The destination changes hands while Bioshift is on the stack.
                game.state = game.state.updateEntity(giant) { c -> c.with(ControllerComponent(game.player2Id)) }
                game.resolveStack()

                withClue("no number is asked for and no counters move") {
                    (game.getPendingDecision() is ChooseNumberDecision) shouldBe false
                }
                game.plusOnes(bears) shouldBe 3
                game.plusOnes(giant) shouldBe 0
            }
        }
    }
}
