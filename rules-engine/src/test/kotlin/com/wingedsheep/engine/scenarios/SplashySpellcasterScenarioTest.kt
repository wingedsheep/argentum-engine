package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Splashy Spellcaster. */
class SplashySpellcasterScenarioTest : ScenarioTestBase() {

    init {
        context("Splashy Spellcaster — up to one OTHER target creature you control") {
            test("casting an instant crowns another creature with a Sorcerer Role") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Splashy Spellcaster", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Shock", bears)
                // The cast trigger goes on the stack above Shock and asks for its target first.
                game.selectTargets(listOf(bears))
                game.resolveStack()

                val role = game.findPermanent("Sorcerer Role")
                withClue("the Sorcerer Role token was created") { role shouldNotBe null }
                withClue("attached to the Bears, not the Spellcaster") {
                    game.state.getEntity(role!!)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
            }

            test("declining the optional target creates no Role token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Splashy Spellcaster", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Shock", bears)
                game.skipTargets()
                game.resolveStack()

                withClue("no target chosen -> no Sorcerer Role (per the card's ruling)") {
                    game.findPermanent("Sorcerer Role") shouldBe null
                }
            }
        }
    }
}
