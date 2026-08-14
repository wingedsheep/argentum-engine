package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shrill Howler // Howling Chorus (EMN #168).
 *
 *   Front (3/1) — "Creatures with power less than this creature's power can't block it."
 *                 "{5}{G}: Transform this creature."
 *   Back  (3/5) — same evasion clause, plus "Whenever this creature deals combat damage to a player,
 *                 create a 3/2 colorless Eldrazi Horror creature token."
 *
 * Covers the attacker-side power evasion on the front face, the activated flip, and the back face's
 * combat-damage token trigger.
 */
class ShrillHowlerScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature("Test Runt", ManaCost.parse("{2}"), emptySet(), power = 2, toughness = 2)
        )
        cardRegistry.register(
            CardDefinition.creature("Test Bruiser", ManaCost.parse("{3}"), emptySet(), power = 3, toughness = 3)
        )

        context("Shrill Howler") {

            test("a power-2 creature can't block the power-3 Shrill Howler") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shrill Howler", summoningSickness = false)
                    .withCardOnBattlefield(2, "Test Runt")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Shrill Howler" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Test Runt" to listOf("Shrill Howler")))
                withClue("power 2 < power 3 — the block is illegal") {
                    block.error shouldNotBe null
                }
            }

            test("a power-3 creature can block it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shrill Howler", summoningSickness = false)
                    .withCardOnBattlefield(2, "Test Bruiser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Shrill Howler" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Test Bruiser" to listOf("Shrill Howler")))
                withClue("equal power is not *less* power — the block is legal: ${block.error}") {
                    block.error shouldBe null
                }
            }

            test("{5}{G} transforms it into a 3/5 Howling Chorus that keeps the evasion") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shrill Howler", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardOnBattlefield(2, "Test Runt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val howler = game.findPermanent("Shrill Howler")!!
                val abilityId = cardRegistry.getCard("Shrill Howler")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = howler, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("transformed to the back face") {
                    game.state.getEntity(howler)!!.get<CardComponent>()!!.name shouldBe "Howling Chorus"
                    game.state.projectedState.getPower(howler) shouldBe 3
                    game.state.projectedState.getToughness(howler) shouldBe 5
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Howling Chorus" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Test Runt" to listOf("Howling Chorus")))
                withClue("the back face carries the same evasion clause") {
                    block.error shouldNotBe null
                }
            }

            test("Howling Chorus makes a 3/2 Eldrazi Horror on combat damage to a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shrill Howler", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val howler = game.findPermanent("Shrill Howler")!!
                val abilityId = cardRegistry.getCard("Shrill Howler")!!.activatedAbilities.first().id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = howler, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Howling Chorus" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("3 combat damage went through unblocked") {
                    game.getLifeTotal(2) shouldBe 17
                }
                // The CreateToken facade names type-only tokens "<types> Token".
                val tokens = game.findAllPermanents("Eldrazi Horror Token")
                withClue("the combat-damage trigger created one 3/2 Eldrazi Horror") {
                    tokens.size shouldBe 1
                    game.state.projectedState.getPower(tokens.first()) shouldBe 3
                    game.state.projectedState.getToughness(tokens.first()) shouldBe 2
                }
            }
        }
    }
}
