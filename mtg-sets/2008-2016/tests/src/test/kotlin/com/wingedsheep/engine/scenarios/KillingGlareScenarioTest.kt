package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Killing Glare (GTC) — {X}{B} Instant.
 *
 * "Destroy target creature with power X or less."
 *
 * The target's power cap is the X paid on casting, so the test proves the cast-time X reaches
 * target legality (CR 601.2c) and the resolution re-check (2013-01-24 ruling: a creature whose
 * power has grown above X by then makes the spell fail to resolve).
 */
class KillingGlareScenarioTest : ScenarioTestBase() {

    init {
        context("Killing Glare") {

            test("X = 2 destroys a creature with power 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Killing Glare")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = castKillingGlare(game, bears, xValue = 2)
                withClue("Casting Killing Glare for X=2 at a 2-power creature should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears (power 2 <= X) is destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("a creature with power greater than X is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Killing Glare")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val result = castKillingGlare(game, giant, xValue = 2)
                withClue("Hill Giant (power 3) can't be targeted with X=2") {
                    result.error shouldNotBe null
                }
                withClue("Hill Giant stays on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("does nothing if the target's power rises above X before it resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Killing Glare")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(2, "Giant Growth")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                castKillingGlare(game, bears, xValue = 2).error shouldBe null
                game.passPriority()

                // The opponent pumps the Bears to 5/5 in response.
                val growth = game.castSpell(2, "Giant Growth", bears)
                withClue("Giant Growth in response should be castable: ${growth.error}") {
                    growth.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears (now power 5 > X) survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Killing Glare still goes to its owner's graveyard") {
                    game.isInGraveyard(1, "Killing Glare") shouldBe true
                }
            }
        }
    }

    private fun castKillingGlare(game: TestGame, target: EntityId, xValue: Int): ExecutionResult {
        val cardId = game.state.getHand(game.player1Id).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Killing Glare"
        }
        return game.execute(
            CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Permanent(target)), xValue = xValue)
        )
    }
}
