package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Riptide Laboratory.
 *
 * Card reference:
 * - Riptide Laboratory: Land
 *   {T}: Add {C}.
 *   {1}{U}, {T}: Return target Wizard you control to its owner's hand.
 */
class RiptideLaboratoryScenarioTest : ScenarioTestBase() {

    init {
        context("Riptide Laboratory bounce ability") {

            test("returns a Wizard you control to your hand") {
                val game = scenario()
                    .withPlayers("Wizard Player", "Opponent")
                    .withCardOnBattlefield(1, "Riptide Laboratory")
                    .withCardOnBattlefield(1, "Information Dealer") // Human Wizard
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lab = game.findPermanent("Riptide Laboratory")!!
                val wizard = game.findPermanent("Information Dealer")!!

                val cardDef = cardRegistry.getCard("Riptide Laboratory")!!
                val bounceAbility = cardDef.script.activatedAbilities[1] // second ability

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lab,
                        abilityId = bounceAbility.id,
                        targets = listOf(ChosenTarget.Permanent(wizard))
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Information Dealer should no longer be on the battlefield") {
                    game.isOnBattlefield("Information Dealer") shouldBe false
                }

                withClue("Information Dealer should be in hand") {
                    game.isInHand(1, "Information Dealer") shouldBe true
                }
            }

            test("cannot target a non-Wizard creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Riptide Laboratory")
                    .withCardOnBattlefield(1, "Glory Seeker") // Human Soldier, not a Wizard
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lab = game.findPermanent("Riptide Laboratory")!!
                val soldier = game.findPermanent("Glory Seeker")!!

                val cardDef = cardRegistry.getCard("Riptide Laboratory")!!
                val bounceAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lab,
                        abilityId = bounceAbility.id,
                        targets = listOf(ChosenTarget.Permanent(soldier))
                    )
                )

                withClue("Should fail targeting a non-Wizard") {
                    result.error shouldNotBe null
                }
            }

            test("cannot target an opponent's Wizard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Riptide Laboratory")
                    .withCardOnBattlefield(2, "Information Dealer") // Opponent's Wizard
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lab = game.findPermanent("Riptide Laboratory")!!
                val opponentWizard = game.findPermanent("Information Dealer")!!

                val cardDef = cardRegistry.getCard("Riptide Laboratory")!!
                val bounceAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lab,
                        abilityId = bounceAbility.id,
                        targets = listOf(ChosenTarget.Permanent(opponentWizard))
                    )
                )

                withClue("Should fail targeting opponent's Wizard") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
