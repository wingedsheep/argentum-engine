package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invasion of Ravnica // Guildpact Paragon.
 *
 * Front: exile target nonland permanent an opponent controls that isn't exactly two colors — a
 * two-color permanent is an illegal target, a three-color one is not. Back: casting a spell that's
 * exactly two colors digs six for a card that's exactly two colors; a monocolored spell does nothing.
 */
class InvasionOfRavnicaScenarioTest : ScenarioTestBase() {

    private fun TestGame.defeatSiegeAndCastBack() {
        checkStateBasedActions()
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackersWithPermanentTargets(
            permanentAttackers = mapOf("Serra Angel" to "Invasion of Ravnica")
        ).error shouldBe null
        var guard = 0
        while (state.pendingDecision == null && guard++ < 30) {
            if (state.step == Step.DECLARE_BLOCKERS &&
                state.getEntity(player2Id)
                    ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
            ) {
                declareNoBlockers()
            } else {
                passPriority()
            }
        }
        answerYesNo(true).error shouldBe null
        resolveStack()
    }

    private fun backFaceGame(extra: ScenarioBuilder.() -> ScenarioBuilder) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Invasion of Ravnica")
        .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .extra()
        .build()

    init {
        context("front face — Invasion of Ravnica") {
            test("a two-color permanent can't be targeted; a three-color one is exiled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Ravnica")
                    .withLandsOnBattlefield(1, "Wastes", 5)
                    .withCardOnBattlefield(2, "Watchwolf")
                    .withCardOnBattlefield(2, "Sprouting Thrinax")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Ravnica").error shouldBe null
                game.resolveStack()

                withClue("Watchwolf is exactly two colors (green and white)") {
                    game.selectTargets(listOf(game.findPermanent("Watchwolf")!!)).error shouldNotBe null
                }
                game.selectTargets(listOf(game.findPermanent("Sprouting Thrinax")!!)).error shouldBe null
                game.resolveStack()

                withClue("a three-color permanent isn't exactly two colors") {
                    game.isOnBattlefield("Sprouting Thrinax") shouldBe false
                }
                game.isOnBattlefield("Watchwolf") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        context("back face — Guildpact Paragon") {
            test("casting a two-color spell digs six for a two-color card") {
                val game = backFaceGame {
                    withCardInHand(1, "Watchwolf")
                        .withCardInLibrary(1, "Boros Guildmage")
                        .withCardInLibrary(1, "Grizzly Bears")
                        .withCardInLibrary(1, "Island")
                        .withCardInLibrary(1, "Island")
                }

                game.defeatSiegeAndCastBack()
                game.isOnBattlefield("Guildpact Paragon") shouldBe true
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val guildmage = game.findCardsInLibrary(1, "Boros Guildmage").first()
                game.castSpell(1, "Watchwolf").error shouldBe null
                game.resolveStack()
                game.getPendingDecision() ?: error("expected the look-six selection")
                game.selectCards(listOf(guildmage)).error shouldBe null
                game.resolveStack()

                withClue("the two-color Boros Guildmage was revealed into hand") {
                    game.findCardsInHand(1, "Boros Guildmage").size shouldBe 1
                }
                game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
                game.isOnBattlefield("Watchwolf") shouldBe true
            }

            test("a monocolored spell doesn't trigger it") {
                val game = backFaceGame {
                    withCardInHand(1, "Grizzly Bears")
                        .withCardInLibrary(1, "Boros Guildmage")
                }

                game.defeatSiegeAndCastBack()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("no look-six decision for a one-color spell") {
                    game.getPendingDecision() shouldBe null
                }
                game.findCardsInHand(1, "Boros Guildmage").size shouldBe 0
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
