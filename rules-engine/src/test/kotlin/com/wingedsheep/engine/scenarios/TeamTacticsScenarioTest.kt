package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Team Tactics (MSH #155) — {1}{R} Instant.
 *
 *   Teamwork 1
 *   Target creature gains double strike until end of turn. If this spell was cast using teamwork,
 *   that creature also gains trample until end of turn.
 *
 * Both branches are pinned against projected keywords: double strike always, trample only on the
 * teamwork cast (CR 702.194b).
 */
class TeamTacticsScenarioTest : ScenarioTestBase() {

    init {
        context("Team Tactics") {

            test("cast without teamwork grants double strike but not trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Team Tactics")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castSpell(1, "Team Tactics", targetId = giant).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(giant, Keyword.DOUBLE_STRIKE) shouldBe true
                withClue("the trample rider is off without a teamwork declaration") {
                    game.state.projectedState.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
                }
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            }

            test("cast using teamwork grants double strike and trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Team Tactics")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                // Teamwork 1 — the 2/2 Bears clears the threshold on its own.
                game.castSpellWithTeamwork(
                    1, "Team Tactics", "Grizzly Bears", targetId = giant,
                ).error shouldBe null
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                game.state.projectedState.hasKeyword(giant, Keyword.DOUBLE_STRIKE) shouldBe true
                game.state.projectedState.hasKeyword(giant, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
