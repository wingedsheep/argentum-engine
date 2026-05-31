package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Villainous Wealth (KTK) — {X}{B}{G}{U} Sorcery.
 *
 * "Target opponent exiles the top X cards of their library. You may cast any number of spells
 * with mana value X or less from among them without paying their mana costs."
 *
 * Shares the [com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect]
 * primitive with Kotis, the Fangkeeper, but reaches it through an {X} sorcery targeting a player
 * rather than a combat-damage trigger. Confirms the migration off the old until-end-of-turn grant:
 * the free casts happen during the spell's resolution (the 2014-09-20 ruling: "you can't wait to
 * cast them later in the turn"), and only nonland cards with mana value ≤ X are offered.
 */
class VillainousWealthScenarioTest : ScenarioTestBase() {

    init {
        context("Villainous Wealth") {

            test("exiles top X of the opponent's library and casts a mana-value-≤-X spell for free during resolution") {
                // X = 2: exile the top two of the opponent's library — Grizzly Bears (MV 2,
                // castable) and Hill Giant (MV 4, too big).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Villainous Wealth")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Villainous Wealth"
                }
                val result = game.execute(
                    CastSpell(game.player1Id, cardId, listOf(ChosenTarget.Player(game.player2Id)), xValue = 2)
                )
                withClue("Casting Villainous Wealth for X=2 should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("X = 2 → the opponent exiles the top two cards of their library") {
                    namesInExile(game, 2) shouldBe setOf("Grizzly Bears", "Hill Giant")
                }

                val decision = game.getPendingDecision()
                withClue("Villainous Wealth pauses for a cast-from-exile choice during resolution") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                decision as SelectCardsDecision
                withClue("Only the nonland card with mana value ≤ X (Grizzly Bears) is offered") {
                    optionNames(game, decision) shouldBe setOf("Grizzly Bears")
                }

                val bearsId = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                game.selectCards(listOf(bearsId))
                game.resolveStack()

                withClue("The free-cast Grizzly Bears resolves onto the battlefield (under the caster's control)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant (MV 4 > X) stays in exile") {
                    namesInExile(game, 2) shouldBe setOf("Hill Giant")
                }
            }
        }
    }

    private fun optionNames(game: TestGame, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    private fun namesInExile(game: TestGame, playerNumber: Int): Set<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }.toSet()
    }
}
