package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Foul Emissary (EMN #158) — {2}{G} Creature — Human Horror 1/1.
 *
 *   When this creature enters, look at the top four cards of your library. You may reveal a
 *   creature card from among them and put it into your hand. Put the rest on the bottom of your
 *   library in any order.
 *   When you sacrifice this creature while casting a spell with emerge, create a 3/2 colorless
 *   Eldrazi Horror creature token.
 *
 * The ETB dig is the same `lookAtTopRevealMatchingToHand` Courageous Outrider's test covers; these
 * tests pin the emerge-sacrifice trigger (CR 702.119a) — it fires on that sacrifice and no other.
 */
class FoulEmissaryScenarioTest : ScenarioTestBase() {

    init {
        context("Foul Emissary") {

            test("sacrificed to pay an emerge cost, it leaves a 3/2 Eldrazi Horror") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wretched Gryff")
                    .withCardOnBattlefield(1, "Foul Emissary") // {2}{G} → mana value 3
                    .withCardInLibrary(1, "Grizzly Bears")
                    // Emerge {5}{U} reduced by 3 → {2}{U}.
                    .withLandsOnBattlefield(1, "Island", 3)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Wretched Gryff", "Foul Emissary")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(1, "Foul Emissary") shouldBe true
                game.isOnBattlefield("Wretched Gryff") shouldBe true
                val tokens = game.findAllPermanents("Eldrazi Horror Token")
                withClue("the emerge-sacrifice trigger created one 3/2 Eldrazi Horror") {
                    tokens shouldHaveSize 1
                    game.state.projectedState.getPower(tokens.single()) shouldBe 3
                    game.state.projectedState.getToughness(tokens.single()) shouldBe 2
                }
            }

            test("sacrificed to any other cost, it leaves nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Village Rites")
                    .withCardOnBattlefield(1, "Foul Emissary")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .build()

                val cast = game.castSpellWithAdditionalSacrifice(1, "Village Rites", "Foul Emissary")
                withClue("Village Rites should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(1, "Foul Emissary") shouldBe true
                withClue("a sacrifice that didn't pay an emerge cost doesn't fire the trigger") {
                    game.findAllPermanents("Eldrazi Horror Token").shouldBeEmpty()
                }
            }

            test("another creature sacrificed for emerge doesn't fire it") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wretched Gryff")
                    .withCardOnBattlefield(1, "Foul Emissary")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .build()

                val cast = game.castSpellWithEmerge(1, "Wretched Gryff", "Centaur Courser")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Foul Emissary") shouldBe true
                withClue("only Foul Emissary's own emerge sacrifice triggers it") {
                    game.findAllPermanents("Eldrazi Horror Token").shouldBeEmpty()
                }
            }
        }
    }
}
