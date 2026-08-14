package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ecl.cards.TimidShieldbearer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Curious Colossus.
 *
 * "When this creature enters, each creature target opponent controls loses all abilities,
 * becomes a Coward in addition to its other types, and has base power and toughness 1/1."
 *
 * The load-bearing bit is CR 613.4: the trigger sets *base* power and toughness in layer 7b,
 * so every +N/+N modification (layer 7c) still applies on top of it — no matter whether that
 * modification started to apply before or after the Colossus resolved. The card's own ruling
 * spells this out: "Effects that modify a creature's power and/or toughness … will apply to
 * the creature no matter when they started to take effect."
 */
class CuriousColossusScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun game() = scenario()
        .withPlayers("Alice", "Bob")
        // Bob (player 2) is the Colossus caster; Alice (player 1) is the target opponent.
        .withCardInHand(2, "Curious Colossus")
        .withLandsOnBattlefield(2, "Plains", 7)
        .withCardOnBattlefield(1, "Timid Shieldbearer", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Curious Colossus") {
            test("sets each of the target opponent's creatures to base 1/1 and strips their abilities") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val shieldbearer = game.findPermanent("Timid Shieldbearer")!!

                game.castSpell(2, "Curious Colossus").error shouldBe null
                game.resolveStack()

                val after = projector.project(game.state)
                withClue("Alice's creatures all become 1/1") {
                    after.getPower(bears) shouldBe 1
                    after.getToughness(bears) shouldBe 1
                    after.getPower(shieldbearer) shouldBe 1
                    after.getToughness(shieldbearer) shouldBe 1
                }
                withClue("Bob's own Colossus is untouched — the ability only hits the target opponent") {
                    val colossus = game.findPermanent("Curious Colossus")!!
                    after.getPower(colossus) shouldBe 7
                    after.getToughness(colossus) shouldBe 7
                }
            }

            test("a +1/+1 pump that resolved BEFORE the Colossus still applies on top of base 1/1") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val shieldbearer = game.findPermanent("Timid Shieldbearer")!!

                // Bob casts the Colossus; Alice responds with the pump, so the pump resolves
                // first (layer 7c, earlier timestamp) and the Colossus's 1/1 lands on top.
                game.castSpell(2, "Curious Colossus").error shouldBe null
                game.passPriority()

                val pump = TimidShieldbearer.activatedAbilities[0].id
                val activation = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = shieldbearer, abilityId = pump)
                )
                withClue("Activating Timid Shieldbearer should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val after = projector.project(game.state)
                withClue("Base P/T is set in layer 7b; the earlier +1/+1 still applies in layer 7c") {
                    after.getPower(bears) shouldBe 2
                    after.getToughness(bears) shouldBe 2
                    after.getPower(shieldbearer) shouldBe 2
                    after.getToughness(shieldbearer) shouldBe 2
                }
            }

            test("a pump that resolves AFTER the Colossus also applies on top of base 1/1") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(2, "Curious Colossus").error shouldBe null
                game.resolveStack()
                if (game.state.priorityPlayerId != game.player1Id) game.passPriority()

                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val after = projector.project(game.state)
                withClue("Base 1/1 from layer 7b, +3/+3 from layer 7c") {
                    after.getPower(bears) shouldBe 4
                    after.getToughness(bears) shouldBe 4
                }
            }

            test("Timid Shieldbearer loses its activated ability to the Colossus") {
                val game = game()
                val shieldbearer = game.findPermanent("Timid Shieldbearer")!!

                game.castSpell(2, "Curious Colossus").error shouldBe null
                game.resolveStack()

                withClue("Its ability was removed by 'loses all abilities'") {
                    game.getLegalActions(1)
                        .none { (it.action as? ActivateAbility)?.sourceId == shieldbearer } shouldBe true
                }
            }
        }
    }
}
