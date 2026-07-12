package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.CanAttackDespiteDefenderThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Steelclad Spirit (VOW #80) — {1}{U} Creature — Spirit, 3/3, Defender.
 *
 *   Defender
 *   Whenever an enchantment you control enters, this creature can attack this turn as though it
 *   didn't have defender.
 *
 * Exercises the enchantment-enters trigger granting the temporary can-attack-despite-defender
 * marker: with Defender it cannot attack before any enchantment enters, but once an enchantment
 * you control enters it gains the marker and can attack this turn.
 */
class SteelcladSpiritScenarioTest : ScenarioTestBase() {

    init {
        context("Steelclad Spirit — enchantment-enters grants attack despite Defender") {

            test("cannot attack with Defender before any enchantment enters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Steelclad Spirit", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val result = game.declareAttackers(mapOf("Steelclad Spirit" to 2))
                withClue("Defender blocks the attack before any enchantment enters") {
                    (result.error != null) shouldBe true
                }
            }

            test("an enchantment you control entering lets it attack this turn despite Defender") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Steelclad Spirit", tapped = false, summoningSickness = false)
                    .withCardInHand(1, "Test Enchantment") // {1}{W}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spirit = game.findPermanent("Steelclad Spirit")!!

                val cast = game.castSpell(1, "Test Enchantment")
                withClue("Casting Test Enchantment should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("the trigger fired — Steelclad Spirit gains the can-attack-despite-defender marker") {
                    game.state.getEntity(spirit)
                        ?.get<CanAttackDespiteDefenderThisTurnComponent>().shouldNotBeNull()
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Steelclad Spirit" to 2))
                withClue("after the trigger fired it can attack despite Defender: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("Steelclad Spirit is now an attacker") {
                    game.state.getEntity(spirit)?.get<AttackingComponent>().shouldNotBeNull()
                }
            }
        }
    }
}
