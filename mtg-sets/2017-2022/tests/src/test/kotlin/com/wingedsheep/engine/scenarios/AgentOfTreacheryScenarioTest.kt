package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.AgentOfTreachery
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Agent of Treachery (M20 #43) — "When this creature enters, gain control of target permanent.
 * At the beginning of your end step, if you control three or more permanents you don't own, draw
 * three cards."
 *
 * The steal targets any permanent (a land here) and is permanent; the end-step draw counts
 * permanents you control but don't own, so the Agent itself never counts and the threshold is
 * exactly three.
 */
class AgentOfTreacheryScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(AgentOfTreachery)

        context("Agent of Treachery") {

            fun TestGame.castAgentStealing(victim: EntityId) {
                castSpell(1, "Agent of Treachery").error shouldBe null
                resolveStack()
                state.pendingDecision?.let {
                    submitDecision(TargetsResponse(it.id, mapOf(0 to listOf(victim))))
                }
                resolveStack()
            }

            fun TestGame.actOfTreason(victim: EntityId) {
                castSpell(1, "Act of Treason", victim).error shouldBe null
                resolveStack()
            }

            fun build(actsOfTreason: Int) = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Agent of Treachery")
                .apply { repeat(actsOfTreason) { withCardInHand(1, "Act of Treason") } }
                .withLandsOnBattlefield(1, "Island", 7)
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardOnBattlefield(2, "Forest")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("steals a land on entry and keeps it past end of turn") {
                val game = build(actsOfTreason = 0)
                val forest = game.findPermanent("Forest")!!

                game.castAgentStealing(forest)

                withClue("Agent of Treachery can take any permanent, not only a creature") {
                    game.state.projectedState.getController(forest) shouldBe game.player1Id
                }

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)

                withClue("The steal has no duration — it survives the turn") {
                    game.state.projectedState.getController(forest) shouldBe game.player1Id
                }
            }

            test("draws three at your end step with three permanents you don't own") {
                val game = build(actsOfTreason = 2)

                game.castAgentStealing(game.findPermanent("Forest")!!)
                game.actOfTreason(game.findPermanent("Grizzly Bears")!!)
                game.actOfTreason(game.findPermanent("Hill Giant")!!)

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Forest, Bears and Hill Giant are three permanents Player controls but doesn't own") {
                    game.handSize(1) shouldBe handBefore + 3
                }
            }

            test("draws nothing with only two permanents you don't own") {
                val game = build(actsOfTreason = 1)

                game.castAgentStealing(game.findPermanent("Forest")!!)
                game.actOfTreason(game.findPermanent("Grizzly Bears")!!)

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Agent of Treachery itself is owned by Player, so it doesn't count toward three") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
