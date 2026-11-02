package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Transmogrifying Wand (M19 #247) — enters with three charge counters; {1}, {T}, remove a charge
 * counter: destroy target creature, and its controller creates a 2/4 white Ox. Sorcery speed.
 */
class TransmogrifyingWandScenarioTest : ScenarioTestBase() {
    init {
        test("enters with three charge counters, and the Ox goes to the destroyed creature's controller") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Transmogrifying Wand")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Transmogrifying Wand").error shouldBe null
            game.resolveStack()
            val wand = game.findPermanent("Transmogrifying Wand")!!
            fun charges() = game.state.getEntity(wand)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0
            charges() shouldBe 3

            val giant = game.findPermanent("Hill Giant")!!
            val abilityId = cardRegistry.getCard("Transmogrifying Wand")!!.script.activatedAbilities[0].id
            game.execute(
                ActivateAbility(game.player1Id, wand, abilityId, targets = listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            game.resolveStack()

            charges() shouldBe 2
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            val ox = game.findPermanent("Ox Token")!!
            game.state.getEntity(ox)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
            game.state.projectedState.getPower(ox) shouldBe 2
            game.state.projectedState.getToughness(ox) shouldBe 4
        }

        test("activates only as a sorcery: refused in combat, allowed in a main phase") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Transmogrifying Wand")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Transmogrifying Wand").error shouldBe null
            game.resolveStack()
            val wand = game.findPermanent("Transmogrifying Wand")!!
            val abilityId = cardRegistry.getCard("Transmogrifying Wand")!!.script.activatedAbilities[0].id
            fun activate() = game.execute(
                ActivateAbility(
                    game.player1Id, wand, abilityId,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)),
                )
            )

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            activate().error shouldNotBe null

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            activate().error shouldBe null
        }
    }
}
