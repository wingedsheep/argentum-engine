package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ghosts of the Innocent (RAV #20) — {5}{W}{W} Creature — Spirit, 4/5.
 *
 *   If a source would deal damage to a permanent or player, it deals half that damage, rounded
 *   down, to that permanent or player instead.
 *
 * The new [com.wingedsheep.sdk.scripting.HalveDamage] replacement, and the four things its 2005
 * rulings actually pin down: the halving is *multiplicative* (so it cannot be a `ModifyDamageAmount`
 * with a negative modifier), it rounds down hard enough that a 1-damage source deals nothing at
 * all, it scopes to neither a source nor a recipient — combat and burn, creatures and players, its
 * own controller included — and two copies compound rather than overlapping, because each
 * applicable replacement applies once (CR 616.1).
 */
class GhostsOfTheInnocentScenarioTest : ScenarioTestBase() {

    private fun TestGame.markedDamage(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.attackWith(creature: String) {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf(creature to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        resolveStack()
        if (getPendingDecision() is CombatResolutionDecision) {
            submitDefaultCombatDamage()
            resolveStack()
        }
    }

    init {
        context("Ghosts of the Innocent") {

            test("burn dealt to a player is halved, rounded down") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ghosts of the Innocent")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("3 damage halved and rounded down is 1") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("a 1-damage source deals no damage at all") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ghosts of the Innocent")
                    .withCardOnBattlefield(1, "Mons's Goblin Raiders")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWith("Mons's Goblin Raiders")

                withClue("half of 1, rounded down, is 0 — the 1/1 connects for nothing") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("halving is scoped to neither controller — it shrinks damage dealt to its own side too") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ghosts of the Innocent")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the card names no source and no recipient, so its controller is halved too") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("damage dealt to a creature is halved, so a 3-damage burn spell spares a 2/2") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ghosts of the Innocent")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("a permanent is a legal recipient for the halving, not just a player") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.markedDamage(bears) shouldBe 1
                }
            }

            test("combat damage is halved as well") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ghosts of the Innocent")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWith("Craw Wurm")

                withClue("the Wurm's 6 power lands as 3") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("two copies compound — 6 damage becomes 3, then 1") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ghosts of the Innocent")
                    .withCardOnBattlefield(1, "Ghosts of the Innocent")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.attackWith("Craw Wurm")
                val order = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                order.playerId shouldBe game.player2Id
                order.options.size shouldBe 2
                game.submitDecision(OptionChosenResponse(order.id, 0)).error shouldBe null
                game.getPendingDecision() shouldBe null


                withClue("each replacement applies once (CR 616.1): 6 → 3 → 1, not 6 → 3") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }
    }
}
