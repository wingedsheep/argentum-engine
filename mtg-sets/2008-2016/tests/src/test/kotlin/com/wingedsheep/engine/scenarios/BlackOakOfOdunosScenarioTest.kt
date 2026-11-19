package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Black Oak of Odunos (BNG #62) — "{B}, Tap another untapped creature you control: This creature
 * gets +1/+1 until end of turn."
 *
 * The tap cost must be paid with *another* creature — the Oak can't tap itself for it.
 */
class BlackOakOfOdunosScenarioTest : ScenarioTestBase() {

    private val pumpAbilityId by lazy {
        cardRegistry.requireCard("Black Oak of Odunos").activatedAbilities[0].id
    }

    private fun setup(): TestGame = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(1, "Black Oak of Odunos")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("tapping another creature and paying {B} gives it +1/+1") {
            val game = setup()
            val oak = game.findPermanent("Black Oak of Odunos")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = oak,
                    abilityId = pumpAbilityId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the Oak is 1/6 and the Bears paid for it") {
                projected.getPower(oak) shouldBe 1
                projected.getToughness(oak) shouldBe 6
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }
        }

        test("the Oak can't tap itself to pay") {
            val game = setup()
            val oak = game.findPermanent("Black Oak of Odunos")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = oak,
                    abilityId = pumpAbilityId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(oak)),
                )
            ).error shouldNotBe null
        }
    }
}
