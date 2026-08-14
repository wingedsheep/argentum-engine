package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Foggy Swamp Visions (Avatar: The Last Airbender) — the first registered card
 * using the **waterbend {X}** cost. X both bounds the targeting (`dynamicMaxCount = XValue`) and is
 * the waterbend amount paid by tapping permanents. The spell exiles X target creature cards from
 * graveyards and creates a token copy of each (which sacrifices itself at the next end step).
 */
class FoggySwampVisionsScenarioTest : ScenarioTestBase() {

    init {
        test("exiles X target creature cards and creates a token copy of each, paying waterbend {X} by tapping") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Foggy Swamp Visions")
                .withLandsOnBattlefield(1, "Swamp", 3)          // base {1}{B}{B}
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardOnBattlefield(1, "Glory Seeker")        // tap both to pay waterbend {2}
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Grizzly Bears")          // the X = 2 target creature cards
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val graveyardCreatures = game.findCardsInGraveyard(1, "Grizzly Bears")
            graveyardCreatures.size shouldBe 2
            val tappers = game.findAllPermanents("Glory Seeker")
            tappers.size shouldBe 2

            val action = game.getLegalActions(1).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.isAffordable && it.hasTapForGeneric
            }
            withClue("Foggy Swamp Visions should be offered as an X-carrying waterbend cast") {
                action shouldNotBe null
                action!!.hasXCost shouldBe true
            }

            // Choose X = 2: target the two graveyard creatures, and tap the two creatures to pay waterbend {2}.
            val cast = (action!!.action as CastSpell).copy(
                xValue = 2,
                targets = graveyardCreatures.map { ChosenTarget.Card(it, game.player1Id, Zone.GRAVEYARD) },
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = tappers.toSet()),
            )
            val result = game.execute(cast)
            withClue("casting Foggy Swamp Visions for waterbend {X=2} should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("both targeted creature cards are exiled (no longer in the graveyard)") {
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
            }
            withClue("a token copy of each exiled creature card is created on the battlefield") {
                game.findAllPermanents("Grizzly Bears").size shouldBe 2
            }
        }
    }
}
