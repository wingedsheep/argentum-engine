package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grothama, All-Devouring — {3}{G}{G} 10/8 Legendary Creature — Wurm.
 *  - Other creatures have "Whenever this creature attacks, you may have it fight Grothama."
 *  - When Grothama leaves the battlefield, each player draws cards equal to the amount of damage
 *    dealt to Grothama this turn by sources they controlled.
 *
 * Pinned: combat damage counts toward the per-player tally, not just fight and burn damage.
 */
class GrothamaAllDevouringScenarioTest : ScenarioTestBase() {

    init {
        context("Grothama, All-Devouring — leaves-the-battlefield draw") {
            test("a blocker's combat damage makes its controller draw that many when Grothama leaves") {
                val game = scenario()
                    .withPlayers("Grothama Owner", "Blocker Owner")
                    .withCardOnBattlefield(1, "Grothama, All-Devouring", summoningSickness = false)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .apply { repeat(5) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grothama, All-Devouring" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Centaur Courser" to listOf("Grothama, All-Devouring"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val ownerHand = game.handSize(1)
                val blockerHand = game.handSize(2)
                game.castSpell(1, "Doom Blade", game.findPermanent("Grothama, All-Devouring")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grothama, All-Devouring") shouldBe true
                withClue("the Courser dealt 3 combat damage to Grothama") {
                    game.handSize(2) shouldBe blockerHand + 3
                }
                withClue("Doom Blade dealt no damage; the owner draws nothing (and cast one card)") {
                    game.handSize(1) shouldBe ownerHand - 1
                }
            }
        }
    }
}
