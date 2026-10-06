package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A spell with more than one *selection* additional cost — "discard a card and sacrifice a
 * creature" (Ruthless Disposal). CR 601.2h has the caster pay every additional cost, so the cast
 * action offers each picker (the first as `additionalCostInfo`, the rest in its `alsoRequired`),
 * the spell is castable only when every one of them can be paid, and a payment missing any of
 * them is rejected.
 */
class MultipleSelectionAdditionalCostsTest : ScenarioTestBase() {

    private val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    private val rock = card("Test Rock") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    // "As an additional cost, discard a card and sacrifice a creature. You gain 3 life."
    private val bargain = card("Test Bargain") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.DiscardCards())
        additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))
        spell {
            effect = Effects.GainLife(3)
        }
    }

    private fun TestGame.cast(discarded: List<EntityId>, sacrificed: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Test Bargain").single(),
            additionalCostPayment = AdditionalCostPayment(discardedCards = discarded, sacrificedPermanents = sacrificed),
        )
    )

    private fun TestGame.myBear() =
        findPermanents("Test Bear").single { state.projectedState.getController(it) == player1Id }

    init {
        cardRegistry.register(bear)
        cardRegistry.register(rock)
        cardRegistry.register(bargain)

        fun board(rocksInHand: Int = 1, bears: Int = 1) = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Test Bargain")
            .apply { repeat(rocksInHand) { withCardInHand(1, "Test Rock") } }
            .withLandsOnBattlefield(1, "Swamp", 1)
            .apply { repeat(bears) { withCardOnBattlefield(1, "Test Bear") } }
            .withCardOnBattlefield(2, "Test Bear")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("the cast action offers both pickers, the second chained under the first") {
            val game = board()
            val info = game.getLegalActions(1).single { it.description == "Cast Test Bargain" }
                .additionalCostInfo.shouldNotBeNull()
            val all = listOf(info) + info.alsoRequired
            all.map { it.costType }.toSet() shouldBe setOf("DiscardCard", "SacrificePermanent")
            info.alsoRequired.flatMap { it.alsoRequired }.shouldBeEmpty()
            val discard = all.single { it.costType == "DiscardCard" }
            discard.validDiscardTargets shouldContainExactly game.findCardsInHand(1, "Test Rock")
            discard.discardCount shouldBe 1
            val sacrifice = all.single { it.costType == "SacrificePermanent" }
            sacrifice.validSacrificeTargets shouldContainExactly listOf(game.myBear())
            sacrifice.sacrificeCount shouldBe 1
        }

        test("paying both discards the card, sacrifices the creature, and the spell resolves") {
            val game = board()
            val rockId = game.findCardsInHand(1, "Test Rock").single()
            val bearId = game.myBear()
            game.cast(listOf(rockId), listOf(bearId)).error shouldBe null
            game.isInGraveyard(1, "Test Rock") shouldBe true
            game.isInGraveyard(1, "Test Bear") shouldBe true
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
        }

        test("a payment missing either cost is rejected") {
            val game = board()
            val rockId = game.findCardsInHand(1, "Test Rock").single()
            val bearId = game.myBear()
            game.cast(emptyList(), listOf(bearId)).error shouldNotBe null
            game.cast(listOf(rockId), emptyList()).error shouldNotBe null
            game.findCardsInHand(1, "Test Rock") shouldHaveSize 1
            game.findPermanents("Test Bear") shouldHaveSize 2
        }

        test("the spell itself can't be discarded to pay for itself") {
            val game = board()
            val self = game.findCardsInHand(1, "Test Bargain").single()
            game.cast(listOf(self), listOf(game.myBear())).error shouldNotBe null
        }

        test("not castable when either cost can't be paid") {
            fun castable(game: TestGame) = game.getLegalActions(1).any { it.description == "Cast Test Bargain" }
            castable(board(rocksInHand = 0)) shouldBe false
            castable(board(bears = 0)) shouldBe false
            castable(board()) shouldBe true
        }
    }
}
