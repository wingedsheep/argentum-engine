package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A variable "exile X cards from your graveyard" cost whose X has a floor — `ExileXFromGraveyard`
 * under the ability's `minimumXValue`, with no `{X}` mana so X is the size of the selection.
 * "Exile one or more creature cards" (Corpseweft) is floor 1; the floor is general, so these tests
 * use 2. A cost that can't be paid can't be activated, so with fewer matching cards the ability
 * is neither enumerated nor accepted; the selection is floored; a smaller submitted pile is
 * rejected; and a floor of 0 keeps the old any-number behaviour (Winter, Cursed Rider).
 */
class ExileXMinimumCountTest : FunSpec({
    fun engine(name: String, minimum: Int) = card(name) {
        typeLine = "Enchantment"
        activatedAbility {
            cost = Costs.ExileXFromGraveyard(GameObjectFilter.Creature)
            minimumXValue = minimum
            effect = Effects.GainLife(DynamicAmounts.xValue())
        }
    }
    val floored = engine("Floor Two Engine", 2)
    val free = engine("Free Engine", 0)

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(floored, free))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.offered(source: com.wingedsheep.sdk.model.EntityId) =
        legalActions(activePlayer!!).any { (it.action as? ActivateAbility)?.sourceId == source }
    fun GameTestDriver.abilityOf(name: String) = cardRegistry.getCard(name)!!.script.activatedAbilities[0].id

    test("fewer matching cards than the floor: not enumerated and rejected engine-direct") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(p, floored.name)
        d.putCardInGraveyard(p, "Grizzly Bears")
        d.putCardInGraveyard(p, "Forest") // matches no creature filter — doesn't count
        d.offered(source) shouldBe false
        d.submit(ActivateAbility(playerId = p, sourceId = source, abilityId = d.abilityOf(floored.name)))
            .error shouldNotBe null
    }

    test("enough cards: the selection is floored at the minimum and X is the count picked") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(p, floored.name)
        val bears = List(3) { d.putCardInGraveyard(p, "Grizzly Bears") }
        d.offered(source) shouldBe true
        d.submit(ActivateAbility(playerId = p, sourceId = source, abilityId = d.abilityOf(floored.name)))
            .error shouldBe null
        val decision = d.pendingDecision as SelectCardsDecision
        decision.minSelections shouldBe 2
        decision.maxSelections shouldBe 3
        val life = d.getLifeTotal(p)
        d.submitCardSelection(p, bears.take(3)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(p) shouldBe life + 3
        d.state.getZone(ZoneKey(p, Zone.EXILE)).containsAll(bears) shouldBe true
    }

    test("candidates exactly at the floor still prompt, so X is bound by the pick") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(p, floored.name)
        List(2) { d.putCardInGraveyard(p, "Grizzly Bears") }
        d.submit(ActivateAbility(playerId = p, sourceId = source, abilityId = d.abilityOf(floored.name)))
            .error shouldBe null
        val decision = d.pendingDecision as SelectCardsDecision
        decision.minSelections shouldBe 2
        decision.maxSelections shouldBe 2
    }

    test("a submitted pile below the floor is rejected and exiles nothing") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(p, floored.name)
        val bears = List(3) { d.putCardInGraveyard(p, "Grizzly Bears") }
        d.submit(
            ActivateAbility(
                playerId = p, sourceId = source, abilityId = d.abilityOf(floored.name),
                xValue = 1, costPayment = AdditionalCostPayment(exiledCards = bears.take(1)),
            )
        ).error shouldNotBe null
        d.state.getZone(ZoneKey(p, Zone.GRAVEYARD)).containsAll(bears) shouldBe true
    }

    test("no floor keeps any-number: empty graveyard still activates for X = 0") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(p, free.name)
        d.offered(source) shouldBe true
        d.submit(ActivateAbility(playerId = p, sourceId = source, abilityId = d.abilityOf(free.name)))
            .error shouldBe null
        d.pendingDecision shouldBe null
    }
})
