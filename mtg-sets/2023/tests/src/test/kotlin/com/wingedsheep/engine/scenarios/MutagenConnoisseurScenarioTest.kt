package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mutagen Connoisseur — 0/5 flying, vigilance; "+1/+0 for each transformed permanent you control."
 *
 * Smoldering Werewolf ({4}{R}{R}: Transform this creature) is the transforming permanent: on its
 * front face it is a double-faced card but not a transformed permanent (CR 701.27g), and flipping
 * it is what makes it count.
 */
class MutagenConnoisseurScenarioTest : ScenarioTestBase() {

    private fun TestGame.transformWerewolf() {
        val wolf = findPermanent("Smoldering Werewolf")!!
        val abilityId = cardRegistry.getCard("Smoldering Werewolf")!!.activatedAbilities.first().id
        execute(ActivateAbility(playerId = player1Id, sourceId = wolf, abilityId = abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Mutagen Connoisseur") {

            test("a front-face double-faced permanent doesn't count; transforming it does") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mutagen Connoisseur")
                    .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val connoisseur = game.findPermanent("Mutagen Connoisseur")!!
                withClue("front face up is not transformed (CR 701.27g)") {
                    game.state.projectedState.getPower(connoisseur) shouldBe 0
                }

                game.transformWerewolf()
                withClue("the flipped Erupting Dreadwolf is a transformed permanent") {
                    game.state.projectedState.getPower(connoisseur) shouldBe 1
                    game.state.projectedState.getToughness(connoisseur) shouldBe 5
                }
            }

            test("an opponent's transformed permanent doesn't count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Mutagen Connoisseur")
                    .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.transformWerewolf()
                game.state.projectedState.getPower(game.findPermanent("Mutagen Connoisseur")!!) shouldBe 0
            }

            test("a Siege cast transformed counts once its back face is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mutagen Connoisseur")
                    .withCardOnBattlefield(1, "Invasion of Innistrad")
                    .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
                    .withCardOnBattlefield(1, "Shivan Dragon", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val connoisseur = game.findPermanent("Mutagen Connoisseur")!!
                game.checkStateBasedActions()
                withClue("the Siege on its front face is not transformed") {
                    game.state.projectedState.getPower(connoisseur) shouldBe 0
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf(
                        "Serra Angel" to "Invasion of Innistrad",
                        "Shivan Dragon" to "Invasion of Innistrad",
                    )
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

                withClue("Deluge of the Dead entered back face up — a transformed permanent") {
                    game.isOnBattlefield("Deluge of the Dead") shouldBe true
                    game.state.projectedState.getPower(connoisseur) shouldBe 1
                }
            }
        }
    }
}
