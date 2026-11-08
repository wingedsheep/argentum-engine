package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cultist's Staff (EMN #194) — {2} Artifact — Equipment.
 *
 *   Equipped creature gets +2/+2.
 *   Equip {3}
 */
class CultistsStaffScenarioTest : ScenarioTestBase() {

    private val equipAbilityId =
        cardRegistry.getCard("Cultist's Staff")!!.activatedAbilities.first { it.isEquipAbility }.id

    init {
        context("Cultist's Staff") {

            test("equip {3} gives the equipped creature +2/+2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cultist's Staff")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val staff = game.findPermanent("Cultist's Staff")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("unequipped, the Staff does nothing") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }

                val equip = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = staff,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
        }
    }
}
