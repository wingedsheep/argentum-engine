package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Bleed Dry (VOW #94) — {2}{B}{B} Instant.
 *
 *   Target creature gets -13/-13 until end of turn. If that creature would die this turn,
 *   exile it instead.
 *
 * Exercises the -13/-13 shrink and the exile-instead-of-die replacement: a 3/3 target is
 * reduced to a lethally-negative toughness, dies to state-based actions, and ends up exiled
 * rather than in its owner's graveyard.
 */
class BleedDryScenarioTest : ScenarioTestBase() {

    init {
        context("Bleed Dry -13/-13 and exile-on-death") {

            test("shrinks the target to lethal toughness and exiles it instead of letting it die") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bleed Dry")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false) // 3/3
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Bleed Dry", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is no longer on the battlefield (it died to -13/-13)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("It is exiled, not sent to the graveyard") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe false
                }
            }
        }
    }
}
