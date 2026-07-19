package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Embereth Veteran (WOE #127) — {R} 2/1 Human Knight.
 *
 * "{1}, Sacrifice this creature: Create a Young Hero Role token attached to another target creature."
 *
 * Exercises the new Young Hero Role token, whose granted ability puts a +1/+1 counter on the
 * enchanted creature when it attacks *if its toughness is 3 or less* (an intervening-if gate).
 */
class EmberethVeteranScenarioTest : ScenarioTestBase() {

    private fun emberethAbilityId() =
        cardRegistry.getCard("Embereth Veteran")!!.activatedAbilities.first().id

    init {
        context("Embereth Veteran") {

            test("activating {1}, Sacrifice creates a Young Hero Role on another creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Embereth Veteran")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val veteran = game.findPermanent("Embereth Veteran")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = veteran,
                        abilityId = emberethAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Embereth Veteran was sacrificed as a cost") {
                    game.findPermanent("Embereth Veteran") shouldBe null
                }
                withClue("A Young Hero Role enters attached to Grizzly Bears") {
                    game.findPermanent("Young Hero Role") shouldNotBe null
                }
            }

            test("enchanted creature with toughness 3 or less gets a +1/+1 counter when it attacks") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Embereth Veteran")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 — toughness ≤ 3
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val veteran = game.findPermanent("Embereth Veteran")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = veteran,
                        abilityId = emberethAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                val projector = StateProjector()
                withClue("2/2 (toughness ≤ 3) attacking gains a +1/+1 counter → 3/3") {
                    projector.getProjectedPower(game.state, bears) shouldBe 3
                    projector.getProjectedToughness(game.state, bears) shouldBe 3
                }
            }

            test("enchanted creature with toughness greater than 3 gets no counter when it attacks") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Embereth Veteran")
                    .withCardOnBattlefield(1, "Archive Dragon") // 4/6 — toughness > 3
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val veteran = game.findPermanent("Embereth Veteran")!!
                val dragon = game.findPermanent("Archive Dragon")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = veteran,
                        abilityId = emberethAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(dragon))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Archive Dragon" to 2)).error shouldBe null
                game.resolveStack()

                val projector = StateProjector()
                withClue("toughness 6 > 3, so the intervening-if fails and no counter is added") {
                    projector.getProjectedPower(game.state, dragon) shouldBe 4
                    projector.getProjectedToughness(game.state, dragon) shouldBe 6
                }
            }
        }
    }
}
