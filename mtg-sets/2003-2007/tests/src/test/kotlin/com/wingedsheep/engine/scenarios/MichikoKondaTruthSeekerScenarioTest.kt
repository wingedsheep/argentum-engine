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
 * Michiko Konda, Truth Seeker (SOK #19) — "Whenever a source an opponent controls deals damage
 * to you, that player sacrifices a permanent of their choice."
 *
 * "That player" is the source's controller, read last-known for a burn spell that has already
 * left the stack; the damage amount doesn't matter, and your own sources never trigger it.
 */
class MichikoKondaTruthSeekerScenarioTest : ScenarioTestBase() {

    init {
        context("Michiko Konda, Truth Seeker") {

            test("an opponent's burn spell to you makes that opponent sacrifice a permanent of their choice") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Michiko Konda, Truth Seeker")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                game.resolveStack()

                withClue("Alice took Shock's 2 damage") { game.getLifeTotal(1) shouldBe 18 }

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Bob, the Shock's controller, makes the choice") {
                    decision.playerId shouldBe game.player2Id
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Bob's chosen creature was sacrificed and his land kept") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Mountain") shouldBe true
                }
                withClue("Alice's permanents are untouched") {
                    game.isOnBattlefield("Michiko Konda, Truth Seeker") shouldBe true
                }
            }

            test("an opponent's creature dealing combat damage to you triggers it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Michiko Konda, Truth Seeker", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.getPendingDecision() is CombatResolutionDecision) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("Alice took the Bears' 2 damage") { game.getLifeTotal(1) shouldBe 18 }

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Bob, the attacking creature's controller, makes the choice") {
                    decision.playerId shouldBe game.player2Id
                }
                game.selectCards(listOf(forest)).error shouldBe null
                game.resolveStack()

                withClue("Bob sacrificed the chosen land, keeping the attacker") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("damage from a source you control doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Michiko Konda, Truth Seeker")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
                game.resolveStack()

                withClue("Alice shocked herself") { game.getLifeTotal(1) shouldBe 18 }
                withClue("no sacrifice prompt — the source is Alice's own") {
                    game.getPendingDecision() shouldBe null
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Michiko Konda, Truth Seeker") shouldBe true
                game.isOnBattlefield("Mountain") shouldBe true
            }
        }
    }
}
