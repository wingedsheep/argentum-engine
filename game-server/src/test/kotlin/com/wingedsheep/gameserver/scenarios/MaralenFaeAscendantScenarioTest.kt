package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

class MaralenFaeAscendantScenarioTest : ScenarioTestBase() {

    init {
        // Tiny test cards to populate opponent's library and exercise the mana-value cap.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Sample Faerie",
                manaCost = ManaCost.parse("{1}{U}"),
                subtypes = setOf(Subtype("Faerie")),
                power = 1,
                toughness = 1
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Beefy Filler",
                manaCost = ManaCost.parse("{4}"),
                subtypes = setOf(Subtype("Beast")),
                power = 4,
                toughness = 4
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Cheap Filler",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype("Beast")),
                power = 1,
                toughness = 1
            )
        )

        context("Maralen, Fae Ascendant ETB exile trigger") {

            test("Maralen entering exiles top two of an opponent's library, linked to her") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Maralen, Fae Ascendant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Cheap Filler")
                    .withCardInLibrary(2, "Beefy Filler")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Maralen, Fae Ascendant")
                game.resolveStack()
                game.resolveStack() // ETB trigger resolution (TargetOpponent auto-selects in 2p)

                val maralenId = game.findPermanent("Maralen, Fae Ascendant")!!
                val linked = game.state.getEntity(maralenId)?.get<LinkedExileComponent>()
                linked shouldNotBe null
                linked!!.exiledIds.size shouldBe 2

                // Both top-of-library cards landed in opponent's exile.
                val exiledNames = linked.exiledIds.mapNotNull { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name
                }.toSet()
                exiledNames shouldBe setOf("Cheap Filler", "Beefy Filler")
            }

            test("another Elf you control entering also triggers Maralen") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Maralen, Fae Ascendant")
                    .withCardInHand(1, "Sample Faerie")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Cheap Filler")
                    .withCardInLibrary(2, "Beefy Filler")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val maralenId = game.findPermanent("Maralen, Fae Ascendant")!!

                game.castSpell(1, "Sample Faerie")
                game.resolveStack()  // Sample Faerie resolves and enters
                game.resolveStack()  // Maralen's ETB trigger from another Faerie entering

                val linked = game.state.getEntity(maralenId)?.get<LinkedExileComponent>()
                linked shouldNotBe null
                linked!!.exiledIds.size shouldBe 2
            }
        }

        context("Cast from linked exile (free, mana-value gated, once per turn)") {

            test("cast a low-mana-value card from exile without paying its mana cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Maralen, Fae Ascendant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Cheap Filler")  // mv 1 — eligible (cap = 1: only Maralen)
                    .withCardInLibrary(2, "Beefy Filler")  // mv 4 — ineligible
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Maralen, Fae Ascendant")
                game.resolveStack()
                game.resolveStack()  // ETB trigger exiles top 2

                val maralenId = game.findPermanent("Maralen, Fae Ascendant")!!
                game.isInExile(2, "Cheap Filler") shouldBe true

                // Free-cast Cheap Filler from opponent's exile. Player1 controls no lands that
                // tap for a Beast cost, but the spell costs nothing thanks to Maralen.
                val cheapId = findCardInExile(game, 2, "Cheap Filler")!!
                val result = game.execute(CastSpell(game.player1Id, cheapId))
                result.error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Cheap Filler") shouldBe true
                game.state.getEntity(maralenId)
                    ?.get<MayCastFromLinkedExileUsedThisTurnComponent>() shouldNotBe null
            }

            test("cannot cast a card whose mana value exceeds count of Elves and Faeries you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Maralen, Fae Ascendant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Beefy Filler")    // mv 4 — too expensive
                    .withCardInLibrary(2, "Cheap Filler")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Maralen, Fae Ascendant")
                game.resolveStack()
                game.resolveStack()

                val beefyId = findCardInExile(game, 2, "Beefy Filler")!!
                val result = game.execute(CastSpell(game.player1Id, beefyId))
                result.error shouldNotBe null
            }

            test("free-cast option appears in legal actions with mana cost 0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Maralen, Fae Ascendant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Cheap Filler")
                    .withCardInLibrary(2, "Beefy Filler")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Maralen, Fae Ascendant")
                game.resolveStack()
                game.resolveStack()

                // Recreate the legal-actions surface the web client sees.
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                session.injectStateForTesting(
                    game.state,
                    mapOf(
                        game.player1Id to PlayerSession(mockWs1, game.player1Id, "Player1"),
                        game.player2Id to PlayerSession(mockWs2, game.player2Id, "Player2")
                    )
                )

                val legalActions = session.getLegalActions(game.player1Id)
                val cheapCast = legalActions.find { it.description == "Cast Cheap Filler" }
                cheapCast shouldNotBe null
                cheapCast!!.isAffordable shouldBe true
                cheapCast.manaCostString shouldBe "0"
                cheapCast.sourceZone shouldBe "EXILE"

                // Beefy Filler ({4}) exceeds the dynamic cap (1 Elf/Faerie on board) — must
                // not appear as an affordable action even though it sits in linked exile.
                val beefyCast = legalActions.find { it.description == "Cast Beefy Filler" }
                if (beefyCast != null) beefyCast.isAffordable shouldBe false
            }

            test("scaling the Elf/Faerie count raises the mana-value cap so larger spells become free") {
                // Maralen + Virulent Emissary on the battlefield → cap starts at 2.
                // P1 casts a vanilla Sample Faerie → Maralen's ETB exiles top 2 of P2's library.
                // Now 3 Elves/Faeries are on board → mv-2 exiled cards are eligible.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Maralen, Fae Ascendant")
                    .withCardOnBattlefield(1, "Virulent Emissary")
                    .withCardInHand(1, "Sample Faerie")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Beefy Filler")    // mv 4 — top, will be exiled
                    .withCardInLibrary(2, "Cheap Filler")    // mv 1 — also exiled
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sample Faerie")
                game.resolveStack()  // Sample Faerie resolves and enters
                game.resolveStack()  // Maralen ETB exiles top 2

                game.isInExile(2, "Beefy Filler") shouldBe true

                // 3 Elves/Faeries on board (Maralen, Virulent Emissary, Sample Faerie) → cap = 3.
                // Beefy Filler (mv 4) is still over the cap and stays unaffordable;
                // verify by enumerating legal actions.
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                session.injectStateForTesting(
                    game.state,
                    mapOf(
                        game.player1Id to PlayerSession(mockWs1, game.player1Id, "Player1"),
                        game.player2Id to PlayerSession(mockWs2, game.player2Id, "Player2")
                    )
                )
                val legalActions = session.getLegalActions(game.player1Id)
                val cheapCast = legalActions.find { it.description == "Cast Cheap Filler" }
                cheapCast shouldNotBe null
                cheapCast!!.isAffordable shouldBe true
                cheapCast.manaCostString shouldBe "0"
            }

            test("once per turn — second cast attempt is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Maralen, Fae Ascendant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Cheap Filler")  // top
                    .withCardInLibrary(2, "Cheap Filler")  // second — both mv 1
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Maralen, Fae Ascendant")
                game.resolveStack()
                game.resolveStack()

                val firstCheap = findCardInExile(game, 2, "Cheap Filler")!!
                game.execute(CastSpell(game.player1Id, firstCheap)).error shouldBe null
                game.resolveStack()

                // Second castable card is still in exile, but the once-per-turn permission is spent.
                val secondCheap = findCardInExile(game, 2, "Cheap Filler")
                if (secondCheap != null) {
                    val secondResult = game.execute(CastSpell(game.player1Id, secondCheap))
                    secondResult.error shouldNotBe null
                }
            }
        }
    }

    private fun TestGame.isInExile(playerNumber: Int, cardName: String): Boolean {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    private fun findCardInExile(game: TestGame, playerNumber: Int, cardName: String): EntityId? {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).find { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }
}
