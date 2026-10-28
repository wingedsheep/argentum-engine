package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Endurance (MH2) — {1}{G}{G} Creature — Elemental Incarnation 3/4.
 *
 * "Flash / Reach / When this creature enters, up to one target player puts all the cards from their
 * graveyard on the bottom of their library in a random order. / Evoke—Exile a green card from
 * your hand."
 *
 * The first evoke cost with no mana in it (CR 702.74): the exile is evoke's bundled additional
 * cost, owed only when evoke is the alternative cost chosen.
 */
class EnduranceScenarioTest : ScenarioTestBase() {

    private fun game(vararg hand: String, lands: Int = 0, ownGraveyard: String? = null) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Endurance")
        .apply { hand.forEach { withCardInHand(1, it) } }
        .apply { if (lands > 0) withLandsOnBattlefield(1, "Forest", lands) }
        .apply { ownGraveyard?.let { withCardInGraveyard(1, it) } }
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInGraveyard(2, "Lightning Bolt")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.evoke(exiled: String) = execute(
        CastSpell(
            player1Id,
            findCardsInHand(1, "Endurance").single(),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.EVOKE,
            additionalCostPayment = AdditionalCostPayment(
                exiledCards = if (exiled == "Endurance") findCardsInHand(1, "Endurance")
                else listOf(findCardsInHand(1, exiled).first())
            ),
        )
    )

    private fun TestGame.targetOpponentWithTrigger() {
        val decision = state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected the enters trigger's target prompt; got ${state.pendingDecision}")
        submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(player2Id)))).error shouldBe null
    }

    init {
        test("evoke is offered for no mana when a green card is in hand, naming the exile") {
            val game = game("Llanowar Elves")
            val evoke = game.getLegalActions(1).singleOrNull {
                (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.EVOKE
            }.shouldNotBeNull()
            evoke.description shouldBe "Evoke Endurance (exile a green card from your hand)"
            withClue("the green card is the only exile candidate — Endurance can't pitch itself") {
                evoke.additionalCostInfo.shouldNotBeNull().validExileTargets shouldBe
                    game.findCardsInHand(1, "Llanowar Elves")
            }
        }

        test("evoke isn't offered without another green card in hand") {
            val game = game("Lightning Bolt")
            game.getLegalActions(1).none {
                (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.EVOKE
            } shouldBe true
        }

        test("evoking exiles the green card, resolves the trigger, then sacrifices Endurance") {
            val game = game("Llanowar Elves")
            game.evoke("Llanowar Elves").error shouldBe null
            game.state.getExile(game.player1Id).size shouldBe 1
            game.resolveStack()
            game.targetOpponentWithTrigger()
            game.resolveStack()

            withClue("the opponent's graveyard went to the bottom of their library") {
                game.graveyardSize(2) shouldBe 0
                game.librarySize(2) shouldBe 3
                game.state.getLibrary(game.player2Id).first().let { top ->
                    game.state.getEntity(top)!!.get<CardComponent>()!!.name
                } shouldBe "Mountain"
            }
            withClue("evoke's sacrifice trigger put Endurance into the graveyard") {
                game.findPermanent("Endurance").shouldBeNull()
                game.isInGraveyard(1, "Endurance") shouldBe true
            }
        }

        test("a nongreen card can't pay the evoke cost") {
            val game = game("Lightning Bolt")
            game.evoke("Lightning Bolt").error shouldNotBe null
            game.state.getExile(game.player1Id).size shouldBe 0
        }

        test("Endurance can't exile itself to pay its own evoke cost") {
            val game = game()
            game.evoke("Endurance").error shouldNotBe null
            game.findCardsInHand(1, "Endurance").size shouldBe 1
        }

        test("hard-casting owes no exile and keeps Endurance on the battlefield") {
            val game = game("Llanowar Elves", lands = 3)
            game.castSpell(1, "Endurance").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()
            game.targetOpponentWithTrigger()
            game.resolveStack()

            game.findPermanent("Endurance").shouldNotBeNull()
            game.findCardsInHand(1, "Llanowar Elves").size shouldBe 1
            game.state.getExile(game.player1Id).size shouldBe 0
            game.graveyardSize(2) shouldBe 0
        }

        test("up to one: choosing no player leaves every graveyard alone") {
            val game = game("Llanowar Elves", ownGraveyard = "Hill Giant")
            game.evoke("Llanowar Elves").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected the enters trigger's target prompt; got ${game.state.pendingDecision}")
            game.submitDecision(TargetsResponse(decision.id, mapOf(0 to emptyList()))).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 2
            withClue("the controller's own graveyard isn't swept as a fallback") {
                game.librarySize(1) shouldBe 1
            }
            game.state.getGraveyard(game.player1Id).map {
                game.state.getEntity(it)!!.get<CardComponent>()!!.name
            } shouldContain "Hill Giant"
        }
    }
}
