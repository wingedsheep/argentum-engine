package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bassara Tower Archer (JOU #117) — {G}{G} Creature — Human Archer 2/1, Reach, hexproof.
 */
class BassaraTowerArcherScenarioTest : ScenarioTestBase() {
    init {
        test("hexproof: an opponent's Shock can't target it") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Bassara Tower Archer")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val archer = game.findPermanent("Bassara Tower Archer")!!
            withClue("targeting a hexproof creature with an opponent's spell is illegal") {
                game.castSpell(2, "Shock", archer).error shouldNotBe null
            }
            game.isOnBattlefield("Bassara Tower Archer") shouldBe true
        }

        test("reach: it blocks an attacking flier") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Bassara Tower Archer")
                .withCardOnBattlefield(2, "Wind Drake")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Wind Drake" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("reach lets the Archer block the flying Wind Drake") {
                game.declareBlockers(mapOf("Bassara Tower Archer" to listOf("Wind Drake"))).error shouldBe null
            }
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the blocked Drake dealt no damage to P1") {
                game.getLifeTotal(1) shouldBe 20
            }
            withClue("2 damage kills the 2/2 Drake; 2 damage kills the 2/1 Archer") {
                game.isInGraveyard(2, "Wind Drake") shouldBe true
                game.isInGraveyard(1, "Bassara Tower Archer") shouldBe true
            }
        }
    }
}
