package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lullmage's Domination — {X}{U}{U}{U} Sorcery.
 *   "This spell costs {3} less to cast if it targets a creature whose controller has eight or
 *    more cards in their graveyard. Gain control of target creature with mana value X."
 *
 * The reduction is `FixedIfAnyTargetMatches` over `controllerMeets(CardsInGraveyardAtLeast(8))`:
 * the graveyard counted is the *targeted creature's controller's*, not the caster's.
 */
class LullmagesDominationScenarioTest : ScenarioTestBase() {

    private fun TestGame.castDomination(target: com.wingedsheep.sdk.model.EntityId, x: Int) =
        execute(
            CastSpell(
                player1Id,
                state.getHand(player1Id).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == "Lullmage's Domination"
                },
                listOf(ChosenTarget.Permanent(target)),
                xValue = x,
            )
        )

    private fun board(islands: Int, opponentGraveyard: Int, myGraveyard: Int = 0): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Lullmage's Domination")
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(opponentGraveyard) { builder = builder.withCardInGraveyard(2, "Forest") }
        repeat(myGraveyard) { builder = builder.withCardInGraveyard(1, "Island") }
        return builder.build()
    }

    init {
        test("costs {3} less when the target's controller has eight cards in their graveyard") {
            val game = board(islands = 3, opponentGraveyard = 8)
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("X=2 makes {2}{U}{U}{U}; the {3} reduction leaves {U}{U}{U}") {
                game.castDomination(bears, x = 2).error shouldBe null
            }
            game.resolveStack()

            game.state.projectedState.getController(bears) shouldBe game.player1Id
        }

        test("no reduction when the target's controller has only seven cards in their graveyard") {
            val game = board(islands = 3, opponentGraveyard = 7)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castDomination(bears, x = 2).error shouldNotBe null
            game.state.projectedState.getController(bears) shouldBe game.player2Id
        }

        test("the caster's own graveyard doesn't count — it's the creature's controller's") {
            val game = board(islands = 3, opponentGraveyard = 0, myGraveyard = 8)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castDomination(bears, x = 2).error shouldNotBe null
        }

        test("full price steals the creature without the graveyard condition") {
            val game = board(islands = 5, opponentGraveyard = 0)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castDomination(bears, x = 2).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getController(bears) shouldBe game.player1Id
        }

        test("the target's mana value must equal X") {
            val game = board(islands = 5, opponentGraveyard = 8)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castDomination(bears, x = 1).error shouldNotBe null
        }
    }
}
