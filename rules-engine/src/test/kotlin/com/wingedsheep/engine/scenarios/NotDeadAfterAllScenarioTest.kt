package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Not Dead After All. */
class NotDeadAfterAllScenarioTest : ScenarioTestBase() {

    init {
        context("Not Dead After All — return tapped, then crown with a Wicked Role") {
            test("the granted creature dies and comes back tapped wearing a Wicked Role") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Not Dead After All")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Not Dead After All", bears)
                game.resolveStack()

                game.castSpell(1, "Doom Blade", bears)
                game.resolveStack()

                withClue("same entity is back on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.findPermanent("Grizzly Bears") shouldBe bears
                }
                withClue("…tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
                val role = game.findPermanent("Wicked Role")
                withClue("…wearing a Wicked Role") {
                    role shouldNotBe null
                    game.state.getEntity(role!!)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("2/2 Bears + the Wicked Role's +1/+1 = 3/3") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("the grant is until end of turn only — a later death is permanent (CR 400.7)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Not Dead After All")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Not Dead After All", bears)
                game.resolveStack()

                withClue("while it is live the grant rides the Bears") {
                    game.state.grantedTriggeredAbilities.any { it.entityId == bears } shouldBe true
                }

                game.castSpell(1, "Doom Blade", bears)
                game.resolveStack()

                withClue("the returned Bears is a new object — the grant did not follow it") {
                    game.state.grantedTriggeredAbilities.any { it.entityId == bears } shouldBe false
                }
            }
        }
    }
}
