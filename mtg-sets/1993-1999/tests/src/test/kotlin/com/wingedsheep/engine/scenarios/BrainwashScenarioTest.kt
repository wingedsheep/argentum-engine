package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Brainwash.
 *
 * A non-zero attack tax makes `declareAttackers` *pause* for the attacking player to confirm before
 * any of their mana is tapped — it does not error — so "the tax was charged" shows up as a pending
 * decision, not as a rejected declaration. That is what the first two cases assert, and the reason
 * blocking is asserted by the *absence* of such a pause: `appliesToBlocking = false` is the whole
 * difference from Myr Prototype's printed "can't attack or block unless…", and a card modelled on
 * that wording would stop to charge here.
 */
class BrainwashScenarioTest : ScenarioTestBase() {

    init {
        context("Brainwash") {

            test("declaring the enchanted creature as an attacker stops to charge the {3}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(2, "Brainwash", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                withClue("the Aura's tax stopped the declaration to charge for it") {
                    game.hasPendingDecision() shouldBe true
                }
                withClue("and with no lands there is nothing to pay it with") {
                    game.state.getBattlefield().none {
                        game.state.getEntity(it)
                            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                            ?.typeLine?.isLand == true
                    } shouldBe true
                }
            }

            test("the charge still happens when the controller can afford it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(2, "Brainwash", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                withClue("it still pauses to charge, but now the {3} is payable") {
                    game.hasPendingDecision() shouldBe true
                }
            }

            test("blocking is free — the tax is attack-only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    // The Brainwashed creature belongs to the *defending* player here, with no mana.
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Brainwash", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Craw Wurm" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Brainwash says nothing about blocking") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Craw Wurm"))).error shouldBe null
                }
                withClue("and charges nothing, so the block never stops to be paid for") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
