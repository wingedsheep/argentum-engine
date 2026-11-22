package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Wildwood Rebirth (GTC) — Return target creature card from your graveyard to your hand. */
class WildwoodRebirthScenarioTest : ScenarioTestBase() {
    init {
        test("returns a creature card from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Wildwood Rebirth")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingGraveyardCard(1, "Wildwood Rebirth", 1, "Grizzly Bears")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("Grizzly Bears moved from graveyard to hand") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
        }

        test("can't target a creature card in an opponent's graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Wildwood Rebirth")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingGraveyardCard(1, "Wildwood Rebirth", 2, "Grizzly Bears")
            withClue("opponent's graveyard is not a legal target") { cast.error shouldNotBe null }
        }
    }
}
