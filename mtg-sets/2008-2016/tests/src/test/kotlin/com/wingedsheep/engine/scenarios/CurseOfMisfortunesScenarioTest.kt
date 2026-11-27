package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Curse of Misfortunes (DKA #56) — {4}{B} Enchantment — Aura Curse.
 *
 * "Enchant player. At the beginning of your upkeep, you may search your library for a Curse card
 * that doesn't have the same name as a Curse attached to enchanted player, put it onto the
 * battlefield attached to that player, then shuffle."
 *
 * Pins that the fetched Curse enters attached to the *enchanted player* under the searcher's
 * control, that every Curse name already on that player is excluded (Curse of Misfortunes itself
 * and a Curse attached earlier), that non-Curse cards aren't offered, and that declining searches
 * nothing.
 */
class CurseOfMisfortunesScenarioTest : ScenarioTestBase() {

    private fun board(opponentHasLeyline: Boolean = false, prepare: (TestGame) -> Unit = {}): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Curse of Misfortunes")
            .withCardOnBattlefield(1, "Curse of Exhaustion")
            .withCardInLibrary(1, "Curse of Misfortunes")
            .withCardInLibrary(1, "Curse of Exhaustion")
            .withCardInLibrary(1, "Curse of Opulence")
            .withCardInLibrary(1, "Grizzly Bears")
            .apply { repeat(3) { withCardInLibrary(1, "Swamp") } }
            .withCardInLibrary(2, "Swamp")
            .apply { if (opponentHasLeyline) withCardOnBattlefield(2, "Leyline of Sanctity") }
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()
        for (name in listOf("Curse of Misfortunes", "Curse of Exhaustion")) {
            val curse = game.findPermanent(name)!!
            game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
                .updateEntity(game.player2Id) { player ->
                    val existing = player.get<AttachmentsComponent>()?.attachedIds.orEmpty()
                    player.with(AttachmentsComponent(existing + curse))
                }
        }
        prepare(game)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("searching finds only a Curse whose name isn't already on the enchanted player, and attaches it to them") {
            val game = board()
            val opulence = game.findCardsInLibrary(1, "Curse of Opulence").single()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Curse of Misfortunes and Curse of Exhaustion are already on the opponent; Grizzly Bears isn't a Curse") {
                decision.options shouldContainExactlyInAnyOrder listOf(opulence)
            }
            game.selectCards(listOf(opulence)).error shouldBe null
            game.resolveStack()

            val onBattlefield = game.findPermanent("Curse of Opulence")!!
            game.state.getEntity(onBattlefield)!!.get<AttachedToComponent>()?.targetId shouldBe game.player2Id
            game.state.projectedState.getController(onBattlefield) shouldBe game.player1Id
            game.findCardsInLibrary(1, "Curse of Opulence").shouldBeEmpty()
            withClue("the opponent records it, so the next upkeep's name check sees it") {
                game.state.getEntity(game.player2Id)!!.get<AttachmentsComponent>()!!.attachedIds shouldContain onBattlefield
            }
        }

        test("hexproof doesn't stop it: nothing is targeted") {
            val game = board(opponentHasLeyline = true)
            val opulence = game.findCardsInLibrary(1, "Curse of Opulence").single()
            game.answerYesNo(true).error shouldBe null
            game.selectCards(listOf(opulence)).error shouldBe null
            game.resolveStack()

            val onBattlefield = game.findPermanent("Curse of Opulence")!!
            game.state.getEntity(onBattlefield)!!.get<AttachedToComponent>()?.targetId shouldBe game.player2Id
        }

        test("a player with protection from the Curse's colour can't be enchanted by it, so it stays in the library") {
            val game = board { g ->
                g.state = g.state.updateEntity(g.player2Id) {
                    it.with(PlayerProtectionComponent(listOf(PlayerProtectionComponent.Grant(ProtectionScope.Color(Color.RED)))))
                }
            }
            val opulence = game.findCardsInLibrary(1, "Curse of Opulence").single()
            game.answerYesNo(true).error shouldBe null
            game.selectCards(listOf(opulence)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Curse of Opulence") shouldBe null
            game.findCardsInLibrary(1, "Curse of Opulence").size shouldBe 1
        }

        test("declining leaves the library alone") {
            val game = board()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.findPermanent("Curse of Opulence") shouldBe null
            game.findCardsInLibrary(1, "Curse of Opulence").size shouldBe 1
        }
    }
}
