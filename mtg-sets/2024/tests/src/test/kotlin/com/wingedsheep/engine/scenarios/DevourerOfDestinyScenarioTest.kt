package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.DevourerOfDestiny
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DevourerOfDestinyScenarioTest : ScenarioTestBase() {

    /** A real game start with Devourer of Destiny in P1's opening hand, both players keeping. */
    private fun openingHandWithDevourer(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + DevourerOfDestiny)
        d.initGame(
            Deck.of("Devourer of Destiny" to 1, "Forest" to 20, "Island" to 19),
            Deck.of("Island" to 40),
            skipMulligans = false
        )
        var s = d.state
        val p1 = d.player1
        val devourer = s.getLibrary(p1).firstOrNull {
            s.getEntity(it)?.get<CardComponent>()?.name == "Devourer of Destiny"
        }
        if (devourer != null) {
            val swapped = s.getHand(p1).first()
            s = s.removeFromZone(ZoneKey(p1, Zone.LIBRARY), devourer).addToZone(ZoneKey(p1, Zone.HAND), devourer)
                .removeFromZone(ZoneKey(p1, Zone.HAND), swapped).addToZone(ZoneKey(p1, Zone.LIBRARY), swapped)
            d.replaceState(s)
        }
        d.submit(KeepHand(d.player1)).error shouldBe null
        d.submit(KeepHand(d.player2)).error shouldBe null
        return d
    }

    init {
        test("revealed from the opening hand: at the first upkeep keep one of the top four, exile the rest") {
            val d = openingHandWithDevourer()
            val p1 = d.player1
            val prompt = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            prompt.prompt shouldBe "Reveal Devourer of Destiny from your opening hand?"
            d.submitYesNo(p1, true).error shouldBe null

            val topFour = d.state.getLibrary(p1).take(4)
            d.passPriorityUntil(Step.UPKEEP)
            // The delayed trigger is on the stack; resolve it.
            d.bothPass()
            val selection = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            selection.options.toSet() shouldBe topFour.toSet()
            val kept = topFour[2]
            d.submitCardSelection(p1, listOf(kept)).error shouldBe null
            d.pendingDecision shouldBe null

            d.state.getLibrary(p1).first() shouldBe kept
            d.state.getZone(ZoneKey(p1, Zone.EXILE)) shouldContainExactlyInAnyOrder topFour - kept
            d.state.getHand(p1).any { d.state.getEntity(it)?.get<CardComponent>()?.name == "Devourer of Destiny" } shouldBe true
        }

        test("putting none back exiles all four") {
            val d = openingHandWithDevourer()
            val p1 = d.player1
            d.submitYesNo(p1, true).error shouldBe null
            val topFour = d.state.getLibrary(p1).take(4)
            d.passPriorityUntil(Step.UPKEEP)
            d.bothPass()
            d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            d.submitCardSelection(p1, emptyList()).error shouldBe null
            d.state.getZone(ZoneKey(p1, Zone.EXILE)) shouldContainExactlyInAnyOrder topFour
        }

        test("not revealing leaves the library untouched") {
            val d = openingHandWithDevourer()
            val p1 = d.player1
            d.submitYesNo(p1, false).error shouldBe null
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.state.getZone(ZoneKey(p1, Zone.EXILE)) shouldBe emptyList()
        }

        test("when you cast it, exile target permanent that's one or more colors") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Devourer of Destiny")
                .withLandsOnBattlefield(1, "Snow-Covered Wastes", 7)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, "Devourer of Destiny").single()))
                .error shouldBe null
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Devourer of Destiny") shouldBe true
        }
    }
}
