package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.tsp.cards.SpriteNoble
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sprite Noble (Time Spiral #81) — {1}{U}{U} Creature — Faerie Noble 2/2, Flying.
 * "Other creatures you control with flying get +0/+1.
 *  {T}: Other creatures you control with flying get +1/+0 until end of turn."
 */
class SpriteNobleScenarioTest : ScenarioTestBase() {
    private val pumpAbility = SpriteNoble.activatedAbilities.single().id

    init {
        test("other fliers you control get +0/+1, and +1/+0 more when it taps") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Sprite Noble")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wind Drake")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val noble = game.findPermanent("Sprite Noble")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val drakes = game.findPermanents("Wind Drake")
            val myDrake = drakes.single { it in game.state.getBattlefield(game.player1Id) }
            val theirDrake = drakes.single { it != myDrake }
            val p = { game.state.projectedState }

            p().getPower(myDrake) shouldBe 2
            p().getToughness(myDrake) shouldBe 3
            withClue("not the Noble itself, a non-flier, or an opponent's flier") {
                p().getToughness(noble) shouldBe 2
                p().getToughness(bears) shouldBe 2
                p().getToughness(theirDrake) shouldBe 2
            }

            game.execute(ActivateAbility(game.player1Id, noble, pumpAbility)).error shouldBe null
            game.resolveStack()

            p().getPower(myDrake) shouldBe 3
            p().getToughness(myDrake) shouldBe 3
            p().getPower(noble) shouldBe 2
            p().getPower(bears) shouldBe 2
            p().getPower(theirDrake) shouldBe 2
        }
    }
}
