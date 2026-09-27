package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `Costs.additional.SacrificePermanents` — a spell's variable-count sacrifice cost, offered on the
 * main cast rail as the `SacrificeVariable` picker and paid from
 * `additionalCostPayment.variableCostPermanents`. Devouring Greed / Rage cover the zero-floor card
 * shape; this pins the floor, the duplicate check, and the no-candidate case.
 */
class SacrificePermanentsSpellCostScenarioTest : ScenarioTestBase() {

    private val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // "As an additional cost, sacrifice one or more creatures. You gain 3 life for each."
    private val feast = card("Test Feast") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.SacrificePermanents(GameObjectFilter.Creature, minCount = 1))
        spell {
            effect = Effects.GainLife(DynamicAmounts.permanentsSacrificedThisWay() * 3)
        }
    }

    // "As an additional cost, you may sacrifice any number of creatures. You gain 1 life for each."
    private val snack = card("Test Snack") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.SacrificePermanents(GameObjectFilter.Creature))
        spell {
            effect = Effects.GainLife(DynamicAmounts.permanentsSacrificedThisWay())
        }
    }

    private fun TestGame.cast(name: String, sacrificed: List<EntityId>?) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, name).single(),
            additionalCostPayment = sacrificed?.let { AdditionalCostPayment(variableCostPermanents = it) },
        )
    )

    init {
        cardRegistry.register(bear)
        cardRegistry.register(feast)
        cardRegistry.register(snack)

        fun board(spell: String, bears: Int) = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, spell)
            .withLandsOnBattlefield(1, "Forest", 1)
            .apply { repeat(bears) { withCardOnBattlefield(1, "Test Bear") } }
            .withCardOnBattlefield(2, "Test Bear")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("the picker offers only your matching permanents and carries the floor") {
            val game = board("Test Feast", 2)
            val info = game.getLegalActions(1).single { it.description == "Cast Test Feast" }
                .additionalCostInfo.shouldNotBeNull()
            info.costType shouldBe "SacrificeVariable"
            info.sacrificeCount shouldBe 1
            info.validSacrificeTargets.toSet() shouldBe
                game.findPermanents("Test Bear").filter { game.state.projectedState.getController(it) == game.player1Id }.toSet()
        }

        test("the count sacrificed is read at resolution after the permanents are gone") {
            val game = board("Test Feast", 2)
            val mine = game.findPermanents("Test Bear").filter { game.state.projectedState.getController(it) == game.player1Id }
            game.cast("Test Feast", mine).error shouldBe null
            game.findPermanents("Test Bear").size shouldBe 1
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 26
        }

        test("a floor of one rejects an empty payment and hides the cast without candidates") {
            val game = board("Test Feast", 1)
            game.cast("Test Feast", null).error shouldNotBe null
            game.cast("Test Feast", emptyList()).error shouldNotBe null

            val empty = board("Test Feast", 0)
            empty.getLegalActions(1).none { it.description == "Cast Test Feast" } shouldBe true
        }

        test("the same permanent can't pay twice, and an opponent's can't pay at all") {
            val game = board("Test Snack", 1)
            val mine = game.findPermanents("Test Bear").single { game.state.projectedState.getController(it) == game.player1Id }
            val theirs = game.findPermanents("Test Bear").single { it != mine }
            game.cast("Test Snack", listOf(mine, mine)).error shouldNotBe null
            game.cast("Test Snack", listOf(theirs)).error shouldNotBe null
        }

        test("a zero-floor cost with no candidates offers no picker") {
            val game = board("Test Snack", 0)
            game.getLegalActions(1).single { it.description == "Cast Test Snack" }
                .additionalCostInfo.shouldBeNull()
            game.cast("Test Snack", null).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
