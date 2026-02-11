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
 * Scenario test for Daru Encampment.
 *
 * Card reference:
 * - Daru Encampment: Land
 *   {T}: Add {C}.
 *   {W}, {T}: Target Soldier creature gets +1/+1 until end of turn.
 */
class DaruEncampmentScenarioTest : ScenarioTestBase() {

    init {
        context("Daru Encampment pump ability") {
            test("gives +1/+1 to a Soldier creature") {
                val game = scenario()
                    .withPlayers("Soldier Player", "Opponent")
                    .withCardOnBattlefield(1, "Daru Encampment")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 Human Soldier
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val encampment = game.findPermanent("Daru Encampment")!!
                val soldier = game.findPermanent("Glory Seeker")!!

                val cardDef = cardRegistry.getCard("Daru Encampment")!!
                val pumpAbility = cardDef.script.activatedAbilities[1] // second ability

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = encampment,
                        abilityId = pumpAbility.id,
                        targets = listOf(ChosenTarget.Permanent(soldier))
                    )
                )

                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Glory Seeker should now be 3/3 (base 2/2 + 1/+1)
                val clientState = game.getClientState(1)
                val soldierInfo = clientState.cards[soldier]
                withClue("Glory Seeker should be 3/3 after pump") {
                    soldierInfo shouldNotBe null
                    soldierInfo!!.power shouldBe 3
                    soldierInfo.toughness shouldBe 3
                }
            }

            test("cannot target a non-Soldier creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Daru Encampment")
                    .withCardOnBattlefield(1, "Elvish Warrior") // 2/3 Elf Warrior, not a Soldier
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val encampment = game.findPermanent("Daru Encampment")!!
                val elf = game.findPermanent("Elvish Warrior")!!

                val cardDef = cardRegistry.getCard("Daru Encampment")!!
                val pumpAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = encampment,
                        abilityId = pumpAbility.id,
                        targets = listOf(ChosenTarget.Permanent(elf))
                    )
                )

                withClue("Should fail targeting a non-Soldier") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
