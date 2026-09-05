package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AnyPlayerDiscardActivationScenarioTest : FunSpec({
    for (random in listOf(false, true)) {
        test("opponent discard cost honors count and filter with random=$random") {
            val sourceCard = card("Shared Discard Test") {
                manaCost = "{U}"
                typeLine = "Enchantment"
                activatedAbility {
                    cost = Costs.Discard(filter = GameObjectFilter.Creature, count = 2, atRandom = random)
                    effect = Effects.DrawCards(1)
                    restrictions = listOf(ActivationRestriction.AnyPlayerMay)
                }
            }
            val d = GameTestDriver().apply {
                registerCards(TestCards.all + sourceCard)
                initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val source = d.putPermanentOnBattlefield(d.player1, sourceCard.name)
            d.passPriority(d.player1).error shouldBe null
            fun options() = d.legalActions(d.player2).filter { (it.action as? ActivateAbility)?.sourceId == source }
            options().size shouldBe 0
            val first = d.putCardInHand(d.player2, "Grizzly Bears")
            options().size shouldBe 0
            val second = d.putCardInHand(d.player2, "Grizzly Bears")
            val option = options().single()
            if (random) {
                option.additionalCostInfo shouldBe null
            } else {
                option.additionalCostInfo!!.validDiscardTargets.toSet() shouldBe setOf(first, second)
                option.additionalCostInfo!!.discardCount shouldBe 2
            }
            val controllersHand = d.getHand(d.player1).toList()
            d.submit((option.action as ActivateAbility).copy(costPayment =
                if (random) null else AdditionalCostPayment(discardedCards = listOf(first, second))
            )).error shouldBe null
            d.bothPass().error shouldBe null
            d.state.getZone(d.player2, Zone.GRAVEYARD).toSet() shouldBe setOf(first, second)
            d.getHand(d.player1) shouldBe controllersHand
        }
    }
})
