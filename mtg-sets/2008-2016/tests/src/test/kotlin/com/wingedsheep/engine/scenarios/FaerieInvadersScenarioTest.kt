package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Faerie Invaders (M13 #51) — {4}{U} 3/3 Creature — Faerie Rogue. Flash; Flying.
 */
class FaerieInvadersScenarioTest : ScenarioTestBase() {
    init {
        test("flash lets it be cast outside a main phase; it enters with flying") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Faerie Invaders")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 5)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            withClue("a creature without flash can't be cast in the beginning of combat step") {
                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
            }

            game.castSpell(1, "Faerie Invaders").error shouldBe null
            game.resolveStack()

            val invaders = game.findPermanent("Faerie Invaders")!!
            game.state.projectedState.hasKeyword(invaders, Keyword.FLYING) shouldBe true
        }
    }
}
