package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Distended Mindbender — {8} 5/5 Eldrazi Insect with emerge {5}{B}{B} and
 * "When you cast this spell, target opponent reveals their hand. You choose from it a nonland card
 * with mana value 3 or less and a card with mana value 4 or greater. That player discards those
 * cards."
 *
 * Two selections from one revealed hand, each with its own filter. The two mana-value bands are
 * disjoint, so a single card can never be picked for both.
 */
class DistendedMindbenderScenarioTest : ScenarioTestBase() {

    init {
        context("Distended Mindbender") {

            test("emerge cast strips one cheap and one expensive card from the opponent's hand") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Distended Mindbender")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    // Emerge {5}{B}{B} reduced by 3 → {2}{B}{B}: four Swamps.
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(2, "Grizzly Bears") // {1}{G} → mana value 2
                    .withCardInHand(2, "Force of Nature") // {3}{G}{G} → mana value 5
                    .withCardInHand(2, "Island") // a land — never a legal pick for either band
                    .build()

                val cast = game.castSpellWithEmerge(
                    1, "Distended Mindbender", "Centaur Courser",
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Centaur Courser") shouldBe true

                // Cast trigger: reveal, then two filtered picks by the caster.
                val cheap = game.findCardsInHand(2, "Grizzly Bears").single()
                val expensive = game.findCardsInHand(2, "Force of Nature").single()
                game.resolveStack()
                game.selectCards(listOf(cheap)).error shouldBe null
                game.selectCards(listOf(expensive)).error shouldBe null
                game.resolveStack()

                withClue("both picks were discarded, the land was untouchable") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Force of Nature") shouldBe true
                    game.isInHand(2, "Island") shouldBe true
                }
                game.isOnBattlefield("Distended Mindbender") shouldBe true
            }
        }
    }
}
