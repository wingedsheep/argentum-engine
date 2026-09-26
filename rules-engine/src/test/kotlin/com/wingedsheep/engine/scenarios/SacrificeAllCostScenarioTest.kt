package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import io.kotest.matchers.shouldBe

/**
 * `CostAtom.SacrificeAll` paid as an *activated-ability* cost. Soulblast covers the spell path; this
 * pins that the ability path snapshots the sacrificed permanents too — nothing is chosen, so the
 * action arrives with no sacrifice selection, and without the snapshot "the sacrificed creatures'
 * total power" would read 0.
 */
class SacrificeAllCostScenarioTest : ScenarioTestBase() {

    private val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    private val giant = card("Test Giant") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Giant"
        power = 3
        toughness = 3
    }

    private val pyre = card("Test Pyre") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, AbilityCost.Atom(CostAtom.SacrificeAll(GameObjectFilter.Creature)))
            effect = Effects.GainLife(DynamicAmounts.totalPowerSacrificedThisWay())
            description = "You gain life equal to the total power of the sacrificed creatures."
        }
    }

    init {
        cardRegistry.register(bear)
        cardRegistry.register(giant)
        cardRegistry.register(pyre)

        test("an activated ability reads the total power of every creature it sacrificed") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Test Pyre")
                .withCardOnBattlefield(1, "Test Bear")
                .withCardOnBattlefield(1, "Test Giant")
                .withCardOnBattlefield(2, "Test Bear")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val abilityId = pyre.activatedAbilities.first().id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Test Pyre")!!,
                    abilityId = abilityId,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )
            ).error shouldBe null

            game.isInGraveyard(1, "Test Bear") shouldBe true
            game.isInGraveyard(1, "Test Giant") shouldBe true
            (game.findPermanent("Test Bear") != null) shouldBe true // the opponent's

            game.resolveStack()
            game.getLifeTotal(1) shouldBe 25
        }
    }
}
