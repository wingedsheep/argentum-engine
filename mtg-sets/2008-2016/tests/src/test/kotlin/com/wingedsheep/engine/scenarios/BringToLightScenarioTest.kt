package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bring to Light (BFZ #209) — {3}{G}{U} Sorcery.
 *
 *   Converge — Search your library for a creature, instant, or sorcery card with mana value less
 *   than or equal to the number of colors of mana spent to cast this spell, exile that card, then
 *   shuffle. You may cast that card without paying its mana cost.
 *
 * Pins the converge cap as read by the library search while the spell resolves (the colors of its
 * own payment), the card-type restriction, the free cast, and that a declined cast leaves the card
 * in exile.
 */
class BringToLightScenarioTest : ScenarioTestBase() {

    init {
        context("Bring to Light") {

            fun base() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bring to Light")
                .withCardInLibrary(1, "Grizzly Bears")   // creature, mana value 2
                .withCardInLibrary(1, "Divination")      // sorcery, mana value 3
                .withCardInLibrary(1, "Siege Mastodon")  // creature, mana value 5
                .withCardInLibrary(1, "Pacifism")        // enchantment, mana value 2 — wrong type
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            test("two colors spent: only creature/instant/sorcery cards of mana value 2 or less are found") {
                val game = base()
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .build()

                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                game.castSpell(1, "Bring to Light").error shouldBe null
                game.resolveStack()

                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("G + U spent → cap 2; Divination (3), Mastodon (5) and Pacifism (enchantment) excluded") {
                    search.options shouldContainExactlyInAnyOrder listOf(bears)
                }

                game.selectCards(listOf(bears)).error shouldBe null
                withClue("casting the exiled card is optional") { game.hasPendingDecision() shouldBe true }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears was cast for free and resolved") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInExile(1, "Grizzly Bears") shouldBe false
                }
            }

            test("five colors spent: a mana value 5 creature is found and cast without paying its mana cost") {
                val game = base()
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()

                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                val divination = game.findCardsInLibrary(1, "Divination").single()
                val mastodon = game.findCardsInLibrary(1, "Siege Mastodon").single()
                game.castSpell(1, "Bring to Light").error shouldBe null
                game.resolveStack()

                val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("five colors spent → cap 5; Pacifism is still the wrong card type") {
                    search.options shouldContainExactlyInAnyOrder listOf(bears, divination, mastodon)
                }

                game.selectCards(listOf(mastodon)).error shouldBe null
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Siege Mastodon") shouldBe true
            }

            test("declining the cast leaves the found card in exile") {
                val game = base()
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .build()

                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                game.castSpell(1, "Bring to Light").error shouldBe null
                game.resolveStack()

                game.selectCards(listOf(bears)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                withClue("an uncast card stays exiled") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
