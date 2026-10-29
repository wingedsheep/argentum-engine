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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Feature test for `GrantLoyaltyActivationPermissionEffect.additionalActivations`
 * (`Effects.AdditionalLoyaltyActivations`) — The Chain Veil's "you may activate one of its loyalty
 * abilities once this turn as though none of its loyalty abilities have been activated this turn".
 *
 * Unlike "twice rather than only once" allowances (which take the largest), additional activations
 * add up: each grant is one more activation, on top of Oath of Teferi's maximum and of a
 * per-permanent "twice" grant. The grant is held by the player, so a planeswalker that enters after
 * it resolved is covered, and it lapses at end of turn. Validator and enumerator agree throughout.
 */
class AdditionalLoyaltyActivationsScenarioTest : ScenarioTestBase() {

    private val walker = card("Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = "0: You gain 1 life."
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val veil = card("Test Veil") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "For each planeswalker you control, you may activate one of its loyalty abilities once this turn as though none of its loyalty abilities have been activated this turn."
        spell { effect = Effects.AdditionalLoyaltyActivations() }
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
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "You may activate the loyalty abilities of planeswalkers you control twice each turn rather than only once."
        staticAbility { ability = LoyaltyActivationPermission(times = 2) }
    }

    private fun board(vararg extra: String, veils: Int = 1, walkerInHand: Boolean = false): TestGame {
        val b = scenario()
            .withPlayers("Player", "Opponent")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (walkerInHand) b.withCardInHand(1, "Test Walker").withLandsOnBattlefield(1, "Plains", 3)
        else b.withCardOnBattlefield(1, "Test Walker")
        repeat(veils) { b.withCardInHand(1, "Test Veil") }
        extra.forEach { b.withCardInHand(1, it) }
        return b.build()
    }

    private fun TestGame.activate() = run {
        val source = findPermanent("Test Walker")!!
        val ability = cardRegistry.getCard("Test Walker")!!.script.activatedAbilities
            .first { it.cost is AbilityCost.Loyalty }
        execute(ActivateAbility(player1Id, source, ability.id))
    }

    private fun TestGame.activateAndResolve() {
        withClue("loyalty activation should be legal") { activate().error shouldBe null }
        resolveStack()
    }

    private fun TestGame.offered(): Boolean {
        val source = findPermanent("Test Walker") ?: return false
        return getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == source }
    }

    private fun TestGame.cast(name: String, targetId: com.wingedsheep.sdk.model.EntityId? = null) {
        castSpell(1, name, targetId = targetId).error shouldBe null
        resolveStack()
    }

    /** Activate while offered; returns how many activations were made. */
    private fun TestGame.activateAll(): Int {
        var n = 0
        while (offered()) { activateAndResolve(); n++ }
        activate().error shouldNotBe null
        return n
    }

    init {
        cardRegistry.register(walker)
        cardRegistry.register(veil)
        cardRegistry.register(rush)
        cardRegistry.register(oath)

        test("one grant before any activation allows two activations in all") {
            val game = board()
            game.cast("Test Veil")
            game.activateAll() shouldBe 2
        }

        test("a grant after the walker's activation allows one more") {
            val game = board()
            game.activateAndResolve()
            game.offered() shouldBe false
            game.cast("Test Veil")
            game.activateAll() shouldBe 1
        }

        test("two grants add up to three activations") {
            val game = board(veils = 2)
            game.cast("Test Veil")
            game.cast("Test Veil")
            game.activateAll() shouldBe 3
        }

        test("adds on top of Oath of Teferi's twice") {
            val game = board("Test Oath")
            game.castSpell(1, "Test Oath").error shouldBe null
            game.resolveStack()
            game.cast("Test Veil")
            game.activateAll() shouldBe 3
        }

        test("adds on top of a per-permanent twice grant") {
            val game = board("Test Rush")
            game.cast("Test Rush", game.findPermanent("Test Walker")!!)
            game.cast("Test Veil")
            game.activateAll() shouldBe 3
        }

        test("covers a planeswalker that comes under your control after it resolved") {
            val game = board(walkerInHand = true)
            game.cast("Test Veil")
            game.cast("Test Walker")
            game.activateAll() shouldBe 2
        }

        test("lapses at end of turn") {
            val game = board()
            game.cast("Test Veil")
            game.state.getEntity(game.player1Id)!!.has<LoyaltyActivationPermissionsComponent>() shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.getEntity(game.player1Id)!!.has<LoyaltyActivationPermissionsComponent>() shouldBe false
        }
    }
}
