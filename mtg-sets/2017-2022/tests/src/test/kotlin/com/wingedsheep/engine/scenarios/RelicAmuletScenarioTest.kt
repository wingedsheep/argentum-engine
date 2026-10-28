package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Relic Amulet — charge counters from instant/sorcery/Wizard casts, then
 * "{2}, {T}, Remove all charge counters from this artifact: It deals that much damage to target
 * creature." The damage is the number the cost removed (the counters are gone by resolution).
 */
class RelicAmuletScenarioTest : ScenarioTestBase() {

    private fun TestGame.charge(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    private fun TestGame.addCharge(id: EntityId, n: Int) {
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.CHARGE, n))
        }
    }

    private val fireAbility by lazy {
        cardRegistry.requireCard("Relic Amulet").script.activatedAbilities[0].id
    }

    init {
        test("casting an instant puts a charge counter on the Amulet") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Relic Amulet")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val amulet = game.findPermanent("Relic Amulet")!!

            game.execute(
                CastSpell(
                    game.player1Id,
                    game.findCardsInHand(1, "Shock").single(),
                    listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.charge(amulet) shouldBe 1
            game.getLifeTotal(2) shouldBe 18
        }

        test("activating removes every charge counter and deals that much damage to the creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Relic Amulet")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val amulet = game.findPermanent("Relic Amulet")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.addCharge(amulet, 2)

            game.execute(
                ActivateAbility(
                    game.player1Id, amulet, fireAbility,
                    targets = listOf(ChosenTarget.Permanent(giant)),
                )
            ).error shouldBe null
            game.charge(amulet) shouldBe 0
            // Counters arriving while the ability is on the stack don't change "that much".
            game.addCharge(amulet, 5)
            game.resolveStack()

            game.state.getEntity(giant)!!.get<DamageComponent>()?.amount shouldBe 2
            game.state.getEntity(amulet)!!.has<TappedComponent>() shouldBe true
            game.charge(amulet) shouldBe 5
        }
    }
}
