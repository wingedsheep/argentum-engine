package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Lava Coil (GRN #108) — {1}{R} Sorcery.
 *
 *   Lava Coil deals 4 damage to target creature. If that creature would die this turn, exile it instead.
 */
class LavaCoilScenarioTest : ScenarioTestBase() {

    init {
        test("lethal damage: the creature is exiled instead of dying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Lava Coil")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lava Coil", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInExile(2, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe false
        }

        test("a survivor that dies later this turn is still exiled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Lava Coil")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Murmuring Mystic")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mystic = game.findPermanent("Murmuring Mystic")!!
            game.castSpell(1, "Lava Coil", mystic).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Murmuring Mystic") shouldBe true

            game.castSpell(1, "Lightning Bolt", mystic).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Murmuring Mystic") shouldBe true
            game.isInGraveyard(2, "Murmuring Mystic") shouldBe false
        }
    }
}
