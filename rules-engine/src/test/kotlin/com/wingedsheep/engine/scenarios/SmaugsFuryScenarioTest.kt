package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Smaug's Fury (HOB) — {1}{R} Instant.
 * "Target creature gets +3/+0 and gains reach and first strike until end of turn."
 *
 * All three riders land on the same creature: a power-only pump (toughness must not move) plus
 * two granted keywords. The reach grant is then shown to actually let the creature block a flier.
 */
class SmaugsFuryScenarioTest : ScenarioTestBase() {

    init {
        context("Smaug's Fury") {

            test("grants +3/+0, reach and first strike to the target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Smaug's Fury")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                withClue("Centaur Courser is a plain 3/3 beforehand") {
                    game.state.projectedState.getPower(courser) shouldBe 3
                    game.state.projectedState.hasKeyword(courser, Keyword.REACH) shouldBe false
                    game.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false
                }

                game.castSpell(1, "Smaug's Fury", targetId = courser).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("+3/+0 — power moves, toughness does not") {
                    game.state.projectedState.getPower(courser) shouldBe 6
                    game.state.projectedState.getToughness(courser) shouldBe 3
                }
                withClue("both keywords are granted") {
                    game.state.projectedState.hasKeyword(courser, Keyword.REACH) shouldBe true
                    game.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe true
                }
            }

            test("without the spell a ground creature cannot block the flier (control)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Old Thrush is a 1/2 flier attacking into a ground blocker.
                    .withCardOnBattlefield(1, "Old Thrush")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Old Thrush" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("no reach — the block is illegal") {
                    game.declareBlockers(mapOf("Centaur Courser" to listOf("Old Thrush")))
                        .error shouldNotBe null
                }
            }

            test("the granted reach lets that same creature block the flier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Thrush")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInHand(2, "Smaug's Fury")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(1)
                    // Player 2 holds priority during Player 1's main phase to cast the instant.
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(2, "Smaug's Fury", targetId = courser).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.state.projectedState.hasKeyword(courser, Keyword.REACH) shouldBe true

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Old Thrush" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Centaur Courser" to listOf("Old Thrush")))
                withClue("reach granted this turn makes the block legal: ${block.error}") {
                    block.error shouldBe null
                }
            }
        }
    }
}
