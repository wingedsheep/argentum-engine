package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Purple Worm (AFR #201): costs {2} less to cast if a creature died this turn. Ward {2}.
 */
class PurpleWormScenarioTest : ScenarioTestBase() {

    private fun TestGame.wormAffordable(): Boolean =
        getLegalActions(1).firstOrNull {
            it.actionType == "CastSpell" && it.action is CastSpell && it.description.contains("Purple Worm")
        }?.isAffordable ?: false

    init {
        test("five lands can't cast it before a creature dies, but can after") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Purple Worm")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true

            withClue("with only five Forests untapped, the reduction makes it castable") {
                game.wormAffordable() shouldBe true
            }
            game.castSpell(1, "Purple Worm").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Purple Worm") shouldBe true
        }

        test("no creature died: five lands aren't enough") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Purple Worm")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.wormAffordable() shouldBe false
        }
    }
}
