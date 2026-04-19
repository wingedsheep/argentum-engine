package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Caretaker's Talent level 3 static ability:
 * "Creature tokens you control get +2/+2."
 *
 * Regression: the buff must NOT apply to tokens controlled by an opponent,
 * nor to non-token creatures.
 */
class CaretakersTalentTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun ScenarioTestBase.TestGame.markAsToken(cardName: String) {
        val cardId = findPermanent(cardName)!!
        val container = state.getEntity(cardId)!!.with(TokenComponent)
        state = state.withEntity(cardId, container)
    }

    init {
        context("Caretaker's Talent Level 3 — creature tokens you control get +2/+2") {

            test("buffs a creature token you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Caretaker's Talent", classLevel = 3)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2, will be tagged as token
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.markAsToken("Glory Seeker")

                val tokenId = game.findPermanent("Glory Seeker")!!
                val projected = stateProjector.project(game.state)

                withClue("Token you control should be 4/4 with Level 3 bonus") {
                    projected.getPower(tokenId) shouldBe 4
                    projected.getToughness(tokenId) shouldBe 4
                }
            }

            test("does NOT buff an opponent's creature token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Caretaker's Talent", classLevel = 3)
                    .withCardOnBattlefield(2, "Glory Seeker") // opponent's token, 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.markAsToken("Glory Seeker")

                val opponentTokenId = game.findPermanent("Glory Seeker")!!
                val projected = stateProjector.project(game.state)

                withClue("Opponent's token must not be buffed — should remain 2/2") {
                    projected.getPower(opponentTokenId) shouldBe 2
                    projected.getToughness(opponentTokenId) shouldBe 2
                }
            }

            test("does NOT buff your non-token creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Caretaker's Talent", classLevel = 3)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 non-token
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val projected = stateProjector.project(game.state)

                withClue("Non-token creature you control should stay 2/2") {
                    projected.getPower(bearsId) shouldBe 2
                    projected.getToughness(bearsId) shouldBe 2
                }
            }
        }
    }
}
