package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Guise of Fire (AVR #137): "Enchant creature. Enchanted creature gets +1/-1 and attacks each
 * combat if able."
 *
 * Pins that the attack requirement lands on the enchanted creature, not on the Aura.
 */
class GuiseOfFireScenarioTest : ScenarioTestBase() {

    init {
        test("the enchanted creature gets +1/-1 and must attack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withCardAttachedTo(1, "Guise of Fire", "Hill Giant")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            withClue("Hill Giant is a 4/2") {
                game.state.projectedState.getPower(giant) shouldBe 4
                game.state.projectedState.getToughness(giant) shouldBe 2
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("declaring no attackers breaks the enchanted creature's requirement") {
                game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldNotBe null
            }
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
        }
    }
}
