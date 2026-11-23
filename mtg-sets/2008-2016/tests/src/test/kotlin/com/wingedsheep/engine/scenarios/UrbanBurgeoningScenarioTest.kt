package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Urban Burgeoning (RTR #138) — "Enchant land. Enchanted land has 'Untap this land during each
 * other player's untap step.'"
 *
 * The untap is a granted static read off the land's projected abilities, so it follows the land:
 * it untaps in every untap step but its own controller's, whoever controls the Aura.
 */
class UrbanBurgeoningScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.isTapped(name: String): Boolean =
        state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    init {
        test("the enchanted land untaps during the opponent's untap step; an unenchanted land stays tapped") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardAttachedTo(1, "Urban Burgeoning", "Forest")
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("it is now the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            withClue("the enchanted Forest untapped in the opponent's untap step") { game.isTapped("Forest") shouldBe false }
            withClue("the unenchanted Mountain did not") { game.isTapped("Mountain") shouldBe true }
        }

        test("on an opponent's land, the land untaps during the Aura controller's untap step") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .withCardAttachedTo(1, "Urban Burgeoning", "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("it is now the Aura controller's turn") { game.state.activePlayerId shouldBe game.player1Id }
            withClue("the opponent's enchanted Forest untapped in another player's untap step") {
                game.isTapped("Forest") shouldBe false
            }
        }
    }
}
