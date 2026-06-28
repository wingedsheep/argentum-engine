package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PermanentLeftBattlefieldThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Krang & Shredder's "Disappear" end-step ability (TMT).
 *
 * "At the beginning of your end step, if a permanent left the battlefield under your control this
 *  turn, you may cast a card exiled with Krang & Shredder without paying its mana cost."
 *
 * Regression guard: the ability casts exactly ONE card (the player chooses), not the entire
 * accumulated linked-exile pile. The earlier implementation granted a blanket
 * play-from-exile-without-paying permission over the whole pile, letting the controller cast every
 * card exiled with Krang for free that turn. The fix gathers the pile, asks the player to pick one,
 * and grants the free cast on just that card.
 */
class KrangAndShredderScenarioTest : ScenarioTestBase() {

    init {
        context("Krang & Shredder Disappear") {

            test("casts only the chosen card from the linked-exile pile, leaving the rest exiled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Krang & Shredder")
                    .withCardInExile(2, "Centaur Courser") // two opponent cards "exiled with Krang"
                    .withCardInExile(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val krang = game.findPermanent("Krang & Shredder")!!
                fun exiledId(name: String) = game.state.getExile(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == name
                }
                val centaur = exiledId("Centaur Courser")
                val grizzly = exiledId("Grizzly Bears")

                // Seed the linked-exile pile (as if Krang's enter/attack triggers had exiled both) and
                // satisfy "a permanent left the battlefield under your control this turn".
                game.state = game.state
                    .updateEntity(krang) { it.with(LinkedExileComponent(listOf(centaur, grizzly))) }
                    .updateEntity(game.player1Id) { it.with(PermanentLeftBattlefieldThisTurnComponent(count = 1)) }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                // Disappear is a "you may" — accept it.
                withClue("Disappear asks whether to cast a card") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)

                // The player picks ONE card from the exiled pile (not the whole pile).
                val select = game.getPendingDecision()
                withClue("Disappear presents a single-card selection over the exiled pile") {
                    (select is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf(centaur))
                game.resolveStack()

                // The chosen card is actually CAST during resolution (cascade-style) and enters the
                // battlefield under the Disappear controller — not merely granted a "play it later"
                // permission, which the earlier implementation left to expire unused at end of turn.
                val centaurOnBattlefield = game.findPermanent("Centaur Courser")
                withClue("The chosen card (Centaur Courser) entered the battlefield") {
                    centaurOnBattlefield shouldNotBe null
                }
                withClue("...under the Disappear controller's control, even though the opponent owns it") {
                    game.state.projectedState.getController(centaurOnBattlefield!!) shouldBe game.player1Id
                }

                // The other exiled card was NOT cast — only the one card the player chose, not the
                // whole linked-exile pile.
                withClue("The other card (Grizzly Bears) stays exiled") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("The chosen card is no longer in exile") {
                    game.isInExile(2, "Centaur Courser") shouldBe false
                }
            }

            // Regression: Krang exiles the opponent's library "until they exile a nonland card", so
            // the linked-exile pile routinely contains LAND cards. "you may CAST a card" — lands are
            // played, not cast (CR 305/601), so an exiled land must never be offered or playable here.
            test("a land in the linked-exile pile is never offered — only nonland cards can be cast") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Krang & Shredder")
                    .withCardInExile(2, "Grizzly Bears")  // nonland — castable
                    .withCardInExile(2, "Centaur Courser") // nonland — castable
                    .withCardInExile(2, "Forest")          // land — must NOT be castable
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val krang = game.findPermanent("Krang & Shredder")!!
                fun exiledId(name: String) = game.state.getExile(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == name
                }
                val grizzly = exiledId("Grizzly Bears")
                val centaur = exiledId("Centaur Courser")
                val forest = exiledId("Forest")

                game.state = game.state
                    .updateEntity(krang) { it.with(LinkedExileComponent(listOf(grizzly, centaur, forest))) }
                    .updateEntity(game.player1Id) { it.with(PermanentLeftBattlefieldThisTurnComponent(count = 1)) }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(true)

                val select = game.getPendingDecision()
                (select is SelectCardsDecision) shouldBe true
                val offered = (select as SelectCardsDecision).cardInfo!!
                withClue("Both nonland cards are offered to cast") {
                    offered.values.any { it.name == "Grizzly Bears" } shouldBe true
                    offered.values.any { it.name == "Centaur Courser" } shouldBe true
                }
                withClue("The land is NOT offered — it can't be cast") {
                    offered.values.any { it.name == "Forest" } shouldBe false
                }

                game.selectCards(listOf(grizzly))
                game.resolveStack()

                withClue("The chosen nonland was cast and entered the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
                withClue("The land stays exiled — never played") {
                    game.isInExile(2, "Forest") shouldBe true
                    game.isOnBattlefield("Forest") shouldBe false
                }
            }
        }
    }
}
