package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Detonate (ATQ #24).
 *
 * "{X}{R} Sorcery — Destroy target artifact with mana value X. It can't be regenerated.
 *  Detonate deals X damage to that artifact's controller."
 *
 * Regression focus: the X damage must hit the artifact's CONTROLLER, not the caster nor
 * (when they differ) the owner. Su-Chi is an artifact creature with mana value 4, so casting
 * Detonate with X = 4 targeting an opponent's Su-Chi must deal 4 damage to that opponent and
 * destroy the artifact.
 */
class DetonateScenarioTest : ScenarioTestBase() {

    init {
        test("Detonate deals X damage to the targeted artifact's controller and destroys it") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "Detonate")
                // X{R} with X = 4 → need 5 red mana sources.
                .withLandsOnBattlefield(1, "Mountain", 5)
                // Su-Chi: artifact creature, mana value 4, controlled (and owned) by player 2.
                .withCardOnBattlefield(2, "Su-Chi")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val suChi = game.findPermanent("Su-Chi")!!

            val cast = game.castXSpell(1, "Detonate", xValue = 4, targetId = suChi)
            withClue("Casting Detonate (X=4) at Su-Chi should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Su-Chi (the targeted artifact) should be destroyed") {
                game.isOnBattlefield("Su-Chi") shouldBe false
            }
            withClue("The caster (player 1) should take no damage") {
                game.getLifeTotal(1) shouldBe 20
            }
            withClue("The artifact's controller (player 2) should lose 4 life from Detonate's damage") {
                game.getLifeTotal(2) shouldBe 16
            }
        }
    }
}
