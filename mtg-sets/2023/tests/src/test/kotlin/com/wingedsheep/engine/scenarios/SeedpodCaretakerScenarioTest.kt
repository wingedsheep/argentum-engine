package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Seedpod Caretaker (MOM #325) — "When this creature enters, choose one — • Put a +1/+1 counter
 * on target artifact or creature you control. • Transform target Incubator token you control."
 */
class SeedpodCaretakerScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun controllerOf(game: TestGame, id: EntityId): EntityId? =
        game.state.getEntity(id)?.get<ControllerComponent>()?.playerId

    /** Six Plains: three for a Progenitor Exarch at X = 1 (one Incubator), three for the Caretaker. */
    private fun boardWithIncubator(): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Progenitor Exarch")
            .withCardInHand(1, "Seedpod Caretaker")
            .withLandsOnBattlefield(1, "Plains", 6)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castXSpell(1, "Progenitor Exarch", xValue = 1).error shouldBe null
        game.resolveStack()
        val incubator = game.findPermanent("Incubator") ?: error("expected an Incubator token")
        return game to incubator
    }

    private fun castCaretakerAndChooseMode(game: TestGame, mode: Int): ChooseTargetsDecision {
        game.castSpell(1, "Seedpod Caretaker").error shouldBe null
        game.resolveStack()
        val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
            ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
        game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = mode))
        return game.state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected a ChooseTargetsDecision; got ${game.state.pendingDecision}")
    }

    init {
        context("Seedpod Caretaker") {

            test("counter mode puts a +1/+1 counter on target creature you control, not an opponent's") {
                val (game, _) = boardWithIncubator()
                val myBears = game.findPermanents("Grizzly Bears").first { controllerOf(game, it) == game.player1Id }
                val theirBears = game.findPermanents("Grizzly Bears").first { controllerOf(game, it) == game.player2Id }

                val decision = castCaretakerAndChooseMode(game, 0)
                val legal = decision.legalTargets[0].orEmpty()
                withClue("opponent's creature is not a legal target") { legal shouldNotContain theirBears }
                withClue("the Incubator is an artifact you control") {
                    legal shouldContain game.findPermanent("Incubator")!!
                }

                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(myBears))))
                game.resolveStack()

                plusOnes(game, myBears) shouldBe 1
            }

            test("counter mode can target a noncreature artifact you control (the Incubator)") {
                val (game, incubator) = boardWithIncubator()
                plusOnes(game, incubator) shouldBe 3

                val decision = castCaretakerAndChooseMode(game, 0)
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(incubator))))
                game.resolveStack()

                plusOnes(game, incubator) shouldBe 4
                withClue("still an untransformed Incubator") { game.findPermanent("Incubator") shouldBe incubator }
            }

            test("transform mode transforms target Incubator token you control") {
                val (game, incubator) = boardWithIncubator()

                val decision = castCaretakerAndChooseMode(game, 1)
                withClue("only the Incubator is a legal target") {
                    decision.legalTargets[0].orEmpty() shouldBe listOf(incubator)
                }
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(incubator))))
                game.resolveStack()

                withClue("the Incubator flipped to its Phyrexian face, keeping its three counters") {
                    game.findPermanent("Incubator") shouldBe null
                    game.findPermanent("Phyrexian") shouldBe incubator
                    plusOnes(game, incubator) shouldBe 3
                }
            }
        }
    }
}
