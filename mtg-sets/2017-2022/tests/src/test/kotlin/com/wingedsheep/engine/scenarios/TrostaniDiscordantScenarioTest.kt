package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.grn.cards.TrostaniDiscordant
import com.wingedsheep.mtg.sets.definitions.m20.cards.AgentOfTreachery
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Trostani Discordant (GRN #208) — "At the beginning of your end step, each player gains control
 * of all creatures they own."
 *
 * First card to hand each creature back through `GiveControl` + `Player.OwnerOfIterationEntity`:
 * a permanent steal (Agent of Treachery) is undone at Trostani's controller's end step, while the
 * creatures already under their owner's control stay put.
 */
class TrostaniDiscordantScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(TrostaniDiscordant)
        cardRegistry.register(AgentOfTreachery)

        context("Trostani Discordant") {

            fun build() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Trostani Discordant")
                .withCardInHand(1, "Agent of Treachery")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("returns a stolen creature to its owner at your end step") {
                val game = build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val trostani = game.findPermanent("Trostani Discordant")!!

                game.castSpell(1, "Agent of Treachery").error shouldBe null
                game.resolveStack()
                game.state.pendingDecision?.let {
                    game.submitDecision(TargetsResponse(it.id, mapOf(0 to listOf(bears))))
                }
                game.resolveStack()
                val agent = game.findPermanent("Agent of Treachery")!!

                withClue("The stolen Bears get Trostani's anthem while Player controls them") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.state.projectedState.getPower(bears) shouldBe 3
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Grizzly Bears go back to the opponent who owns them") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
                withClue("Creatures already with their owners stay where they are") {
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                    game.state.projectedState.getController(agent) shouldBe game.player1Id
                    game.state.projectedState.getController(trostani) shouldBe game.player1Id
                }
            }
        }
    }
}
