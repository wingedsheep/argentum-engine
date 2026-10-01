package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Contagious Vorrac (ONE #164) — {2}{G} 3/3 Creature — Phyrexian Boar Beast.
 *
 * "When this creature enters, look at the top four cards of your library. You may reveal a land
 * card from among them and put it into your hand. Put the rest on the bottom of your library in a
 * random order. If you didn't put a card into your hand this way, proliferate."
 */
class ContagiousVorracScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun setup(withLandOnTop: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Contagious Vorrac")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withLandOnTop) builder.withCardInLibrary(1, "Plains")
        repeat(if (withLandOnTop) 3 else 4) { builder.withCardInLibrary(1, "Hill Giant") }
        builder.withCardInLibrary(1, "Grizzly Bears")
        val game = builder.build()
        seed(game, game.findPermanent("Grizzly Bears")!!, 1)
        return game
    }

    /** Resolve everything, answering the land choice with [landChoice] and proliferate with [proliferateChoice]. */
    private fun drive(game: TestGame, landChoice: (SelectCardsDecision) -> List<EntityId>, proliferateChoice: List<EntityId>): Int {
        var proliferatePrompts = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 30) {
            val decision = game.state.pendingDecision
            if (decision == null) {
                game.resolveStack()
                continue
            }
            val libraryIds = game.state.getLibrary(game.player1Id).toSet()
            val lookedAt = decision is SelectCardsDecision &&
                (decision.options + decision.nonSelectableOptions).any { it in libraryIds }
            if (lookedAt) {
                game.selectCards(landChoice(decision as SelectCardsDecision))
            } else {
                proliferatePrompts++
                game.selectCards(proliferateChoice)
            }
        }
        return proliferatePrompts
    }

    init {
        test("taking a land puts it into hand and does not proliferate") {
            val game = setup(withLandOnTop = true)
            val bears = game.findPermanent("Grizzly Bears")!!
            val plains = game.findCardsInLibrary(1, "Plains").single()

            game.castSpell(1, "Contagious Vorrac").error shouldBe null
            val prompts = drive(game, { d ->
                withClue("Only the land is selectable, and the choice is optional") {
                    d.options shouldBe listOf(plains)
                    d.minSelections shouldBe 0
                }
                listOf(plains)
            }, listOf(bears))

            withClue("Plains went to hand") { game.isInHand(1, "Plains") shouldBe true }
            withClue("The other three cards go under the unseen fifth card") {
                game.librarySize(1) shouldBe 4
                game.state.getEntity(game.state.getLibrary(game.player1Id).first())
                    ?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
            }
            withClue("No proliferate prompt") { prompts shouldBe 0 }
            counters(game, bears) shouldBe 1
            game.isOnBattlefield("Contagious Vorrac") shouldBe true
        }

        test("declining the land proliferates") {
            val game = setup(withLandOnTop = true)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Contagious Vorrac").error shouldBe null
            val prompts = drive(game, { emptyList() }, listOf(bears))

            game.isInHand(1, "Plains") shouldBe false
            game.librarySize(1) shouldBe 5
            prompts shouldBe 1
            counters(game, bears) shouldBe 2
        }

        test("no land among the four proliferates") {
            val game = setup(withLandOnTop = false)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Contagious Vorrac").error shouldBe null
            val prompts = drive(game, { emptyList() }, listOf(bears))

            game.librarySize(1) shouldBe 5
            game.handSize(1) shouldBe 0
            prompts shouldBe 1
            counters(game, bears) shouldBe 2
        }
    }
}
