package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Suppression Ray (MH3) — "Tap all creatures target player controls. You may pay any amount of
 * {E}. If you do, choose up to that many creatures tapped this way. Put a stun counter on each of
 * them."
 *
 * Pins the 2024-06-07 ruling: creatures already tapped before the spell resolved were not
 * "tapped this way" and can't be chosen for a stun counter.
 */
class SuppressionRayScenarioTest : ScenarioTestBase() {

    private fun energy(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun stun(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun tapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    private fun giveEnergy(game: TestGame, n: Int) {
        game.state = game.state.updateEntity(game.player1Id) {
            it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
        }
    }

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Suppression Ray")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withCardOnBattlefield(2, "Hill Giant", tapped = true)
        .withCardOnBattlefield(1, "Savannah Lions")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Suppression Ray") {

            test("taps the target player's creatures; paid energy caps the stun choice to newly tapped ones") {
                val game = board()
                giveEnergy(game, 3)
                val bears = game.findPermanent("Grizzly Bears")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val giant = game.findPermanent("Hill Giant")!!
                val lions = game.findPermanent("Savannah Lions")!!

                game.castSpellTargetingPlayer(1, "Suppression Ray", 2).error shouldBe null
                game.resolveStack()

                withClue("all of the target player's creatures are tapped, none of the caster's") {
                    tapped(game, bears) shouldBe true
                    tapped(game, wurm) shouldBe true
                    tapped(game, giant) shouldBe true
                    tapped(game, lions) shouldBe false
                }

                val pay = game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                pay.minValue shouldBe 0
                pay.maxValue shouldBe 3
                game.chooseNumber(1).error shouldBe null

                val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the already-tapped Hill Giant wasn't tapped this way") {
                    pick.options shouldContainExactlyInAnyOrder listOf(bears, wurm)
                    pick.maxSelections shouldBe 1
                }
                game.selectCards(listOf(wurm)).error shouldBe null

                stun(game, wurm) shouldBe 1
                stun(game, bears) shouldBe 0
                stun(game, giant) shouldBe 0
                energy(game, game.player1Id) shouldBe 2
            }

            test("paying two stuns both newly tapped creatures") {
                val game = board()
                giveEnergy(game, 5)
                val bears = game.findPermanent("Grizzly Bears")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castSpellTargetingPlayer(1, "Suppression Ray", 2).error shouldBe null
                game.resolveStack()
                game.chooseNumber(2).error shouldBe null
                game.selectCards(listOf(bears, wurm)).error shouldBe null

                stun(game, bears) shouldBe 1
                stun(game, wurm) shouldBe 1
                energy(game, game.player1Id) shouldBe 3
            }

            test("paying zero taps but stuns nothing") {
                val game = board()
                giveEnergy(game, 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castSpellTargetingPlayer(1, "Suppression Ray", 2).error shouldBe null
                game.resolveStack()
                game.chooseNumber(0).error shouldBe null

                game.getPendingDecision() shouldBe null
                tapped(game, bears) shouldBe true
                stun(game, bears) shouldBe 0
                stun(game, wurm) shouldBe 0
                energy(game, game.player1Id) shouldBe 2
            }

            test("with no energy it just taps") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingPlayer(1, "Suppression Ray", 2).error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                tapped(game, bears) shouldBe true
                stun(game, bears) shouldBe 0
            }
        }
    }
}
