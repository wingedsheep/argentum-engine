package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Interplanar Beacon (WAR #247) — Land.
 *
 *   Whenever you cast a planeswalker spell, you gain 1 life.
 *   {T}: Add {C}.
 *   {1}, {T}: Add two mana of different colors. Spend this mana only to cast planeswalker spells.
 */
class InterplanarBeaconScenarioTest : ScenarioTestBase() {

    init {
        test("its two different colors cast a planeswalker, and casting one gains 1 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Interplanar Beacon")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Domri, Anarch of Bolas")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // {1}{R}{G}: one Plains funds the Beacon's {1}, the other pays the generic, the Beacon {R}{G}.
            game.castSpell(1, "Domri, Anarch of Bolas").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Domri, Anarch of Bolas") shouldBe true
            game.getLifeTotal(1) shouldBe 21
        }

        test("its colored mana can't pay for a spell that isn't a planeswalker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Interplanar Beacon")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error.shouldNotBeNull()
            game.getLifeTotal(1) shouldBe 20
        }

        test("{T}: Add {C} pays generic for any spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Interplanar Beacon")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
