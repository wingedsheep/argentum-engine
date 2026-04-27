package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kindle the Inner Flame (Lorwyn Eclipsed).
 *
 * {3}{R} Kindred Sorcery — Elemental
 * Create a token that's a copy of target creature you control, except it has haste
 * and "At the beginning of the end step, sacrifice this token."
 * Flashback—{1}{R}, Behold three Elementals.
 */
class KindleTheInnerFlameScenarioTest : ScenarioTestBase() {

    init {
        context("Cast from hand") {
            test("creates a token copy of target creature with haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kindle the Inner Flame")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val castResult = game.castSpell(1, "Kindle the Inner Flame", hillGiantId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // There should be two Hill Giants on the battlefield: the original + a token
                val hillGiants = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                }
                withClue("Should have original Hill Giant + token copy") {
                    hillGiants shouldHaveSize 2
                }

                val tokenId = hillGiants.first { it != hillGiantId }
                val tokenContainer = game.state.getEntity(tokenId)!!
                withClue("Token should have TokenComponent") {
                    tokenContainer.get<TokenComponent>() shouldBe TokenComponent
                }

                // Token has haste in its base keywords (added via addedKeywords)
                val tokenCard = tokenContainer.get<CardComponent>()!!
                withClue("Token should have haste in base keywords") {
                    tokenCard.baseKeywords shouldContain Keyword.HASTE
                }

                // Token controlled by player 1
                tokenContainer.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            }

            test("token is sacrificed at the beginning of the end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kindle the Inner Flame")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Kindle the Inner Flame", hillGiantId)
                game.resolveStack()

                val hillGiantsAfterCast = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                }
                hillGiantsAfterCast shouldHaveSize 2

                // Advance to end step (the trigger goes on the stack here)
                game.passUntilPhase(Phase.ENDING, Step.END)
                // Resolve the sacrifice trigger
                game.resolveStack()

                val hillGiantsAtEnd = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                }
                withClue("Token should have been sacrificed at end step, original remains") {
                    hillGiantsAtEnd shouldHaveSize 1
                    hillGiantsAtEnd.first() shouldBe hillGiantId
                }
            }
        }

        context("Flashback with Behold three Elementals") {
            test("cast from graveyard with three Elementals on battlefield exiles spell, beholds creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Kindle the Inner Flame")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Lavaleaper")
                    .withCardOnBattlefield(1, "Fire Elemental")
                    .withCardOnBattlefield(1, "Thundermare")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val lavaleaperId = game.findPermanent("Lavaleaper")!!
                val fireElementalId = game.findPermanent("Fire Elemental")!!
                val thundermareId = game.findPermanent("Thundermare")!!

                val cardId = game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Kindle the Inner Flame"
                }

                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(hillGiantId)),
                        useAlternativeCost = true,
                        additionalCostPayment = AdditionalCostPayment(
                            beheldCards = listOf(lavaleaperId, fireElementalId, thundermareId)
                        )
                    )
                )
                withClue("Flashback cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Token copy of Hill Giant created
                val hillGiants = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                }
                withClue("Should have original + token") { hillGiants shouldHaveSize 2 }

                // Beheld Elementals are still on the battlefield (Behold doesn't move them)
                game.state.getBattlefield() shouldContain lavaleaperId
                game.state.getBattlefield() shouldContain fireElementalId
                game.state.getBattlefield() shouldContain thundermareId

                // Spell exiled (flashback)
                val exile = game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE))
                val spellExiled = exile.any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Kindle the Inner Flame"
                }
                withClue("Flashback should exile the spell") { spellExiled shouldBe true }

                // Spell no longer in graveyard
                game.isInGraveyard(1, "Kindle the Inner Flame") shouldBe false
            }

            test("flashback rejected when fewer than three Elementals available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Kindle the Inner Flame")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Lavaleaper")
                    .withCardOnBattlefield(1, "Fire Elemental")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val lavaleaperId = game.findPermanent("Lavaleaper")!!
                val fireElementalId = game.findPermanent("Fire Elemental")!!
                val cardId = game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Kindle the Inner Flame"
                }

                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(hillGiantId)),
                        useAlternativeCost = true,
                        additionalCostPayment = AdditionalCostPayment(
                            beheldCards = listOf(lavaleaperId, fireElementalId)
                        )
                    )
                )
                withClue("Flashback with only two Elementals should fail validation") {
                    castResult.error shouldNotBe null
                }
            }
        }
    }

}
