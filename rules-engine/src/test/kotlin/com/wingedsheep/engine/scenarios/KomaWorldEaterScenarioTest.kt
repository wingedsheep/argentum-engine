package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Koma, World-Eater (FDN #121) — {3}{G}{G}{U}{U} Legendary Creature — Serpent 8/12.
 *
 * "This spell can't be countered."
 * "Trample, ward {4}"
 * "Whenever Koma deals combat damage to a player, create four 3/3 blue Serpent creature tokens
 *  named Koma's Coil."
 *
 * The keyword layer (trample/ward) and the can't-be-countered flag are covered by the compiled-card
 * snapshot; this test exercises the one behavioral ability — the combat-damage-to-a-player trigger
 * that spawns four Koma's Coil tokens.
 */
class KomaWorldEaterScenarioTest : ScenarioTestBase() {

    init {
        context("Koma, World-Eater") {

            test("has trample in projected state") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Koma, World-Eater", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val koma = game.findPermanent("Koma, World-Eater")!!
                withClue("Koma should have trample") {
                    game.state.projectedState.hasKeyword(koma, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("dealing combat damage to a player creates four Koma's Coil tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Koma, World-Eater", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val attack = game.declareAttackers(mapOf("Koma, World-Eater" to 2))
                withClue("Koma should be able to attack: ${attack.error}") {
                    attack.error shouldBe null
                }

                // Player2 declares no blockers; combat damage auto-resolves and the trigger creates tokens.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Player2 took 8 combat damage from Koma (8/12)") {
                    game.getLifeTotal(2) shouldBe 12
                }
                withClue("Koma's combat damage created four 3/3 Koma's Coil tokens under its controller") {
                    game.findPermanents("Koma's Coil").size shouldBe 4
                }
            }
        }
    }
}
