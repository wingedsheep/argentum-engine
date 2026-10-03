package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * It That Heralds the End (MH3 #9).
 *
 * Colorless spells you cast with mana value 7 or greater cost {1} less; other colorless creatures
 * you control get +1/+1.
 */
class ItThatHeraldsTheEndScenarioTest : ScenarioTestBase() {

    private fun costOf(game: TestGame, cardName: String, player: EntityId = game.player1Id): Int {
        val calculator = CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator)
        return calculator.calculateEffectiveCost(game.state, cardRegistry.requireCard(cardName), player).cmc
    }

    private fun board(): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "It That Heralds the End")
        .withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Ornithopter")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("colorless spells with mana value 7 or greater cost {1} less, others don't") {
            val game = board()
            costOf(game, "Artisan of Kozilek") shouldBe 8 // {9}
            costOf(game, "Darksteel Colossus") shouldBe 10 // {11}
            withClue("colorless but mana value below 7") { costOf(game, "Dragon Engine") shouldBe 3 }
            withClue("mana value 8 but colored") { costOf(game, "Scion of Darkness") shouldBe 8 }
            withClue("the opponent's spells aren't discounted") {
                costOf(game, "Artisan of Kozilek", game.player2Id) shouldBe 9
            }
        }

        test("other colorless creatures you control get +1/+1") {
            val game = board()
            val projected = game.state.projectedState
            val herald = game.findPermanent("It That Heralds the End")!!
            val myThopter = game.findPermanents("Ornithopter").single {
                game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id
            }
            val theirThopter = game.findPermanents("Ornithopter").single { it != myThopter }
            val bears = game.findPermanent("Grizzly Bears")!!

            projected.getPower(myThopter) shouldBe 1
            projected.getToughness(myThopter) shouldBe 3
            withClue("not itself") { projected.getPower(herald) shouldBe 2 }
            withClue("colored creature untouched") { projected.getPower(bears) shouldBe 2 }
            withClue("opponent's colorless creature untouched") { projected.getPower(theirThopter) shouldBe 0 }
        }
    }
}
