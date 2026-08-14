package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bloodvial Purveyor (VOW #98).
 *
 *   5/4 Flying, trample.
 *   Whenever an opponent casts a spell, that player creates a Blood token.
 *   Whenever this creature attacks, it gets +1/+0 until end of turn for each Blood token defending
 *   player controls.
 *
 * Exercises the opponent-casts → that-player-gets-Blood trigger (the Blood is controlled by the
 * opponent, not the Purveyor's controller) and the attack pump reading the *defending* player's
 * Blood-token count.
 */
class BloodvialPurveyorScenarioTest : ScenarioTestBase() {

    init {
        context("Bloodvial Purveyor") {

            test("when an opponent casts a spell, that opponent creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodvial Purveyor", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // The opponent (Player2) casts a spell; the trigger gives *that player* the Blood.
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                game.resolveStack()

                withClue("the casting opponent controls exactly one Blood token") {
                    val blood = game.findPermanents("Blood")
                    blood.size shouldBe 1
                    game.state.getEntity(blood.first())!!
                        .get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()!!
                        .playerId shouldBe game.player2Id
                }
            }

            test("attacking, it gets +1/+0 for each Blood token the defending player controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodvial Purveyor", tapped = false, summoningSickness = false)
                    // The defending player (Player2) controls two Blood tokens.
                    .withCardOnBattlefield(2, "Blood", isToken = true)
                    .withCardOnBattlefield(2, "Blood", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val purveyor = game.findPermanent("Bloodvial Purveyor")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Bloodvial Purveyor" to 2))
                withClue("declaring the Purveyor as attacker succeeds: ${attack.error}") {
                    attack.error shouldBe null
                }
                game.resolveStack()

                withClue("+1/+0 per defending Blood: 5 base + 2 → 7 power, toughness unchanged") {
                    game.state.projectedState.getPower(purveyor) shouldBe 7
                    game.state.projectedState.getToughness(purveyor) shouldBe 4
                }
            }
        }
    }
}
