package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.AllowLoyaltyActivationsThisTurnEffect] —
 * "you may activate loyalty abilities of [this planeswalker] twice this turn rather than only once"
 * (Kaito, Dancing Shadow), driven through an inline instant that grants it to a target planeswalker.
 *
 * Proves, against CR 606.3 (one loyalty activation per permanent per turn) and Kaito's rulings: the
 * baseline is once; a grant allows exactly two (the same ability twice is fine); a grant made after
 * one activation allows exactly one more; two grants, or a grant plus Oath of Teferi, still allow
 * only two (not additive); the grant is per permanent (another planeswalker stays at one); and it
 * lapses at end of turn. The validator and the legal-action enumerator agree at every step.
 */
class AllowLoyaltyActivationsThisTurnScenarioTest : ScenarioTestBase() {

    private val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val otherWalker = card("Other Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Other"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val rush = card("Test Rush") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "You may activate loyalty abilities of target planeswalker twice this turn rather than only once."
        spell {
            val t = target(TargetFilter.Planeswalker)
            effect = Effects.AllowLoyaltyActivationsThisTurn(target = t)
        }
    }

    private val oath = card("Test Oath") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        oracleText = "You may activate the loyalty abilities of planeswalkers you control twice each turn rather than only once."
        staticAbility { ability = ExtraLoyaltyActivation }
    }

    private fun board(vararg extra: String, rushes: Int = 1): TestGame {
        val b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Walker")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(rushes) { b.withCardInHand(1, "Test Rush") }
        extra.forEach { b.withCardOnBattlefield(1, it) }
        return b.build()
    }

    private fun TestGame.activate(name: String = "Test Walker") = run {
        val source = findPermanent(name)!!
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities
            .first { it.cost is AbilityCost.Loyalty }
        execute(ActivateAbility(player1Id, source, ability.id))
    }

    /** Activate and resolve; asserts the activation was legal. */
    private fun TestGame.activateAndResolve(name: String = "Test Walker") {
        withClue("activation of $name should be legal") { activate(name).error shouldBe null }
        resolveStack()
    }

    private fun TestGame.canActivateOffered(name: String = "Test Walker"): Boolean {
        val source = findPermanent(name)!!
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == source }
    }

    private fun TestGame.grant(name: String = "Test Walker") {
        castSpell(1, "Test Rush", targetId = findPermanent(name)!!).error shouldBe null
        resolveStack()
    }

    init {
        cardRegistry.register(walker)
        cardRegistry.register(otherWalker)
        cardRegistry.register(rush)
        cardRegistry.register(oath)

        test("baseline: one loyalty activation per planeswalker per turn") {
            val game = board()
            game.activateAndResolve()
            game.canActivateOffered() shouldBe false
            game.activate().error shouldNotBe null
        }

        test("grant before any activation allows exactly two — the same ability twice") {
            val game = board()
            game.grant()
            game.activateAndResolve()
            game.canActivateOffered() shouldBe true
            game.activateAndResolve()
            game.canActivateOffered() shouldBe false
            game.activate().error shouldNotBe null
            game.getLifeTotal(1) shouldBe 22
        }

        test("grant after one activation allows exactly one more") {
            val game = board()
            game.activateAndResolve()
            game.canActivateOffered() shouldBe false
            game.grant()
            game.canActivateOffered() shouldBe true
            game.activateAndResolve()
            game.activate().error shouldNotBe null
        }

        test("two grants are not additive") {
            val game = board(rushes = 2)
            game.grant()
            game.grant()
            game.activateAndResolve()
            game.activateAndResolve()
            game.canActivateOffered() shouldBe false
            game.activate().error shouldNotBe null
        }

        test("a grant does not stack with Oath of Teferi") {
            val game = board("Test Oath")
            game.grant()
            game.activateAndResolve()
            game.activateAndResolve()
            game.canActivateOffered() shouldBe false
            game.activate().error shouldNotBe null
        }

        test("the grant is per permanent — another planeswalker stays at once") {
            val game = board("Other Walker")
            game.grant("Test Walker")
            game.activateAndResolve("Other Walker")
            game.canActivateOffered("Other Walker") shouldBe false
            game.activate("Other Walker").error shouldNotBe null
            game.activateAndResolve("Test Walker")
            game.activateAndResolve("Test Walker")
        }

        test("the allowance lapses at end of turn") {
            val game = board()
            game.grant()
            game.state.getEntity(game.findPermanent("Test Walker")!!)
                ?.get<AbilityActivatedThisTurnComponent>().shouldNotBeNull()
                .loyaltyActivationLimit shouldBe 2
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.getEntity(game.findPermanent("Test Walker")!!)
                ?.get<AbilityActivatedThisTurnComponent>() shouldBe null
        }
    }
}
