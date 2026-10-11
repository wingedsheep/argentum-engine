package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rite of Flame — "Add {R}{R}, then add {R} for each card named Rite of Flame in each graveyard."
 * Counts copies in every graveyard, and never the resolving copy (it is on the stack).
 */
class RiteOfFlameScenarioTest : ScenarioTestBase() {
    init {
        fun redAfterCasting(game: TestGame): Int {
            game.castSpell(1, "Rite of Flame").error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }
            return game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.red
        }

        test("with no other copies in graveyards it adds exactly RR") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Rite of Flame")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            redAfterCasting(game) shouldBe 2
        }

        test("counts copies in every player's graveyard") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Rite of Flame")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInGraveyard(1, "Rite of Flame")
                .withCardInGraveyard(2, "Rite of Flame")
                .withCardInGraveyard(2, "Rite of Flame")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            redAfterCasting(game) shouldBe 5
        }
    }
}
