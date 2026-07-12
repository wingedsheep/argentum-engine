package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Bloodcrazed Socialite (VOW #288) — {3}{B} Creature — Vampire, 3/3.
 *
 *   Menace
 *   When this creature enters, create a Blood token.
 *   Whenever this creature attacks, you may sacrifice a Blood token. If you do, it gets +2/+2
 *   until end of turn.
 *
 * Exercises the ETB Blood token creation and the attack-trigger gated sacrifice: paying by
 * sacrificing the Blood token pumps the attacker +2/+2; declining leaves it unbuffed.
 */
class BloodcrazedSocialiteScenarioTest : ScenarioTestBase() {

    init {
        context("Bloodcrazed Socialite") {

            test("entering the battlefield creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bloodcrazed Socialite")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Bloodcrazed Socialite").error shouldBe null
                game.resolveStack()

                withClue("a Blood token is created on entering the battlefield") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }

            test("attacking and sacrificing the Blood token gives +2/+2 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodcrazed Socialite", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val socialite = game.findPermanent("Bloodcrazed Socialite")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Bloodcrazed Socialite" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger offers a yes/no to sacrifice the Blood token") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the Blood token was sacrificed") {
                    game.findPermanents("Blood").size shouldBe 0
                }
                withClue("Bloodcrazed Socialite gets +2/+2 (becomes 5/5)") {
                    game.state.projectedState.getPower(socialite) shouldBe 5
                    game.state.projectedState.getToughness(socialite) shouldBe 5
                }
            }

            test("declining the sacrifice leaves the attacker at its base stats") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodcrazed Socialite", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val socialite = game.findPermanent("Bloodcrazed Socialite")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Bloodcrazed Socialite" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("the Blood token is not sacrificed") {
                    game.findPermanents("Blood").size shouldBe 1
                }
                withClue("Bloodcrazed Socialite stays at 3/3") {
                    game.state.projectedState.getPower(socialite) shouldBe 3
                    game.state.projectedState.getToughness(socialite) shouldBe 3
                }
            }
        }
    }
}
