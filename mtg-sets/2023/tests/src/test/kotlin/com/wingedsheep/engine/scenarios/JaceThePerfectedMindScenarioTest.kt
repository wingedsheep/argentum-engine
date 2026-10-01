package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.matchers.shouldBe

/**
 * Jace, the Perfected Mind (ONE #57, {2}{U}{U/P}, loyalty 5).
 *
 *   +1: Until your next turn, up to one target creature gets -3/-0.
 *   −2: Target player mills three cards. Then if a graveyard has twenty or more cards in it,
 *       you draw three cards. Otherwise, you draw a card.
 *   −X: Target player mills three times X cards.
 */
class JaceThePerfectedMindScenarioTest : ScenarioTestBase() {

    init {
        val abilities = cardRegistry.getCard("Jace, the Perfected Mind")!!.script.activatedAbilities
        val plusOne = abilities.single { (it.cost as? AbilityCost.Loyalty)?.change == 1 }.id
        val minusTwo = abilities.single { (it.cost as? AbilityCost.Loyalty)?.change == -2 }.id
        val minusX = abilities.single { it.cost is AbilityCost.LoyaltyX }.id

        fun board(p1Graveyard: Int = 0, p2Graveyard: Int = 0): Pair<TestGame, EntityId> {
            var b = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Jace, the Perfected Mind")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { b = b.withCardInLibrary(1, "Island") }
            repeat(10) { b = b.withCardInLibrary(2, "Forest") }
            repeat(p1Graveyard) { b = b.withCardInGraveyard(1, "Island") }
            repeat(p2Graveyard) { b = b.withCardInGraveyard(2, "Forest") }
            val game = b.build()
            return game to game.findPermanent("Jace, the Perfected Mind")!!
        }

        fun TestGame.loyalty(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        test("+1 gives up to one target creature -3/-0") {
            val (game, jace) = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(game.player1Id, jace, plusOne, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.loyalty(jace) shouldBe 6
            game.state.projectedState.getPower(bears) shouldBe -1
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("+1's -3/-0 lasts through the opponent's turn and ends on your next turn") {
            val (game, jace) = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(game.player1Id, jace, plusOne, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.projectedState.getPower(bears) shouldBe -1

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.projectedState.getPower(bears) shouldBe 2
        }

        test("+1 may be activated with no target") {
            val (game, jace) = board()
            game.execute(ActivateAbility(game.player1Id, jace, plusOne)).error shouldBe null
            game.resolveStack()
            game.loyalty(jace) shouldBe 6
        }

        test("−2 mills three and draws one card when no graveyard has twenty cards") {
            val (game, jace) = board()
            game.execute(
                ActivateAbility(game.player1Id, jace, minusTwo, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 3
            game.handSize(1) shouldBe 1
            game.loyalty(jace) shouldBe 3
        }

        test("−2 draws three when a non-targeted graveyard has twenty or more cards") {
            val (game, jace) = board(p1Graveyard = 20)
            game.execute(
                ActivateAbility(game.player1Id, jace, minusTwo, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 3
            game.handSize(1) shouldBe 3
        }

        test("−2 counts the cards it just milled toward twenty") {
            val (game, jace) = board(p2Graveyard = 17)
            game.execute(
                ActivateAbility(game.player1Id, jace, minusTwo, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 20
            game.handSize(1) shouldBe 3
        }

        test("−2 at nineteen cards across two graveyards still draws only one") {
            val (game, jace) = board(p1Graveyard = 10, p2Graveyard = 6)
            game.execute(
                ActivateAbility(game.player1Id, jace, minusTwo, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 9
            game.handSize(1) shouldBe 1
        }

        test("−X mills three times X cards") {
            val (game, jace) = board()
            game.execute(
                ActivateAbility(
                    game.player1Id, jace, minusX,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    xValue = 2
                )
            ).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 6
            game.librarySize(2) shouldBe 4
            game.loyalty(jace) shouldBe 3
        }
    }
}
