package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thieving Skydiver — {1}{U} 2/1 flier. "Kicker {X}. X can't be 0. When this creature enters, if it
 * was kicked, gain control of target artifact with mana value X or less. If that artifact is an
 * Equipment, attach it to this creature."
 *
 * The kicked cast's X floor is `OptionalAdditionalCost.minimumXValue`: the kicked offer starts its
 * X choice at 1 and a kicked cast announcing X = 0 is rejected.
 */
class ThievingSkydiverScenarioTest : ScenarioTestBase() {

    private fun board(islands: Int, vararg opponentArtifacts: String): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Thieving Skydiver")
            .withLandsOnBattlefield(1, "Island", islands)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponentArtifacts.forEach { builder = builder.withCardOnBattlefield(2, it) }
        return builder.build()
    }

    private fun TestGame.skydiverInHand() = state.getHand(player1Id).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Thieving Skydiver"
    }

    private fun TestGame.castKicked(x: Int) = execute(
        CastSpell(player1Id, skydiverInHand(), xValue = x, declaredCostSlot = ChoiceSlot.KICKED)
    )

    init {
        test("the kicked offer floors X at 1") {
            val game = board(islands = 4, "Bonesplitter")
            val offer = game.getLegalActions(1).single {
                it.actionType == "CastWithKicker" && (it.action as CastSpell).cardId == game.skydiverInHand()
            }
            offer.isAffordable shouldBe true
            offer.hasXCost shouldBe true
            offer.minX shouldBe 1
            offer.maxAffordableX shouldBe 2
        }

        test("a kicked cast with X = 0 is rejected") {
            val game = board(islands = 2, "Bonesplitter")
            game.castKicked(x = 0).error shouldNotBe null
        }

        test("the kicked offer is unaffordable when only X = 0 could be paid") {
            val game = board(islands = 2, "Bonesplitter")
            val offer = game.getLegalActions(1).single {
                it.actionType == "CastWithKicker" && (it.action as CastSpell).cardId == game.skydiverInHand()
            }
            offer.isAffordable shouldBe false
        }

        test("kicked for X = 1 steals an Equipment and attaches it to the Skydiver") {
            val game = board(islands = 3, "Bonesplitter")
            val bonesplitter = game.findPermanent("Bonesplitter")!!

            game.castKicked(x = 1).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bonesplitter)).error shouldBe null
            game.resolveStack()

            val skydiver = game.findPermanent("Thieving Skydiver")!!
            game.state.projectedState.getController(bonesplitter) shouldBe game.player1Id
            withClue("Bonesplitter is attached to the Skydiver") {
                game.state.getEntity(bonesplitter)?.get<AttachedToComponent>()?.targetId shouldBe skydiver
            }
        }

        test("an artifact with mana value above X can't be targeted") {
            val game = board(islands = 3, "Bonesplitter", "Millstone")
            val millstone = game.findPermanent("Millstone")!!

            game.castKicked(x = 1).error shouldBe null
            game.resolveStack()
            withClue("Millstone (MV 2) is out of reach at X = 1") {
                game.selectTargets(listOf(millstone)).error shouldNotBe null
            }
        }

        test("a non-Equipment artifact is stolen but not attached") {
            val game = board(islands = 4, "Millstone")
            val millstone = game.findPermanent("Millstone")!!

            game.castKicked(x = 2).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(millstone)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getController(millstone) shouldBe game.player1Id
            game.state.getEntity(millstone)?.get<AttachedToComponent>() shouldBe null
        }

        test("unkicked, nothing is stolen") {
            val game = board(islands = 2, "Bonesplitter")
            val bonesplitter = game.findPermanent("Bonesplitter")!!
            game.execute(CastSpell(game.player1Id, game.skydiverInHand())).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.state.projectedState.getController(bonesplitter) shouldBe game.player2Id
        }
    }
}
