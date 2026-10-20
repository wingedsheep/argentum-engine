package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hellfire (LEG) — destroys every nonblack creature, then deals X plus 3 damage to its caster,
 * where X counts the creatures that actually *died* (were put into a graveyard) this way.
 *
 * Pinned:
 *  - black creatures survive; indestructible ones aren't destroyed and so aren't counted;
 *  - with nothing destroyed the caster still takes 3;
 *  - a creature a replacement effect exiles instead (Leyline of the Void) was destroyed but did
 *    not die, so it isn't counted (2009-10-01 ruling).
 */
class HellfireScenarioTest : ScenarioTestBase() {

    init {
        context("Hellfire") {

            test("destroys nonblack creatures on both sides and deals X plus 3 to the caster") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hellfire")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Drudge Skeletons")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "River Boa")
                    .withCardOnBattlefield(2, "Darksteel Myr")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hellfire").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "River Boa") shouldBe true
                game.isOnBattlefield("Drudge Skeletons") shouldBe true
                game.isOnBattlefield("Darksteel Myr") shouldBe true
                // Three creatures died: 3 + 3 = 6 damage to the caster.
                game.getLifeTotal(1) shouldBe 14
                game.getLifeTotal(2) shouldBe 20
            }

            test("with no nonblack creatures the caster still takes 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hellfire")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Drudge Skeletons")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hellfire").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Drudge Skeletons") shouldBe true
                game.getLifeTotal(1) shouldBe 17
            }

            test("creatures exiled instead of put into a graveyard did not die and aren't counted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hellfire")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Leyline of the Void")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "River Boa")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hellfire").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "River Boa") shouldBe true
                // Only Player1's Bears died: 1 + 3 = 4 damage.
                game.getLifeTotal(1) shouldBe 16
            }
        }
    }
}
