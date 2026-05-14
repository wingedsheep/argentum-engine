package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Wildsear, Scouring Maw (BLC).
 *
 * Enchantment spells you cast from your hand have cascade (CR 702.85a). Cast →
 * cascade trigger → exile until a nonland with lower mana value → ask the
 * controller whether to cast it for free → put every uncast exiled card on the
 * bottom of the library in a random order.
 */
class WildsearScouringMawScenarioTest : ScenarioTestBase() {

    init {
        context("Wildsear, Scouring Maw — cascade on enchantment cast from hand") {

            test("declining the may-cast bottoms every exiled card (cascade hit + skipped lands)") {
                val game = scenario()
                    .withPlayers("Wildsear Player", "Opponent")
                    .withCardOnBattlefield(1, "Wildsear, Scouring Maw")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Hoarder's Overflow")
                    // Library top-down: two Mountains (lands, skipped), then Elvish Pioneer
                    // (mv 1, the cascade hit), then a buffer so the library is non-empty.
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Elvish Pioneer")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.state.getLibrary(game.player1Id).size

                val castResult = game.castSpell(1, "Hoarder's Overflow")
                withClue("Casting the enchantment should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolving the stack reaches cascade resolution, which pauses for the
                // may-cast decision. answerYesNo(false) declines.
                game.resolveStack()
                game.state.pendingDecision shouldNotBe null
                (game.state.pendingDecision as? YesNoDecision) shouldNotBe null
                game.answerYesNo(false)
                // Resolve the enchantment itself.
                game.resolveStack()

                val exile = game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE))
                withClue("On decline, exile should be empty — every exiled card was bottomed") {
                    exile shouldHaveSize 0
                }

                val library = game.state.getLibrary(game.player1Id)
                withClue("Library size should be unchanged from before the cast (all exiled cards returned)") {
                    library shouldHaveSize librarySizeBefore
                }
                val bottomThree = library.takeLast(3).map { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name
                }
                withClue("The two skipped Mountains and the Elvish Pioneer should occupy the bottom three slots") {
                    bottomThree shouldContainAll listOf("Mountain", "Mountain", "Elvish Pioneer")
                }
            }

            test("accepting the may-cast puts the cascade card on the stack and bottoms the leftovers") {
                val game = scenario()
                    .withPlayers("Wildsear Player", "Opponent")
                    .withCardOnBattlefield(1, "Wildsear, Scouring Maw")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Hoarder's Overflow")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Elvish Pioneer")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hoarder's Overflow").error shouldBe null
                game.resolveStack()

                (game.state.pendingDecision as? YesNoDecision) shouldNotBe null
                game.answerYesNo(true)
                // Cascade card cast resolves (it's a 1/1 with a may-trigger that has no
                // legal target since no basic lands are in hand → auto-resolves), then
                // the enchantment resolves.
                game.resolveStack()

                val battlefield = game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD))
                val pioneerOnBattlefield = battlefield.any { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Elvish Pioneer"
                }
                withClue("On accept, Elvish Pioneer should be on the battlefield (cast for free + resolved)") {
                    pioneerOnBattlefield shouldBe true
                }

                val library = game.state.getLibrary(game.player1Id)
                val bottomTwo = library.takeLast(2).map { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name
                }
                withClue("The two skipped Mountains should be on the bottom of the library") {
                    bottomTwo shouldContainAll listOf("Mountain", "Mountain")
                }

                val exile = game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE))
                withClue("Exile should be empty — cascade card was cast, lands were bottomed") {
                    exile shouldHaveSize 0
                }

                // No lingering MayPlayPermission or PlayWithoutPayingCost on any card.
                game.state.getEntity(
                    library.firstOrNull { game.state.getEntity(it)?.get<CardComponent>()?.name == "Elvish Pioneer" }
                        ?: battlefield.first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Elvish Pioneer" }
                )?.get<PlayWithoutPayingCostComponent>() shouldBe null
            }

            test("library exhausted without a hit bottoms every exiled card and offers no may-cast") {
                // Library has only lands and no nonland with mv < 2 → cascade walks the
                // entire library, finds no hit, and bottoms everything without pausing.
                val game = scenario()
                    .withPlayers("Wildsear Player", "Opponent")
                    .withCardOnBattlefield(1, "Wildsear, Scouring Maw")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Hoarder's Overflow")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val librarySizeBefore = game.state.getLibrary(game.player1Id).size

                game.castSpell(1, "Hoarder's Overflow").error shouldBe null
                game.resolveStack()

                withClue("No may-cast pause should occur when the cascade walk finds no hit") {
                    game.state.pendingDecision shouldBe null
                }

                val library = game.state.getLibrary(game.player1Id)
                withClue("Library size should be unchanged from before the cast") {
                    library shouldHaveSize librarySizeBefore
                }
                val exile = game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE))
                withClue("Exile should be empty — every exiled card was bottomed") {
                    exile shouldHaveSize 0
                }
            }

            test("no cascade trigger when the enchantment is cast from somewhere other than hand") {
                // Putting the enchantment directly on the battlefield should NOT trigger
                // Wildsear (the trigger requires casting from hand).
                val game = scenario()
                    .withPlayers("Wildsear Player", "Opponent")
                    .withCardOnBattlefield(1, "Wildsear, Scouring Maw")
                    .withCardOnBattlefield(1, "Hoarder's Overflow")
                    .withCardInLibrary(1, "Elvish Pioneer")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.resolveStack()

                val exile = game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE))
                withClue("Exile should be empty — no cascade should have triggered") {
                    exile shouldHaveSize 0
                }
            }
        }
    }
}
