package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Firemind Vessel (WAR #237) — {4} Artifact.
 *
 *   This artifact enters tapped.
 *   {T}: Add two mana of different colors.
 */
class FiremindVesselScenarioTest : ScenarioTestBase() {

    init {
        test("it enters tapped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Firemind Vessel")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Firemind Vessel").error shouldBe null
            game.resolveStack()

            val vessel = game.findPermanent("Firemind Vessel").shouldNotBeNull()
            game.state.getEntity(vessel)?.has<TappedComponent>() shouldBe true
        }

        test("one tap pays both colored pips of a two-color spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Firemind Vessel")
                .withCardInHand(1, "Cruel Celebrant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("{W}{B} comes entirely from the Vessel's two different colors") {
                game.castSpell(1, "Cruel Celebrant").error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield("Cruel Celebrant") shouldBe true
        }

        test("one tap can't pay two pips of the same color") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Firemind Vessel")
                .withCardInHand(1, "Jace's Projection")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // {2}{U}{U}: the Plains cover {2}, but the Vessel makes one {U} and a *different* color.
            game.castSpell(1, "Jace's Projection").error.shouldNotBeNull()
            game.isInHand(1, "Jace's Projection") shouldBe true
        }
    }
}
