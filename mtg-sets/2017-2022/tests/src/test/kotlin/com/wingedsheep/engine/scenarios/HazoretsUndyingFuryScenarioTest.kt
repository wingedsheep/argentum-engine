package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hazoret's Undying Fury — {4}{R}{R} Sorcery.
 *
 * "Shuffle your library, then exile the top four cards. You may cast any number of spells with mana
 * value 5 or less from among them without paying their mana costs. Lands you control don't untap
 * during your next untap step."
 *
 * The caster's library holds exactly four cards, so the shuffle can't change which four are exiled.
 * Only the nonland cards with mana value ≤ 5 are offered; the MV-6 creature and the land stay in
 * exile, and the caster's lands skip their next untap step.
 */
class HazoretsUndyingFuryScenarioTest : ScenarioTestBase() {

    init {
        test("casts the eligible exiled spells free, leaves the rest in exile, and holds back the caster's lands") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hazoret's Undying Fury")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Craw Wurm")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Hazoret's Undying Fury")
            withClue("casting should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("all four library cards are exiled") {
                namesInExile(game) shouldBe setOf("Grizzly Bears", "Hill Giant", "Craw Wurm", "Mountain")
            }

            val first = game.getPendingDecision()
            withClue("the free-cast choice is raised during resolution: $first") {
                (first is SelectCardsDecision) shouldBe true
            }
            first as SelectCardsDecision
            withClue("only the nonland cards with mana value 5 or less are offered") {
                optionNames(game, first) shouldBe setOf("Grizzly Bears", "Hill Giant")
            }
            game.selectCards(listOf(first.options.single { nameOf(game, it) == "Grizzly Bears" })).error shouldBe null

            val second = game.getPendingDecision()
            withClue("any number: the prompt returns with the remaining eligible spell: $second") {
                (second is SelectCardsDecision) shouldBe true
            }
            second as SelectCardsDecision
            optionNames(game, second) shouldBe setOf("Hill Giant")
            game.selectCards(second.options).error shouldBe null
            game.resolveStack()

            withClue("both free spells resolve onto the battlefield") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
            withClue("the MV-6 creature and the land are never castable and stay in exile") {
                namesInExile(game) shouldBe setOf("Craw Wurm", "Mountain")
            }

            val mountains = game.findPermanents("Mountain")
            withClue("the six Mountains paid for the spell") {
                mountains.size shouldBe 6
                mountains.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            }

            // Through the opponent's turn to the caster's next upkeep (after their untap step,
            // before their draw from the now-empty library).
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("now in the opponent's upkeep") { game.state.activePlayerId shouldBe game.player2Id }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("now in the caster's upkeep") { game.state.activePlayerId shouldBe game.player1Id }

            withClue("the caster's lands didn't untap during their next untap step") {
                mountains.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            }
        }
    }

    private fun nameOf(game: TestGame, id: EntityId): String? =
        game.state.getEntity(id)?.get<CardComponent>()?.name

    private fun optionNames(game: TestGame, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { nameOf(game, it) }.toSet()

    private fun namesInExile(game: TestGame): Set<String> =
        game.state.getExile(game.player1Id).mapNotNull { nameOf(game, it) }.toSet()
}
