package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for House Cartographer (DSK #185).
 *
 * House Cartographer — {1}{G} Creature — Human Scout Survivor, 2/2
 *   "Survival — At the beginning of your second main phase, if this creature is tapped, reveal
 *    cards from the top of your library until you reveal a land card. Put that card into your hand
 *    and the rest on the bottom of your library in a random order."
 *
 * Verifies the intervening-if (only when the Cartographer is tapped, CR 603.4), the reveal-until-land
 * dig, the land landing in hand, the non-land reveals going to the bottom of the library, and that an
 * untapped Cartographer produces no trigger.
 */
class HouseCartographerScenarioTest : ScenarioTestBase() {

    init {
        context("House Cartographer — Survival trigger") {

            test("a tapped Cartographer reveals until a land, puts it in hand, bottoms the rest") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "House Cartographer", tapped = true)
                    // Top of library: two nonlands, then a Forest, then a deeper card.
                    .withCardInLibrary(1, "Grizzly Bears") // top
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Forest") // first land revealed → to hand
                    .withCardInLibrary(1, "Island") // never revealed (below the matched land)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player1 = game.player1Id

                // Advance to the second main phase — the Survival trigger fires (source is tapped).
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(8) { if (game.state.stack.isNotEmpty()) game.resolveStack() }

                val hand = game.state.getZone(ZoneKey(player1, Zone.HAND))
                val handNames = hand.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
                withClue("the revealed land is put into hand") {
                    handNames.contains("Forest") shouldBe true
                }

                val library = game.state.getZone(ZoneKey(player1, Zone.LIBRARY))
                val libraryNames = library.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
                withClue("the two non-land reveals are bottomed back into the library") {
                    libraryNames.contains("Grizzly Bears") shouldBe true
                    libraryNames.contains("Centaur Courser") shouldBe true
                }
                withClue("the deeper unrevealed card stays in the library") {
                    libraryNames.contains("Island") shouldBe true
                }
                withClue("the revealed non-lands didn't end up in hand or graveyard") {
                    handNames.contains("Grizzly Bears") shouldBe false
                    handNames.contains("Centaur Courser") shouldBe false
                    game.state.getZone(ZoneKey(player1, Zone.GRAVEYARD)).isEmpty() shouldBe true
                }
            }

            test("an untapped Cartographer does NOT fire the Survival trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "House Cartographer", tapped = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player1 = game.player1Id
                val handBefore = game.state.getZone(ZoneKey(player1, Zone.HAND)).size

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { if (game.state.stack.isNotEmpty()) game.resolveStack() }

                withClue("no Survival trigger — the Cartographer is untapped, so the intervening-if fails") {
                    game.state.getZone(ZoneKey(player1, Zone.HAND)).size shouldBe handBefore
                }
            }
        }
    }
}
