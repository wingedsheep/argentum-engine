package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Narset, Enlightened Exile (MAT #38) — {1}{U}{R}{W} 3/4.
 *
 * "Creatures you control have prowess. Whenever Narset attacks, exile target noncreature, nonland
 * card with mana value less than Narset's power from a graveyard and copy it. You may cast the copy
 * without paying its mana cost."
 *
 * Covers the strict "less than Narset's power" target cap (a live, prowess-pumpable comparison),
 * targeting any graveyard (the opponent's included), the granted prowess on Narset and her team,
 * and the free cast of the copy — taken and declined.
 */
class NarsetEnlightenedExileScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Narset, Enlightened Exile")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInHand(1, "Shock")
        .withCardInGraveyard(2, "Lightning Bolt")  // MV 1 — noncreature, nonland
        .withCardInGraveyard(2, "Divination")      // MV 3 — legal only once Narset's power exceeds 3
        .withCardInGraveyard(2, "Grizzly Bears")   // creature card — never legal
        .withCardInGraveyard(2, "Island")          // land card — never legal
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.graveyardCard(playerNumber: Int, name: String): EntityId =
        state.getGraveyard(if (playerNumber == 1) player1Id else player2Id).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    private fun TestGame.exileCount(playerNumber: Int, name: String): Int =
        state.getExile(if (playerNumber == 1) player1Id else player2Id).count { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    private fun TestGame.attackAndAwaitTrigger(): ChooseTargetsDecision {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Narset, Enlightened Exile" to 2)).error shouldBe null
        var guard = 0
        while (state.pendingDecision !is ChooseTargetsDecision && guard++ < 20) resolveStack()
        return state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected Narset's attack trigger to ask for a target; got ${state.pendingDecision}")
    }

    init {
        test("only noncreature, nonland cards with mana value less than Narset's power are targetable") {
            val game = board()
            val decision = game.attackAndAwaitTrigger()

            withClue("power 3: Lightning Bolt (MV 1) qualifies; Divination (MV 3) is not *less than* 3") {
                decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(game.graveyardCard(2, "Lightning Bolt"))
            }
        }

        test("copies an opponent's Lightning Bolt and casts the copy for free") {
            val game = board()
            game.attackAndAwaitTrigger()
            val bolt = game.graveyardCard(2, "Lightning Bolt")

            game.selectTargets(listOf(bolt)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("the Bolt copy deals 3 to the opponent") { game.getLifeTotal(2) shouldBe 17 }
            withClue("the original Bolt is exiled, with no lingering copy") { game.exileCount(2, "Lightning Bolt") shouldBe 1 }
            game.isInGraveyard(2, "Lightning Bolt") shouldBe false
        }

        test("declining the cast still exiles the card and leaves no copy") {
            val game = board()
            game.attackAndAwaitTrigger()

            game.selectTargets(listOf(game.graveyardCard(2, "Lightning Bolt"))).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.exileCount(2, "Lightning Bolt") shouldBe 1
            game.exileCount(1, "Lightning Bolt") shouldBe 0
        }

        test("granted prowess pumps Narset and her team, raising the mana value cap") {
            val game = board()
            val narset = game.findPermanent("Narset, Enlightened Exile")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("Narset and the Bears both have prowess") {
                game.state.projectedState.hasKeyword(narset, Keyword.PROWESS) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.PROWESS) shouldBe true
            }

            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()

            withClue("one prowess trigger each from a single noncreature spell") {
                game.state.projectedState.getPower(narset) shouldBe 4
                game.state.projectedState.getPower(bears) shouldBe 3
            }

            val decision = game.attackAndAwaitTrigger()
            withClue("power 4: Divination (MV 3) and the Shock now in your own graveyard qualify too") {
                decision.legalTargets[0]!!.shouldContainExactlyInAnyOrder(
                    game.graveyardCard(2, "Lightning Bolt"),
                    game.graveyardCard(2, "Divination"),
                    game.graveyardCard(1, "Shock"),
                )
            }
        }
    }
}
