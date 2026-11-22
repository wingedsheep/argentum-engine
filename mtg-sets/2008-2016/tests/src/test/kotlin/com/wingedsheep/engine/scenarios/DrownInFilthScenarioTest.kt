package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Drown in Filth — "Choose target creature. Mill four cards, then that creature gets -1/-1 until
 * end of turn for each land card in your graveyard."
 *
 * The land count is taken after the mill: lands just milled count alongside lands already in the
 * caster's graveyard, while lands in an opponent's graveyard don't.
 */
class DrownInFilthScenarioTest : ScenarioTestBase() {

    init {
        test("counts land cards in your graveyard after milling four") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Drown in Filth")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Swamp")
                .withCardInGraveyard(2, "Forest")
                .withCardInGraveyard(2, "Forest")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Drown in Filth", wurm).error shouldBe null
            game.resolveStack()

            withClue("all four cards were milled") {
                game.librarySize(1) shouldBe 0
                game.graveyardSize(1) shouldBe 6 // Swamp + 4 milled + Drown in Filth
            }
            withClue("two milled Forests + the Swamp already there = -3/-3; opponent's lands don't count") {
                val projected = game.state.projectedState
                projected.getPower(wurm) shouldBe 3
                projected.getToughness(wurm) shouldBe 1
            }
        }

        test("a creature whose toughness drops to zero dies") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Drown in Filth")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Drown in Filth", giant).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }
    }
}
