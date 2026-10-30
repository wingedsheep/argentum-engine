package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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
 * Teferi, Timeless Voyager (M21 #324).
 *
 *   −3: Put target creature on top of its owner's library.
 *   −8: Each creature target opponent controls phases out. Until the end of your next turn, they
 *       can't phase in.
 *
 * Ruling (2020-06-23): the creatures don't phase in until the duration has expired; they phase in
 * during their controller's next untap step after your next turn.
 */
class TeferiTimelessVoyagerScenarioTest : ScenarioTestBase() {
    private val name = "Teferi, Timeless Voyager"

    private fun TestGame.loyalty(index: Int, target: ChosenTarget? = null) = ActivateAbility(
        player1Id,
        findPermanent(name)!!,
        cardRegistry.getCard(name)!!.script.activatedAbilities[index].id,
        targets = listOfNotNull(target),
    )

    private fun TestGame.setLoyalty(n: Int) {
        val id = findPermanent(name)!!
        state = state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, n)) }
    }

    private fun TestGame.isPhasedIn(id: EntityId) = id in state.getBattlefield()

    private fun TestGame.toUpkeepOf(playerId: EntityId) {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        state.activePlayerId shouldBe playerId
    }

    private fun board(): TestGame = scenario().withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, name)
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply { repeat(8) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Plains") } }
        .build()

    init {
        test("−3 puts target creature on top of its owner's library") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.execute(game.loyalty(1, ChosenTarget.Permanent(giant))).error shouldBe null
            game.resolveStack()
            game.findPermanent("Hill Giant") shouldBe null
            game.state.getLibrary(game.player2Id).first() shouldBe giant
        }

        test("−8 phases out each creature the opponent controls until the end of your next turn") {
            val game = board()
            game.setLoyalty(8)
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findPermanent("Savannah Lions")!!

            game.execute(game.loyalty(2, ChosenTarget.Player(game.player2Id))).error shouldBe null
            game.resolveStack()

            game.isPhasedIn(bears) shouldBe false
            game.isPhasedIn(giant) shouldBe false
            withClue("only the target opponent's creatures phase out") {
                game.isPhasedIn(lions) shouldBe true
            }

            withClue("they skip the opponent's next untap step") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(bears) shouldBe false
                game.isPhasedIn(giant) shouldBe false
            }

            game.toUpkeepOf(game.player1Id)

            withClue("after your next turn ends they phase in at their controller's untap step") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(bears) shouldBe true
                game.isPhasedIn(giant) shouldBe true
            }
        }
    }
}
