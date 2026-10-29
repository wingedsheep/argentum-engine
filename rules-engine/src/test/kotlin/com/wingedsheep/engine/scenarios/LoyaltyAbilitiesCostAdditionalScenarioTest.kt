package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.LoyaltyAbilitiesCostAdditional
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Feature test for [LoyaltyAbilitiesCostAdditional] — Carth the Lion's "Planeswalkers' loyalty
 * abilities you activate cost an additional [+1] to activate."
 *
 * The surcharge nets into the one loyalty payment (printed rulings): [+1] → [+2], [0] → [+1],
 * [−3] → [−2] (so it can be activated with two loyalty), [−X] → [−X+1] with X unchanged for the
 * effect. Two surcharges are cumulative; an opponent's doesn't reach your activations. Validator and
 * enumerator agree throughout.
 */
class LoyaltyAbilitiesCostAdditionalScenarioTest : ScenarioTestBase() {

    private val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "+1: You gain 1 life.\n0: You gain 2 life.\n−3: You gain 3 life.\n−X: You gain X life."
        loyaltyAbility(1) { effect = Effects.GainLife(1) }
        loyaltyAbility(0) { effect = Effects.GainLife(2) }
        loyaltyAbility(-3) { effect = Effects.GainLife(3) }
        loyaltyAbilityX { effect = Effects.GainLife(DynamicAmounts.xValue()) }
    }

    private val lowWalker = card("Low Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Low"
        startingLoyalty = 2
        oracleText = "−3: You gain 3 life.\n−X: You gain X life."
        loyaltyAbility(-3) { effect = Effects.GainLife(3) }
        loyaltyAbilityX { effect = Effects.GainLife(DynamicAmounts.xValue()) }
    }

    private val lion = card("Test Lion") {
        manaCost = "{2}"
        typeLine = "Creature — Cat"
        power = 2
        toughness = 2
        oracleText = "Planeswalkers' loyalty abilities you activate cost an additional [+1] to activate."
        staticAbility { ability = LoyaltyAbilitiesCostAdditional(1) }
    }

    private fun board(walkerName: String = "Test Walker", lions: Int = 1, opponentLions: Int = 0): TestGame {
        val b = scenario()
            .withPlayers("Player", "Opponent")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, walkerName)
        repeat(lions) { b.withCardOnBattlefield(1, "Test Lion") }
        repeat(opponentLions) { b.withCardOnBattlefield(2, "Test Lion") }
        return b.build()
    }

    private fun TestGame.abilityId(walkerName: String, cost: AbilityCost) =
        cardRegistry.getCard(walkerName)!!.script.activatedAbilities.first { it.cost == cost }.id

    private fun TestGame.loyalty(walkerName: String): Int =
        state.getEntity(findPermanent(walkerName)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    private fun TestGame.offered(walkerName: String, cost: AbilityCost): Boolean {
        val source: EntityId = findPermanent(walkerName) ?: return false
        val id = abilityId(walkerName, cost)
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.let { a -> a.sourceId == source && a.abilityId == id } == true }
    }

    private fun TestGame.activate(walkerName: String, cost: AbilityCost, x: Int? = null) {
        val result = execute(ActivateAbility(player1Id, findPermanent(walkerName)!!, abilityId(walkerName, cost), xValue = x))
        withClue("activation should be legal") { result.error shouldBe null }
        resolveStack()
    }

    init {
        cardRegistry.register(walker)
        cardRegistry.register(lowWalker)
        cardRegistry.register(lion)

        test("a [+1] puts two loyalty counters on") {
            val game = board()
            game.activate("Test Walker", AbilityCost.Loyalty(1))
            game.loyalty("Test Walker") shouldBe 7
            game.getLifeTotal(1) shouldBe 21
        }

        test("a [0] costs [+1]") {
            val game = board()
            game.activate("Test Walker", AbilityCost.Loyalty(0))
            game.loyalty("Test Walker") shouldBe 6
            game.getLifeTotal(1) shouldBe 22
        }

        test("a [-3] removes two counters") {
            val game = board()
            game.activate("Test Walker", AbilityCost.Loyalty(-3))
            game.loyalty("Test Walker") shouldBe 3
            game.getLifeTotal(1) shouldBe 23
        }

        test("a [-3] can be activated with two loyalty") {
            val game = board("Low Walker")
            game.offered("Low Walker", AbilityCost.Loyalty(-3)) shouldBe true
            game.activate("Low Walker", AbilityCost.Loyalty(-3))
            game.getLifeTotal(1) shouldBe 23
            game.isInGraveyard(1, "Low Walker") shouldBe true
        }

        test("without the surcharge a [-3] can't be activated with two loyalty") {
            val game = board("Low Walker", lions = 0)
            game.offered("Low Walker", AbilityCost.Loyalty(-3)) shouldBe false
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent("Low Walker")!!, game.abilityId("Low Walker", AbilityCost.Loyalty(-3)))
            ).error shouldNotBe null
        }

        test("a [-X] removes X-1 counters and the effect reads X") {
            val game = board()
            game.activate("Test Walker", AbilityCost.LoyaltyX, x = 4)
            game.loyalty("Test Walker") shouldBe 2
            game.getLifeTotal(1) shouldBe 24
        }

        test("a [-X] can choose X up to loyalty plus the surcharge") {
            val game = board("Low Walker")
            val action = game.getLegalActions(1).first {
                (it.action as? ActivateAbility)?.abilityId == game.abilityId("Low Walker", AbilityCost.LoyaltyX)
            }
            action.maxAffordableX shouldBe 3
            game.activate("Low Walker", AbilityCost.LoyaltyX, x = 3)
            game.getLifeTotal(1) shouldBe 23
            game.isInGraveyard(1, "Low Walker") shouldBe true
        }

        test("the X choice offers up to loyalty plus the surcharge") {
            val game = board("Low Walker")
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent("Low Walker")!!, game.abilityId("Low Walker", AbilityCost.LoyaltyX))
            ).error shouldBe null
            val decision = game.getPendingDecision().shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
            decision.maxValue shouldBe 3
            game.chooseNumber(3).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 23
            game.isInGraveyard(1, "Low Walker") shouldBe true
        }

        test("two surcharges are cumulative") {
            val game = board(lions = 2)
            game.activate("Test Walker", AbilityCost.Loyalty(1))
            game.loyalty("Test Walker") shouldBe 8
        }

        test("the client's ability menu shows the surcharged costs") {
            val game = board()
            val view = com.wingedsheep.engine.view.ClientStateTransformer(
                cardRegistry = cardRegistry,
                predicateEvaluator = com.wingedsheep.engine.handlers.PredicateEvaluator(cardRegistry = null),
            ).transform(game.state, viewingPlayerId = game.player1Id)
            val menu = view.cards[game.findPermanent("Test Walker")!!]!!.planeswalkerAbilities!!
            menu.map { it.loyaltyChange } shouldBe listOf(2, 1, -2, 0)
            menu.map { it.loyaltyX } shouldBe listOf(false, false, false, true)
        }

        test("an opponent's surcharge doesn't reach your activations") {
            val game = board(lions = 0, opponentLions = 1)
            game.activate("Test Walker", AbilityCost.Loyalty(1))
            game.loyalty("Test Walker") shouldBe 6
        }
    }
}
