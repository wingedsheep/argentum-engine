package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Woodcutter's Grit (EMN #179) — {2}{G} Instant.
 *
 *   Target creature you control gets +3/+3 and gains hexproof until end of turn.
 */
class WoodcuttersGritScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Woodcutter's Grit")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("your creature gets +3/+3 and hexproof") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!

            val cast = game.castSpell(1, "Woodcutter's Grit", bears)
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 5
            projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
        }

        test("cannot target a creature you don't control") {
            val game = game()
            val giant = game.findPermanent("Hill Giant")!!

            val cast = game.castSpell(1, "Woodcutter's Grit", giant)
            withClue("an opponent's creature is not a legal target") { cast.error shouldNotBe null }
        }
    }
}
