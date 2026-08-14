package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

class RiseFromTheWreckScenarioTest : ScenarioTestBase() {
    init {
        context("Rise from the Wreck") {
            test("returns one card chosen for each of its four independent target slots") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rise from the Wreck")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInGraveyard(1, "Bulwark Ox")
                    .withCardInGraveyard(1, "Boosted Sloop")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun target(name: String) = ChosenTarget.Card(
                    game.findCardsInGraveyard(1, name).single(),
                    game.player1Id,
                    Zone.GRAVEYARD,
                )

                val spell = game.findCardsInHand(1, "Rise from the Wreck").single()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(
                            target("Llanowar Elves"),
                            target("Bulwark Ox"),
                            target("Boosted Sloop"),
                            target("Grizzly Bears"),
                        ),
                    ),
                ).error shouldBe null
                game.resolveStack()

                listOf("Llanowar Elves", "Bulwark Ox", "Boosted Sloop", "Grizzly Bears").forEach {
                    game.isInHand(1, it) shouldBe true
                }
            }

            test("can be cast with all four optional target slots empty") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rise from the Wreck")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Rise from the Wreck").single()
                game.execute(CastSpell(game.player1Id, spell, emptyList())).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Rise from the Wreck") shouldBe true
            }
        }
    }
}
