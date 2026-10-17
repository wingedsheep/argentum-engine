package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoyaltyActivationPermission
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The static [LoyaltyActivationPermission] with `instantSpeed = true` lifts only the timing half of
 * CR 606.3 — The Wandering Emperor's "as long as this entered this turn" shape.
 */
class InstantSpeedLoyaltyStaticScenarioTest : ScenarioTestBase() {
    private val walker = card("Entered Instant Walker") {
        typeLine = "Planeswalker — Test"
        startingLoyalty = 5
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = LoyaltyActivationPermission(GameObjectFilter.Any.sourceItself(), instantSpeed = true),
                condition = Conditions.SourceEnteredThisTurn,
            )
        }
        loyaltyAbility(+1) { effect = Effects.GainLife(1) }
    }
    private val ordinary = card("Ordinary Sorcery Walker") {
        typeLine = "Planeswalker — Other"
        startingLoyalty = 5
        loyaltyAbility(+1) { effect = Effects.GainLife(1) }
    }
    private val suppress = card("Suppress Instant Walker") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }

    private fun board(enteredThisTurn: Boolean): TestGame = scenario().withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, walker.name, enteredThisTurn = enteredThisTurn)
        .withCardOnBattlefield(1, ordinary.name, enteredThisTurn = enteredThisTurn)
        .withCardInHand(1, suppress.name)
        .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
        .withActivePlayer(2).withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.action(name: String = walker.name) =
        ActivateAbility(player1Id, findPermanent(name)!!, cardRegistry.getCard(name)!!.script.activatedAbilities[0].id)
    private fun TestGame.offered(name: String = walker.name) =
        getLegalActions(1).any { it.action == action(name) }

    init {
        listOf(walker, ordinary, suppress).forEach { cardRegistry.register(it) }

        test("a walker that entered this turn activates on an opponent's turn, once") {
            val game = board(enteredThisTurn = true)
            game.offered() shouldBe true
            game.execute(game.action()).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
            game.state = game.state.copy(priorityPlayerId = game.player1Id)
            game.offered() shouldBe false
            game.execute(game.action()).error shouldNotBe null
        }
        test("the permission covers only its own walker") {
            val game = board(enteredThisTurn = true)
            game.offered(ordinary.name) shouldBe false
            game.execute(game.action(ordinary.name)).error shouldNotBe null
        }
        test("a walker that didn't enter this turn keeps sorcery timing") {
            val game = board(enteredThisTurn = false)
            game.offered() shouldBe false
            game.execute(game.action()).error shouldNotBe null
        }
        test("it also works with a spell on the stack") {
            val game = board(enteredThisTurn = true)
            game.castSpell(1, suppress.name, targetId = game.findPermanent(ordinary.name)!!).error shouldBe null
            game.offered() shouldBe true
            game.execute(game.action()).error shouldBe null
        }
        test("losing all abilities removes the permission") {
            val game = board(enteredThisTurn = true)
            game.castSpell(1, suppress.name, targetId = game.findPermanent(walker.name)!!).error shouldBe null
            game.resolveStack()
            game.state = game.state.copy(priorityPlayerId = game.player1Id)
            game.offered() shouldBe false
            game.execute(game.action()).error shouldNotBe null
        }
    }
}
