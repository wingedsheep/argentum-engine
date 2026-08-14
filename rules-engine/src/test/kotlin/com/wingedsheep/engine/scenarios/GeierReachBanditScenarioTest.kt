package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Geier Reach Bandit // Vildin-Pack Alpha (SOI #159).
 *
 *   Front (3/2, haste) — "At the beginning of each upkeep, if no spells were cast last turn,
 *                         transform this creature."
 *   Back  (4/3)        — "Whenever a Werewolf you control enters, you may transform it."
 *                        "At the beginning of each upkeep, if a player cast two or more spells
 *                         last turn, transform this creature."
 *
 * The back face is the first `TransformEffect` in the codebase aimed at something other than
 * `EffectTarget.Self`, so these tests pin that it flips the Werewolf that *entered* — not the
 * Alpha — and that declining the "may" leaves the newcomer on its front face.
 */
class GeierReachBanditScenarioTest : ScenarioTestBase() {

    init {
        context("Vildin-Pack Alpha") {

            test("accepting the may-transform flips the Werewolf that entered, not the Alpha") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vildin-Pack Alpha", summoningSickness = false)
                    .withCardInHand(1, "Hinterland Logger")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val alpha = game.findPermanent("Vildin-Pack Alpha")!!

                game.castSpell(1, "Hinterland Logger").error shouldBe null
                game.resolveStack()

                withClue("the enters trigger asks whether to transform the newcomer") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Hinterland Logger flipped to its back face, Timber Shredder") {
                    game.isOnBattlefield("Timber Shredder") shouldBe true
                    game.isOnBattlefield("Hinterland Logger") shouldBe false
                }
                withClue("the Alpha itself is untouched — the effect targets the triggering entity") {
                    game.state.getEntity(alpha)!!.get<CardComponent>()!!.name shouldBe "Vildin-Pack Alpha"
                }
            }

            test("declining the may-transform leaves the newcomer on its front face") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vildin-Pack Alpha", summoningSickness = false)
                    .withCardInHand(1, "Hinterland Logger")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hinterland Logger").error shouldBe null
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                withClue("declined — still Hinterland Logger") {
                    game.isOnBattlefield("Hinterland Logger") shouldBe true
                    game.isOnBattlefield("Timber Shredder") shouldBe false
                }
            }

            test("a non-Werewolf entering does not trigger the Alpha") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vildin-Pack Alpha", summoningSickness = false)
                    .withCardInHand(1, "Thraben Inspector")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thraben Inspector").error shouldBe null
                game.resolveStack()

                withClue("a Human Soldier entering never asks the Werewolf question") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                }
                game.isOnBattlefield("Thraben Inspector") shouldBe true
            }
        }
    }
}
