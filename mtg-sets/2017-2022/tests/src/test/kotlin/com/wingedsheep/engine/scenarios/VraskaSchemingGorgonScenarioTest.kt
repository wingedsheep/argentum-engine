package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vraska, Scheming Gorgon (RIX #197, {4}{B}{B}, Loyalty 5).
 *
 *   −10: Until end of turn, creatures you control gain deathtouch and "Whenever this creature deals
 *   damage to an opponent, that player loses the game."
 *
 * Covers the granted trigger firing off combat damage to the opponent, and not firing when the
 * creature only deals damage to a blocking creature (which deathtouch kills).
 */
class VraskaSchemingGorgonScenarioTest : ScenarioTestBase() {

    init {
        context("Vraska, Scheming Gorgon's −10") {

            test("a creature you control that deals combat damage to the opponent makes them lose the game") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraska, Scheming Gorgon")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vraska = game.findPermanent("Vraska, Scheming Gorgon")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                setLoyalty(game, vraska, 10)
                activate(game, vraska, index = 2)
                game.resolveStack()

                withClue("Grizzly Bears gained deathtouch") {
                    game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }

            test("damage dealt only to a blocking creature doesn't trigger, but deathtouch kills the blocker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraska, Scheming Gorgon")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vraska = game.findPermanent("Vraska, Scheming Gorgon")!!
                setLoyalty(game, vraska, 10)
                activate(game, vraska, index = 2)
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("the opponent was never dealt damage") { game.state.gameOver shouldBe false }
                withClue("deathtouch killed the 3/3 blocker") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard("Vraska, Scheming Gorgon")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
            )
        ).error shouldBe null
    }

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
