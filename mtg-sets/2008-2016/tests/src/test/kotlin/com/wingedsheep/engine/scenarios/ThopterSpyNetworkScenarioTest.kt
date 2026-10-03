package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thopter Spy Network (ORI #79) — an artifact-gated upkeep Thopter, and a batch draw when artifact
 * creatures you control connect.
 */
class ThopterSpyNetworkScenarioTest : ScenarioTestBase() {
    init {
        fun thopters(game: TestGame) = game.state.getZone(game.player1Id, Zone.BATTLEFIELD).filter {
            game.state.projectedState.hasSubtype(it, "Thopter")
        }

        test("upkeep makes a flying 1/1 Thopter artifact token while you control an artifact") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Thopter Spy Network")
                .withCardOnBattlefield(1, "Sol Ring")
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            val made = thopters(game)
            made.size shouldBe 1
            val projected = game.state.projectedState
            projected.isCreature(made.single()) shouldBe true
            projected.hasType(made.single(), "ARTIFACT") shouldBe true
            projected.hasKeyword(made.single(), Keyword.FLYING) shouldBe true
            projected.getPower(made.single()) shouldBe 1
        }

        test("no artifact, no Thopter") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Thopter Spy Network")
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()
            thopters(game).size shouldBe 0
        }

        test("two artifact creatures connecting draw one card; a non-artifact attacker draws none") {
            for (artifacts in listOf(true, false)) {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Thopter Spy Network")
                    .apply {
                        if (artifacts) {
                            withCardOnBattlefield(1, "Memnite")
                            withCardOnBattlefield(1, "Memnite")
                        } else {
                            withCardOnBattlefield(1, "Grizzly Bears")
                        }
                    }
                    .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val attackers = game.findAllPermanents(if (artifacts) "Memnite" else "Grizzly Bears")
                val handBefore = game.handSize(1)
                game.execute(DeclareAttackers(game.player1Id, attackers.associateWith { game.player2Id }))
                    .error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("artifact attackers = $artifacts") {
                    game.getLifeTotal(2) shouldBe 18
                    game.handSize(1) shouldBe handBefore + (if (artifacts) 1 else 0)
                }
            }
        }
    }
}
