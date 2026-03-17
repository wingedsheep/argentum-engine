package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ainok Bond-Kin's static ability:
 * "Each creature you control with a +1/+1 counter on it has first strike."
 *
 * When Ainok Bond-Kin is stolen via Act of Treason, the buff should apply to
 * the new controller's creatures with +1/+1 counters, not the original controller's.
 */
class AinokBondKinScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Ainok Bond-Kin static ability with control change") {

            test("stealing Ainok Bond-Kin grants first strike to new controller's creatures with +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Player 1 owns Ainok Bond-Kin and a creature with a +1/+1 counter
                    .withCardOnBattlefield(1, "Ainok Bond-Kin")
                    .withCardOnBattlefield(1, "Glory Seeker")  // Will get a +1/+1 counter
                    // Player 2 has Act of Treason and a creature with a +1/+1 counter
                    .withCardInHand(2, "Act of Treason")
                    .withCardOnBattlefield(2, "Grizzly Bears")  // Will get a +1/+1 counter
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ainokId = game.findPermanent("Ainok Bond-Kin")!!
                val glorySeekerIdP1 = game.findPermanent("Glory Seeker")!!
                val grizzlyBearsIdP2 = game.findPermanent("Grizzly Bears")!!

                // Add +1/+1 counters to both Glory Seeker and Grizzly Bears
                val counter = CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1)
                game.state = game.state
                    .updateEntity(glorySeekerIdP1) { c -> c.with(counter) }
                    .updateEntity(grizzlyBearsIdP2) { c -> c.with(counter) }

                // Before Act of Treason: Player 1 controls Ainok Bond-Kin,
                // so Player 1's Glory Seeker should have first strike, Player 2's Grizzly Bears should not
                var projected = stateProjector.project(game.state)

                withClue("Before steal: Glory Seeker (P1) should have first strike from Ainok Bond-Kin") {
                    projected.hasKeyword(glorySeekerIdP1, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("Before steal: Grizzly Bears (P2) should NOT have first strike") {
                    projected.hasKeyword(grizzlyBearsIdP2, Keyword.FIRST_STRIKE) shouldBe false
                }

                // Player 2 casts Act of Treason targeting Ainok Bond-Kin
                game.castSpell(2, "Act of Treason", ainokId)
                game.resolveStack()

                // After Act of Treason: Player 2 now controls Ainok Bond-Kin,
                // so Player 2's Grizzly Bears should have first strike, Player 1's Glory Seeker should not
                projected = stateProjector.project(game.state)

                withClue("After steal: Ainok Bond-Kin should be controlled by Player 2") {
                    projected.getController(ainokId) shouldBe game.player2Id
                }
                withClue("After steal: Grizzly Bears (P2) should have first strike from stolen Ainok Bond-Kin") {
                    projected.hasKeyword(grizzlyBearsIdP2, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("After steal: Glory Seeker (P1) should NOT have first strike anymore") {
                    projected.hasKeyword(glorySeekerIdP1, Keyword.FIRST_STRIKE) shouldBe false
                }
            }
        }
    }
}
