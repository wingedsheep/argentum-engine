package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Static permissions change only the count half of CR 606.3; all activation paths must agree. */
class StaticLoyaltyAllowanceScenarioTest : ScenarioTestBase() {
    private val selfWalker = card("Self Allowance Walker") {
        typeLine = "Planeswalker — Test"
        startingLoyalty = 5
        staticAbility { ability = ExtraLoyaltyActivation(GameObjectFilter.Any.sourceItself()) }
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
        loyaltyAbility(-6) { effect = Effects.GainLife(6) }
    }
    private val ordinary = card("Ordinary Allowance Walker") {
        typeLine = "Planeswalker — Other"
        startingLoyalty = 5
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }
    private fun grant(name: String, times: Int) = card(name) {
        manaCost = "{0}"
        typeLine = "Enchantment"
        staticAbility { ability = ExtraLoyaltyActivation(times = times) }
    }
    private val conditional = card("Conditional Allowance") {
        typeLine = "Enchantment"
        staticAbility {
            ability = com.wingedsheep.sdk.scripting.ConditionalStaticAbility(
                ability = ExtraLoyaltyActivation(), condition = Conditions.LifeAtLeast(21))
        }
    }
    private val gift = card("Gift Allowance") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.GrantStaticAbility(ExtraLoyaltyActivation(GameObjectFilter.Any.sourceItself()), t)
        }
    }
    private val ban = card("Ban Allowance") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.CantActivateLoyaltyAbilities(com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller) }
    }
    private val suppress = card("Suppress Allowance") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }
    private val steal = card("Steal Allowance") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.GainControl(t) }
    }
    private val rush = card("Resolved Allowance") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.AllowLoyaltyActivationsThisTurn(target = t) }
    }

    private fun board(vararg grants: String): TestGame {
        val builder = scenario().withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, selfWalker.name).withCardOnBattlefield(1, ordinary.name)
            .withCardInHand(1, gift.name).withCardInHand(1, ban.name).withCardInHand(1, "Double Allowance").withCardInHand(1, suppress.name).withCardInHand(1, steal.name).withCardInHand(1, rush.name)
            .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        grants.forEach { builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }
    private fun TestGame.action(name: String = selfWalker.name, index: Int = 0) =
        ActivateAbility(player1Id, findPermanent(name)!!, cardRegistry.getCard(name)!!.script.activatedAbilities[index].id)
    private fun TestGame.offered(name: String = selfWalker.name, index: Int = 0) =
        getLegalActions(1).any { it.action == action(name, index) }
    private fun TestGame.activate(name: String = selfWalker.name) {
        offered(name) shouldBe true
        execute(action(name)).error shouldBe null
        resolveStack()
    }
    private fun TestGame.blocked(name: String = selfWalker.name, index: Int = 0) {
        offered(name, index) shouldBe false
        execute(action(name, index)).error shouldNotBe null
    }
    init {
        listOf(selfWalker, ordinary, grant("Double Allowance", 2), grant("Triple Allowance", 3),
            suppress, steal, rush, conditional, gift, ban).forEach { cardRegistry.register(it) }

        test("self permission allows the same ability twice and does not affect another walker") {
            val game = board()
            game.activate(); game.activate(); game.blocked()
            game.activate(ordinary.name); game.blocked(ordinary.name)
            game.getLifeTotal(1) shouldBe 23
        }
        test("self reference is object identity even for two same-named walkers") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, selfWalker.name).withCardOnBattlefield(1, selfWalker.name)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val ids = game.findPermanents(selfWalker.name)
            for (id in ids) {
                val action = ActivateAbility(game.player1Id, id, selfWalker.script.activatedAbilities.first().id)
                repeat(2) { game.execute(action).error shouldBe null; game.resolveStack() }
                game.execute(action).error shouldNotBe null
            }
        }
        test("duplicate statics and resolved permissions take the largest maximum") {
            val game = board("Double Allowance", "Double Allowance", "Triple Allowance")
            game.castSpell(1, rush.name, targetId = game.findPermanent(selfWalker.name)!!).error shouldBe null
            game.resolveStack()
            repeat(3) { game.activate() }
            game.blocked()
            repeat(3) { game.activate(ordinary.name) }
            game.blocked(ordinary.name)
        }
        test("a permission gained after one activation allows exactly the remaining activation") {
            val game = board()
            game.activate(ordinary.name)
            game.blocked(ordinary.name)
            game.castSpell(1, "Double Allowance").error shouldBe null
            game.resolveStack()
            game.activate(ordinary.name)
            game.blocked(ordinary.name)
        }
        test("removing a granting ability immediately restores the normal limit") {
            val game = board("Double Allowance")
            game.activate(ordinary.name)
            game.castSpell(1, suppress.name, targetId = game.findPermanent("Double Allowance")!!).error shouldBe null
            game.resolveStack()
            game.blocked(ordinary.name)
            game.activate(); game.activate(); game.blocked()
        }
        test("projected controller changes determine who receives a global permission") {
            val game = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, ordinary.name).withCardOnBattlefield(2, "Double Allowance")
                .withCardInHand(1, steal.name)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.activate(ordinary.name); game.blocked(ordinary.name)
            val oath = game.findPermanent("Double Allowance")!!
            game.castSpell(1, steal.name, targetId = oath).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getController(oath) shouldBe game.player1Id
            game.activate(ordinary.name); game.blocked(ordinary.name)
        }
        test("conditional allowances reevaluate against their source controller") {
            val game = board(conditional.name)
            // At twenty life the conditional source provides no extra activation.
            game.activate(ordinary.name) // gain one life, enabling the permission
            game.activate(ordinary.name); game.blocked(ordinary.name)
        }
        test("a granted self permission applies to its holder rather than the granting spell") {
            val game = board()
            game.activate(ordinary.name); game.blocked(ordinary.name)
            game.castSpell(1, gift.name, targetId = game.findPermanent(ordinary.name)!!).error shouldBe null
            game.resolveStack()
            game.activate(ordinary.name); game.blocked(ordinary.name)
        }
        test("an activation ban wins over a static extra allowance") {
            val game = board()
            game.castSpell(1, ban.name).error shouldBe null; game.resolveStack()
            game.blocked()
        }
        test("a controller change never resets an object's activation count") {
            val game = board()
            game.activate()
            val id = game.findPermanent(selfWalker.name)!!
            game.state = game.state.updateEntity(id) { it.with(ControllerComponent(game.player2Id)) }
                .copy(activePlayerId = game.player2Id, priorityPlayerId = game.player2Id)
            val action = ActivateAbility(game.player2Id, id, selfWalker.script.activatedAbilities.first().id)
            game.execute(action).error shouldBe null; game.resolveStack()
            game.execute(action).error shouldNotBe null
        }
        test("the count resets next turn while the static permission persists") {
            val game = board()
            game.activate(); game.activate(); game.blocked()
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.getEntity(game.findPermanent(selfWalker.name)!!)!!
                .get<AbilityActivatedThisTurnComponent>() shouldBe null
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.activate(); game.activate(); game.blocked()
        }
        test("extra activations do not waive timing, an empty stack, or loyalty costs") {
            val game = board()
            game.blocked(index = 1)
            game.advanceToPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.blocked()
            game.advanceToPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.execute(game.action()).error shouldBe null
            game.blocked()
            game.resolveStack()
            game.activate(); game.blocked()
        }
    }
}
