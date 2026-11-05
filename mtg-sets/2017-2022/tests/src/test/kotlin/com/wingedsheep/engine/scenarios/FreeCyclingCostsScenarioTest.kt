package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * `FreeCyclingCosts` — "you may pay {0} rather than pay cycling costs" (New Perspectives) — across
 * the shapes cycling takes:
 *  - typecycling is cycling (CR 702.29e), so it is free too;
 *  - the {0} is an alternative cost, so a cost increase still applies on top of it
 *    (CR 601.2f via CR 602.2b) — Tithe Taker's tax makes the cycle cost {1};
 *  - an `{X}` cycling cost is free at X = 0 (an alternative cost without X sets X to 0, CR 107.3);
 *    announcing a larger X pays the printed cost.
 *
 * Every board holds New Perspectives and seven cards in the controller's hand.
 */
class FreeCyclingCostsScenarioTest : ScenarioTestBase() {

    init {
        test("typecycling is free: Twisted Abomination swampcycles with no mana") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "New Perspectives")
                .withCardInHand(1, "Twisted Abomination")
                .withCardsInHand(1, "Island", 6)
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val abomination = game.findCardsInHand(1, "Twisted Abomination").first()
            val offered = game.getLegalActions(1).first { (it.action as? TypecycleCard)?.cardId == abomination }
            offered.isAffordable shouldBe true
            offered.manaCostString shouldBe "{0}"

            game.execute(TypecycleCard(game.player1Id, abomination)).error shouldBe null
            game.isInGraveyard(1, "Twisted Abomination") shouldBe true
        }

        test("a cost increase applies on top of the {0}: Tithe Taker makes the cycle cost {1}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "New Perspectives")
                .withCardInHand(1, "Hieroglyphic Illumination")
                .withCardsInHand(1, "Plains", 6)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Island")
                .withCardOnBattlefield(2, "Tithe Taker")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val illumination = game.findCardsInHand(1, "Hieroglyphic Illumination").first()
            val offered = game.getLegalActions(1).first { (it.action as? CycleCard)?.cardId == illumination }
            withClue("{0} + the {1} tax — payable with a Plains, though the printed cost is {U}") {
                offered.manaCostString shouldBe "{1}"
                offered.isAffordable shouldBe true
            }

            game.execute(CycleCard(game.player1Id, illumination)).error shouldBe null
            game.isInGraveyard(1, "Hieroglyphic Illumination") shouldBe true
            val plains = game.state.getBattlefield(game.player1Id).first { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Plains" }
            game.state.getEntity(plains)?.has<TappedComponent>() shouldBe true
        }

        test("an {X} cycling cost is free at X = 0 and pays the printed cost for a larger X") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "New Perspectives")
                .withCardInHand(1, "Webstrike Elite")
                .withCardInHand(1, "Webstrike Elite")
                .withCardsInHand(1, "Island", 5)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (first, second) = game.findCardsInHand(1, "Webstrike Elite")
            val offered = game.getLegalActions(1).first { (it.action as? CycleCard)?.cardId == first }
            withClue("affordable from nothing, the X chooser measured against the printed {X}{G}{G}") {
                offered.isAffordable shouldBe true
                offered.maxAffordableX shouldBe 1
            }

            game.execute(CycleCard(game.player1Id, first)).error shouldBe null
            val decision = game.state.pendingDecision as? ChooseNumberDecision
            decision.shouldNotBeNull()
            decision.prompt shouldContain "X = 0 cycles it for {0}"
            game.chooseNumber(0).error shouldBe null
            game.resolveStack()
            withClue("X = 0: free — no Forest tapped") {
                game.isInGraveyard(1, "Webstrike Elite") shouldBe true
                game.state.getBattlefield(game.player1Id).none { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
            }

            withClue("still seven cards in hand, so the {0} is still on offer") { game.handSize(1) shouldBe 7 }
            game.execute(CycleCard(game.player1Id, second, xValue = 1)).error shouldBe null
            game.resolveStack()
            withClue("X = 1: the printed {1}{G}{G} is paid — all three Forests tapped") {
                game.state.getBattlefield(game.player1Id).count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 3
            }
        }
    }
}
