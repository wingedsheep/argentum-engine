package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rushed Rebirth (STX #228) — "Choose target creature. When that creature dies this turn, search
 * your library for a creature card with lesser mana value, put it onto the battlefield tapped,
 * then shuffle."
 *
 * Pins down the delayed trigger's search: it is capped strictly below the dead creature's mana
 * value (read after it has left the battlefield), the card enters tapped, and the search is always
 * the caster's own library — even when the watched creature was an opponent's.
 */
class RushedRebirthScenarioTest : ScenarioTestBase() {

    init {
        fun setup(giantOwner: Int): TestGame = scenario()
            .withPlayers("Caster", "Opponent")
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInHand(1, "Rushed Rebirth")
            .withCardInHand(1, "Lightning Bolt")
            .withCardOnBattlefield(giantOwner, "Hill Giant")
            .withCardInLibrary(1, "Llanowar Elves")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Serra Angel")
            .withCardInLibrary(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.rebirthAndBolt() {
            val giant = findPermanent("Hill Giant")!!
            castSpell(1, "Rushed Rebirth", giant).error shouldBe null
            resolveStack()
            castSpell(1, "Lightning Bolt", giant).error shouldBe null
            resolveStack()
        }

        test("finds a creature card of lesser mana value and puts it onto the battlefield tapped") {
            val game = setup(giantOwner = 1)
            game.rebirthAndBolt()

            val search = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only creature cards with mana value less than Hill Giant's 4 are offered") {
                search.options shouldContainExactlyInAnyOrder
                    game.findCardsInLibrary(1, "Llanowar Elves") + game.findCardsInLibrary(1, "Grizzly Bears")
            }
            game.selectCards(game.findCardsInLibrary(1, "Grizzly Bears")).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getController(bears) shouldBe game.player1Id
            withClue("the found creature enters tapped") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }
        }

        test("when an opponent's creature dies, the caster searches their own library") {
            val game = setup(giantOwner = 2)
            game.rebirthAndBolt()

            val search = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            search.playerId shouldBe game.player1Id
            search.options shouldContainExactlyInAnyOrder
                game.findCardsInLibrary(1, "Llanowar Elves") + game.findCardsInLibrary(1, "Grizzly Bears")
            game.selectCards(game.findCardsInLibrary(1, "Llanowar Elves")).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getController(game.findPermanent("Llanowar Elves")!!) shouldBe game.player1Id
        }
    }
}
