package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mirko Vosk, Mind Drinker (DGM #85) — {3}{U}{B} 2/4 flying.
 *
 *   Whenever Mirko Vosk deals combat damage to a player, that player reveals cards from the top of
 *   their library until they reveal four land cards, then puts those cards into their graveyard.
 */
class MirkoVoskMindDrinkerScenarioTest : ScenarioTestBase() {

    init {
        test("the damaged player bins their library down to the fourth land") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mirko Vosk, Mind Drinker")
                // Opponent's library, top first: four lands among three spells, then two cards that stay.
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Savannah Lions")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Craw Wurm")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Mirko Vosk, Mind Drinker" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.graveyardSize(2) shouldBe 7
            game.findCardsInGraveyard(2, "Forest").size shouldBe 4
            game.librarySize(2) shouldBe 2
            game.findCardsInLibrary(2, "Craw Wurm").size shouldBe 1
            game.graveyardSize(1) shouldBe 0
        }

        test("with fewer than four lands the whole library is revealed and milled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mirko Vosk, Mind Drinker")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Mirko Vosk, Mind Drinker" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.librarySize(2) shouldBe 0
            game.graveyardSize(2) shouldBe 3
        }
    }
}
