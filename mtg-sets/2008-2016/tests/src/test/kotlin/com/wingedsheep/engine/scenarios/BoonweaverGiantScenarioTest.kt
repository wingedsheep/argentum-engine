package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Boonweaver Giant (M15 #5) — "When this creature enters, you may search your graveyard, hand,
 * and/or library for an Aura card and put it onto the battlefield attached to this creature. If you
 * search your library this way, shuffle."
 *
 * Pins each zone's Aura entering attached, that an Aura which can't enchant the Giant isn't offered,
 * that leaving the library out of the search leaves it unshuffled, that "you may" declines, and the
 * ruling that with the Giant gone no Aura enters.
 */
class BoonweaverGiantScenarioTest : ScenarioTestBase() {

    private val allZones = "Search your graveyard, hand, and library"
    private val graveyardAndHand = "Search your graveyard and hand"

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Boonweaver Giant")
        .withCardInHand(1, "Holy Strength")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Unholy Strength")
        .withCardInGraveyard(1, "Erosion")
        .withCardInLibrary(1, "Spirit Link")
        .apply { repeat(4) { withCardInLibrary(1, "Plains") } }
        .withLandsOnBattlefield(1, "Plains", 7)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.choose(option: String) {
        val choice = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(choice.id, choice.options.indexOf(option))).error shouldBe null
    }

    private fun TestGame.castGiant() {
        castSpell(1, "Boonweaver Giant").error shouldBe null
        resolveStack()
    }

    private fun TestGame.attachedHost(cardName: String) =
        state.getEntity(findPermanent(cardName)!!)!!.get<AttachedToComponent>()?.targetId

    init {
        context("Boonweaver Giant") {

            test("searching all three zones offers every Aura that could enchant it; a library Aura enters attached") {
                val game = board().build()
                val holy = game.findCardsInHand(1, "Holy Strength").single()
                val unholy = game.findCardsInGraveyard(1, "Unholy Strength").single()
                val link = game.findCardsInLibrary(1, "Spirit Link").single()

                game.castGiant()
                game.choose(allZones)

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Erosion (enchant land) and the non-Aura Grizzly Bears are not offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(holy, unholy, link)
                }
                game.selectCards(listOf(link)).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Boonweaver Giant")!!
                game.attachedHost("Spirit Link") shouldBe giant
                game.findCardsInLibrary(1, "Spirit Link").size shouldBe 0
            }

            test("an Aura from hand enters attached to the Giant") {
                val game = board().build()
                val holy = game.findCardsInHand(1, "Holy Strength").single()

                game.castGiant()
                game.choose(graveyardAndHand)
                game.selectCards(listOf(holy)).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Boonweaver Giant")!!
                game.attachedHost("Holy Strength") shouldBe giant
                game.state.projectedState.getPower(giant) shouldBe 5
                game.state.projectedState.getToughness(giant) shouldBe 6
                game.isInHand(1, "Holy Strength") shouldBe false
            }

            test("an Aura from the graveyard enters attached, and leaving the library out doesn't shuffle it") {
                val game = board().build()
                val unholy = game.findCardsInGraveyard(1, "Unholy Strength").single()
                val libraryBefore = game.state.getLibrary(game.player1Id)

                game.castGiant()
                game.choose(graveyardAndHand)

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the library's Spirit Link is not in a graveyard-and-hand search") {
                    decision.options shouldContainExactlyInAnyOrder
                        listOf(game.findCardsInHand(1, "Holy Strength").single(), unholy)
                }
                game.selectCards(listOf(unholy)).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Boonweaver Giant")!!
                game.attachedHost("Unholy Strength") shouldBe giant
                game.state.projectedState.getPower(giant) shouldBe 6
                game.state.projectedState.getToughness(giant) shouldBe 5
                withClue("the library keeps its order") {
                    game.state.getLibrary(game.player1Id) shouldBe libraryBefore
                }
            }

            test("declining searches nothing") {
                val game = board().build()
                val libraryBefore = game.state.getLibrary(game.player1Id)

                game.castGiant()
                game.choose("Don't search")
                game.resolveStack()

                game.isOnBattlefield("Holy Strength") shouldBe false
                game.isOnBattlefield("Unholy Strength") shouldBe false
                game.isOnBattlefield("Spirit Link") shouldBe false
                game.isInHand(1, "Holy Strength") shouldBe true
                game.isInGraveyard(1, "Unholy Strength") shouldBe true
                game.state.getLibrary(game.player1Id) shouldBe libraryBefore
            }

            test("with the Giant gone by resolution, the chosen Aura stays where it was") {
                val game = board().build()
                game.castSpell(1, "Boonweaver Giant").error shouldBe null
                // Resolve the creature spell only; its enters trigger goes on the stack.
                game.passPriority()
                game.passPriority()
                val giant = game.findPermanent("Boonweaver Giant")!!
                withClue("the enters trigger is waiting on the stack") { game.state.stack.size shouldBe 1 }

                game.state = zones.moveToZone(game.state, giant, Zone.GRAVEYARD).state
                game.resolveStack()
                game.choose(graveyardAndHand)
                val holy = game.findCardsInHand(1, "Holy Strength").single()
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(listOf(holy)).error shouldBe null
                }
                game.resolveStack()

                withClue("Holy Strength never enters without the Giant to enchant") {
                    game.isOnBattlefield("Holy Strength") shouldBe false
                    game.isInHand(1, "Holy Strength") shouldBe true
                }
            }
        }
    }
}
