package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Show of Confidence (STX #28) — "When you cast this spell, copy it for each other instant and
 * sorcery spell you've cast this turn. You may choose new targets for the copies. Put a +1/+1
 * counter on target creature. It gains vigilance until end of turn."
 *
 * The copy count comes from the turn's cast history minus Show of Confidence's own record, read
 * by its cast trigger.
 */
class ShowOfConfidenceScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Show of Confidence")
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Show of Confidence") {

            test("one earlier instant this turn makes one copy, which may take a new target") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Show of Confidence", bears).error shouldBe null
                withClue("the spell plus its cast trigger") {
                    game.state.stack.size shouldBe 2
                }
                game.resolveStack()

                withClue("one copy is made, and its controller may retarget it") {
                    game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.plusOneCounters(bears) shouldBe 1
                game.plusOneCounters(giant) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                game.state.projectedState.hasKeyword(giant, Keyword.VIGILANCE) shouldBe true
            }

            test("cast as the first spell of the turn, it makes no copies") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Show of Confidence", bears).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.state.stack.size shouldBe 0
                game.plusOneCounters(bears) shouldBe 1
                game.plusOneCounters(giant) shouldBe 0
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }

            test("an instant cast in response to its cast trigger also counts") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Show of Confidence")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Show of Confidence", bears).error shouldBe null
                withClue("the spell plus its cast trigger") {
                    game.state.stack.size shouldBe 2
                }
                game.castSpell(1, "Giant Growth", giant).error shouldBe null
                game.resolveStack()

                withClue("the instant cast in response is counted when the trigger resolves") {
                    game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.plusOneCounters(bears) shouldBe 1
                game.plusOneCounters(giant) shouldBe 1
            }
        }
    }
}
