package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Akroan Phalanx (BNG #2) — "{2}{R}: Creatures you control get +1/+0 until end of turn."
 *
 * The pump reaches every creature its controller has, itself included, and none of the opponent's.
 */
class AkroanPhalanxScenarioTest : ScenarioTestBase() {

    private val pumpAbilityId by lazy {
        cardRegistry.requireCard("Akroan Phalanx").activatedAbilities[0].id
    }

    init {
        test("{2}{R} gives each creature you control +1/+0 and leaves the opponent's alone") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Akroan Phalanx")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val phalanx = game.findPermanent("Akroan Phalanx")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = phalanx, abilityId = pumpAbilityId)
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("your creatures get +1/+0") {
                projected.getPower(phalanx) shouldBe 4
                projected.getToughness(phalanx) shouldBe 3
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 2
            }
            withClue("the opponent's creature is untouched") {
                projected.getPower(giant) shouldBe 3
            }
        }
    }
}
