package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Crumble (ATQ #32).
 *
 * "{G} Instant — Destroy target artifact. It can't be regenerated. That artifact's
 *  controller gains life equal to its mana value."
 *
 * Su-Chi has mana value 4 and is controlled by player 2; player 1 casts Crumble at it, so
 * player 2 (the artifact's controller) gains 4 life and the artifact is destroyed.
 */
class CrumbleScenarioTest : ScenarioTestBase() {

    init {
        test("Crumble destroys the artifact and its controller gains life equal to its mana value") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "Crumble")
                .withLandsOnBattlefield(1, "Forest", 1)
                // Su-Chi: artifact creature, mana value 4, controlled by player 2.
                .withCardOnBattlefield(2, "Su-Chi")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val suChi = game.findPermanent("Su-Chi")!!

            val cast = game.castSpell(1, "Crumble", targetId = suChi)
            withClue("Casting Crumble at Su-Chi should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Su-Chi (the targeted artifact) should be destroyed") {
                game.isOnBattlefield("Su-Chi") shouldBe false
            }
            withClue("The artifact's controller (player 2) should gain 4 life (Su-Chi's mana value)") {
                game.getLifeTotal(2) shouldBe 24
            }
            withClue("The caster (player 1) should not gain life") {
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
