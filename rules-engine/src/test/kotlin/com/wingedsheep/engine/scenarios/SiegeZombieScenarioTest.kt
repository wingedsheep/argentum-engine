package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.AngelsTomb
import com.wingedsheep.mtg.sets.definitions.emn.cards.GrappleWithThePast
import com.wingedsheep.mtg.sets.definitions.mid.cards.EccentricFarmer
import com.wingedsheep.mtg.sets.definitions.mid.cards.SiegeZombie
import com.wingedsheep.mtg.sets.definitions.zen.cards.BlazingTorch
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Siege Zombie. */
class SiegeZombieScenarioTest : FunSpec({

    val batch = listOf(GrappleWithThePast, EccentricFarmer, SiegeZombie, AngelsTomb, BlazingTorch)
    val projector = StateProjector()

    fun setup(deck: Deck = Deck.of("Forest" to 40)): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + batch)
        initMirrorMatch(deck = deck, startingLife = 20, skipMulligans = true)
    }

    // ── Siege Zombie ─────────────────────────────────────────────────────────

    test("Siege Zombie: taps three creatures (itself included) and drains each opponent for 1") {
        val d = setup(Deck.of("Swamp" to 40))
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val zombie = d.putCreatureOnBattlefield(you, "Siege Zombie")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(you, "Hill Giant")
        val abilityId = SiegeZombie.activatedAbilities.single().id

        // No `{T}` in the printed cost, so summoning sickness does not gate the tap and the
        // Zombie may tap itself as one of the three.
        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = zombie,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(zombie, bear, giant))
            )
        ).isSuccess shouldBe true

        listOf(zombie, bear, giant).forEach { d.isTapped(it) shouldBe true }
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 19
        d.getLifeTotal(you) shouldBe 20
    }

    test("Siege Zombie: cannot be activated with only two untapped creatures") {
        val d = setup(Deck.of("Swamp" to 40))
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val zombie = d.putCreatureOnBattlefield(you, "Siege Zombie")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val abilityId = SiegeZombie.activatedAbilities.single().id

        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = zombie,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(zombie, bear))
            )
        ).isSuccess shouldBe false
    }
})
