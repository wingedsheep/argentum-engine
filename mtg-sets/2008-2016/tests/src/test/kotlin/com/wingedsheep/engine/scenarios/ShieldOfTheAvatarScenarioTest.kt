package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shield of the Avatar (M15 #230) — {1} Artifact — Equipment.
 *
 *   If a source would deal damage to equipped creature, prevent X of that damage, where X is the
 *   number of creatures you control.
 *   Equip {2}
 */
class ShieldOfTheAvatarScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    /** Player 1: Hill Giant (3/3) wearing the Shield, plus [bears] Grizzly Bears. Player 2 holds a Lightning Bolt. */
    private fun shielded(bears: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardAttachedTo(1, "Shield of the Avatar", "Hill Giant")
        repeat(bears) { builder = builder.withCardOnBattlefield(1, "Grizzly Bears") }
        return builder
            .withCardInHand(2, "Lightning Bolt")
            .withLandsOnBattlefield(2, "Mountain", 1)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    private fun TestGame.boltGiant() {
        val giant = findPermanent("Hill Giant")!!
        castSpell(2, "Lightning Bolt", giant).error shouldBe null
        resolveStack()
    }

    init {
        context("Shield of the Avatar") {

            test("alone, the equipped creature counts itself: X = 1, Bolt deals 2") {
                val game = shielded(bears = 0)
                game.boltGiant()
                game.damageOn(game.findPermanent("Hill Giant")!!) shouldBe 2
            }

            test("with two more creatures X = 3 and the Bolt is fully prevented") {
                val game = shielded(bears = 2)
                game.boltGiant()
                game.damageOn(game.findPermanent("Hill Giant")!!) shouldBe 0
            }

            test("only the equipped creature is shielded") {
                val game = shielded(bears = 1)
                val bear = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Lightning Bolt", bear).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("equip {2} moves the shield onto the chosen creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shield of the Avatar")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val shield = game.findPermanent("Shield of the Avatar")!!
                val giant = game.findPermanent("Hill Giant")!!
                val equipId = cardRegistry.getCard("Shield of the Avatar")!!.activatedAbilities.first { it.isEquipAbility }.id

                val equip = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = shield,
                        abilityId = equipId,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                )
                withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // Two creatures: X = 2, so the Bolt deals 1.
                game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.damageOn(giant) shouldBe 1
            }
        }
    }
}
