package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cmr.cards.OppositionAgent
import com.wingedsheep.mtg.sets.definitions.mir.cards.RampantGrowth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Opposition Agent (CMR #141; {2}{B}, 3/2, flash).
 *
 *   You control your opponents while they're searching their libraries.
 *   While an opponent is searching their library, they exile each card they find. You may play those
 *   cards for as long as they remain exiled, and you may spend mana as though it were mana of any
 *   color to cast them.
 *
 * The rules matrix lives in the engine's `ControlOpponentsLibrarySearchesTest`; this pins the card
 * against a real ramp spell: flashed in, it takes the opponent's Rampant Growth land.
 */
class OppositionAgentScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(OppositionAgent, RampantGrowth))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("flashed in, it takes the land an opponent's Rampant Growth finds") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, d.putCardInHand(me, "Rampant Growth")).error shouldBe null
        // The opponent responds by flashing in the Agent.
        d.passPriority(me)
        d.giveMana(opp, Color.BLACK, 3)
        d.castSpell(opp, d.putCardInHand(opp, "Opposition Agent")).error shouldBe null
        d.bothPass() // Agent resolves
        d.bothPass() // Rampant Growth resolves

        val pick = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the Agent's controller searches for the opponent") { pick.playerId shouldBe opp }
        val forest = pick.options.first()
        val landsBefore = d.state.getBattlefield().count { d.getController(it) == me }
        d.submitCardSelection(opp, listOf(forest)).error shouldBe null

        d.state.getBattlefield().count { d.getController(it) == me } shouldBe landsBefore
        d.getExile(me) shouldContain forest
        withClue("the Agent's controller may play the exiled Forest") {
            d.state.mayPlayPermissions.any { it.controllerId == opp && forest in it.cardIds } shouldBe true
        }
    }
})
