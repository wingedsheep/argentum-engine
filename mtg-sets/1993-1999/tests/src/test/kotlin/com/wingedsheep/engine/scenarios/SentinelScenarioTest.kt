package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sentinel — {0}: This creature's base toughness becomes equal to 1 plus the power of target
 * creature blocking or blocked by this creature. (This effect lasts indefinitely.)
 *
 * Proves the three things the script can't show on its own: the target must be combat-paired
 * with Sentinel, the toughness is set from the blocker's power at resolution (so Sentinel survives
 * the block), and the new base toughness outlives the turn.
 */
class SentinelScenarioTest : ScenarioTestBase() {

    private val sentinelAbilityId by lazy {
        cardRegistry.getCard("Sentinel")!!.activatedAbilities[0].id
    }

    init {
        context("Sentinel") {
            test("blocked by a 3/3, it becomes 1/4, survives combat, and keeps toughness 4 next turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sentinel", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sentinel" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Sentinel"))).error shouldBe null
                game.passPriority() // the defender passes; the attacker gets priority in the same step

                val sentinel = game.findPermanent("Sentinel")!!
                val giant = game.findPermanent("Hill Giant")!!
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sentinel,
                        abilityId = sentinelAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                )
                withClue("Sentinel's ability should target its blocker: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Base toughness becomes 1 + the Giant's power (3)") {
                    game.state.projectedState.getToughness(sentinel) shouldBe 4
                    game.state.projectedState.getPower(sentinel) shouldBe 1
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("A 1/4 Sentinel survives 3 damage from the Giant") {
                    game.isOnBattlefield("Sentinel") shouldBe true
                }

                // Through cleanup and into the opponent's turn — the effect lasts indefinitely.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("The new base toughness persists past end of turn") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.projectedState.getToughness(sentinel) shouldBe 4
                }
            }

            test("can't target a creature that isn't blocking or blocked by it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sentinel", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentinel = game.findPermanent("Sentinel")!!
                val giant = game.findPermanent("Hill Giant")!!
                val attempt = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sentinel,
                        abilityId = sentinelAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                )
                withClue("An unpaired creature is not a legal target") {
                    attempt.error shouldNotBe null
                }
                game.state.projectedState.getToughness(sentinel) shouldBe 1
            }
        }
    }
}
