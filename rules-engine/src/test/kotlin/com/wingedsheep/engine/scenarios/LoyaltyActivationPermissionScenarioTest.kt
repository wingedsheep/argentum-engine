package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.LoyaltyActivationPermissionsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.LoyaltyActivationPermission
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for the count axis of
 * [com.wingedsheep.sdk.scripting.effects.GrantLoyaltyActivationPermissionEffect] —
 * `Effects.ExtraLoyaltyActivations()`: "you may activate the loyalty abilities of planeswalkers you
 * control twice this turn rather than only once" (Urza Assembles the Titans III).
 *
 * CR 606.3 allows one loyalty activation per permanent per turn at sorcery speed. The grant is held
 * by the player, so it covers planeswalkers that enter after it resolves and outlives its source;
 * it never adds to another allowance (the 2022-09-09 ruling: "You won't be able to activate
 * abilities of planeswalkers you control more than twice in a turn") and ends with the turn.
 */
class LoyaltyActivationPermissionScenarioTest : ScenarioTestBase() {

    private val walker = card("Test Walker") {
        manaCost = "{0}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val lateWalker = card("Late Walker") {
        manaCost = "{0}"
        typeLine = "Legendary Planeswalker — Late"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val titans = card("Test Titans") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You may activate the loyalty abilities of planeswalkers you control twice this turn rather than only once."
        spell { effect = Effects.ExtraLoyaltyActivations() }
    }

    private val oath = card("Test Oath") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        oracleText = "You may activate the loyalty abilities of planeswalkers you control twice each turn rather than only once."
        staticAbility { ability = LoyaltyActivationPermission(times = 2) }
    }

    private fun board(vararg extra: String, titans: Int = 1, inHand: List<String> = emptyList()): TestGame {
        val b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Walker")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(titans) { b.withCardInHand(1, "Test Titans") }
        extra.forEach { b.withCardOnBattlefield(1, it) }
        inHand.forEach { b.withCardInHand(1, it) }
        return b.build()
    }

    private fun TestGame.activate(name: String = "Test Walker") = run {
        val source = findPermanent(name)!!
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities
            .first { it.cost is AbilityCost.Loyalty }
        execute(ActivateAbility(player1Id, source, ability.id))
    }

    private fun TestGame.activateAndResolve(name: String = "Test Walker") {
        withClue("activation of $name should be legal") { activate(name).error shouldBe null }
        resolveStack()
    }

    private fun TestGame.offered(name: String = "Test Walker"): Boolean {
        val source = findPermanent(name)!!
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == source }
    }

    private fun TestGame.grant() {
        castSpell(1, "Test Titans").error shouldBe null
        resolveStack()
    }

    private fun TestGame.exhausted(name: String = "Test Walker") {
        offered(name) shouldBe false
        activate(name).error shouldNotBe null
    }

    init {
        cardRegistry.register(walker)
        cardRegistry.register(lateWalker)
        cardRegistry.register(titans)
        cardRegistry.register(oath)

        test("baseline: one loyalty activation per planeswalker per turn") {
            val game = board()
            game.activateAndResolve()
            game.exhausted()
        }

        test("the grant allows exactly two activations, the same ability twice") {
            val game = board()
            game.grant()
            game.activateAndResolve()
            game.offered() shouldBe true
            game.activateAndResolve()
            game.exhausted()
            game.getLifeTotal(1) shouldBe 22
        }

        test("a grant after one activation allows exactly one more") {
            val game = board()
            game.activateAndResolve()
            game.offered() shouldBe false
            game.grant()
            game.offered() shouldBe true
            game.activateAndResolve()
            game.exhausted()
        }

        test("a planeswalker that enters after the grant is covered") {
            val game = board(inHand = listOf("Late Walker"))
            game.grant()
            game.castSpell(1, "Late Walker").error shouldBe null
            game.resolveStack()
            game.activateAndResolve("Late Walker")
            game.activateAndResolve("Late Walker")
            game.exhausted("Late Walker")
        }

        test("two grants and an Oath of Teferi never add up past two") {
            val game = board("Test Oath", titans = 2)
            game.grant()
            game.grant()
            game.activateAndResolve()
            game.activateAndResolve()
            game.exhausted()
        }

        test("the grant ends with the turn") {
            val game = board()
            game.grant()
            game.state.getEntity(game.player1Id)!!.has<LoyaltyActivationPermissionsComponent>() shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.getEntity(game.player1Id)!!.has<LoyaltyActivationPermissionsComponent>() shouldBe false
        }

        test("the grant lifts only the count — sorcery timing still applies") {
            val game = board(titans = 2)
            game.grant()
            game.castSpell(1, "Test Titans").error shouldBe null
            withClue("a non-empty stack is not sorcery timing") {
                game.offered() shouldBe false
                game.activate().error shouldNotBe null
            }
        }
    }
}
