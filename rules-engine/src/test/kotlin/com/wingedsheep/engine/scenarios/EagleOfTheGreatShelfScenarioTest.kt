package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Eagle of the Great Shelf (HOB #11) — {4}{W} Creature — Bird Soldier 2/5
 * "Flying. Whenever this creature attacks, it gets +1/+1 until end of turn for each other creature
 * you control."
 *
 * Covers the two things the `excludeSelf` aggregate can get wrong: the Eagle counting itself, and
 * opponents' creatures leaking into "you control".
 */
class EagleOfTheGreatShelfScenarioTest : ScenarioTestBase() {

    init {
        test("the bonus counts other creatures you control, not the Eagle and not the opponent's") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Eagle of the Great Shelf", tapped = false, summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                // The opponent's creature must not be counted.
                .withCardOnBattlefield(2, "Glory Seeker", tapped = false, summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eagle = game.findPermanent("Eagle of the Great Shelf")!!

            withClue("printed stats before combat") {
                game.state.projectedState.getPower(eagle) shouldBe 2
                game.state.projectedState.getToughness(eagle) shouldBe 5
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Eagle of the Great Shelf" to 2)).error shouldBe null
            game.resolveStack()

            withClue("+2/+2 for the two other creatures Player1 controls") {
                game.state.projectedState.getPower(eagle) shouldBe 4
                game.state.projectedState.getToughness(eagle) shouldBe 7
            }
        }

        test("attacking alone gives no bonus") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Eagle of the Great Shelf", tapped = false, summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eagle = game.findPermanent("Eagle of the Great Shelf")!!

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Eagle of the Great Shelf" to 2)).error shouldBe null
            game.resolveStack()

            withClue("the Eagle never counts itself") {
                game.state.projectedState.getPower(eagle) shouldBe 2
                game.state.projectedState.getToughness(eagle) shouldBe 5
            }
        }
    }
}
