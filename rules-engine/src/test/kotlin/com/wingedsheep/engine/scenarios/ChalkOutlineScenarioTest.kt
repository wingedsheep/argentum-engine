package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ChalkOutline
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chalk Outline (MKM #157) — {3}{G} Enchantment.
 *
 * "Whenever one or more creature cards leave your graveyard, create a 2/2 white and blue Detective
 *  creature token, then investigate."
 *
 * Both halves of the payoff and the batch semantics of the trigger. The printed ruling — "if
 * multiple creature cards leave your graveyard at the same time, Chalk Outline's ability will
 * trigger only once" — is the one that a per-card trigger would get wrong, so it gets its own test;
 * the filter (creature cards, not any card) gets the other.
 */
class ChalkOutlineScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(ChalkOutline)

        context("Chalk Outline") {

            test("a creature card leaving your graveyard makes a Detective and a Clue") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Chalk Outline")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .build()

                withClue("nothing has left the graveyard yet") {
                    game.findPermanents("Detective Token").size shouldBe 0
                    game.findPermanents("Clue").size shouldBe 0
                }

                game.castSpellTargetingGraveyardCard(1, "Raise Dead", 1, "Grizzly Bears")
                    .error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                withClue("one Detective token, then one Clue") {
                    game.findPermanents("Detective Token").size shouldBe 1
                    game.findPermanents("Clue").size shouldBe 1
                }
            }

            test("a noncreature card leaving your graveyard triggers nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Chalk Outline")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInHand(1, "Regrowth")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Regrowth", 1, "Lightning Bolt")
                    .error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Lightning Bolt") shouldBe true
                withClue("the filter is creature cards, so an instant leaving pays nothing") {
                    game.findPermanents("Detective Token").size shouldBe 0
                    game.findPermanents("Clue").size shouldBe 0
                }
            }

            test("a creature card leaving an opponent's graveyard triggers nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Chalk Outline")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInHand(2, "Raise Dead")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .build()

                game.castSpellTargetingGraveyardCard(2, "Raise Dead", 2, "Grizzly Bears")
                    .error shouldBe null
                game.resolveStack()

                withClue("\"leave **your** graveyard\" is scoped to the controller's own graveyard") {
                    game.findPermanents("Detective Token").size shouldBe 0
                    game.findPermanents("Clue").size shouldBe 0
                }
            }
        }
    }
}
