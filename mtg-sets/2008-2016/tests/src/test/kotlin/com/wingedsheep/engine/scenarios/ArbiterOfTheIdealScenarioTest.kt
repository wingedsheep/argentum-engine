package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Arbiter of the Ideal (BNG #31) — "Inspired — Whenever this creature becomes untapped, reveal the
 * top card of your library. If it's an artifact, creature, or land card, you may put it onto the
 * battlefield with a manifestation counter on it. That permanent is an enchantment in addition to
 * its other types."
 */
class ArbiterOfTheIdealScenarioTest : ScenarioTestBase() {

    /** Arbiter starts tapped on P1's side; P1's next untap step fires inspired. */
    private fun untapArbiter(topCard: String, withEidolon: Boolean = false): TestGame {
        val builder = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Arbiter of the Ideal", tapped = true)
            .withCardInLibrary(1, topCard)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        if (withEidolon) builder.withCardOnBattlefield(1, "Eidolon of Blossoms")
        val game = builder.build()

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("a revealed creature card enters as an enchantment with a manifestation counter") {
            val game = untapArbiter("Grizzly Bears")

            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            val permanent = game.findPermanents("Grizzly Bears").single()
            val projected = game.state.projectedState
            withClue("it is an enchantment creature carrying one manifestation counter") {
                projected.hasType(permanent, "ENCHANTMENT") shouldBe true
                projected.isCreature(permanent) shouldBe true
                game.state.getEntity(permanent)?.get<CountersComponent>()
                    ?.getCount(CounterType.MANIFESTATION) shouldBe 1
            }
        }

        test("entering as an enchantment triggers constellation") {
            val game = untapArbiter("Grizzly Bears", withEidolon = true)
            val handBefore = game.state.getZone(game.player1Id, Zone.HAND).size

            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Eidolon of Blossoms drew a card off the entering enchantment") {
                game.state.getZone(game.player1Id, Zone.HAND).size shouldBe handBefore + 1
            }
        }

        test("declining leaves the revealed card on top of the library") {
            val game = untapArbiter("Grizzly Bears")

            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.findPermanents("Grizzly Bears").shouldBeEmpty()
            game.state.getZone(game.player1Id, Zone.LIBRARY).first() shouldBe bears
        }

        test("a revealed instant can't be put onto the battlefield") {
            val game = untapArbiter("Lightning Bolt")
            val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the instant is shown but not selectable") {
                decision.nonSelectableOptions.contains(bolt) shouldBe true
            }
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.LIBRARY).first() shouldBe bolt
        }
    }
}
