package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Task Force (MMQ) — "Whenever this creature becomes the target of a spell or ability, it gets
 * +0/+3 until end of turn."
 *
 * The trigger goes on the stack above whatever targeted it and resolves first, so a Lightning Bolt
 * aimed at the 1/3 meets a 1/6. It fires for abilities as well as spells, and the bonus wears off
 * at end of turn.
 */
class TaskForceScenarioTest : ScenarioTestBase() {

    init {
        context("becomes the target of a spell") {
            test("the +0/+3 resolves before the spell, so Lightning Bolt doesn't kill it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Task Force")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val taskForce = game.findPermanent("Task Force")
                taskForce.shouldNotBeNull()

                game.castSpell(2, "Lightning Bolt", taskForce).error shouldBe null
                game.resolveStack()

                withClue("the trigger resolved first, making it a 1/6 that survives 3 damage") {
                    game.isOnBattlefield("Task Force") shouldBe true
                    game.state.projectedState.getPower(taskForce) shouldBe 1
                    game.state.projectedState.getToughness(taskForce) shouldBe 6
                }
            }
        }

        context("becomes the target of an ability") {
            test("also triggers, and the bonus ends at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Task Force")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val taskForce = game.findPermanent("Task Force")
                taskForce.shouldNotBeNull()
                val tim = game.findPermanent("Prodigal Sorcerer")!!
                val abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!
                    .script.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = tim,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, taskForce))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("an ability targeting it triggers the +0/+3 too") {
                    game.isOnBattlefield("Task Force") shouldBe true
                    game.state.projectedState.getToughness(taskForce) shouldBe 6
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("the bonus lasted only until end of turn") {
                    game.isOnBattlefield("Task Force") shouldBe true
                    game.state.projectedState.getToughness(taskForce) shouldBe 3
                }
            }
        }
    }
}
