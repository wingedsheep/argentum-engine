package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gruff Triplets (WOE #172) — {3}{G}{G}{G} 3/3 Satyr Warrior with trample.
 * "When this creature enters, if it isn't a token, create two tokens that are copies of it."
 * "When this creature dies, put a number of +1/+1 counters equal to its power on each creature
 *  you control named Gruff Triplets."
 *
 * Two things worth pinning. The "if it isn't a token" intervening-if is the only thing standing
 * between this card and an unbounded token loop, so the copy count is asserted exactly. And the
 * death trigger reads "its power" off a permanent that is already in the graveyard — a last-known
 * information read — which must find 3, not 0, and must land on the *surviving* siblings rather
 * than on the dying body.
 */
class GruffTripletsScenarioTest : ScenarioTestBase() {

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Gruff Triplets")
            .withCardInHand(1, "Doom Blade")
            // Six for the Triplets ({3}{G}{G}{G}) plus slack so Doom Blade ({1}{B}) is still
            // payable whichever lands auto-pay taps first.
            .withLandsOnBattlefield(1, "Forest", 6)
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.isToken(id: EntityId): Boolean =
            state.getEntity(id)?.get<TokenComponent>() != null

        fun TestGame.plusOneCounters(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun TestGame.resolveTriplets() {
            withClue("cast should succeed") { castSpell(1, "Gruff Triplets").error shouldBe null }
            resolveStack()
            // The enters trigger goes on the stack after the creature resolves.
            resolveStack()
        }

        test("entering as a nontoken creates exactly two token copies, and they do not multiply") {
            val game = board()
            game.resolveTriplets()

            val triplets = game.findAllPermanents("Gruff Triplets")
            withClue("one printed body plus two token copies — no runaway") {
                triplets.size shouldBe 3
            }
            withClue("the tokens' own enters trigger is switched off by \"if it isn't a token\"") {
                triplets.count { game.isToken(it) } shouldBe 2
            }
        }

        test("dying hands +1/+1 counters equal to its last-known power to each surviving sibling") {
            val game = board()
            game.resolveTriplets()

            val triplets = game.findAllPermanents("Gruff Triplets")
            val doomed = triplets.first { game.isToken(it) }
            val survivors = triplets.filter { it != doomed }

            withClue("nobody starts with counters") {
                survivors.map { game.plusOneCounters(it) } shouldBe listOf(0, 0)
            }

            withClue("removal should be castable") {
                game.castSpell(1, "Doom Blade", doomed).error shouldBe null
            }
            game.resolveStack()
            // The dies trigger goes on the stack once the Triplet hits the graveyard.
            game.resolveStack()

            withClue("the bolted Triplet died") {
                game.state.getBattlefield().contains(doomed) shouldBe false
            }
            withClue("each survivor got counters equal to the dying body's power (3), not 0") {
                survivors.forEach { game.plusOneCounters(it) shouldBe 3 }
            }
            withClue("the two survivors are now 6/6") {
                val projected = game.state.projectedState
                survivors.forEach {
                    projected.getPower(it) shouldBe 6
                    projected.getToughness(it) shouldBe 6
                }
            }
        }
    }
}
