package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Bewildering Blizzard (TDM #38).
 *
 * "Draw three cards. Creatures your opponents control get -3/-0 until end of turn."
 *
 * Verifies the caster draws three and only opponents' creatures get the -3/-0,
 * while the caster's own creature is unaffected. The caster's 2/2 Grizzly Bears
 * and the opponent's 3/3 Hill Giant are distinct names so each is identifiable.
 */
class BewilderingBlizzardScenarioTest : ScenarioTestBase() {

    init {
        context("Bewildering Blizzard") {

            test("draws three and shrinks opponents' creatures by -3/-0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bewildering Blizzard")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    // Library cards so the draw-three actually has cards to pull.
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .build()

                val handBefore = game.handSize(1)
                val myBear = game.findPermanent("Grizzly Bears")!!
                val theirGiant = game.findPermanent("Hill Giant")!!

                val cast = game.castSpell(1, "Bewildering Blizzard")
                withClue("Casting Bewildering Blizzard should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Casting drew three (hand had Blizzard, cast it, drew 3 → +2 net)") {
                    game.handSize(1) shouldBe handBefore - 1 + 3
                }
                withClue("Opponent's Hill Giant should be 0/3 with -3/-0 (3/3 base)") {
                    game.state.projectedState.getPower(theirGiant) shouldBe 0
                    game.state.projectedState.getToughness(theirGiant) shouldBe 3
                }
                withClue("Caster's own Grizzly Bears should be unaffected (2/2)") {
                    game.state.projectedState.getPower(myBear) shouldBe 2
                    game.state.projectedState.getToughness(myBear) shouldBe 2
                }
            }
        }
    }
}
