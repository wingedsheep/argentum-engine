package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Curse of Oblivion (ISD #95) — {3}{B} Enchantment — Aura Curse.
 *
 * "Enchant player. At the beginning of enchanted player's upkeep, that player exiles two cards from
 * their graveyard."
 *
 * Pins that the *enchanted player* (not the Curse's controller) picks the two cards from their own
 * graveyard, that the Curse's controller's graveyard is untouched, and that a graveyard with fewer
 * than two cards loses what it has without a prompt.
 */
class CurseOfOblivionScenarioTest : ScenarioTestBase() {

    private fun board(opponentGraveyard: List<String>): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Curse of Oblivion")
            .withCardInGraveyard(1, "Grizzly Bears")
            .apply { opponentGraveyard.forEach { withCardInGraveyard(2, it) } }
            .apply { repeat(3) { withCardInLibrary(1, "Swamp") } }
            .apply { repeat(3) { withCardInLibrary(2, "Swamp") } }
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()
        val curse = game.findPermanent("Curse of Oblivion")!!
        game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
            .updateEntity(game.player2Id) { player ->
                val existing = player.get<AttachmentsComponent>()?.attachedIds.orEmpty()
                player.with(AttachmentsComponent(existing + curse))
            }
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("the enchanted player chooses two cards from their own graveyard to exile") {
            val game = board(listOf("Hill Giant", "Raging Goblin", "Llanowar Elves"))
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()
            val goblin = game.findCardsInGraveyard(2, "Raging Goblin").single()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the enchanted player decides, not the Curse's controller") {
                decision.playerId shouldBe game.player2Id
            }
            decision.options.size shouldBe 3
            game.selectCards(listOf(giant, goblin)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.isInExile(2, "Raging Goblin") shouldBe true
            game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            withClue("the Curse's controller's graveyard is untouched") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }

        test("with only one card in the graveyard, that card is exiled") {
            val game = board(listOf("Hill Giant"))

            game.isInExile(2, "Hill Giant") shouldBe true
            game.graveyardSize(2) shouldBe 0
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
