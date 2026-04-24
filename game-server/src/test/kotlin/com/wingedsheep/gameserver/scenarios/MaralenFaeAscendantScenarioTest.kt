package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

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
