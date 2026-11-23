package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Epic Experiment (RTR) — {X}{U}{R} Sorcery.
 *
 * "Exile the top X cards of your library. You may cast instant and sorcery spells with mana value X
 * or less from among them without paying their mana costs. Then put all cards exiled this way that
 * weren't cast into your graveyard."
 *
 * X = 3 exiles Divination (sorcery, MV 3 — castable), Grizzly Bears (a creature — never offered) and
 * Lava Axe (sorcery, MV 5 > X — never offered). The two Islands below stay in the library.
 */
class EpicExperimentScenarioTest : ScenarioTestBase() {

    init {
        context("Epic Experiment") {

            fun castEpicExperiment(): TestGame {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Epic Experiment")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Divination")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lava Axe")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Epic Experiment"
                }
                val result = game.execute(CastSpell(game.player1Id, cardId, xValue = 3))
                withClue("Casting Epic Experiment for X=3 should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()
                return game
            }

            test("casts a qualifying sorcery for free and puts the rest of the exiled cards into the graveyard") {
                val game = castEpicExperiment()

                val decision = game.getPendingDecision()
                withClue("Epic Experiment pauses for a cast-from-exile choice during resolution") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                decision as SelectCardsDecision
                withClue("Only the instant/sorcery with mana value ≤ X (Divination) is offered") {
                    optionNames(game, decision) shouldBe setOf("Divination")
                }

                game.selectCards(listOf(decision.options.single()))
                game.resolveStack()

                withClue("Divination was cast and resolved — it drew the two Islands") {
                    game.handSize(1) shouldBe 2
                }
                withClue("Nothing exiled by Epic Experiment stays in exile") {
                    exileNames(game) shouldBe emptySet()
                }
                withClue("The uncast creature and the too-expensive sorcery went to the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Lava Axe") shouldBe true
                    game.isInGraveyard(1, "Divination") shouldBe true
                    game.isInGraveyard(1, "Epic Experiment") shouldBe true
                }
            }

            test("declining the free cast puts every exiled card into the graveyard") {
                val game = castEpicExperiment()

                val decision = game.getPendingDecision()
                (decision is SelectCardsDecision) shouldBe true
                game.selectCards(emptyList())
                game.resolveStack()

                withClue("No card was drawn — Divination was not cast") {
                    game.handSize(1) shouldBe 0
                }
                withClue("Nothing exiled by Epic Experiment stays in exile") {
                    exileNames(game) shouldBe emptySet()
                }
                withClue("All three exiled cards went to the graveyard uncast") {
                    game.isInGraveyard(1, "Divination") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Lava Axe") shouldBe true
                }
            }
        }
    }

    private fun optionNames(game: TestGame, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    private fun exileNames(game: TestGame): Set<String> =
        game.state.getExile(game.player1Id).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }.toSet()
}
