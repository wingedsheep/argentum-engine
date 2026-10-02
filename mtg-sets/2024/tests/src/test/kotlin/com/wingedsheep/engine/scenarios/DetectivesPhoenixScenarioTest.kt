package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Detective's Phoenix (MH3 #116) — "You may cast this card from your graveyard using its bestow
 * ability." From the graveyard it is offered and castable only bestowed, for {R} plus collect
 * evidence 6; an ordinary cast from the graveyard is illegal.
 */
class DetectivesPhoenixScenarioTest : ScenarioTestBase() {

    private fun phoenixInGraveyard() = scenario().withPlayers()
        .withCardOnBattlefield(1, "Llanowar Elves")
        .withCardInGraveyard(1, "Detective's Phoenix")
        .withCardInGraveyard(1, "Hill Giant")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.bestowFromGraveyard(host: EntityId, evidence: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInGraveyard(1, "Detective's Phoenix").single(),
            targets = listOf(ChosenTarget.Permanent(host)),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.BESTOW,
            additionalCostPayment = AdditionalCostPayment(exiledCards = evidence)
        )
    )

    private fun TestGame.evidence() =
        findCardsInGraveyard(1, "Hill Giant") + findCardsInGraveyard(1, "Grizzly Bears")

    init {
        test("from the graveyard it is offered only as a bestow cast") {
            val game = phoenixInGraveyard().build()
            val phoenix = game.findCardsInGraveyard(1, "Detective's Phoenix").single()

            val casts = game.getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == phoenix }
            casts shouldHaveSize 1
            (casts.single().action as CastSpell).alternativeCostType shouldBe AlternativeCostType.BESTOW
        }

        test("cast bestowed from the graveyard: {R} plus evidence, and the host gets +2/+2, flying and haste") {
            val game = phoenixInGraveyard().build()
            val elves = game.findPermanent("Llanowar Elves")!!
            val evidence = game.evidence()

            game.bestowFromGraveyard(elves, evidence).error shouldBe null
            game.resolveStack()

            val phoenix = game.findPermanent("Detective's Phoenix")!!
            game.state.getEntity(phoenix)?.get<AttachedToComponent>()?.targetId shouldBe elves
            game.state.getExile(game.player1Id) shouldContainAll evidence
            val projected = game.state.projectedState
            projected.getPower(elves) shouldBe 3
            projected.getToughness(elves) shouldBe 3
            projected.hasKeyword(elves, Keyword.FLYING) shouldBe true
            projected.hasKeyword(elves, Keyword.HASTE) shouldBe true
            projected.isCreature(phoenix) shouldBe false
        }

        test("an ordinary cast from the graveyard is illegal") {
            val game = phoenixInGraveyard().build()
            val phoenix = game.findCardsInGraveyard(1, "Detective's Phoenix").single()

            game.execute(CastSpell(playerId = game.player1Id, cardId = phoenix)).error shouldNotBe null
            game.isInGraveyard(1, "Detective's Phoenix") shouldBe true
        }

        test("without evidence totalling six mana value it can't be cast from the graveyard") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardInGraveyard(1, "Detective's Phoenix")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elves = game.findPermanent("Llanowar Elves")!!
            val phoenix = game.findCardsInGraveyard(1, "Detective's Phoenix").single()

            game.getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == phoenix }.shouldBeEmpty()
            // The Phoenix is on the stack while its costs are paid, so it can't be its own evidence.
            // Hill Giant (4) plus the Phoenix itself (3) would reach six.
            game.bestowFromGraveyard(elves, game.findCardsInGraveyard(1, "Hill Giant") + phoenix)
                .error shouldNotBe null
        }

        test("when the host dies the Phoenix stays on the battlefield as a 2/2 flying, haste creature") {
            val game = phoenixInGraveyard()
                .withCardInHand(1, "Lightning Bolt")
                .build()
            val elves = game.findPermanent("Llanowar Elves")!!
            game.bestowFromGraveyard(elves, game.evidence()).error shouldBe null
            game.resolveStack()

            game.castSpell(1, "Lightning Bolt", elves).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Llanowar Elves") shouldBe true
            val phoenix = game.findPermanent("Detective's Phoenix")!!
            game.state.getEntity(phoenix)?.get<AttachedToComponent>() shouldBe null
            val projected = game.state.projectedState
            projected.isCreature(phoenix) shouldBe true
            projected.getPower(phoenix) shouldBe 2
            projected.hasKeyword(phoenix, Keyword.FLYING) shouldBe true
        }
    }
}
