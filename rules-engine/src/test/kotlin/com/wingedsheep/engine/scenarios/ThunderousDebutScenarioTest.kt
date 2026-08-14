package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thunderous Debut {6}{G}{G} Sorcery — Wilds of Eldraine #190.
 *
 * "Bargain. Look at the top twenty cards of your library. You may reveal up to two creature cards
 *  from among them. If this spell was bargained, put the revealed cards onto the battlefield.
 *  Otherwise, put the revealed cards into your hand. Then shuffle."
 *
 * The load-bearing shape here is a pipeline collection read *inside* a gate frame, after the
 * selection paused for a player decision: the same picked collection is moved either to the
 * battlefield or to hand depending on [com.wingedsheep.sdk.dsl.Conditions.WasBargained]. Both
 * branches have to actually move the cards — a collection that didn't survive the pause would move
 * silently zero. The other implicit contract is that gathering the top twenty only *reads* ids, so
 * the eighteen unpicked cards must still be in the library for "Then shuffle" to matter.
 */
class ThunderousDebutScenarioTest : ScenarioTestBase() {

    /**
     * Player 1 with the spell, bargain fodder, eight Forests, and a twenty-card library whose top
     * cards are [creatures] followed by eighteen Forests.
     */
    private fun game(
        creatures: List<String> = listOf("Grizzly Bears", "Centaur Courser")
    ): TestGame {
        val builder = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Thunderous Debut")
            .withCardOnBattlefield(1, "Food", isToken = true)
            .withLandsOnBattlefield(1, "Forest", 8)
        creatures.forEach { builder.withCardInLibrary(1, it) }
        repeat(20 - creatures.size) { builder.withCardInLibrary(1, "Forest") }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    init {
        context("Thunderous Debut") {

            test("unbargained, the revealed creature cards go to hand and the rest stay put") {
                val game = game()
                val library = game.state.getLibrary(game.player1Id)
                val bears = library[0]
                val courser = library[1]

                game.castSpell(1, "Thunderous Debut").error shouldBe null
                game.resolveStack()

                val pick = game.getPendingDecision()
                pick.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the creature cards among the twenty may be revealed") {
                    pick.options shouldContainExactlyInAnyOrder listOf(bears, courser)
                    pick.maxSelections shouldBe 2
                }

                game.selectCards(listOf(bears, courser))
                game.resolveStack()

                withClue("unbargained sends the picks to hand, not to the battlefield") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Centaur Courser") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                }
                withClue("gathering the top twenty only read ids — the other eighteen never moved") {
                    game.librarySize(1) shouldBe 18
                }
                game.isInGraveyard(1, "Thunderous Debut") shouldBe true
                withClue("the bargain cost was not paid") {
                    game.findPermanents("Food").size shouldBe 1
                }
            }

            test("bargained, the revealed creatures enter the battlefield and their triggers fire") {
                val game = game(creatures = listOf("Provisions Merchant", "Grizzly Bears"))
                val library = game.state.getLibrary(game.player1Id)
                val merchant = library[0]
                val bears = library[1]

                game.castSpellBargained(1, "Thunderous Debut", "Food").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(merchant, bears))
                game.resolveStack()

                withClue("bargained sends the same picks to the battlefield instead of hand") {
                    game.isOnBattlefield("Provisions Merchant") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInHand(1, "Provisions Merchant") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
                withClue("the Merchant entered, so its enters trigger made a fresh Food") {
                    // The only Food on the battlefield was sacrificed to bargain.
                    game.findPermanents("Food").size shouldBe 1
                }
                game.librarySize(1) shouldBe 18
                game.isInGraveyard(1, "Thunderous Debut") shouldBe true
            }

            test("revealing nothing moves nothing, and the shuffle still happens") {
                val game = game()
                val library = game.state.getLibrary(game.player1Id)

                game.castSpell(1, "Thunderous Debut").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                val result = game.skipSelection()
                withClue("'Then shuffle' runs even when nothing was revealed") {
                    result.events.any { it is LibraryShuffledEvent } shouldBe true
                }
                game.resolveStack()

                withClue("every card that was looked at is still in the library") {
                    game.state.getLibrary(game.player1Id) shouldContainExactlyInAnyOrder library
                }
                game.handSize(1) shouldBe 0
                game.isInGraveyard(1, "Thunderous Debut") shouldBe true
            }
        }
    }
}
