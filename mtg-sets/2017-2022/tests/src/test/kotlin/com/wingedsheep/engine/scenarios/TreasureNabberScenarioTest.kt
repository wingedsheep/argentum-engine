package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Treasure Nabber (C18 #27) — "Whenever an opponent taps an artifact for mana, gain control of that
 * artifact until the end of your next turn."
 *
 * The first card on `EventPattern.PermanentTappedForMana` with a non-land source: the tapped
 * artifact is the triggering entity, the artifact stays tapped, and control runs through the end of
 * the Nabber controller's *next* turn — which is the one after the tap even when the tap came on
 * the Nabber controller's own turn.
 */
class TreasureNabberScenarioTest : ScenarioTestBase() {

    /** Player 1 has the Nabber; player 2 has a Sol Ring and a Forest, and it is [active]'s turn. */
    private fun setup(active: Int): TestGame {
        var builder = scenario()
            .withPlayers("Nabber", "Opponent")
            .withCardOnBattlefield(1, "Treasure Nabber")
            .withCardOnBattlefield(2, "Sol Ring")
            .withCardOnBattlefield(2, "Forest")
            .withActivePlayer(active)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withPriorityPlayer(2)
        repeat(10) { builder = builder.withCardInLibrary(1, "Plains").withCardInLibrary(2, "Island") }
        return builder.build()
    }

    private fun TestGame.tapForMana(permanentName: String) {
        val id = findPermanent(permanentName)!!
        val ability = cardRegistry.getCard(permanentName)!!.script.activatedAbilities.single().id
        execute(ActivateAbility(player2Id, id, ability)).error shouldBe null
    }

    private fun TestGame.controllerOf(id: EntityId) = state.projectedState.getController(id)

    private fun TestGame.nextMain() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
    }

    init {
        context("Treasure Nabber") {
            test("an opponent tapping an artifact for mana hands it to you, still tapped") {
                val game = setup(active = 2)
                val ring = game.findPermanent("Sol Ring")!!

                game.tapForMana("Sol Ring")
                withClue("the trigger uses the stack") { game.state.stack.size shouldBe 1 }
                game.resolveStack()

                game.controllerOf(ring) shouldBe game.player1Id
                withClue("the artifact remains tapped") { game.state.getEntity(ring)!!.has<TappedComponent>() shouldBe true }
            }

            test("a land tapped for mana isn't an artifact: nothing happens") {
                val game = setup(active = 2)
                val forest = game.findPermanent("Forest")!!

                game.tapForMana("Forest")

                game.state.stack.size shouldBe 0
                game.controllerOf(forest) shouldBe game.player2Id
            }

            test("tapped on the opponent's turn, it is yours through your next turn and returns after it") {
                val game = setup(active = 2)
                val ring = game.findPermanent("Sol Ring")!!
                game.tapForMana("Sol Ring")
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("still yours at the end of their turn") { game.controllerOf(ring) shouldBe game.player1Id }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("their turn is over; it is your next turn") { game.state.activePlayerId shouldBe game.player1Id }
                game.controllerOf(ring) shouldBe game.player1Id

                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("you keep it to the end of your next turn") { game.controllerOf(ring) shouldBe game.player1Id }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("then it goes back") { game.controllerOf(ring) shouldBe game.player2Id }
            }

            test("tapped on your own turn, you still keep it through your *next* turn") {
                val game = setup(active = 1)
                val ring = game.findPermanent("Sol Ring")!!
                game.tapForMana("Sol Ring")
                game.resolveStack()
                game.controllerOf(ring) shouldBe game.player1Id

                game.nextMain() // the opponent's turn
                withClue("still yours during their turn") { game.controllerOf(ring) shouldBe game.player1Id }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // your next turn begins
                withClue("still yours during your next turn") { game.controllerOf(ring) shouldBe game.player1Id }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("returned once your next turn ended") { game.controllerOf(ring) shouldBe game.player2Id }
            }
        }
    }
}
