package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Connive // Concoct (GRN 222).
 *
 * Concoct: "Surveil 3, then return a creature card from your graveyard to the battlefield." The
 * creature card is chosen only after the surveil, so a card just surveilled into the graveyard is
 * eligible (2018-10-05 ruling) — and only your own graveyard is searched.
 */
class ConniveConcoctScenarioTest : ScenarioTestBase() {

    init {
        test("Concoct can return a creature card it just surveilled into the graveyard") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Connive // Concoct")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Savannah Lions")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Connive // Concoct").first()
            game.execute(CastSpell(game.player1Id, cardId, faceIndex = 1)).error.shouldBeNull()
            game.resolveStack()

            val surveil = game.getPendingDecision()
            surveil.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("surveil looks at exactly the top three cards") { surveil.options.size shouldBe 3 }
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()
            game.selectCards(listOf(giant)).error.shouldBeNull()
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

            val choice = game.getPendingDecision()
            choice.shouldBeInstanceOf<SelectCardsDecision>()
            val bears = game.state.getGraveyard(game.player1Id).single { it != giant && it != cardId }
            val lions = game.state.getGraveyard(game.player2Id).single()
            withClue("the just-surveilled creature card and the one already there are both offered") {
                choice.options shouldContain giant
                choice.options shouldContain bears
            }
            withClue("an opponent's graveyard is not searched") { choice.options shouldNotContain lions }
            game.selectCards(listOf(giant)).error.shouldBeNull()
            game.resolveStack()

            withClue("Hill Giant returned to the battlefield") { game.findPermanent("Hill Giant").shouldNotBeNull() }
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Savannah Lions") shouldBe true
        }
    }
}
