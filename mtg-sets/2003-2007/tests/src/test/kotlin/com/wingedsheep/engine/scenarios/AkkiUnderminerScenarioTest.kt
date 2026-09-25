package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Akki Underminer (CHK #155) — "Whenever this creature deals combat damage to a player, that
 * player sacrifices a permanent of their choice."
 *
 * The damaged player chooses, and any permanent type is eligible — a land counts as much as a
 * creature.
 */
class AkkiUnderminerScenarioTest : ScenarioTestBase() {

    init {
        context("Akki Underminer") {

            test("the damaged player sacrifices a permanent of their choice, lands included") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Akki Underminer")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Akki Underminer" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.getPendingDecision() is CombatResolutionDecision) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("Bob took the Underminer's 1 damage") { game.getLifeTotal(2) shouldBe 19 }

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Bob, the damaged player, makes the choice") {
                    decision.playerId shouldBe game.player2Id
                }
                game.selectCards(listOf(forest)).error shouldBe null
                game.resolveStack()

                withClue("the chosen land was sacrificed and the creature kept") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
