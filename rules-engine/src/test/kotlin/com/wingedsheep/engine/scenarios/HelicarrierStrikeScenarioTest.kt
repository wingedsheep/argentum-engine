package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Helicarrier Strike (MSH #15) — {W} Instant.
 *
 *   Teamwork 2
 *   Helicarrier Strike deals 2 damage to target attacking or blocking creature. If this spell was
 *   cast using teamwork, it deals 4 damage to that creature instead.
 *
 * Both branches are pinned: the plain cast marks 2 damage on a 3/3 attacker, the teamwork cast
 * (CR 702.194a — tapping creatures with total power 2 or more) deals 4 and kills it. The target
 * restriction means a combat has to exist, so the test declares an attacker first.
 */
class HelicarrierStrikeScenarioTest : ScenarioTestBase() {

    init {
        context("Helicarrier Strike") {

            test("cast without teamwork deals 2 damage and taps nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Helicarrier Strike")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castSpell(1, "Helicarrier Strike", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("the 3/3 attacker survives with 2 marked damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                }
                withClue("no teamwork cost was declared, so nothing tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }
            }

            test("cast using teamwork deals 4 damage instead and taps the payers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Helicarrier Strike")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                // Grizzly Bears is a 2/2 — exactly the teamwork 2 threshold on its own.
                game.castSpellWithTeamwork(
                    1, "Helicarrier Strike", "Grizzly Bears", targetId = giant,
                ).error shouldBe null

                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true

                game.resolveStack()
                withClue("4 damage kills the 3/3 attacker") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
            }
        }
    }
}
