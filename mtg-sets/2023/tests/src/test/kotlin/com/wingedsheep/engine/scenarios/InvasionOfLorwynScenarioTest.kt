package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Invasion of Lorwyn // Winnowing Forces.
 *
 * Front: "destroy target non-Elf creature an opponent controls with power X or less, where X is the
 * number of lands you control" — `powerAtMostDynamic(landsYouControl())`, so the cap moves with the
 * land count, and an Elf or a creature over the cap is never offered. Back: a `*`/`*` equal to the
 * number of lands you control.
 */
class InvasionOfLorwynScenarioTest : ScenarioTestBase() {

    private fun TestGame.castInvasion(): ChooseTargetsDecision {
        castSpell(1, "Invasion of Lorwyn").error shouldBe null
        resolveStack()
        return getPendingDecision() as ChooseTargetsDecision
    }

    init {
        context("front face — the enters trigger") {

            test("only non-Elf creatures an opponent controls with power at most your land count are targets") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Lorwyn")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Shivan Dragon")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Scaled Wurm")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val decision = game.castInvasion()
                withClue("6 lands: power 5 and 6 qualify; the 7-power Wurm, the Elf and your own Bears don't") {
                    decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(
                        game.findPermanent("Shivan Dragon")!!,
                        game.findPermanent("Craw Wurm")!!,
                    )
                }

                game.selectTargets(listOf(game.findPermanent("Craw Wurm")!!)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Craw Wurm") shouldBe false
                game.isInGraveyard(2, "Craw Wurm") shouldBe true
                game.isOnBattlefield("Shivan Dragon") shouldBe true
            }

            test("the cap is the current land count — more lands widen it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Lorwyn")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(2, "Scaled Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val decision = game.castInvasion()
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(game.findPermanent("Scaled Wurm")!!)

                game.selectTargets(listOf(game.findPermanent("Scaled Wurm")!!)).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Scaled Wurm") shouldBe true
            }

            test("the cap is re-read on resolution — losing a land in response makes the target illegal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Lorwyn")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(2, "Scaled Wurm")
                    .withCardOnBattlefield(2, "Strip Mine", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castInvasion()
                game.selectTargets(listOf(game.findPermanent("Scaled Wurm")!!)).error shouldBe null
                game.passPriority().error shouldBe null

                // With the trigger on the stack, the opponent Strip Mines a Swamp: 7 lands become 6.
                val stripMine = cardRegistry.getCard("Strip Mine")!!.script.activatedAbilities[1]
                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = game.findPermanent("Strip Mine")!!,
                        abilityId = stripMine.id,
                        targets = listOf(entityIdToChosenTarget(game.state, game.findPermanent("Swamp")!!)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Swamp") shouldBe true
                game.state.stack.isEmpty() shouldBe true
                withClue("power 7 is over the new cap of 6, so the trigger's only target is illegal") {
                    game.isOnBattlefield("Scaled Wurm") shouldBe true
                }
            }
        }

        context("back face — Winnowing Forces") {

            test("its power and toughness each equal the number of lands you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Invasion of Lorwyn")
                    .withCardOnBattlefield(1, "Shivan Dragon", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Shivan Dragon" to "Invasion of Lorwyn")
                ).error shouldBe null
                var guard = 0
                while (game.state.pendingDecision == null && guard++ < 30) {
                    if (game.state.step == Step.DECLARE_BLOCKERS &&
                        game.state.getEntity(game.player2Id)
                            ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
                    ) {
                        game.declareNoBlockers()
                    } else {
                        game.passPriority()
                    }
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val forces = game.findPermanent("Winnowing Forces")!!
                withClue("4 lands you control — the opponent's Swamps don't count") {
                    game.state.projectedState.getPower(forces) shouldBe 4
                    game.state.projectedState.getToughness(forces) shouldBe 4
                }
            }
        }
    }
}
