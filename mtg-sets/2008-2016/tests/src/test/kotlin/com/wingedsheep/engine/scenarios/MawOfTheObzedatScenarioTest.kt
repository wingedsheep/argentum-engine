package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dgm.cards.MawOfTheObzedat
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Maw of the Obzedat (DGM #83) — {3}{W}{B} 3/3 Creature — Thrull.
 *
 *   Sacrifice a creature: Creatures you control get +1/+1 until end of turn.
 */
class MawOfTheObzedatScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing a creature pumps every creature you control, not the opponent's") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Maw of the Obzedat")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val maw = game.findPermanent("Maw of the Obzedat")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findPermanent("Savannah Lions")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = maw,
                    abilityId = MawOfTheObzedat.activatedAbilities.single().id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the sacrificed creature is gone") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            val projected = game.state.projectedState
            withClue("each creature you control gets +1/+1") {
                projected.getPower(maw) shouldBe 4
                projected.getToughness(maw) shouldBe 4
                projected.getPower(giant) shouldBe 4
                projected.getToughness(giant) shouldBe 4
            }
            withClue("the opponent's creature is unchanged") {
                projected.getPower(lions) shouldBe 1
                projected.getToughness(lions) shouldBe 1
            }
        }
    }
}
