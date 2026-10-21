package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A turn-keyed may-play window ([MayPlayExpiry.UntilControllerStep]) closes as its step *begins*
 * on the controller's qualifying turn, not at that turn's cleanup: "until your next end step"
 * (Rocco, Street Chef; Ob Nixilis, Captive Kingpin) can't be used during that end step. A
 * `Step.CLEANUP` window ("until the end of your next turn") still lasts through the end step.
 */
class StepKeyedMayPlayExpiryTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            listOf(
                card("Test Main-Phase Impulse") {
                    manaCost = "{G}"
                    typeLine = "Sorcery"
                    spell {
                        effect = Patterns.Exile.impulse(expiry = MayPlayExpiry.UntilNextEndStep)
                    }
                },
                card("Test End-Step Impulser") {
                    manaCost = "{G}"
                    typeLine = "Creature — Elf"
                    power = 1
                    toughness = 1
                    triggeredAbility {
                        trigger = Triggers.you.beginningOf(Step.END)
                        effect = Patterns.Exile.impulse(storeAs = "endStepExiled", expiry = MayPlayExpiry.UntilNextEndStep) then
                            Effects.GrantPlayWithoutPayingCost("endStepExiled", MayPlayExpiry.UntilNextEndStep)
                    }
                },
                card("Test Next-Turn Impulse") {
                    manaCost = "{G}"
                    typeLine = "Sorcery"
                    spell {
                        effect = Patterns.Exile.impulse(expiry = MayPlayExpiry.UntilEndOfNextTurn)
                    }
                },
            )
        )

        test("granted in your main phase, 'until your next end step' ends as this turn's end step begins") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test Main-Phase Impulse")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Main-Phase Impulse").error shouldBe null
            game.resolveStack()
            val bears = exiled(game, game.player1Id, "Grizzly Bears")
            withClue("playable for the rest of the main phase") { mayPlay(game, bears) shouldBe true }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("still playable in the postcombat main phase") { mayPlay(game, bears) shouldBe true }

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("the window closed as the end step began") { mayPlay(game, bears) shouldBe false }
        }

        test("granted at your end step, the window spans the opponent's turn and closes at your next end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test End-Step Impulser")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            val bears = exiled(game, game.player1Id, "Grizzly Bears")
            withClue("the end step the grant was made in is not 'your next end step'") {
                mayPlay(game, bears) shouldBe true
                hasWaiver(game, bears) shouldBe true
            }

            passUntilTurnOf(game, game.player2Id)
            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("an opponent's end step doesn't close it") { mayPlay(game, bears) shouldBe true }

            passUntilTurnOf(game, game.player1Id)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("still open through your next turn's main phases") {
                mayPlay(game, bears) shouldBe true
                hasWaiver(game, bears) shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("closed — with its paired free-cast waiver — as your next end step began") {
                mayPlay(game, bears) shouldBe false
                hasWaiver(game, bears) shouldBe false
            }
        }

        test("'until the end of your next turn' still lasts through that turn's end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test Next-Turn Impulse")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Next-Turn Impulse").error shouldBe null
            game.resolveStack()
            val bears = exiled(game, game.player1Id, "Grizzly Bears")

            passUntilTurnOf(game, game.player2Id)
            passUntilTurnOf(game, game.player1Id)
            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("a cleanup window is untouched by the end step") { mayPlay(game, bears) shouldBe true }

            passUntilTurnOf(game, game.player2Id)
            withClue("and gone once that turn's cleanup has run") { mayPlay(game, bears) shouldBe false }
        }
    }

    private fun mayPlay(game: TestGame, cardId: EntityId): Boolean =
        game.state.mayPlayPermissions.any { cardId in it.cardIds }

    private fun hasWaiver(game: TestGame, cardId: EntityId): Boolean =
        game.state.getEntity(cardId)?.get<PlayWithoutPayingCostComponent>() != null

    private fun exiled(game: TestGame, playerId: EntityId, name: String): EntityId =
        game.state.getExile(playerId).first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

    /** Pass priority until [playerId]'s upkeep. */
    private fun passUntilTurnOf(game: TestGame, playerId: EntityId) {
        var guard = 0
        do {
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            if (game.state.activePlayerId != playerId) game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        } while (game.state.activePlayerId != playerId && guard++ < 4)
    }
}
