package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goblin Researcher (J22) — {3}{R} 3/3 Goblin Wizard.
 *
 * "When this creature enters, exile the top card of your library. During any turn you attacked
 * with this creature, you may play that card."
 *
 * The permission is gated on the Researcher having attacked this turn, re-checked on every query:
 * closed before it attacks, open once it has (even after it has died in combat, per the ruling),
 * closed again on a later turn it doesn't attack, and never opened by another creature attacking.
 */
class GoblinResearcherScenarioTest : ScenarioTestBase() {

    private fun setup(withBears: Boolean = false): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Goblin Researcher")
            .withLandsOnBattlefield(1, "Mountain", 5)
            .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        if (withBears) builder.withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        repeat(4) { builder.withCardInLibrary(1, "Lightning Bolt") }
        repeat(4) { builder.withCardInLibrary(2, "Mountain") }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Casts the Researcher, resolves it and its enters trigger, and returns the exiled card. */
    private fun castResearcher(game: TestGame): EntityId {
        game.castSpell(1, "Goblin Researcher").error shouldBe null
        game.resolveStack()
        val exiled = game.state.getExile(game.player1Id).single()
        withClue("the enters trigger exiled the top card of the library") {
            game.state.getEntity(exiled)?.get<CardComponent>()?.name shouldBe "Lightning Bolt"
        }
        return exiled
    }

    init {
        context("Goblin Researcher") {
            test("the exiled card is playable only during a turn the Researcher attacked") {
                val game = setup()
                val exiled = castResearcher(game)

                withClue("it hasn't attacked this turn, so the card can't be played") {
                    canCast(game, exiled) shouldBe false
                }

                advanceTo(game, game.player1Id, Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("next turn, before attacking, the card still can't be played") {
                    canCast(game, exiled) shouldBe false
                }

                advanceTo(game, game.player1Id, Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Researcher" to 2)).error shouldBe null
                withClue("once the Researcher has attacked, the card can be played") {
                    canCast(game, exiled) shouldBe true
                }
            }

            test("the card stays playable that turn after the Researcher dies in combat, but not the next") {
                val game = setup()
                val exiled = castResearcher(game)

                advanceTo(game, game.player1Id, Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Researcher" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Goblin Researcher"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the Researcher traded with Hill Giant") {
                    game.isOnBattlefield("Goblin Researcher") shouldBe false
                }
                withClue("it attacked this turn, so the card is still playable after it left") {
                    canCast(game, exiled) shouldBe true
                }

                advanceTo(game, game.player1Id, Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("a later turn it didn't attack closes the window; the card stays exiled") {
                    canCast(game, exiled) shouldBe false
                    game.state.getExile(game.player1Id).contains(exiled) shouldBe true
                }
            }

            test("attacking with a different creature doesn't open the window") {
                val game = setup(withBears = true)
                val exiled = castResearcher(game)

                advanceTo(game, game.player1Id, Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                withClue("only the Researcher attacking counts") {
                    canCast(game, exiled) shouldBe false
                }
            }
        }
    }

    private fun canCast(game: TestGame, cardId: EntityId): Boolean =
        game.getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId }

    /**
     * Advance to the next [phase]/[step] of [activePlayer]'s turn, always leaving the current step
     * first. The opponent's attack declaration is submitted empty, since passUntilPhase stops at it.
     */
    private fun advanceTo(game: TestGame, activePlayer: EntityId, phase: Phase, step: Step) {
        var guard = 0
        do {
            check(guard++ < 30) { "Could not reach $phase/$step for $activePlayer" }
            if (game.state.phase == phase && game.state.step == step) {
                if (step == Step.DECLARE_ATTACKERS) game.declareAttackers(emptyMap())
                game.passPriority()
            }
            game.passUntilPhase(phase, step)
        } while (game.state.activePlayerId != activePlayer)
    }
}
