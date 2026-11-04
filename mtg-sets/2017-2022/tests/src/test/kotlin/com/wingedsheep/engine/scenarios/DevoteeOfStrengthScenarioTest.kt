package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.DevoteeOfStrength
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Devotee of Strength (HOU) — {2}{G} 3/2. "{4}{G}: Target creature gets +2/+2 until end of turn."
 */
class DevoteeOfStrengthScenarioTest : ScenarioTestBase() {
    init {
        test("pumps another target creature +2/+2") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Devotee of Strength")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val devotee = game.findPermanent("Devotee of Strength")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, devotee, DevoteeOfStrength.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.state.projectedState.getPower(devotee) shouldBe 3
        }
    }
}
