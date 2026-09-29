package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Storm the Seedcore (MOM #206) — "Distribute four +1/+1 counters among up to four target
 * creatures you control. Creatures you control gain vigilance and trample until end of turn."
 */
class StormTheSeedcoreScenarioTest : ScenarioTestBase() {

    init {
        context("Storm the Seedcore") {

            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Storm the Seedcore")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.plusOnes(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            fun TestGame.cast(targets: List<EntityId>) = execute(
                CastSpell(
                    player1Id,
                    findCardsInHand(1, "Storm the Seedcore").single(),
                    targets.map { ChosenTarget.Permanent(it) }
                )
            )

            test("controller splits four counters unevenly and all its creatures gain vigilance and trample") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val myGiant = game.findPermanents("Hill Giant").first { game.state.projectedState.getController(it) == game.player1Id }
                val theirGiant = game.findPermanents("Hill Giant").first { it != myGiant }

                game.cast(listOf(bears, elves)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
                decision.totalAmount shouldBe 4
                decision.minPerTarget shouldBe 1
                game.submitDecision(DistributionResponse(decision.id, mapOf(bears to 3, elves to 1))).error shouldBe null
                game.resolveStack()

                withClue("the chosen uneven split is honoured") {
                    game.plusOnes(bears) shouldBe 3
                    game.plusOnes(elves) shouldBe 1
                    game.plusOnes(myGiant) shouldBe 0
                }
                val projected = game.state.projectedState
                for (id in listOf(bears, elves, myGiant)) {
                    projected.hasKeyword(id, Keyword.VIGILANCE) shouldBe true
                    projected.hasKeyword(id, Keyword.TRAMPLE) shouldBe true
                }
                withClue("opponent's creature is unaffected") {
                    projected.hasKeyword(theirGiant, Keyword.VIGILANCE) shouldBe false
                    projected.hasKeyword(theirGiant, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("a single target takes all four counters") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.cast(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.plusOnes(bears) shouldBe 4
            }

            test("zero targets still grants vigilance and trample") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.cast(emptyList()).error shouldBe null
                game.resolveStack()
                game.plusOnes(bears) shouldBe 0
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }

            test("cannot target a creature an opponent controls") {
                val game = board()
                val theirGiant = game.findPermanents("Hill Giant").first { game.state.projectedState.getController(it) != game.player1Id }
                game.cast(listOf(theirGiant)).error shouldNotBe null
            }
        }
    }
}
