package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Vampires' Vengeance (VOW #339) — {2}{R} Instant.
 *
 *   Vampires' Vengeance deals 2 damage to each non-Vampire creature. Create a Blood token.
 *
 * Exercises the mass damage sweep filtered by subtype: a non-Vampire creature (Hill Giant, 3/3)
 * takes 2 marked damage and survives, while a Vampire creature (Voldaren Epicure, 1/1) is
 * unaffected. A Blood token is created for the caster.
 */
class VampiresVengeanceScenarioTest : ScenarioTestBase() {

    init {
        context("Vampires' Vengeance — 2 damage to each non-Vampire creature + Blood token") {

            test("non-Vampire creatures take 2 damage; Vampires are unaffected; a Blood token is created") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vampires' Vengeance")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    // Non-Vampire creature — must take 2 damage.
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    // Vampire creature — must be unaffected.
                    .withCardOnBattlefield(2, "Voldaren Epicure", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val vampire = game.findPermanent("Voldaren Epicure")!!

                game.castSpell(1, "Vampires' Vengeance").error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (non-Vampire, 3/3) takes 2 marked damage and survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    val damage = game.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0
                    damage shouldBe 2
                }
                withClue("Voldaren Epicure (Vampire) is unaffected — no marked damage") {
                    game.isOnBattlefield("Voldaren Epicure") shouldBe true
                    val damage = game.state.getEntity(vampire)?.get<DamageComponent>()?.amount ?: 0
                    damage shouldBe 0
                }
                withClue("A Blood token is created") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}
