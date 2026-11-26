package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vraska the Unseen (RTR #208, {3}{B}{G}, loyalty 5).
 *
 * Pins the two abilities whose correctness depends on how the engine runs the script:
 *  - the +1 delayed trigger is scoped to damage dealt *to Vraska*: on the opponent's turn the
 *    creature that hits her is destroyed while one hitting her controller is not, and it still
 *    fires when that damage takes her to 0 loyalty;
 *  - the −7 Assassins make the player they deal combat damage to lose the game.
 */
class VraskaTheUnseenScenarioTest : ScenarioTestBase() {

    init {
        context("the +1") {

            test("only the creature that deals combat damage to Vraska is destroyed") {
                val game = plusOneThenOpponentsCombat(startingLoyalty = 5)

                game.declareAttackersWithPermanentTargets(
                    playerAttackers = mapOf("Hill Giant" to 1),
                    permanentAttackers = mapOf("Grizzly Bears" to "Vraska the Unseen"),
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("Grizzly Bears hit Vraska and were destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant hit the player, not Vraska, and survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                loyalty(game, game.findPermanent("Vraska the Unseen")!!) shouldBe 4
            }

            test("the attacker is destroyed even when its damage kills Vraska") {
                val game = plusOneThenOpponentsCombat(startingLoyalty = 1)

                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Hill Giant" to "Vraska the Unseen"),
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                game.isOnBattlefield("Vraska the Unseen") shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }

        context("the −7") {

            test("an Assassin that deals combat damage to a player makes that player lose the game") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vraska the Unseen")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vraska = game.findPermanent("Vraska the Unseen")!!
                setLoyalty(game, vraska, 7)
                activate(game, vraska, index = 2)
                game.resolveStack()

                game.findPermanents("Assassin Token").size shouldBe 3

                // Through the opponent's turn to our own, when the tokens can attack.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Assassin Token" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()

                withClue("one 1-damage hit is enough — the opponent loses at 19 life") {
                    game.getLifeTotal(2) shouldBe 19
                    game.state.gameOver shouldBe true
                    game.state.winnerId shouldBe game.player1Id
                }
            }
        }
    }

    /** Vraska +1 on our turn, then advance to the opponent's declare-attackers step. */
    private fun plusOneThenOpponentsCombat(startingLoyalty: Int): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Vraska the Unseen")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val vraska = game.findPermanent("Vraska the Unseen")!!
        setLoyalty(game, vraska, startingLoyalty)
        activate(game, vraska, index = 0)
        game.resolveStack()
        loyalty(game, vraska) shouldBe startingLoyalty + 1

        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        game.state.activePlayerId shouldBe game.player2Id
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        return game
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard("Vraska the Unseen")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
