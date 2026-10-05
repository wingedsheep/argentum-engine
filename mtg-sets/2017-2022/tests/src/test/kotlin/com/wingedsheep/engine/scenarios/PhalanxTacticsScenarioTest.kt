package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phalanx Tactics (THB #31) — {1}{W} Instant.
 *
 *   Target creature you control gets +2/+1 until end of turn. Each other creature you control
 *   gets +1/+1 until end of turn.
 */
class PhalanxTacticsScenarioTest : ScenarioTestBase() {
    init {
        test("the target gets +2/+1, each other creature you control +1/+1, opponents' creatures nothing") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Phalanx Tactics")
                .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                .withCardOnBattlefield(1, "Hill Giant") // 3/3
                .withCardOnBattlefield(2, "Centaur Courser") // 3/3
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, "Phalanx Tactics", targetId = bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the target gets +2/+1 only (not the extra +1/+1)") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 3
            }
            withClue("each other creature you control gets +1/+1") {
                projected.getPower(giant) shouldBe 4
                projected.getToughness(giant) shouldBe 4
            }
            withClue("an opponent's creature is untouched") {
                projected.getPower(courser) shouldBe 3
                projected.getToughness(courser) shouldBe 3
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            withClue("the pumps wear off at end of turn") {
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getPower(giant) shouldBe 3
            }
        }

        test("if the target is gone on resolution, no other creature gets +1/+1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Phalanx Tactics")
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant") // 3/3
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Phalanx Tactics", targetId = bears).error shouldBe null
            game.castSpell(1, "Shock", targetId = bears).error shouldBe null
            game.resolveStack()

            withClue("the Bears died first, so Phalanx Tactics didn't resolve") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.isInGraveyard(1, "Phalanx Tactics") shouldBe true
                game.state.projectedState.getPower(giant) shouldBe 3
                game.state.projectedState.getToughness(giant) shouldBe 3
            }
        }

        test("can't target a creature an opponent controls") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Phalanx Tactics")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courser = game.findPermanent("Centaur Courser")!!
            game.castSpell(1, "Phalanx Tactics", targetId = courser).error shouldNotBe null
        }
    }
}
