package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Azgol // Ashen Reaper.
 *
 * Front: target player sacrifices a creature or planeswalker and loses 1 life. Back: "At the
 * beginning of your end step, put a +1/+1 counter on this creature if a permanent was put into a
 * graveyard from the battlefield this turn" — any permanent type, any player's, tokens included.
 */
class InvasionOfAzgolScenarioTest : ScenarioTestBase() {

    private fun TestGame.defeatSiegeAndCastBack() {
        checkStateBasedActions()
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackersWithPermanentTargets(
            permanentAttackers = mapOf("Serra Angel" to "Invasion of Azgol")
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

    private fun TestGame.reaperCounters(): Int =
        state.getEntity(findPermanent("Ashen Reaper")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun backFaceGame(extra: ScenarioBuilder.() -> ScenarioBuilder) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Invasion of Azgol")
        .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .extra()
        .build()

    init {
        context("front face — Invasion of Azgol") {
            test("target player sacrifices a creature and loses 1 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Azgol")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Azgol").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("the opponent's only creature is sacrificed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                game.getLifeTotal(2) shouldBe 19
            }
        }

        context("back face — Ashen Reaper") {
            test("a land put into a graveyard this turn earns a counter at your end step") {
                val game = backFaceGame {
                    withLandsOnBattlefield(2, "Forest", 1)
                        .withCardInHand(1, "Stone Rain")
                        .withLandsOnBattlefield(1, "Mountain", 3)
                }

                game.defeatSiegeAndCastBack()
                game.isOnBattlefield("Ashen Reaper") shouldBe true
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.castSpell(1, "Stone Rain", game.findPermanent("Forest")).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("a land is a permanent — Ashen Reaper counts every type") {
                    game.reaperCounters() shouldBe 1
                }
            }

            test("nothing put into a graveyard from the battlefield — no counter") {
                val game = backFaceGame { this }

                game.defeatSiegeAndCastBack()
                withClue("the defeated Siege was exiled, not put into a graveyard") {
                    game.isOnBattlefield("Ashen Reaper") shouldBe true
                }
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.reaperCounters() shouldBe 0
            }
        }
    }
}
