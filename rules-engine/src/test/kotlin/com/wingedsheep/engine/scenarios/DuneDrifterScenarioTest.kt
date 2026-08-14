package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dune Drifter (DFT #200) — {X}{W}{B} Artifact — Vehicle, 3/3, Crew 2.
 *
 *   "When this Vehicle enters, return target artifact or creature card with mana value X or less
 *    from your graveyard to the battlefield."
 *
 * The enters trigger resolves after the spell has left the stack, so the mana-value cap has to read
 * the *cast* X carried by the permanent (`DynamicAmount.CastX`), not the transient resolution X.
 * These tests pin the cap at X = 3 and at X = 0.
 */
class DuneDrifterScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.graveyardCard(player: Int, name: String): EntityId {
        val playerId = if (player == 1) player1Id else player2Id
        return state.getGraveyard(playerId).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }
    }

    init {
        context("Dune Drifter enters trigger") {

            test("cast for X = 3 reanimates an artifact or creature card with mana value 3 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dune Drifter")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInGraveyard(1, "Grizzly Bears") // Creature, MV 2 — eligible
                    .withCardInGraveyard(1, "Sol Ring")      // Artifact, MV 1 — eligible
                    .withCardInGraveyard(1, "Hill Giant")    // Creature, MV 4 — over the cap
                    .withCardInGraveyard(1, "Pacifism")      // Enchantment, MV 2 — wrong type
                    .withCardInGraveyard(1, "Plains")        // Land — wrong type
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castXSpell(1, "Dune Drifter", xValue = 3)
                withClue("Casting for {3}{W}{B} with five lands should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The Vehicle resolved onto the battlefield") {
                    game.findPermanent("Dune Drifter") shouldNotBe null
                }

                val decision = game.state.pendingDecision as? ChooseTargetsDecision
                withClue("The enters trigger pauses to choose a card in your graveyard") {
                    decision shouldNotBe null
                }
                withClue("Only artifact/creature cards with mana value 3 or less qualify") {
                    (decision!!.legalTargets[0] ?: emptyList()) shouldContainExactlyInAnyOrder listOf(
                        game.graveyardCard(1, "Grizzly Bears"),
                        game.graveyardCard(1, "Sol Ring")
                    )
                }

                game.selectTargets(listOf(game.graveyardCard(1, "Grizzly Bears")))
                game.resolveStack()

                withClue("The chosen creature card returns to the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }

            test("cast for X = 0 can only reanimate mana-value-0 cards, so a MV 2 card is untouchable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dune Drifter")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInGraveyard(1, "Grizzly Bears") // MV 2 — above X = 0
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castXSpell(1, "Dune Drifter", xValue = 0)
                withClue("Casting for {W}{B} should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The Vehicle still enters") {
                    game.findPermanent("Dune Drifter") shouldNotBe null
                }
                withClue("No legal target at X = 0, so the trigger does nothing and asks nothing") {
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }
        }
    }
}
