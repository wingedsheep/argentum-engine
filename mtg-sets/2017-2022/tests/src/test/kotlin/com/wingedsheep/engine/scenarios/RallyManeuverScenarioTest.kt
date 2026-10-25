package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Rally Maneuver (AFR #36) — {2}{W} Instant.
 *
 *   Target creature gets +2/+0 and gains first strike until end of turn. Up to one other target
 *   creature gets +0/+2 and gains lifelink until end of turn.
 */
class RallyManeuverScenarioTest : ScenarioTestBase() {

    init {
        context("Rally Maneuver") {

            test("two targets: primary gets +2/+0 and first strike, second gets +0/+2 and lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rally Maneuver")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2 primary
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3 second
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val card = game.findCardsInHand(1, "Rally Maneuver").first()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("primary (Grizzly Bears) becomes 4/2 with first strike, no lifelink") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                    projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
                }
                withClue("second (Hill Giant) becomes 3/5 with lifelink, no first strike") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 5
                    projected.hasKeyword(giant, Keyword.LIFELINK) shouldBe true
                    projected.hasKeyword(giant, Keyword.FIRST_STRIKE) shouldBe false
                }
            }

            test("only the primary target chosen: other creature is unaffected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rally Maneuver")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val card = game.findCardsInHand(1, "Rally Maneuver").first()
                game.execute(
                    CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("primary still becomes 4/2 with first strike") {
                    projected.getPower(bears) shouldBe 4
                    projected.getToughness(bears) shouldBe 2
                    projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("untargeted Hill Giant stays a plain 3/3") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 3
                    projected.hasKeyword(giant, Keyword.LIFELINK) shouldBe false
                }
            }
        }
    }
}
