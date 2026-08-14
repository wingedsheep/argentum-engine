package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rowan's Grim Search {2}{B} Instant — Wilds of Eldraine #104.
 *
 * "Bargain. If this spell was bargained, look at the top four cards of your library, then put up to
 *  two of them back on top of your library in any order and the rest into your graveyard. You draw
 *  two cards and you lose 2 life."
 *
 * Two things need proving beyond the card's serialization: that the bargained clause is really gated
 * (CR 702.166c — an unbargained cast never digs), and that the dig runs *before* the draw, so the
 * two cards put back on top are the two cards drawn. The `ChooseUpTo` selection also has to survive
 * both degenerate inputs — keeping nothing, and a library shorter than four.
 */
class RowansGrimSearchScenarioTest : ScenarioTestBase() {

    /** Player 1 with the spell, bargain fodder, three Swamps and [library] top-down. */
    private fun game(vararg library: String): TestGame {
        val builder = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Rowan's Grim Search")
            .withCardOnBattlefield(1, "Food", isToken = true)
            .withLandsOnBattlefield(1, "Swamp", 3)
        library.forEach { builder.withCardInLibrary(1, it) }
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    private val sixCards = arrayOf(
        "Grizzly Bears", "Centaur Courser", "Lightning Bolt", "Forest", "Island", "Mountain"
    )

    init {
        context("Rowan's Grim Search") {

            test("an unbargained cast is only draw two and lose 2 — nothing is looked at") {
                val game = game(*sixCards)
                val library = game.state.getLibrary(game.player1Id)

                game.castSpell(1, "Rowan's Grim Search").error shouldBe null
                game.resolveStack()

                withClue("the gated dig never runs, so no selection is ever offered") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("the draw came off the untouched top of the library") {
                    game.state.getHand(game.player1Id) shouldContainExactly library.take(2)
                    game.state.getLibrary(game.player1Id) shouldContainExactly library.drop(2)
                }
                withClue("only the spell itself is in the graveyard") {
                    game.state.getGraveyard(game.player1Id).size shouldBe 1
                    game.isInGraveyard(1, "Rowan's Grim Search") shouldBe true
                }
                game.getLifeTotal(1) shouldBe 18
                game.isOnBattlefield("Food") shouldBe true
            }

            test("a bargained cast digs before it draws, in the order the controller chose") {
                val game = game(*sixCards)
                val library = game.state.getLibrary(game.player1Id)

                game.castSpellBargained(1, "Rowan's Grim Search", "Food").error shouldBe null
                game.resolveStack()

                val look = game.getPendingDecision()
                look.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("exactly the top four are looked at, and at most two may be kept") {
                    look.options shouldContainExactlyInAnyOrder library.take(4)
                    look.maxSelections shouldBe 2
                }

                game.selectCards(listOf(library[0], library[2]))

                // "In any order" — put the third card back above the first.
                val order = game.getPendingDecision()
                order.shouldBeInstanceOf<ReorderLibraryDecision>()
                order.cards shouldContainExactlyInAnyOrder listOf(library[0], library[2])
                game.submitDecision(OrderedResponse(order.id, listOf(library[2], library[0])))
                game.resolveStack()

                withClue("the kept cards were stacked before the draw, so they are what was drawn") {
                    game.state.getHand(game.player1Id) shouldContainExactly
                        listOf(library[2], library[0])
                }
                withClue("the other two looked-at cards were binned") {
                    game.state.getGraveyard(game.player1Id) shouldContainAll
                        listOf(library[1], library[3])
                    game.isInGraveyard(1, "Rowan's Grim Search") shouldBe true
                }
                withClue("nothing below the top four moved") {
                    game.state.getLibrary(game.player1Id) shouldContainExactly library.drop(4)
                }
                game.getLifeTotal(1) shouldBe 18
                game.isOnBattlefield("Food") shouldBe false
            }

            test("keeping none bins all four and the draw comes off the new top") {
                val game = game(*sixCards)
                val library = game.state.getLibrary(game.player1Id)

                game.castSpellBargained(1, "Rowan's Grim Search", "Food").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.skipSelection()

                withClue("nothing kept means nothing to order — no second prompt") {
                    game.hasPendingDecision() shouldBe false
                }
                game.resolveStack()

                withClue("the four looked-at cards are in the graveyard") {
                    game.state.getGraveyard(game.player1Id) shouldContainAll library.take(4)
                }
                withClue("the draw came off what was underneath them") {
                    game.state.getHand(game.player1Id) shouldContainExactly library.drop(4)
                    game.librarySize(1) shouldBe 0
                }
                game.getLifeTotal(1) shouldBe 18
            }

            test("a library of fewer than four cards offers what is there instead of deadlocking") {
                val game = game("Grizzly Bears", "Centaur Courser", "Lightning Bolt")
                val library = game.state.getLibrary(game.player1Id)

                game.castSpellBargained(1, "Rowan's Grim Search", "Food").error shouldBe null
                game.resolveStack()

                val look = game.getPendingDecision()
                look.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("ChooseUpTo caps at the collection size") {
                    look.options shouldContainExactlyInAnyOrder library
                }

                game.selectCards(listOf(library[0], library[1]))
                val order = game.getPendingDecision()
                order.shouldBeInstanceOf<ReorderLibraryDecision>()
                game.submitDecision(OrderedResponse(order.id, listOf(library[0], library[1])))
                game.resolveStack()

                game.state.getHand(game.player1Id) shouldContainExactly
                    listOf(library[0], library[1])
                game.state.getGraveyard(game.player1Id) shouldContainAll listOf(library[2])
                game.librarySize(1) shouldBe 0
                game.getLifeTotal(1) shouldBe 18
            }
        }
    }
}
