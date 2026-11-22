package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Crypt Incursion — "Exile all creature cards from target player's graveyard. You gain 3 life for
 * each card exiled this way."
 *
 * Only creature cards leave the graveyard, and the life gain scales with how many were exiled.
 */
class CryptIncursionScenarioTest : ScenarioTestBase() {

    init {
        test("exiles only the target player's creature cards and gains 3 life per card exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Crypt Incursion")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Hill Giant")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withCardInGraveyard(1, "Savannah Lions")
                .withLifeTotal(1, 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Crypt Incursion", 2).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Savannah Lions") shouldBe true
            game.getLifeTotal(1) shouldBe 16
        }

        test("an empty creature graveyard gains no life") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Crypt Incursion")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInGraveyard(2, "Lightning Bolt")
                .withLifeTotal(1, 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Crypt Incursion", 2).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            game.getLifeTotal(1) shouldBe 10
        }
    }
}
