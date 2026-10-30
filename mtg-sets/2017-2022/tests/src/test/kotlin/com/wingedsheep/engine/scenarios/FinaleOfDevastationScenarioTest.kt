package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Finale of Devastation (WAR #160) — {X}{G}{G} Sorcery.
 *
 * Search your library and/or graveyard for a creature card with mana value X or less and put it
 * onto the battlefield. If you search your library this way, shuffle. If X is 10 or more, creatures
 * you control get +X/+X and gain haste until end of turn.
 *
 * Pins that X still bounds the search after the library / graveyard choice pauses the spell.
 */
class FinaleOfDevastationScenarioTest : ScenarioTestBase() {

    init {
        test("X = 3: after choosing the library, a mana value 3 creature is offered and a 4 is not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Finale of Devastation")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Finale of Devastation", 3).error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(choice.id, choice.options.indexOf("Search your library"))).error shouldBe null

            val courser = game.findCardsInLibrary(1, "Centaur Courser").single()
            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            search.options shouldBe listOf(courser)
            game.selectCards(listOf(courser)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Centaur Courser") shouldNotBe null
        }
    }
}
