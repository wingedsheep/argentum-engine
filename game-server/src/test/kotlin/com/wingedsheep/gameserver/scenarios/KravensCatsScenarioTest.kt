package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kraven's Cats.
 *
 * Card reference:
 * - Kraven's Cats ({1}{G}): Creature — Cat Villain, 2/2
 *   "{2}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn."
 */
class KravensCatsScenarioTest : ScenarioTestBase() {

    init {
        context("Kraven's Cats {2}{G} pump — +2/+2 until end of turn, once per turn") {

            test("activating the ability makes Kraven's Cats 4/4 until end of turn") {
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardOnBattlefield(1, "Kraven's Cats")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cats = game.findPermanent("Kraven's Cats")!!
                val ability = cardRegistry.getCard("Kraven's Cats")!!.script.activatedAbilities.first()

                val projectedBefore = game.state.projectedState
                withClue("Base stats should be 2/2") {
                    projectedBefore.getPower(cats) shouldBe 2
                    projectedBefore.getToughness(cats) shouldBe 2
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cats,
                        abilityId = ability.id
                    )
                )
                withClue("First activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val projectedAfter = game.state.projectedState
                withClue("Kraven's Cats should be 4/4 after the pump") {
                    projectedAfter.getPower(cats) shouldBe 4
                    projectedAfter.getToughness(cats) shouldBe 4
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UNTAP)
                val projectedNextTurn = game.state.projectedState
                withClue("+2/+2 should expire during cleanup; Kraven's Cats back to 2/2 next turn") {
                    projectedNextTurn.getPower(cats) shouldBe 2
                    projectedNextTurn.getToughness(cats) shouldBe 2
                }
            }

            test("second activation in the same turn is rejected by the once-per-turn restriction") {
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardOnBattlefield(1, "Kraven's Cats")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cats = game.findPermanent("Kraven's Cats")!!
                val ability = cardRegistry.getCard("Kraven's Cats")!!.script.activatedAbilities.first()

                val first = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cats,
                        abilityId = ability.id
                    )
                )
                withClue("First activation should succeed: ${first.error}") {
                    first.error shouldBe null
                }
                game.resolveStack()

                val poolBefore = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()

                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cats,
                        abilityId = ability.id
                    )
                )
                withClue("Second activation should be rejected once-per-turn") {
                    second.error shouldNotBe null
                }

                withClue("Stats should remain 4/4 from the first activation") {
                    val projected = game.state.projectedState
                    projected.getPower(cats) shouldBe 4
                    projected.getToughness(cats) shouldBe 4
                }

                withClue("Rejected activation should not have spent mana") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>() shouldBe poolBefore
                }
            }
        }
    }
}
