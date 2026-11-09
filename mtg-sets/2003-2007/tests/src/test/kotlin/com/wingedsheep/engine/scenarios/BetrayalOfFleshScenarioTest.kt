package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Betrayal of Flesh — {5}{B} Instant (MRD #58).
 *
 * "Choose one — • Destroy target creature. • Return target creature card from your graveyard to
 * the battlefield. Entwine—Sacrifice three lands."
 *
 * The non-mana entwine cost: the entwined cast takes both modes and sacrifices three lands once;
 * two lands can't pay it, and both modes without declaring entwine are rejected.
 */
class BetrayalOfFleshScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Betrayal of Flesh")
        .withCardInGraveyard(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 6)
        .withLandsOnBattlefield(1, "Plains", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castBothModes(declared: ChoiceSlot?, sacrificed: List<EntityId>): ExecutionResult {
        val spell = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Betrayal of Flesh" }
        val bears = ChosenTarget.Permanent(findPermanent("Grizzly Bears").shouldNotBeNull())
        val giant = ChosenTarget.Card(findCardsInGraveyard(1, "Hill Giant").single(), player1Id, Zone.GRAVEYARD)
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = spell,
                chosenModes = listOf(0, 1),
                targets = listOf(bears, giant),
                modeTargetsOrdered = listOf(listOf(bears), listOf(giant)),
                declaredCostSlot = declared,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = sacrificed),
            )
        )
    }

    init {
        test("entwined by sacrificing three lands, it destroys the creature and returns the card") {
            val game = board()
            val plains = game.findPermanents("Plains")

            game.castBothModes(ChoiceSlot.ENTWINED, plains).error shouldBe null
            game.findPermanents("Plains").size shouldBe 0
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("two lands can't pay the entwine cost") {
            val game = board()

            game.castBothModes(ChoiceSlot.ENTWINED, game.findPermanents("Plains").take(2)).error.shouldNotBeNull()
            game.findPermanents("Plains").size shouldBe 3
        }

        test("both modes without declaring entwine are rejected") {
            val game = board()

            game.castBothModes(declared = null, sacrificed = emptyList()).error.shouldNotBeNull()
        }

        test("the entwined cast is offered beside the plain one") {
            val game = board()
            game.getLegalActions(1).map { it.description } shouldContain "Cast Betrayal of Flesh (Entwined — Sacrifice three lands)"
        }
    }
}
