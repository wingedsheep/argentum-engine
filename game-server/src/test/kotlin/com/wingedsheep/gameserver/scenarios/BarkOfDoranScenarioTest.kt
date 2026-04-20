package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bark of Doran (ECL).
 *
 * Bark of Doran: {1}{W} Artifact — Equipment
 *   Equipped creature gets +0/+1.
 *   As long as equipped creature's toughness is greater than its power, it assigns combat
 *   damage equal to its toughness rather than its power.
 *   Equip {1}
 */
class BarkOfDoranScenarioTest : ScenarioTestBase() {

    init {
        context("Bark of Doran combat damage substitution") {

            test("equipped 1/2 becomes 1/3 and assigns 3 damage to defending player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bark of Doran")
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false) // 1/2
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val barkId = game.findPermanent("Bark of Doran")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("Bark of Doran")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                val equipResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = barkId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )
                withClue("Equip activation should succeed: ${equipResult.error}") {
                    equipResult.error shouldBe null
                }
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Devoted Hero" to 2))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Defending player should have taken 3 (toughness) combat damage, not 1 (power)") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("equipped 4/2 becomes 4/3 and assigns 4 damage (power, not toughness)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bark of Doran")
                    .withCardOnBattlefield(1, "Alpine Grizzly", summoningSickness = false) // 4/2
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val barkId = game.findPermanent("Bark of Doran")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                val cardDef = cardRegistry.getCard("Bark of Doran")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                val equipResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = barkId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzlyId))
                    )
                )
                withClue("Equip activation should succeed: ${equipResult.error}") {
                    equipResult.error shouldBe null
                }
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Alpine Grizzly" to 2))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Toughness (3) is not greater than power (4), so attacker deals 4 damage") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }
        }
    }
}
