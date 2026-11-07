package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cathar's Shield (EMN #192) — {0} Artifact — Equipment.
 *
 *   Equipped creature gets +0/+3 and has vigilance.
 *   Equip {3}
 */
class CatharsShieldScenarioTest : ScenarioTestBase() {

    private val equipAbilityId =
        cardRegistry.getCard("Cathar's Shield")!!.activatedAbilities.first { it.isEquipAbility }.id

    init {
        context("Cathar's Shield") {

            test("equip {3} gives the equipped creature +0/+3 and vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cathar's Shield")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shield = game.findPermanent("Cathar's Shield")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("unequipped, the Shield does nothing") {
                    game.state.projectedState.getToughness(bears) shouldBe 2
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                }

                val equip = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = shield,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.getToughness(bears) shouldBe 5
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
