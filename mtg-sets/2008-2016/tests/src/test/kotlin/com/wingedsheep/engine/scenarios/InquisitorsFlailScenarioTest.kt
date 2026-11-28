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
 * Inquisitor's Flail (ISD #227) — {2} Artifact — Equipment.
 *
 * "If equipped creature would deal combat damage, it deals double that damage instead.
 * If another creature would deal combat damage to equipped creature, it deals double that damage
 * to equipped creature instead. Equip {2}"
 *
 * Pins both halves — damage dealt *by* the equipped creature and combat damage dealt *to* it by
 * another creature — and that neither touches noncombat damage.
 */
class InquisitorsFlailScenarioTest : ScenarioTestBase() {

    private val sorcererPingAbilityId
        get() = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities.single().id

    private fun TestGame.damageMarkedOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.finishCombatDamage() {
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        resolveStack()
        if (state.pendingDecision != null) {
            submitDefaultCombatDamage()
            resolveStack()
        }
    }

    init {
        test("combat damage the equipped creature deals to a player is doubled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Inquisitor's Flail", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val before = game.getLifeTotal(2)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.finishCombatDamage()

            withClue("a 2/2 with the Flail deals 4") {
                game.getLifeTotal(2) shouldBe before - 4
            }
        }

        test("in a block, the equipped creature deals double and is dealt double") {
            // Indomitable Ancients (2/10) carries the Flail and is blocked by Centaur Courser (3/3):
            // its 2 becomes 4 (the Courser dies), and the Courser's 3 becomes 6 marked on it.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Indomitable Ancients", summoningSickness = false)
                .withCardAttachedTo(1, "Inquisitor's Flail", "Indomitable Ancients")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ancients = game.findPermanent("Indomitable Ancients")!!

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Indomitable Ancients" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Indomitable Ancients"))).error shouldBe null
            game.finishCombatDamage()
            game.checkStateBasedActions()

            withClue("the Courser's 3 combat damage to the equipped creature is doubled to 6") {
                game.damageMarkedOn(ancients) shouldBe 6
            }
            withClue("the Ancients' 2 combat damage is doubled to 4, killing the 3/3 Courser") {
                game.isOnBattlefield("Centaur Courser") shouldBe false
            }
        }

        fun ping(game: TestGame, from: String, at: String) {
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent(from)!!,
                    abilityId = sorcererPingAbilityId,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent(at)!!)),
                )
            ).error shouldBe null
            game.resolveStack()
        }

        test("noncombat damage from the equipped creature is not doubled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                .withCardAttachedTo(1, "Inquisitor's Flail", "Prodigal Sorcerer")
                .withCardOnBattlefield(2, "Indomitable Ancients")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            ping(game, "Prodigal Sorcerer", "Indomitable Ancients")

            game.damageMarkedOn(game.findPermanent("Indomitable Ancients")!!) shouldBe 1
        }

        test("noncombat damage from another creature to the equipped creature is not doubled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardAttachedTo(2, "Inquisitor's Flail", "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            ping(game, "Prodigal Sorcerer", "Hill Giant")

            game.damageMarkedOn(game.findPermanent("Hill Giant")!!) shouldBe 1
        }
    }
}
