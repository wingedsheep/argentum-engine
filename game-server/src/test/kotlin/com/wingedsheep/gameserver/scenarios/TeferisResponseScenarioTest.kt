package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Teferi's Response (Invasion).
 *
 * Card reference:
 * - Teferi's Response: {1}{U} Instant
 *   "Counter target spell or ability an opponent controls that targets a land you
 *    control. If a permanent's ability is countered this way, destroy that permanent.
 *    Draw two cards."
 */
class TeferisResponseScenarioTest : ScenarioTestBase() {

    init {
        context("counters opponent spells targeting your lands") {
            test("counters a Lay Waste targeting your Plains, then draws two") {
                // Player 1 (opponent) casts Lay Waste targeting Player 2's Plains.
                // Player 2 responds with Teferi's Response → spell countered, Plains intact,
                // Player 2 draws two cards.
                val game = scenario()
                    .withPlayers("Opponent", "You")
                    .withCardInHand(1, "Lay Waste")
                    .withCardInHand(2, "Teferi's Response")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val plainsId = game.findPermanent("Plains")!!

                val layWasteCast = game.castSpell(1, "Lay Waste", targetId = plainsId)
                withClue("Lay Waste cast should succeed: ${layWasteCast.error}") {
                    layWasteCast.error shouldBe null
                }
                // Player 1 passes priority — now Player 2 may respond
                game.execute(PassPriority(game.player1Id))

                val layWasteOnStack = game.state.stack.first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Lay Waste"
                }
                val teferisResponseId = game.handCardId(game.player2Id, "Teferi's Response")

                val response = game.execute(
                    CastSpell(
                        game.player2Id,
                        teferisResponseId,
                        listOf(ChosenTarget.Spell(layWasteOnStack))
                    )
                )
                withClue("Teferi's Response cast should succeed: ${response.error}") {
                    response.error shouldBe null
                }

                val handSizeBeforeResolve = game.state.getHand(game.player2Id).size

                game.resolveStack()

                withClue("Lay Waste should be countered (in opponent's graveyard)") {
                    game.isInGraveyard(1, "Lay Waste") shouldBe true
                }
                withClue("Plains should still be on the battlefield") {
                    game.isOnBattlefield("Plains") shouldBe true
                }
                withClue("Player 2 should have drawn two cards") {
                    game.state.getHand(game.player2Id).size shouldBe handSizeBeforeResolve + 2
                }
            }

            test("counters Kamahl's activated ability and destroys Kamahl as its source") {
                // Player 1 controls Kamahl, Fist of Krosa. Player 1 activates the {G}
                // animate-land ability targeting Player 2's Plains. Player 2 responds
                // with Teferi's Response → ability countered, Kamahl destroyed (he is the
                // permanent source of the countered ability), Player 2 draws two cards.
                val game = scenario()
                    .withPlayers("Opponent", "You")
                    .withCardOnBattlefield(1, "Kamahl, Fist of Krosa")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInHand(2, "Teferi's Response")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kamahlId = game.findPermanent("Kamahl, Fist of Krosa")!!
                val plainsId = game.findPermanent("Plains")!!
                val animateLandAbility = cardRegistry.getCard("Kamahl, Fist of Krosa")!!
                    .script.activatedAbilities[0]

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kamahlId,
                        abilityId = animateLandAbility.id,
                        targets = listOf(ChosenTarget.Permanent(plainsId))
                    )
                )
                withClue("Kamahl's ability should activate successfully: ${activate.error}") {
                    activate.error shouldBe null
                }
                // Player 1 passes priority — Player 2 responds
                game.execute(PassPriority(game.player1Id))

                val abilityOnStack = game.state.stack.first()
                val teferisResponseId = game.handCardId(game.player2Id, "Teferi's Response")

                val response = game.execute(
                    CastSpell(
                        game.player2Id,
                        teferisResponseId,
                        listOf(ChosenTarget.Spell(abilityOnStack))
                    )
                )
                withClue("Teferi's Response cast should succeed: ${response.error}") {
                    response.error shouldBe null
                }

                val handSizeBeforeResolve = game.state.getHand(game.player2Id).size

                game.resolveStack()

                withClue("Kamahl should be destroyed and in the opponent's graveyard") {
                    game.isInGraveyard(1, "Kamahl, Fist of Krosa") shouldBe true
                }
                withClue("Kamahl should not be on the battlefield") {
                    game.isOnBattlefield("Kamahl, Fist of Krosa") shouldBe false
                }
                withClue("Plains should still be on the battlefield (ability was countered)") {
                    game.isOnBattlefield("Plains") shouldBe true
                }
                withClue("Player 2 should have drawn two cards") {
                    game.state.getHand(game.player2Id).size shouldBe handSizeBeforeResolve + 2
                }
            }
        }

        context("rejects illegal targets") {
            test("cannot target an opponent spell that does not target a land you control") {
                // Player 1 casts Shock targeting Player 1 (themselves) — no land target.
                // Player 2 should NOT be able to target it with Teferi's Response.
                val game = scenario()
                    .withPlayers("Opponent", "You")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(2, "Teferi's Response")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shockId = game.handCardId(game.player1Id, "Shock")
                val shockCast = game.execute(
                    CastSpell(
                        game.player1Id,
                        shockId,
                        listOf(ChosenTarget.Player(game.player1Id))
                    )
                )
                withClue("Shock cast should succeed: ${shockCast.error}") {
                    shockCast.error shouldBe null
                }
                game.execute(PassPriority(game.player1Id))

                val shockOnStack = game.state.stack.first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Shock"
                }
                val teferisResponseId = game.handCardId(game.player2Id, "Teferi's Response")

                val response = game.execute(
                    CastSpell(
                        game.player2Id,
                        teferisResponseId,
                        listOf(ChosenTarget.Spell(shockOnStack))
                    )
                )
                withClue("Teferi's Response should fail — Shock does not target a land you control") {
                    response.error shouldNotBe null
                }
            }
        }
    }

    private fun com.wingedsheep.gameserver.ScenarioTestBase.TestGame.handCardId(
        playerId: EntityId,
        cardName: String
    ): EntityId = state.getHand(playerId).first { id ->
        state.getEntity(id)?.get<CardComponent>()?.name == cardName
    }
}
