package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sanctum of All (M21 #225) — {W}{U}{B}{R}{G} Legendary Enchantment — Shrine.
 *
 * At the beginning of your upkeep, you may search your library and/or graveyard for a Shrine card
 * and put it onto the battlefield. If you search your library this way, shuffle.
 * If a triggered ability of another Shrine you control triggers while you control six or more
 * Shrines, that ability triggers an additional time.
 *
 * Pins the six-Shrine gate on the doubler (counting Sanctum of All itself), that it doubles only
 * *other* Shrines' triggers, and the upkeep search onto the battlefield.
 */
class SanctumOfAllScenarioTest : ScenarioTestBase() {

    /** Resolve the stack, ordering any simultaneous triggers in their presented order. */
    private fun drain(game: TestGame) {
        var guard = 0
        while (guard++ < 20) {
            val decision = game.getPendingDecision()
            if (decision is OrderTriggersDecision) {
                game.submitDecision(TriggerOrderResponse(decision.id, decision.triggers.indices.toList()))
                    .error shouldBe null
                continue
            }
            if (decision != null || game.state.stack.isEmpty()) return
            game.resolveStack()
        }
    }

    /**
     * Player 1 in their draw step with Sanctum of All, Sanctum of Stone Fangs and [others] Shrines
     * (none of which trigger at the first main phase), then advance to the first main phase.
     */
    private fun stoneFangsAtMain(otherShrines: List<String>): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Sanctum of All")
            .withCardOnBattlefield(1, "Sanctum of Stone Fangs")
        for (shrine in otherShrines) builder = builder.withCardOnBattlefield(1, shrine)
        val game = builder
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.DRAW)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        drain(game)
        return game
    }

    init {
        test("with six Shrines, another Shrine's triggered ability triggers an additional time") {
            val game = stoneFangsAtMain(
                listOf(
                    "Sanctum of Shattered Heights",
                    "Sanctum of Tranquil Light",
                    "Go-Shintai of Lost Wisdom",
                    "Go-Shintai of Hidden Cruelty",
                )
            )
            // Six Shrines: Stone Fangs drains 6, twice.
            game.getLifeTotal(1) shouldBe 20 + 12
            game.getLifeTotal(2) shouldBe 20 - 12
        }

        test("with five Shrines, the Shrine's triggered ability triggers once") {
            val game = stoneFangsAtMain(
                listOf(
                    "Sanctum of Shattered Heights",
                    "Sanctum of Tranquil Light",
                    "Go-Shintai of Lost Wisdom",
                )
            )
            game.getLifeTotal(1) shouldBe 20 + 5
            game.getLifeTotal(2) shouldBe 20 - 5
        }

        test("upkeep: Sanctum of All's own trigger isn't doubled and puts a Shrine from the graveyard onto the battlefield") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Sanctum of All")
                .withCardOnBattlefield(1, "Sanctum of Stone Fangs")
                .withCardOnBattlefield(1, "Sanctum of Shattered Heights")
                .withCardOnBattlefield(1, "Sanctum of Tranquil Light")
                .withCardOnBattlefield(1, "Go-Shintai of Lost Wisdom")
                .withCardOnBattlefield(1, "Go-Shintai of Hidden Cruelty")
                .withCardInGraveyard(1, "Honden of Cleansing Fire")
                .withCardInGraveyard(1, "Honden of Seeing Winds")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(choice.id, choice.options.indexOf("Search your graveyard")))
                .error shouldBe null
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val honden = game.findCardsInGraveyard(1, "Honden of Cleansing Fire").single()
            game.selectCards(listOf(honden)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Honden of Cleansing Fire") shouldBe true
            game.isInGraveyard(1, "Honden of Seeing Winds") shouldBe true
            // Sanctum of All's own ability is not "another Shrine": no second search prompt, and the
            // Honden that entered mid-upkeep doesn't trigger this upkeep (2020-06-23 ruling).
            game.getPendingDecision() shouldBe null
            game.state.stack.isEmpty() shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
