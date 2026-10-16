package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Structural Assault (SNC) — "Destroy all artifacts, then Structural Assault deals damage to each
 * creature equal to the number of artifacts that were put into graveyards from the battlefield this
 * turn."
 *
 * The load-bearing part is "then": the damage amount is read after the wrath, so the artifacts this
 * spell itself destroyed count. A version that evaluated the amount up front would deal 0.
 */
class StructuralAssaultScenarioTest : ScenarioTestBase() {

    init {
        test("damage equals the artifacts the spell itself just destroyed") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Structural Assault")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Bottle Gnomes")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Structural Assault").error shouldBe null
            game.resolveStack()

            withClue("both artifacts are destroyed") {
                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.isInGraveyard(2, "Bottle Gnomes") shouldBe true
            }
            withClue("two artifacts died, so each creature takes 2: the 2/2 dies, the 3/3 survives") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("with no artifacts put into a graveyard this turn, no damage is dealt") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Structural Assault")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Structural Assault").error shouldBe null
            game.resolveStack()

            withClue("zero artifacts, zero damage") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
