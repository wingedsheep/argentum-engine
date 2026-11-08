package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.Providence
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ProvidenceScenarioTest : ScenarioTestBase() {

    /**
     * A real game start with Providence in P1's opening hand and **P2 as the starting player**, so
     * the game's first upkeep is P2's — "the first upkeep" is not "your first upkeep".
     */
    private fun openingHandWithProvidence(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Providence)
        d.initGame(
            Deck.of("Providence" to 1, "Plains" to 39),
            Deck.of("Island" to 40),
            skipMulligans = false,
            startingPlayer = 1
        )
        var s = d.state
        val p1 = d.player1
        val providence = s.getLibrary(p1).firstOrNull {
            s.getEntity(it)?.get<CardComponent>()?.name == "Providence"
        }
        if (providence != null) {
            val swapped = s.getHand(p1).first()
            s = s.removeFromZone(ZoneKey(p1, Zone.LIBRARY), providence).addToZone(ZoneKey(p1, Zone.HAND), providence)
                .removeFromZone(ZoneKey(p1, Zone.HAND), swapped).addToZone(ZoneKey(p1, Zone.LIBRARY), swapped)
            d.replaceState(s)
        }
        d.submit(KeepHand(d.player2)).error shouldBe null
        d.submit(KeepHand(d.player1)).error shouldBe null
        return d
    }

    init {
        test("revealed: life becomes 26 at the game's first upkeep, even on the opponent's turn, and only once") {
            val d = openingHandWithProvidence()
            val p1 = d.player1
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            d.submitYesNo(p1, true).error shouldBe null

            d.passPriorityUntil(Step.UPKEEP)
            d.activePlayer shouldBe d.player2
            d.bothPass()
            d.getLifeTotal(p1) shouldBe 26
            d.getLifeTotal(d.player2) shouldBe 20
            d.state.getHand(p1).any { d.state.getEntity(it)?.get<CardComponent>()?.name == "Providence" } shouldBe true

            // The delayed trigger is spent: P1's own first upkeep doesn't reset the life total.
            d.setLifeTotal(p1, 10)
            d.passPriorityUntil(Step.UPKEEP, p1)
            d.state.stack shouldBe emptyList()
            d.getLifeTotal(p1) shouldBe 10
        }

        test("not revealed: the first upkeep leaves the life total alone") {
            val d = openingHandWithProvidence()
            val p1 = d.player1
            d.submitYesNo(p1, false).error shouldBe null
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.getLifeTotal(p1) shouldBe 20
        }

        test("cast from low life: life total becomes 26") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Providence")
                .withLandsOnBattlefield(1, "Plains", 7)
                .withLifeTotal(1, 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Providence").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 26
            game.getLifeTotal(2) shouldBe 20
        }

        test("cast from high life: life total drops to 26") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Providence")
                .withLandsOnBattlefield(1, "Plains", 7)
                .withLifeTotal(1, 40)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Providence").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 26
        }
    }
}
