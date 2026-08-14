package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Neurok Spy (MRD #44) — 2/2 for {2}{U}, "can't be blocked as long as defending player controls an
 * artifact."
 *
 * Covers the `CantBeBlockedIfDefenderControls` static this card introduced. The load-bearing cases
 * are the ones a naive "any opponent controls an artifact" wiring would get wrong:
 *
 *  - the evasion is off when the defender has no artifact (the block must be *legal*),
 *  - it is off when the *attacking* player is the one holding the artifacts,
 *  - and it is on when the defender's artifact is an artifact *creature*, which is the same card
 *    type and so counts.
 */
class NeurokSpyScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature("Test Blocker", ManaCost.parse("{2}"), emptySet(), power = 2, toughness = 2)
        )

        context("Neurok Spy") {

            test("can't be blocked while the defending player controls an artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Neurok Spy", summoningSickness = false)
                    .withCardOnBattlefield(2, "Test Blocker")
                    .withCardOnBattlefield(2, "Bonesplitter")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Neurok Spy" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Test Blocker" to listOf("Neurok Spy")))
                withClue("the defender controls Bonesplitter — the block is illegal") {
                    block.error shouldNotBe null
                }
            }

            test("CAN be blocked when the defending player controls no artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Neurok Spy", summoningSickness = false)
                    .withCardOnBattlefield(2, "Test Blocker")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Neurok Spy" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Test Blocker" to listOf("Neurok Spy")))
                withClue("no artifact on the defender's side — the block is legal: ${block.error}") {
                    block.error shouldBe null
                }
            }

            test("the Spy's controller holding the artifacts does not switch the evasion on") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Neurok Spy", summoningSickness = false)
                    // Two artifacts, both on the *attacking* side. "Defending player controls" is
                    // defender-relative, so these must not count.
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Fireshrieker")
                    .withCardOnBattlefield(2, "Test Blocker")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Neurok Spy" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Test Blocker" to listOf("Neurok Spy")))
                withClue("the artifacts are the attacker's — the block is legal: ${block.error}") {
                    block.error shouldBe null
                }
            }

            test("an artifact creature the defender controls counts as an artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Neurok Spy", summoningSickness = false)
                    // Steel Wall is an Artifact Creature — Wall: it is both the blocker and the
                    // artifact that turns the evasion on, so it can never legally block the Spy.
                    .withCardOnBattlefield(2, "Steel Wall")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Neurok Spy" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Steel Wall" to listOf("Neurok Spy")))
                withClue("an artifact creature is still an artifact — the block is illegal") {
                    block.error shouldNotBe null
                }
            }
        }
    }
}
